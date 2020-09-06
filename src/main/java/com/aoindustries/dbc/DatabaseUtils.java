/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2015, 2016, 2018, 2020  AO Industries, Inc.
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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Package-private implementation utilities.
 *
 * @author  AO Industries, Inc.
 */
final class DatabaseUtils {

	/**
	 * Gets a user-friendly description of the provided result in a string formatted like
	 * <code>('value', 'value', int_value, null, …)</code>.  This must not be used generate
	 * SQL statements - it is just to provide user display.
	 */
	// TODO: Capitalize "NULL"?
	// TODO: Auto-ellipsis on long values?
	static String getRow(ResultSet result) throws SQLException {
		StringBuilder sb = new StringBuilder();
		sb.append('(');
		ResultSetMetaData metaData = result.getMetaData();
		int colCount = metaData.getColumnCount();
		for(int c=1; c<=colCount; c++) {
			if(c>1) sb.append(", ");
			int colType = metaData.getColumnType(c);
			switch(colType) {
				case Types.BIGINT :
				case Types.BIT :
				case Types.BOOLEAN :
				case Types.DECIMAL :
				case Types.DOUBLE :
				case Types.FLOAT :
				case Types.INTEGER :
				case Types.NUMERIC :
				case Types.REAL :
				case Types.SMALLINT :
				case Types.TINYINT :
					sb.append(result.getObject(c));
					break;
				case Types.CHAR :
				case Types.DATE :
				case Types.LONGNVARCHAR :
				case Types.LONGVARCHAR :
				case Types.NCHAR :
				case Types.NVARCHAR :
				case Types.TIME :
				case Types.TIMESTAMP :
				case Types.VARCHAR :
				default :
					String S = result.getString(c);
					sb.append('\'');
					int i;
					for (i = 0; i < S.length(); i++) {
						char ch = S.charAt(i);
						if(ch == '\'') sb.append("''");
						else if (ch == '\\' || ch == '"' || ch == '%' || ch == '_') {
							sb.append('\\');
						}
						sb.append(ch);
					}
					sb.append('\'');
					break;
				//default :
				//    throw new SQLException("Unexpected column type: "+colType);
			}
		}
		sb.append(')');
		return sb.toString();
	}

	private DatabaseUtils() {}
}
