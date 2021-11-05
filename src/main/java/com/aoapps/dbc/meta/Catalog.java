/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2011, 2013, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-dbc.
 *
 * ao-dbc is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-dbc is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-dbc.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.aoapps.dbc.meta;

import com.aoapps.collections.AoCollections;
import com.aoapps.dbc.NoRowException;
import com.aoapps.hodgepodge.graph.Edge;
import com.aoapps.hodgepodge.graph.SymmetricGraph;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A friendly wrapper around database meta data.
 *
 * @author  AO Industries, Inc.
 */
public class Catalog {

	private final DatabaseMetaData metaData;
	private final String name;

	protected Catalog(DatabaseMetaData metaData, String name) {
		this.metaData = metaData;
		this.name = name;
	}

	@Override
	public String toString() {
		return name;
	}

	public DatabaseMetaData getMetaData() {
		return metaData;
	}

	public String getName() {
		return name;
	}

	private static class GetSchemasLock {/* Empty lock class to help heap profile */}
	private final GetSchemasLock getSchemasLock = new GetSchemasLock();
	private SortedMap<String, Schema> getSchemasCache;

	/**
	 * Gets all schemas for this catalog keyed by unique name.
	 *
	 * @see  java.sql.DatabaseMetaData#getSchemas()
	 */
	@SuppressWarnings("ReturnOfCollectionOrArrayField") // Returning unmodifiable
	public SortedMap<String, Schema> getSchemas() throws SQLException {
		synchronized(getSchemasLock) {
			if(getSchemasCache==null) {
				SortedMap<String, Schema> newSchemas = new TreeMap<>(DatabaseMetaData.getCollator());
				try (ResultSet results = metaData.getMetaData().getSchemas()) {
					ResultSetMetaData resultsMeta = results.getMetaData();
					while(results.next()) {
						int colCount = resultsMeta.getColumnCount();
						//System.err.println("DEBUG: Catalog: getSchemas(): colCount=" + colCount);
						//for(int i=1; i<=colCount; i++) {
						//	resultsMeta.getColumnName(i);
						//	System.err.println("DEBUG: Catalog: getSchemas(): resultsMeta.getColumnName("+i+")=" + resultsMeta.getColumnName(i));
						//}
						//System.err.println("DEBUG: Catalog: getSchemas(): results.getString(\"table_catalog\")=" + results.getString("table_catalog"));
						//System.err.println("DEBUG: Catalog: getSchemas(): results.getString(\"TABLE_CATALOG\")=" + results.getString("TABLE_CATALOG"));
						if(
							colCount==1 // PostgreSQL 8.3 only returns one column
							|| results.getString("TABLE_CATALOG") == null // PostgreSQL 9.4 driver returns null
							|| name.equals(results.getString("TABLE_CATALOG")) // Other driver
						) {
							Schema newSchema = new Schema(this, results.getString("TABLE_SCHEM"));
							if(newSchemas.put(newSchema.getName(), newSchema)!=null) throw new AssertionError("Duplicate schema: "+newSchema);
						}
					}
				}
				getSchemasCache = AoCollections.optimalUnmodifiableSortedMap(newSchemas);
			}
			return getSchemasCache;
		}
	}

	/**
	 * Gets the schema of the provided name.
	 *
	 * @throws  NoRowException if the schema doesn't exist
	 */
	public Schema getSchema(String name) throws NoRowException, SQLException {
		Schema schema = getSchemas().get(name);
		if(schema==null) throw new NoRowException("name=" + name);
		return schema;
	}

	/**
	 * Gets a graph view of the imported/exported table relationships within this catalog.
	 */
	public SymmetricGraph<Table, Edge<Table>, SQLException> getForeignKeyGraph() {
		return getForeignKeyGraph(null);
	}

	/**
	 * Gets a graph view of the imported/exported table relationships within this catalog.
	 *
	 * TODO: Check is symmetric in JUnit test
	 *
	 * @param tableTypes  the set of tables types or {@code null} for all types
	 */
	public SymmetricGraph<Table, Edge<Table>, SQLException> getForeignKeyGraph(final Set<String> tableTypes) {
		return new SymmetricGraph<>() {

			@Override
			public Set<Table> getVertices() throws SQLException {
				Set<Table> vertices = new LinkedHashSet<>();
				for(Schema schema : getSchemas().values()) {
					for(Table table : schema.getTables().values()) {
						if(tableTypes==null || tableTypes.contains(table.getTableType())) vertices.add(table);
					}
				}
				return AoCollections.optimalUnmodifiableSet(vertices);
			}

			@Override
			public Set<Edge<Table>> getEdgesFrom(Table from) throws SQLException {
				Set<? extends Table> tos = from.getImportedTables();
				Set<Edge<Table>> edges = AoCollections.newLinkedHashSet(tos.size());
				for(Table to : tos) {
					if(
						tableTypes==null
						|| tableTypes.contains(from.getTableType())
						|| tableTypes.contains(to.getTableType())
					) edges.add(new Edge<>(from, to));
				}
				return AoCollections.optimalUnmodifiableSet(edges);
			}

			@Override
			public Set<Edge<Table>> getEdgesTo(Table to) throws SQLException {
				Set<? extends Table> froms = to.getExportedTables();
				Set<Edge<Table>> edges = AoCollections.newLinkedHashSet(froms.size());
				for(Table from : froms) {
					if(
						tableTypes==null
						|| tableTypes.contains(from.getTableType())
						|| tableTypes.contains(to.getTableType())
					) edges.add(new Edge<>(from, to));
				}
				return AoCollections.optimalUnmodifiableSet(edges);
			}
		};
	}
}
