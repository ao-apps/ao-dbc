/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2008, 2009, 2010, 2011, 2014, 2015  AO Industries, Inc.
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
package com.aoindustries.dbc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Processes the resultSet from a query.  This is called only once, it is no longer
 * row-by-row.  Iteration over the results is up to the implementor.
 *
 * @author  AO Industries, Inc.
 */
public interface ResultSetHandler<T> extends ResultSetHandlerE<T,RuntimeException> {

	/**
	 * Process one set of results.
	 */
	@Override
	T handleResultSet(ResultSet results) throws SQLException;
}
