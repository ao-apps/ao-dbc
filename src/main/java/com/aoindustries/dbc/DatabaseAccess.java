/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2015, 2018, 2020  AO Industries, Inc.
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

import com.aoindustries.collections.AoCollections;
import com.aoindustries.collections.IntArrayList;
import com.aoindustries.collections.IntList;
import com.aoindustries.collections.LongArrayList;
import com.aoindustries.collections.LongList;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Wraps and simplifies access to a JDBC database.
 *
 * @author  AO Industries, Inc.
 */
// TODO: Clean-up javadoc consistency (especially use of <code> tags)
public interface DatabaseAccess {

	/**
	 * These may be used as parameters to represent null values of specific types.
	 *
	 * @see  Types
	 * @see  PreparedStatement#setNull(int, int)
	 */
	public enum Null {
		BIT(Types.BIT),
		TINYINT(Types.TINYINT),
		SMALLINT(Types.SMALLINT),
		INTEGER(Types.INTEGER),
		BIGINT(Types.BIGINT),
		FLOAT(Types.FLOAT),
		REAL(Types.REAL),
		DOUBLE(Types.DOUBLE),
		NUMERIC(Types.NUMERIC),
		DECIMAL(Types.DECIMAL),
		CHAR(Types.CHAR),
		VARCHAR(Types.VARCHAR),
		LONGVARCHAR(Types.LONGVARCHAR),
		DATE(Types.DATE),
		TIME(Types.TIME),
		TIMESTAMP(Types.TIMESTAMP),
		BINARY(Types.BINARY),
		VARBINARY(Types.VARBINARY),
		LONGVARBINARY(Types.LONGVARBINARY),
		NULL(Types.NULL),
		OTHER(Types.OTHER),
		JAVA_OBJECT(Types.JAVA_OBJECT),
		DISTINCT(Types.DISTINCT),
		STRUCT(Types.STRUCT),
		ARRAY(Types.ARRAY),
		BLOB(Types.BLOB),
		CLOB(Types.CLOB),
		REF(Types.REF),
		DATALINK(Types.DATALINK),
		BOOLEAN(Types.BOOLEAN),
		ROWID(Types.ROWID),
		NCHAR(Types.NCHAR),
		NVARCHAR(Types.NVARCHAR),
		LONGNVARCHAR(Types.LONGNVARCHAR),
		NCLOB(Types.NCLOB),
		SQLXML(Types.SQLXML);

		private final int type;

		private Null(int type) {
			this.type = type;
		}

		/**
		 * @see  Types
		 * @see  PreparedStatement#setNull(int, int)
		 */
		public int getType() {
			return type;
		}
	}

