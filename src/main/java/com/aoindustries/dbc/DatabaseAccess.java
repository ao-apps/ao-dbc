/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2015  AO Industries, Inc.
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

import com.aoindustries.util.IntList;
import com.aoindustries.util.LongList;
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
	 */
	BigDecimal executeBigDecimalQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	boolean executeBooleanQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	byte[] executeByteArrayQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	Date executeDateQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	IntList executeIntListQuery(String sql, Object ... params) throws SQLException;

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
	 */
	int executeIntQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	LongList executeLongListQuery(String sql, Object ... params) throws SQLException;

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
	 */
	long executeLongQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	<T> T executeObjectQuery(Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	<T> T executeObjectQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	<T,E extends Exception> T executeObjectQuery(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, SQLException, E;

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
	 */
	<T> List<T> executeObjectListQuery(Class<T> clazz, String sql, Object ... params) throws SQLException;

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
	 */
	<T> List<T> executeObjectListQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException;

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
	 */
	<T,E extends Exception> List<T> executeObjectListQuery(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

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
	 */
	<T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException;

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
	 */
	<T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException;

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
	 */
	<T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionQuery(C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

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
	 */
	<T> T executeQuery(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException;

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
	 */
	<T,E extends Exception> T executeQuery(Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E;

	/**
	 * Query the database, calling the <code>ResultSetHandlerE</code> once.
	 */
	<T,E extends Exception> T executeQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E;

	/**
	 * Read-only query the database with a <code>List<Short></code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 * </ul>
	 */
	List<Short> executeShortListQuery(String sql, Object ... params) throws SQLException;

	/**
	 * Query the database with a <code>List<Short></code> return type.
	 */
	List<Short> executeShortListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException;

	/**
	 * Read-only query the database with a <code>short</code> return type.
	 * <ul>
	 *   <li>isolationLevel = <code>Connection.TRANSACTION_READ_COMMITTED</code></li>
	 *   <li>readOnly = <code>true</code></li>
	 *   <li>rowRequired = <code>true</code></li>
	 * </ul>
	 */
	short executeShortQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	String executeStringQuery(String sql, Object ... params) throws NoRowException, SQLException;

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
	 */
	List<String> executeStringListQuery(String sql, Object ... params) throws SQLException;

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
	 */
	Timestamp executeTimestampQuery(String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Query the database with a <code>Timestamp</code> return type.
	 */
	Timestamp executeTimestampQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	/**
	 * Performs an update on the database and returns the number of rows affected.
	 */
	int executeUpdate(String sql, Object ... params) throws SQLException;
}
