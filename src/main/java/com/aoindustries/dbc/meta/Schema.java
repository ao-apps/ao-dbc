/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2011, 2013, 2015, 2016, 2019, 2020  AO Industries, Inc.
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
 * along with ao-dbc.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.dbc.meta;

import com.aoindustries.collections.AoCollections;
import com.aoindustries.dbc.NoRowException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A friendly wrapper around database meta data.
 *
 * @author  AO Industries, Inc.
 */
public class Schema {

	private final Catalog catalog;
	private final String name;
	private final int hashCode;

	protected Schema(Catalog catalog, String name) {
		this.catalog = catalog;
		if(name.indexOf('"')!=-1) throw new IllegalArgumentException();
		this.name = name;
		this.hashCode = name.hashCode();
	}

	@Override
	public String toString() {
		return name;
	}

	/**
	 * Two schemas are equal if they have the same name.
	 */
	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Schema)) return false;
		Schema other = (Schema)obj;
		return
			hashCode==other.hashCode
			&& name.equals(other.name)
		;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	public Catalog getCatalog() {
		return catalog;
	}

	public String getName() {
		return name;
	}

	private static class GetTablesLock {}
	private final GetTablesLock getTablesLock = new GetTablesLock();
	private SortedMap<String,Table> getTablesCache;

	/**
	 * Gets all tables for this schema keyed by unique name.
	 *
	 * @see  java.sql.DatabaseMetaData#getTables(java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	public SortedMap<String,Table> getTables() throws SQLException {
		synchronized(getTablesLock) {
			if(getTablesCache==null) {
				SortedMap<String,Table> newTables = new TreeMap<>(DatabaseMetaData.getCollator());
				try (ResultSet results = catalog.getMetaData().getMetaData().getTables(catalog.getName(), name, null, null)) {
					while(results.next()) {
						Table newTable = new Table(this, results.getString("TABLE_NAME"), results.getString("TABLE_TYPE"));
						if(newTables.put(newTable.getName(), newTable)!=null) throw new AssertionError("Duplicate table: "+newTable);
					}
				}
				getTablesCache = AoCollections.optimalUnmodifiableSortedMap(newTables);
			}
			return getTablesCache;
		}
	}

	/**
	 * Gets the table of the provided name.
	 *
	 * @throws  NoRowException if the table doesn't exist
	 */
	public Table getTable(String name) throws NoRowException, SQLException {
		Table table = getTables().get(name);
		if(table==null) throw new NoRowException();
		return table;
	}
}
