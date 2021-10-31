/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2011, 2013, 2015, 2016, 2020, 2021  AO Industries, Inc.
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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.Collator;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * A friendly wrapper around database meta data.
 *
 * @author  AO Industries, Inc.
 */
public class DatabaseMetaData {

	private static final Collator englishCollator = Collator.getInstance(Locale.ENGLISH);

	/**
	 * Gets the collator used for result ordering.
	 */
	public static Collator getCollator() {
		return englishCollator;
	}

	private final java.sql.DatabaseMetaData metaData;

	/**
	 * Creates a new meta data wrapper.  Results are cached, so for a fresh view of
	 * meta data, create a new instance.
	 */
	public DatabaseMetaData(Connection conn) throws SQLException {
		this(conn.getMetaData());
	}

	/**
	 * Creates a new meta data wrapper.  Results are cached, so for a fresh view of
	 * meta data, create a new instance.
	 */
	public DatabaseMetaData(java.sql.DatabaseMetaData metaData) {
		this.metaData = metaData;
	}

	@Override
	public String toString() {
		try {
			String url = metaData.getURL();
			if(url!=null) return url;
		} catch(SQLException exc) {
			// Ignored
		}
		return super.toString();
	}

	public java.sql.DatabaseMetaData getMetaData() {
		return metaData;
	}

	private static class GetCatalogsLock {}
	private final GetCatalogsLock getCatalogsLock = new GetCatalogsLock();
	private SortedMap<String, Catalog> getCatalogsCache;

	/**
	 * Gets all catalogs for this database keyed by unique name.
	 *
	 * @see  java.sql.DatabaseMetaData#getCatalogs()
	 */
	@SuppressWarnings("ReturnOfCollectionOrArrayField") // Returning unmodifiable
	public SortedMap<String, Catalog> getCatalogs() throws SQLException {
		synchronized(getCatalogsLock) {
			if(getCatalogsCache==null) {
				SortedMap<String, Catalog> newCatalogs = new TreeMap<>(englishCollator);
				try (ResultSet results = metaData.getCatalogs()) {
					while(results.next()) {
						Catalog newCatalog = new Catalog(this, results.getString(1));
						if(newCatalogs.put(newCatalog.getName(), newCatalog)!=null) throw new AssertionError("Duplicate catalog: "+newCatalog);
					}
				}
				getCatalogsCache = AoCollections.optimalUnmodifiableSortedMap(newCatalogs);
			}
			return getCatalogsCache;
		}
	}

	/**
	 * Gets the catalog of the provided name.
	 *
	 * @throws  NoRowException if the catalog doesn't exist
	 */
	public Catalog getCatalog(String name) throws NoRowException, SQLException {
		Catalog catalog = getCatalogs().get(name);
		if(catalog==null) throw new NoRowException();
		return catalog;
	}
}
