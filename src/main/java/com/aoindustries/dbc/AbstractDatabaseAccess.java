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

import com.aoindustries.collections.AoCollections;
import com.aoindustries.collections.IntArrayList;
import com.aoindustries.collections.IntList;
import com.aoindustries.collections.LongArrayList;
import com.aoindustries.collections.LongList;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
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
// TODO: Add variants for Supplier and Consumer
abstract public class AbstractDatabaseAccess implements DatabaseAccess {

	@Override
	final public BigDecimal executeBigDecimalQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeBigDecimalQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public BigDecimal executeBigDecimalUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeBigDecimalQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	final public BigDecimal executeBigDecimalQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.BigDecimal, sql, params);
	}

	@Override
	final public boolean executeBooleanQuery(String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		return executeBooleanQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public boolean executeBooleanUpdate(String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		return executeBooleanQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	final public boolean executeBooleanQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		Boolean b = executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.Boolean, sql, params);
		if(b == null) throw new NullDataException();
		return b;
	}

	@Override
	final public byte[] executeByteArrayQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeByteArrayQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public byte[] executeByteArrayUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeByteArrayQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	final public byte[] executeByteArrayQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.ByteArray, sql, params);
	}

	@Override
	final public Date executeDateQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeDateQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public Date executeDateUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeDateQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	final public Date executeDateQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.Date, sql, params);
	}

	@Override
	final public IntList executeIntListQuery(String sql, Object ... params) throws NullDataException, SQLException {
		return executeIntListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	@Override
	final public IntList executeIntListUpdate(String sql, Object ... params) throws NullDataException, SQLException {
		return executeIntListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	@Override
	final public IntList executeIntListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
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

	@Override
	final public int executeIntQuery(String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		return executeIntQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public int executeIntUpdate(String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		return executeIntQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	final public int executeIntQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		Integer i = executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.Integer, sql, params);
		if(i == null) throw new NullDataException();
		return i;
	}

	@Override
	final public LongList executeLongListQuery(String sql, Object ... params) throws NullDataException, SQLException {
		return executeLongListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	@Override
	final public LongList executeLongListUpdate(String sql, Object ... params) throws NullDataException, SQLException {
		return executeLongListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	@Override
	final public LongList executeLongListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
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

	@Override
	final public long executeLongQuery(String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		return executeLongQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public long executeLongUpdate(String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		return executeLongQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	final public long executeLongQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		Long l = executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.Long, sql, params);
		if(l == null) throw new NullDataException();
		return l;
	}

	@Override
	final public <T> T executeObjectQuery(Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, clazz, sql, params);
	}

	@Override
	final public <T> T executeObjectUpdate(Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, clazz, sql, params);
	}

	@Override
	final public <T> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<T> clazz, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, new ObjectFactories.Object<>(clazz), sql, params);
	}

	@Override
	final public <T> T executeObjectQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, objectFactory, sql, params);
	}

	@Override
	final public <T> T executeObjectUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, objectFactory, sql, params);
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
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, true, clazz, sql, params);
	}

	@Override
	final public <T> List<T> executeObjectListUpdate(Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, false, clazz, sql, params);
	}

	@Override
	final public <T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), new ObjectFactories.Object<>(clazz), sql, params)
		);
	}

	@Override
	final public <T> List<T> executeObjectListQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, true, objectFactory, sql, params);
	}

	@Override
	final public <T> List<T> executeObjectListUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, false, objectFactory, sql, params);
	}

	@Override
	final public <T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), objectFactory, sql, params)
		);
	}

	@Override
	final public <T,E extends Exception> List<T> executeObjectListQuery(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, true, eClass, objectFactory, sql, params);
	}

	@Override
	final public <T,E extends Exception> List<T> executeObjectListUpdate(Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return executeObjectListQuery(Connection.TRANSACTION_READ_COMMITTED, false, eClass, objectFactory, sql, params);
	}

	@Override
	final public <T,E extends Exception> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), eClass, objectFactory, sql, params)
		);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, collection, clazz, sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, collection, clazz, sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(isolationLevel, readOnly, collection, new ObjectFactories.Object<>(clazz), sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, true, collection, objectFactory, sql, params);
	}

	@Override
	final public <T,C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
		return executeObjectCollectionQuery(Connection.TRANSACTION_READ_COMMITTED, false, collection, objectFactory, sql, params);
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
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, true, resultSetHandler, sql, params);
	}

	@Override
	final public <T> T executeUpdate(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
		return executeQuery(Connection.TRANSACTION_READ_COMMITTED, false, resultSetHandler, sql, params);
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

	@Override
	final public List<Short> executeShortListQuery(String sql, Object ... params) throws SQLException {
		return executeShortListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	@Override
	final public List<Short> executeShortListUpdate(String sql, Object ... params) throws SQLException {
		return executeShortListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	@Override
	final public List<Short> executeShortListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), ObjectFactories.Short, sql, params)
		);
	}

	@Override
	final public short executeShortQuery(String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		return executeShortQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public short executeShortUpdate(String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		return executeShortQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	final public short executeShortQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, SQLException {
		Short s = executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.Short, sql, params);
		if(s == null) throw new NullDataException();
		return s;
	}

	@Override
	final public String executeStringQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeStringQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public String executeStringUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeStringQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	final public String executeStringQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.String, sql, params);
	}

	@Override
	final public List<String> executeStringListQuery(String sql, Object ... params) throws SQLException {
		return executeStringListQuery(Connection.TRANSACTION_READ_COMMITTED, true, sql, params);
	}

	@Override
	final public List<String> executeStringListUpdate(String sql, Object ... params) throws SQLException {
		return executeStringListQuery(Connection.TRANSACTION_READ_COMMITTED, false, sql, params);
	}

	@Override
	final public List<String> executeStringListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		return AoCollections.optimalUnmodifiableList(
			executeObjectCollectionQuery(isolationLevel, readOnly, new ArrayList<>(), ObjectFactories.String, sql, params)
		);
	}

	@Override
	final public Timestamp executeTimestampQuery(String sql, Object ... params) throws NoRowException, SQLException {
		return executeTimestampQuery(Connection.TRANSACTION_READ_COMMITTED, true, true, sql, params);
	}

	@Override
	final public Timestamp executeTimestampUpdate(String sql, Object ... params) throws NoRowException, SQLException {
		return executeTimestampQuery(Connection.TRANSACTION_READ_COMMITTED, false, true, sql, params);
	}

	@Override
	final public Timestamp executeTimestampQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		return executeObjectQuery(isolationLevel, readOnly, rowRequired, ObjectFactories.Timestamp, sql, params);
	}

	@Override
	abstract public int executeUpdate(String sql, Object ... params) throws SQLException;
}
