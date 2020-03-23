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

import com.aoindustries.util.AoCollections;
import com.aoindustries.util.IntList;
import com.aoindustries.util.LongList;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Wraps and simplifies access to a JDBC database.
 *
 * @author  AO Industries, Inc.
 */
abstract public class AbstractDatabaseAccess implements DatabaseAccess {

	private static final ObjectFactory<BigDecimal> bigDecimalObjectFactory = result -> result.getBigDecimal(1);

	@Override
	final public BigDecimal executeBigDecimalQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, RuntimeException.class, bigDecimalObjectFactory, sql, params);
	}

	@Override
	final public BigDecimal executeBigDecimalUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, RuntimeException.class, bigDecimalObjectFactory, sql, params);
	}

	@Override
	final public BigDecimal executeBigDecimalQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, RuntimeException.class, bigDecimalObjectFactory, sql, params);
	}

	@Override
	final public boolean executeBooleanQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeBooleanQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public boolean executeBooleanUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeBooleanQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	abstract public boolean executeBooleanQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	private static final ObjectFactory<byte[]> byteArrayObjectFactory = result -> result.getBytes(1);

	@Override
	final public byte[] executeByteArrayQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, RuntimeException.class, byteArrayObjectFactory, sql, params);
	}

	@Override
	final public byte[] executeByteArrayUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, RuntimeException.class, byteArrayObjectFactory, sql, params);
	}

	@Override
	final public byte[] executeByteArrayQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, RuntimeException.class, byteArrayObjectFactory, sql, params);
	}

	private static final ObjectFactory<Date> dateObjectFactory = result -> result.getDate(1);

	@Override
	final public Date executeDateQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, RuntimeException.class, dateObjectFactory, sql, params);
	}

	@Override
	final public Date executeDateUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, RuntimeException.class, dateObjectFactory, sql, params);
	}

	@Override
	final public Date executeDateQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, RuntimeException.class, dateObjectFactory, sql, params);
	}

	@Override
	final public IntList executeIntListQuery(String sql, Object ... params) throws SQLException {
		return executeIntListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	@Override
	final public IntList executeIntListUpdate(String sql, Object ... params) throws SQLException {
		return executeIntListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	@Override
	abstract public IntList executeIntListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException;

	@Override
	final public int executeIntQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeIntQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public int executeIntUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeIntQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	abstract public int executeIntQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	@Override
	final public LongList executeLongListQuery(String sql, Object ... params) throws SQLException {
		return executeLongListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	@Override
	final public LongList executeLongListUpdate(String sql, Object ... params) throws SQLException {
		return executeLongListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	@Override
	abstract public LongList executeLongListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException;

	@Override
	final public long executeLongQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeLongQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public long executeLongUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeLongQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	abstract public long executeLongQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	private static class ClassObjectFactory<T> implements ObjectFactory<T> {

		private final Class<T> clazz;
		private final Constructor<T> constructor;

		private ClassObjectFactory(Class<T> clazz) throws SQLException {
			this.clazz = clazz;
			try {
				this.constructor = clazz.getConstructor(ResultSet.class);
			} catch(NoSuchMethodException err) {
				throw new SQLException("Unable to find constructor: "+clazz.getName()+"(java.sql.ResultSet)", err);
			}
		}

		@Override
		public T createObject(ResultSet result) throws SQLException {
			try {
				return constructor.newInstance(result);
			} catch(InstantiationException err) {
				throw new SQLException("Unable to instantiate object: "+clazz.getName()+"(java.sql.ResultSet)", err);
			} catch(IllegalAccessException err) {
				throw new SQLException("Illegal access on constructor: "+clazz.getName()+"(java.sql.ResultSet)", err);
			} catch(InvocationTargetException err) {
				throw new SQLException("Invocation exception on constructor: "+clazz.getName()+"(java.sql.ResultSet)", err);
			}
		}
	};

	@Override
	final public <T> T executeObjectQuery(Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, RuntimeException.class, new ClassObjectFactory<>(clazz), sql, params);
	}

	@Override
	final public <T> T executeObjectUpdate(Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, RuntimeException.class, new ClassObjectFactory<>(clazz), sql, params);
	}

	@Override
	final public <T> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, RuntimeException.class, new ClassObjectFactory<>(clazz), sql, params);
	}

	@Override
	final public <T> T executeObjectQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, RuntimeException.class, objectFactory, sql, params);
	}

	@Override
	final public <T> T executeObjectUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, RuntimeException.class, objectFactory, sql, params);
	}

	@Override
	final public <T> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, RuntimeException.class, objectFactory, sql, params);
	}

	@Override
	final public <T,E extends Exception> T executeObjectQuery(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, SQLException, E {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, eClass, objectFactory, sql, params);
	}

	@Override
	final public <T,E extends Exception> T executeObjectUpdate(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, SQLException, E {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, eClass, objectFactory, sql, params);
	}

	@Override
	abstract public <T,E extends Exception> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, SQLException, E;

	@Override
	final public <T> List<T> executeObjectListQuery(Class<T> clazz, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, new ArrayList<>(), RuntimeException.class, new ClassObjectFactory<>(clazz), sql, params)
		);
	}

	@Override
	final public <T> List<T> executeObjectListUpdate(Class<T> clazz, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, new ArrayList<>(), RuntimeException.class, new ClassObjectFactory<>(clazz), sql, params)
		);
	}

	@Override
	final public <T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), RuntimeException.class, new ClassObjectFactory<>(clazz), sql, params)
		);
	}

	@Override
	final public <T> List<T> executeObjectListQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, new ArrayList<>(), RuntimeException.class, objectFactory, sql, params)
		);
	}

	@Override
	final public <T> List<T> executeObjectListUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, new ArrayList<>(), RuntimeException.class, objectFactory, sql, params)
		);
	}

	@Override
	final public <T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), RuntimeException.class, objectFactory, sql, params)
		);
	}

	@Override
	final public <T,E extends Exception> List<T> executeObjectListQuery(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, new ArrayList<>(), eClass, objectFactory, sql, params)
		);
	}

	@Override
	final public <T,E extends Exception> List<T> executeObjectListUpdate(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, new ArrayList<>(), eClass, objectFactory, sql, params)
		);
	}

	@Override
	final public <T,E extends Exception> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), eClass, objectFactory, sql, params)
		);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, collection, RuntimeException.class, new ClassObjectFactory<>(clazz), sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, collection, RuntimeException.class, new ClassObjectFactory<>(clazz), sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(isolationLevel, readOnly, collection, RuntimeException.class, new ClassObjectFactory<>(clazz), sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, collection, RuntimeException.class, objectFactory, sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, collection, RuntimeException.class, objectFactory, sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(isolationLevel, readOnly, collection, RuntimeException.class, objectFactory, sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionQuery(C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, collection, eClass, objectFactory, sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionUpdate(C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, collection, eClass, objectFactory, sql, params);
	}

	@Override
	abstract public <T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E;

	@Override
	final public <T> T executeQuery(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, true, RuntimeException.class, resultSetHandler, sql, params);
	}

	@Override
	final public <T> T executeUpdate(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, false, RuntimeException.class, resultSetHandler, sql, params);
	}

	@Override
	final public <T> T executeQuery(int isolationLevel, boolean readOnly, ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
		return executeQuery(isolationLevel, readOnly, RuntimeException.class, resultSetHandler, sql, params);
	}

	@Override
	final public <T,E extends Exception> T executeQuery(Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E {
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, true, eClass, resultSetHandler, sql, params);
	}

	@Override
	final public <T,E extends Exception> T executeUpdate(Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E {
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, false, eClass, resultSetHandler, sql, params);
	}

	@Override
	abstract public <T,E extends Exception> T executeQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E;

	private static final ObjectFactory<Short> shortObjectFactory = result-> result.getShort(1);

	@Override
	final public List<Short> executeShortListQuery(String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, new ArrayList<>(), RuntimeException.class, shortObjectFactory, sql, params)
		);
	}

	@Override
	final public List<Short> executeShortListUpdate(String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, new ArrayList<>(), RuntimeException.class, shortObjectFactory, sql, params)
		);
	}

	@Override
	final public List<Short> executeShortListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), RuntimeException.class, shortObjectFactory, sql, params)
		);
	}

	@Override
	final public short executeShortQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeShortQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public short executeShortUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeShortQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	abstract public short executeShortQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException;

	private static final ObjectFactory<String> stringObjectFactory = result -> result.getString(1);

	@Override
	final public String executeStringQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, RuntimeException.class, stringObjectFactory, sql, params);
	}

	@Override
	final public String executeStringUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, RuntimeException.class, stringObjectFactory, sql, params);
	}

	@Override
	final public String executeStringQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, RuntimeException.class, stringObjectFactory, sql, params);
	}

	@Override
	final public List<String> executeStringListQuery(String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, new ArrayList<>(), RuntimeException.class, stringObjectFactory, sql, params)
		);
	}

	@Override
	final public List<String> executeStringListUpdate(String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, new ArrayList<>(), RuntimeException.class, stringObjectFactory, sql, params)
		);
	}

	@Override
	final public List<String> executeStringListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), RuntimeException.class, stringObjectFactory, sql, params)
		);
	}

	private static final ObjectFactory<Timestamp> timestampObjectFactory = result -> result.getTimestamp(1);

	@Override
	final public Timestamp executeTimestampQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, RuntimeException.class, timestampObjectFactory, sql, params);
	}

	@Override
	final public Timestamp executeTimestampUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, RuntimeException.class, timestampObjectFactory, sql, params);
	}

	@Override
	final public Timestamp executeTimestampQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, RuntimeException.class, timestampObjectFactory, sql, params);
	}

	@Override
	abstract public int executeUpdate(String sql, Object ... params) throws SQLException;
}