	/**
	 * Read-only query the database with a <code>BigDecimal</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeBigDecimalUpdate(java.lang.String, java.lang.Object...)
	 */
	default BigDecimal executeBigDecimalQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeBigDecimalQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>BigDecimal</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeBigDecimalQuery(java.lang.String, java.lang.Object...)
	 */
	default BigDecimal executeBigDecimalUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeBigDecimalQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with a <code>BigDecimal</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required, or when row with null value.
	 */
	default BigDecimal executeBigDecimalQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.BigDecimal, sql, params);
	}

	/**
	 * Read-only query the database with a <code>boolean</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeBooleanUpdate(java.lang.String, java.lang.Object...)
	 */
	default boolean executeBooleanQuery(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeBooleanQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>boolean</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeBooleanQuery(java.lang.String, java.lang.Object...)
	 */
	default boolean executeBooleanUpdate(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeBooleanQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with a <code>boolean</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required.
	 *
	 * @throws  NullDataException  When has a row, but with NULL value.
	 */
	default Boolean executeBooleanQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeObjectQuery(
			isolationLevel,
			readOnly,
			rowRequired,
			ObjectFactories.notNull(ObjectFactories.Boolean),
			sql,
			params
		);
	}

	/**
	 * Read-only query the database with a <code>byte[]</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeByteArrayUpdate(java.lang.String, java.lang.Object...)
	 */
	default byte[] executeByteArrayQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeByteArrayQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>byte[]</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeByteArrayQuery(java.lang.String, java.lang.Object...)
	 */
	default byte[] executeByteArrayUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeByteArrayQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with a <code>byte[]</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required, or when row with null value.
	 */
	default byte[] executeByteArrayQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.ByteArray, sql, params);
	}

	/**
	 * Read-only query the database with a <code>java.sql.Date</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeDateUpdate(java.lang.String, java.lang.Object...)
	 */
	default Date executeDateQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeDateQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>java.sql.Date</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeDateQuery(java.lang.String, java.lang.Object...)
	 */
	default Date executeDateUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeDateQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with a <code>java.sql.Date</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required, or when row with null value.
	 */
	default Date executeDateQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.Date, sql, params);
	}

	/**
	 * Read-only query the database with a <code>double</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #updateDouble(java.lang.String, java.lang.Object[])
	 */
	default double executeDoubleQuery(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return queryDouble(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>double</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeDoubleQuery(java.lang.String, java.lang.Object...)
	 */
	default double updateDouble(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return queryDouble(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with a <code>double</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required.
	 *
	 * @throws  NullDataException  When has a row, but with NULL value.
	 */
	default Double queryDouble(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeObjectQuery(
			isolationLevel,
			readOnly,
			rowRequired,
			ObjectFactories.notNull(ObjectFactories.Double),
			sql,
			params
		);
	}

	/**
	 * Read-only query the database with a {@link DoubleStream} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #doubleStreamUpdate(java.lang.String, java.lang.Object[])
	 */
	default DoubleStream doubleStream(String sql, Object ... params) throws NullDataException, SQLException {
		return doubleStream(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	/**
	 * Read-write query the database with a {@link DoubleStream} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #doubleStream(java.lang.String, java.lang.Object[])
	 */
	default DoubleStream doubleStreamUpdate(String sql, Object ... params) throws NullDataException, SQLException {
		return doubleStream(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	/**
	 * Query the database with a {@link DoubleStream} return type.
	 */
	DoubleStream doubleStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException;

	// TODO: Float

	/**
	 * Read-only query the database with an <code>int</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeIntUpdate(java.lang.String, java.lang.Object...)
	 */
	default int executeIntQuery(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeIntQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with an <code>int</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeIntQuery(java.lang.String, java.lang.Object...)
	 */
	default int executeIntUpdate(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeIntQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with an <code>int</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required.
	 *
	 * @throws  NullDataException  When has a row, but with NULL value.
	 */
	default Integer executeIntQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeObjectQuery(
			isolationLevel,
			readOnly,
			rowRequired,
			ObjectFactories.notNull(ObjectFactories.Integer),
			sql,
			params
		);
	}

	/**
	 * Read-only query the database with an <code>IntList</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeIntListUpdate(java.lang.String, java.lang.Object...)
	 */
	default IntList executeIntListQuery(String sql, Object ... params) throws NullDataException, SQLException {
		return executeIntListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	/**
	 * Read-write query the database with an <code>IntList</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeIntListQuery(java.lang.String, java.lang.Object...)
	 */
	default IntList executeIntListUpdate(String sql, Object ... params) throws NullDataException, SQLException {
		return executeIntListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	/**
	 * Query the database with an <code>IntList</code> return type.
	 */
	default IntList executeIntListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
		return executeQuery(
			isolationLevel,
			readOnly,
			results -> {
				IntList list = new IntArrayList();
				while(results.next()) {
					int i = results.getInt(1);
					if(results.wasNull()) throw new NullDataException();
					list.add(i);
				}
				return list;
			},
			sql,
			params
		);
	}

	/**
	 * Read-only query the database with an {@link IntStream} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #intStreamUpdate(java.lang.String, java.lang.Object[])
	 */
	default IntStream intStream(String sql, Object ... params) throws NullDataException, SQLException {
		return intStream(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	/**
	 * Read-write query the database with an {@link IntStream} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #intStream(java.lang.String, java.lang.Object[])
	 */
	default IntStream intStreamUpdate(String sql, Object ... params) throws NullDataException, SQLException {
		return intStream(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	/**
	 * Query the database with an {@link IntStream} return type.
	 */
	IntStream intStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException;

	/**
	 * Read-only query the database with a <code>long</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeLongUpdate(java.lang.String, java.lang.Object...)
	 */
	default long executeLongQuery(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeLongQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>long</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeLongQuery(java.lang.String, java.lang.Object...)
	 */
	default long executeLongUpdate(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeLongQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with a <code>long</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required.
	 *
	 * @throws  NullDataException  When has a row, but with NULL value.
	 */
	default Long executeLongQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeObjectQuery(
			isolationLevel,
			readOnly,
			rowRequired,
			ObjectFactories.notNull(ObjectFactories.Long),
			sql,
			params
		);
	}

	/**
	 * Read-only query the database with a <code>LongList</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeLongListUpdate(java.lang.String, java.lang.Object...)
	 */
	default LongList executeLongListQuery(String sql, Object ... params) throws NullDataException, SQLException {
		return executeLongListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>LongList</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeLongListQuery(java.lang.String, java.lang.Object...)
	 */
	default LongList executeLongListUpdate(String sql, Object ... params) throws NullDataException, SQLException {
		return executeLongListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	/**
	 * Query the database with a <code>LongList</code> return type.
	 */
	default LongList executeLongListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
		return executeQuery(
			isolationLevel,
			readOnly,
			results -> {
				LongList list = new LongArrayList();
				while(results.next()) {
					long l = results.getLong(1);
					if(results.wasNull()) throw new NullDataException();
					list.add(l);
				}
				return list;
			},
			sql,
			params
		);
	}

	/**
	 * Read-only query the database with a {@link LongStream} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #longStreamUpdate(java.lang.String, java.lang.Object[])
	 */
	default LongStream longStream(String sql, Object ... params) throws NullDataException, SQLException {
		return longStream(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	/**
	 * Read-write query the database with a {@link LongStream} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #longStream(java.lang.String, java.lang.Object[])
	 */
	default LongStream longStreamUpdate(String sql, Object ... params) throws NullDataException, SQLException {
		return longStream(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	/**
	 * Query the database with a {@link LongStream} return type.
	 */
	LongStream longStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException;

	/**
	 * Read-only query the database with a <code>&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeObjectUpdate(java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	default <T> T executeObjectQuery(Class<T> clazz, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, clazz, sql, params);
	}

	/**
	 * Read-write query the database with a <code>&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeObjectQuery(java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	default <T> T executeObjectUpdate(Class<T> clazz, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, clazz, sql, params);
	}

	/**
	 * Query the database with a <code>&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 *
	 * @return  The value or {@code null} when no row and row not required, or when row with null value.
	 */
	default <T> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<T> clazz, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, new ObjectFactories.Object<>(clazz), sql, params);
	}

	/**
	 * Read-only query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeObjectUpdate(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	default <T> T executeObjectQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, objectFactory, sql, params);
	}

	/**
	 * Read-write query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeObjectQuery(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	default <T> T executeObjectUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, objectFactory, sql, params);
	}

	/**
	 * Query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 *
	 * @return  The value or {@code null} when no row and row not required, or when row with null value.
	 */
	default <T> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, RuntimeException.class, objectFactory, sql, params);
	}

	/**
	 * Read-only query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeObjectUpdate(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	default <T,E extends Exception> T executeObjectQuery(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException, E {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, eClass, objectFactory, sql, params);
	}

	/**
	 * Read-write query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeObjectQuery(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	default <T,E extends Exception> T executeObjectUpdate(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException, E {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, eClass, objectFactory, sql, params);
	}

	/**
	 * Query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 *
	 * @return  The value or {@code null} when no row and row not required, or when row with null value.
	 */
	default <T,E extends Exception> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException, E {
		return executeQuery(
			isolationLevel,
			readOnly,
			eClass,
			results -> {
				if(results.next()) {
					T object = objectFactory.createObject(results);
					if(results.next()) throw new ExtraRowException();
					return object;
				}
				if(rowRequired) throw new NoRowException();
				return null;
			},
			sql,
			params
		);
	}

	/**
	 * Read-only query the database with a <code>List&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListUpdate(java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	default <T> List<T> executeObjectListQuery(Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, true, clazz, sql, params);
	}

	/**
	 * Read-write query the database with a <code>List&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListQuery(java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	default <T> List<T> executeObjectListUpdate(Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, false, clazz, sql, params);
	}

	/**
	 * Query the database with a <code>List&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 */
	default <T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(isolationLevel, readOnly, new ObjectFactories.Object<>(clazz), sql, params);
	}

	/**
	 * Read-only query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListUpdate(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	default <T> List<T> executeObjectListQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, true, objectFactory, sql, params);
	}

	/**
	 * Read-write query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListQuery(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	default <T> List<T> executeObjectListUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, false, objectFactory, sql, params);
	}

	/**
	 * Query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	default <T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(isolationLevel, readOnly, RuntimeException.class, objectFactory, sql, params);
	}

	/**
	 * Read-only query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListUpdate(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	default <T,E extends Exception> List<T> executeObjectListQuery(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, true, eClass, objectFactory, sql, params);
	}

	/**
	 * Read-write query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListQuery(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	default <T,E extends Exception> List<T> executeObjectListUpdate(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, false, eClass, objectFactory, sql, params);
	}

	/**
	 * Query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	default <T,E extends Exception> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), eClass, objectFactory, sql, params)
		);
	}

	/**
	 * Read-only query the database with a <code>Stream&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #streamUpdate(java.lang.Class, java.lang.String, java.lang.Object[])
	 */
	default <T> Stream<T> stream(Class<T> clazz, String sql, Object ... params) throws SQLException {
		return stream(Connection.TRANSACTION_READ_COMMITTED, true, clazz, sql, params);
	}

	/**
	 * Read-write query the database with a <code>Stream&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #stream(java.lang.Class, java.lang.String, java.lang.Object[])
	 */
	default <T> Stream<T> streamUpdate(Class<T> clazz, String sql, Object ... params) throws SQLException {
		return stream(Connection.TRANSACTION_READ_COMMITTED, false, clazz, sql, params);
	}

	/**
	 * Query the database with a <code>Stream&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 */
	default <T> Stream<T> stream(int isolationLevel, boolean readOnly, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return stream(isolationLevel, readOnly, new ObjectFactories.Object<>(clazz), sql, params);
	}

	/**
	 * Read-only query the database with a <code>Stream&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #streamUpdate(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object[])
	 */
	default <T> Stream<T> stream(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return stream(Connection.TRANSACTION_READ_COMMITTED, true, objectFactory, sql, params);
	}

	/**
	 * Read-write query the database with a <code>Stream&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #stream(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object[])
	 */
	default <T> Stream<T> streamUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return stream(Connection.TRANSACTION_READ_COMMITTED, false, objectFactory, sql, params);
	}

	/**
	 * Query the database with a <code>Stream&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	default <T> Stream<T> stream(int isolationLevel, boolean readOnly, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return stream(isolationLevel, readOnly, RuntimeException.class, objectFactory, sql, params);
	}

	/**
	 * Read-only query the database with a <code>Stream&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #streamUpdate(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object[])
	 */
	default <T,E extends Exception> Stream<T> stream(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return stream(Connection.TRANSACTION_READ_COMMITTED, true, eClass, objectFactory, sql, params);
	}

	/**
	 * Read-write query the database with a <code>Stream&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #stream(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object[])
	 */
	default <T,E extends Exception> Stream<T> streamUpdate(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return stream(Connection.TRANSACTION_READ_COMMITTED, false, eClass, objectFactory, sql, params);
	}

	/**
	 * Query the database with a <code>Stream&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	<T,E extends Exception> Stream<T> stream(int isolationLevel, boolean readOnly, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

	/**
	 * Read-only query the database with a <code>Collection&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionUpdate(java.util.Collection, java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	default <T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, collection, clazz, sql, params);
	}

	/**
	 * Read-write query the database with a <code>Collection&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionQuery(java.util.Collection, java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	default <T,C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, collection, clazz, sql, params);
	}

	/**
	 * Query the database with a <code>Collection&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 */
	default <T,C extends Collection<? super T>> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(isolationLevel, readOnly, collection, new ObjectFactories.Object<>(clazz), sql, params);
	}

	/**
	 * Read-only query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionUpdate(java.util.Collection, com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	default <T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, collection, objectFactory, sql, params);
	}

	/**
	 * Read-write query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionQuery(java.util.Collection, com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	default <T,C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, collection, objectFactory, sql, params);
	}

	/**
	 * Query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	default <T,C extends Collection<? super T>> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(isolationLevel, readOnly, collection, RuntimeException.class, objectFactory, sql, params);
	}

	/**
	 * Read-only query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionUpdate(java.util.Collection, java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	default <T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionQuery(C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, collection, eClass, objectFactory, sql, params);
	}

	/**
	 * Read-write query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionQuery(java.util.Collection, java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	default <T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionUpdate(C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, collection, eClass, objectFactory, sql, params);
	}

	/**
	 * Query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	default <T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return executeQuery(
			isolationLevel,
			readOnly,
			eClass,
			results -> {
				while(results.next()) {
					T newObj = objectFactory.createObject(results);
					if(!collection.add(newObj)) {
						throw new SQLException("Duplicate row in results: " + DatabaseUtils.getRow(results));
					}
				}
				return collection;
			},
			sql,
			params
		);
	}

	/**
	 * Read-only query the database, calling the <code>ResultSetHandler</code> once.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeUpdate(com.aoindustries.dbc.ResultSetHandler, java.lang.String, java.lang.Object...)
	 */
	default <T> T executeQuery(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, true, resultSetHandler, sql, params);
	}

	/**
	 * Read-write query the database, calling the <code>ResultSetHandler</code> once.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeQuery(com.aoindustries.dbc.ResultSetHandler, java.lang.String, java.lang.Object...)
	 */
	default <T> T executeUpdate(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, false, resultSetHandler, sql, params);
	}

	/**
	 * Query the database, calling the <code>ResultSetHandler</code> once.
	 */
	default <T> T executeQuery(int isolationLevel, boolean readOnly, ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
		return executeQuery(isolationLevel, readOnly, RuntimeException.class, resultSetHandler, sql, params);
	}

	/**
	 * Read-only query the database, calling the <code>ResultSetHandlerE</code> once.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeUpdate(java.lang.Class, com.aoindustries.dbc.ResultSetHandlerE, java.lang.String, java.lang.Object...)
	 */
	default <T,E extends Exception> T executeQuery(Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E {
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, true, eClass, resultSetHandler, sql, params);
	}

	/**
	 * Read-write query the database, calling the <code>ResultSetHandlerE</code> once.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeQuery(java.lang.Class, com.aoindustries.dbc.ResultSetHandlerE, java.lang.String, java.lang.Object...)
	 */
	default <T,E extends Exception> T executeUpdate(Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E {
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, false, eClass, resultSetHandler, sql, params);
	}

	/**
	 * Query the database, calling the <code>ResultSetHandlerE</code> once.
	 */
	<T,E extends Exception> T executeQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E;

	/**
	 * Read-only query the database with a <code>short</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeShortUpdate(java.lang.String, java.lang.Object...)
	 */
	default short executeShortQuery(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeShortQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>short</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeShortQuery(java.lang.String, java.lang.Object...)
	 */
	default short executeShortUpdate(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeShortQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with a <code>short</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required.
	 *
	 * @throws  NullDataException  When has a row, but with NULL value.
	 */
	default Short executeShortQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
		return executeObjectQuery(
			isolationLevel,
			readOnly,
			rowRequired,
			ObjectFactories.notNull(ObjectFactories.Short),
			sql,
			params
		);
	}

	/**
	 * Read-only query the database with a {@code List<Short>} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeShortListUpdate(java.lang.String, java.lang.Object...)
	 */
	default List<Short> executeShortListQuery(String sql, Object ... params) throws SQLException {
		return executeShortListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	/**
	 * Read-write query the database with a {@code List<Short>} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeShortListQuery(java.lang.String, java.lang.Object...)
	 */
	default List<Short> executeShortListUpdate(String sql, Object ... params) throws SQLException {
		return executeShortListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	/**
	 * Query the database with a {@code List<Short>} return type.
	 */
	default List<Short> executeShortListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(isolationLevel, readOnly, ObjectFactories.Short, sql, params);
	}

	/**
	 * Read-only query the database with a {@code Stream<Short>} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #streamShortUpdate(java.lang.String, java.lang.Object...)
	 */
	default Stream<Short> streamShort(String sql, Object ... params) throws SQLException {
		return streamShort(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	/**
	 * Read-write query the database with a {@code Stream<Short>} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #streamShort(java.lang.String, java.lang.Object[])
	 */
	default Stream<Short> streamShortUpdate(String sql, Object ... params) throws SQLException {
		return streamShort(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	/**
	 * Query the database with a {@code Stream<Short>} return type.
	 */
	default Stream<Short> streamShort(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		return stream(isolationLevel, readOnly, ObjectFactories.Short, sql, params);
	}

	/**
	 * Read-only query the database with a <code>String</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeStringUpdate(java.lang.String, java.lang.Object...)
	 */
	default String executeStringQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeStringQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>String</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeStringQuery(java.lang.String, java.lang.Object...)
	 */
	default String executeStringUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeStringQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with a <code>String</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required, or when row with null value.
	 */
	default String executeStringQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.String, sql, params);
	}

	/**
	 * Read-only query the database with a <code>List&lt;String&gt;</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeStringListUpdate(java.lang.String, java.lang.Object...)
	 */
	default List<String> executeStringListQuery(String sql, Object ... params) throws SQLException {
		return executeStringListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>List&lt;String&gt;</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeStringListQuery(java.lang.String, java.lang.Object...)
	 */
	default List<String> executeStringListUpdate(String sql, Object ... params) throws SQLException {
		return executeStringListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	/**
	 * Query the database with a <code>List&lt;String&gt;</code> return type.
	 */
	default List<String> executeStringListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(isolationLevel, readOnly, ObjectFactories.String, sql, params);
	}

	/**
	 * Read-only query the database with a <code>Stream&lt;String&gt;</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #streamStringUpdate(java.lang.String, java.lang.Object...)
	 */
	default Stream<String> streamString(String sql, Object ... params) throws SQLException {
		return streamString(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>Stream&lt;String&gt;</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #streamString(java.lang.String, java.lang.Object[])
	 */
	default Stream<String> streamStringUpdate(String sql, Object ... params) throws SQLException {
		return streamString(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	/**
	 * Query the database with a <code>Stream&lt;String&gt;</code> return type.
	 */
	default Stream<String> streamString(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		return stream(isolationLevel, readOnly, ObjectFactories.String, sql, params);
	}

	/**
	 * Read-only query the database with a <code>Timestamp</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeTimestampUpdate(java.lang.String, java.lang.Object...)
	 */
	default Timestamp executeTimestampQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeTimestampQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	/**
	 * Read-write query the database with a <code>Timestamp</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @return  The value or {@code null} when row with null value.
	 *
	 * @see  #executeTimestampQuery(java.lang.String, java.lang.Object...)
	 */
	default Timestamp executeTimestampUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeTimestampQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	/**
	 * Query the database with a <code>Timestamp</code> return type.
	 *
	 * @return  The value or {@code null} when no row and row not required, or when row with null value.
	 */
	default Timestamp executeTimestampQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.Timestamp, sql, params);
	}

	/**
	 * Performs an update on the database and returns the number of rows affected.
	 */
	int executeUpdate(String sql, Object ... params) throws SQLException;
}
