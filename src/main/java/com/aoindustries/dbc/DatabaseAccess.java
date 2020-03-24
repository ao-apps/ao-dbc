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

import com.aoindustries.collections.IntList;
import com.aoindustries.collections.LongList;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.List;

/**
 * Wraps and simplifies access to a JDBC database.
 *
 * @author  AO Industries, Inc.
 */
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
	 * @see  #executeBigDecimalUpdate(java.lang.String, java.lang.Object...)
	 */
	BigDecimal executeBigDecimalQuery(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-write query the database with a <code>BigDecimal</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeBigDecimalQuery(java.lang.String, java.lang.Object...)
	 */
	BigDecimal executeBigDecimalUpdate(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>BigDecimal</code> return type.
	 */
	BigDecimal executeBigDecimalQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

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
	boolean executeBooleanQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	boolean executeBooleanUpdate(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>boolean</code> return type.
	 */
	boolean executeBooleanQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-only query the database with a <code>byte[]</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeByteArrayUpdate(java.lang.String, java.lang.Object...)
	 */
	byte[] executeByteArrayQuery(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-write query the database with a <code>byte[]</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeByteArrayQuery(java.lang.String, java.lang.Object...)
	 */
	byte[] executeByteArrayUpdate(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>byte[]</code> return type.
	 */
	byte[] executeByteArrayQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-only query the database with a <code>java.sql.Date</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeDateUpdate(java.lang.String, java.lang.Object...)
	 */
	Date executeDateQuery(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-write query the database with a <code>java.sql.Date</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeDateQuery(java.lang.String, java.lang.Object...)
	 */
	Date executeDateUpdate(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>java.sql.Date</code> return type.
	 */
	Date executeDateQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-only query the database with an <code>IntList</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeIntListUpdate(java.lang.String, java.lang.Object...)
	 */
	IntList executeIntListQuery(String sql, Object ... params) throws SQLException;

	/**
	 * Read-write query the database with an <code>IntList</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeIntListQuery(java.lang.String, java.lang.Object...)
	 */
	IntList executeIntListUpdate(String sql, Object ... params) throws SQLException;

	/**
	 * Query the database with an <code>IntList</code> return type.
	 */
	IntList executeIntListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException;

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
	int executeIntQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	int executeIntUpdate(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with an <code>int</code> return type.
	 */
	int executeIntQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-only query the database with a <code>LongList</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeLongListUpdate(java.lang.String, java.lang.Object...)
	 */
	LongList executeLongListQuery(String sql, Object ... params) throws SQLException;

	/**
	 * Read-write query the database with a <code>LongList</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeLongListQuery(java.lang.String, java.lang.Object...)
	 */
	LongList executeLongListUpdate(String sql, Object ... params) throws SQLException;

	/**
	 * Query the database with a <code>LongList</code> return type.
	 */
	LongList executeLongListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException;

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
	long executeLongQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	long executeLongUpdate(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>long</code> return type.
	 */
	long executeLongQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-only query the database with a <code>&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectUpdate(java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	<T> T executeObjectQuery(Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-write query the database with a <code>&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectQuery(java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	<T> T executeObjectUpdate(Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 */
	<T> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-only query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectUpdate(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	<T> T executeObjectQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-write query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectQuery(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	<T> T executeObjectUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	<T> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-only query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectUpdate(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	<T,E extends Exception> T executeObjectQuery(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, SQLException, E;

	/**
	 * Read-write query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectQuery(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	<T,E extends Exception> T executeObjectUpdate(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, SQLException, E;

	/**
	 * Query the database with a <code>&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	<T,E extends Exception> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, SQLException, E;

	/**
	 * Read-only query the database with a <code>List&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListUpdate(java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	<T> List<T> executeObjectListQuery(Class<T> clazz, String sql, Object ... params) throws SQLException;

	/**
	 * Read-write query the database with a <code>List&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListQuery(java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	<T> List<T> executeObjectListUpdate(Class<T> clazz, String sql, Object ... params) throws SQLException;

	/**
	 * Query the database with a <code>List&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 */
	<T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<T> clazz, String sql, Object ... params) throws SQLException;

	/**
	 * Read-only query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListUpdate(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	<T> List<T> executeObjectListQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException;

	/**
	 * Read-write query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListQuery(com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	<T> List<T> executeObjectListUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException;

	/**
	 * Query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	<T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException;

	/**
	 * Read-only query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListUpdate(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	<T,E extends Exception> List<T> executeObjectListQuery(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

	/**
	 * Read-write query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectListQuery(java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	<T,E extends Exception> List<T> executeObjectListUpdate(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

	/**
	 * Query the database with a <code>List&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	<T,E extends Exception> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

	/**
	 * Read-only query the database with a <code>Collection&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionUpdate(java.util.Collection, java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	<T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException;

	/**
	 * Read-write query the database with a <code>Collection&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionQuery(java.util.Collection, java.lang.Class, java.lang.String, java.lang.Object...)
	 */
	<T,C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException;

	/**
	 * Query the database with a <code>Collection&lt;T&gt;</code> return type.  Class &lt;T&gt; must have a contructor that takes a single argument of <code>ResultSet</code>.
	 */
	<T,C extends Collection<? super T>> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<T> clazz, String sql, Object ... params) throws SQLException;

	/**
	 * Read-only query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionUpdate(java.util.Collection, com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	<T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException;

	/**
	 * Read-write query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionQuery(java.util.Collection, com.aoindustries.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
	 */
	<T,C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException;

	/**
	 * Query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	<T,C extends Collection<? super T>> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException;

	/**
	 * Read-only query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionUpdate(java.util.Collection, java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	<T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionQuery(C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

	/**
	 * Read-write query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeObjectCollectionQuery(java.util.Collection, java.lang.Class, com.aoindustries.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
	 */
	<T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionUpdate(C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

	/**
	 * Query the database with a <code>Collection&lt;T&gt;</code> return type, objects are created with the provided factory.
	 */
	<T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

	/**
	 * Read-only query the database, calling the <code>ResultSetHandler</code> once.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeUpdate(com.aoindustries.dbc.ResultSetHandler, java.lang.String, java.lang.Object...)
	 */
	<T> T executeQuery(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException;

	/**
	 * Read-write query the database, calling the <code>ResultSetHandler</code> once.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeQuery(com.aoindustries.dbc.ResultSetHandler, java.lang.String, java.lang.Object...)
	 */
	<T> T executeUpdate(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException;

	/**
	 * Query the database, calling the <code>ResultSetHandler</code> once.
	 */
	<T> T executeQuery(int isolationLevel, boolean readOnly, ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException;

	/**
	 * Read-only query the database, calling the <code>ResultSetHandlerE</code> once.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeUpdate(java.lang.Class, com.aoindustries.dbc.ResultSetHandlerE, java.lang.String, java.lang.Object...)
	 */
	<T,E extends Exception> T executeQuery(Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E;

	/**
	 * Read-write query the database, calling the <code>ResultSetHandlerE</code> once.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeQuery(java.lang.Class, com.aoindustries.dbc.ResultSetHandlerE, java.lang.String, java.lang.Object...)
	 */
	<T,E extends Exception> T executeUpdate(Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E;

	/**
	 * Query the database, calling the <code>ResultSetHandlerE</code> once.
	 */
	<T,E extends Exception> T executeQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E;

	/**
	 * Read-only query the database with a {@code List<Short>} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeShortListUpdate(java.lang.String, java.lang.Object...)
	 */
	List<Short> executeShortListQuery(String sql, Object ... params) throws SQLException;

	/**
	 * Read-write query the database with a {@code List<Short>} return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeShortListQuery(java.lang.String, java.lang.Object...)
	 */
	List<Short> executeShortListUpdate(String sql, Object ... params) throws SQLException;

	/**
	 * Query the database with a {@code List<Short>} return type.
	 */
	List<Short> executeShortListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException;

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
	short executeShortQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	short executeShortUpdate(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>short</code> return type.
	 */
	short executeShortQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-only query the database with a <code>String</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeStringUpdate(java.lang.String, java.lang.Object...)
	 */
	String executeStringQuery(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-write query the database with a <code>String</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeStringQuery(java.lang.String, java.lang.Object...)
	 */
	String executeStringUpdate(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>String</code> return type.
	 */
	String executeStringQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-only query the database with a <code>List&lt;String&gt;</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeStringListUpdate(java.lang.String, java.lang.Object...)
	 */
	List<String> executeStringListQuery(String sql, Object ... params) throws SQLException;

	/**
	 * Read-write query the database with a <code>List&lt;String&gt;</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 * </ul>
	 *
	 * @see  #executeStringListQuery(java.lang.String, java.lang.Object...)
	 */
	List<String> executeStringListUpdate(String sql, Object ... params) throws SQLException;

	/**
	 * Query the database with a <code>List&lt;String&gt;</code> return type.
	 */
	List<String> executeStringListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException;

	/**
	 * Read-only query the database with a <code>Timestamp</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeTimestampUpdate(java.lang.String, java.lang.Object...)
	 */
	Timestamp executeTimestampQuery(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Read-write query the database with a <code>Timestamp</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>false</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 *
	 * @see  #executeTimestampQuery(java.lang.String, java.lang.Object...)
	 */
	Timestamp executeTimestampUpdate(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>Timestamp</code> return type.
	 */
	Timestamp executeTimestampQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Performs an update on the database and returns the number of rows affected.
	 */
	int executeUpdate(String sql, Object ... params) throws SQLException;
}
