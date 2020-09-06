/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2020  AO Industries, Inc.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A set of object factories for various types.
 *
 * @author  AO Industries, Inc.
 */
final public class ObjectFactories {

	/**
	 * Make no instances.
	 */
	private ObjectFactories() {
	}

	public static final ObjectFactory<java.math.BigDecimal> BigDecimal = result -> result.getBigDecimal(1);

	public static final ObjectFactory<java.lang.Boolean> Boolean = result -> {
		boolean b = result.getBoolean(1);
		return result.wasNull() ? null : b;
	};

	public static final ObjectFactory<byte[]> ByteArray = result -> result.getBytes(1);

	public static final ObjectFactory<java.sql.Date> Date = result -> result.getDate(1);

	public static final ObjectFactory<java.lang.Integer> Integer = result -> {
		int i = result.getInt(1);
		return result.wasNull() ? null : i;
	};

	public static final ObjectFactory<java.lang.Long> Long = result -> {
		long l = result.getLong(1);
		return result.wasNull() ? null : l;
	};

	public static class Object<T> implements ObjectFactory<T> {

		private final Class<T> clazz;
		private final Constructor<T> constructor;

		public Object(Class<T> clazz) throws SQLException {
			this.clazz = clazz;
			try {
				this.constructor = clazz.getConstructor(ResultSet.class);
			} catch(NoSuchMethodException err) {
				throw new SQLException("Unable to find constructor: "+clazz.getName()+"(java.sql.ResultSet)", err);
			}
		}

		@Override
		@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
		public T createObject(ResultSet result) throws SQLException {
			try {
				return constructor.newInstance(result);
			} catch(InvocationTargetException e) {
				Throwable cause = e.getCause();
				if(cause instanceof Error) throw (Error)cause;
				if(cause instanceof RuntimeException) throw (RuntimeException)cause;
				if(cause instanceof SQLException) throw (SQLException)cause;
				throw new SQLException(clazz.getName() + "(java.sql.ResultSet)", cause == null ? e : cause);
			} catch(Error | RuntimeException e) {
				throw e;
			} catch(Throwable t) {
				throw new SQLException(clazz.getName() + "(java.sql.ResultSet)", t);
			}
		}
	};

	public static final ObjectFactory<java.lang.Short> Short = result -> {
		short s = result.getShort(1);
		return result.wasNull() ? null : s;
	};

	public static final ObjectFactory<java.lang.String> String = result -> result.getString(1);

	public static final ObjectFactory<java.sql.Timestamp> Timestamp = result -> result.getTimestamp(1);
}
