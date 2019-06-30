/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2018, 2019  AO Industries, Inc.
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

import com.aoindustries.sql.AOConnectionPool;
import com.aoindustries.util.IntList;
import com.aoindustries.util.LongList;
import java.sql.Connection;
import java.sql.SQLData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Wraps and simplifies access to a JDBC database.  If used directly as a <code>DatabaseAccess</code> each individual call is a separate transaction.
 * If the current thread is already in a transaction, the calls will be performed using the connection associated with that transaction.
 * For transactions across multiple statements, use <code>DatabaseConnection</code>.
 *
 * @see  #createDatabaseConnection
 * @see  DatabaseConnection
 *
 * @author  AO Industries, Inc.
 */
public class Database extends AbstractDatabaseAccess {

	/**
	 * Only one connection pool is made to the database.
	 */
	private final AOConnectionPool pool;

	private final DataSource dataSource;
	private final Logger logger;

	public Database(String driver, String url, String user, String password, int numConnections, long maxConnectionAge, Logger logger) {
		this(new AOConnectionPool(driver, url, user, password, numConnections, maxConnectionAge, logger));
	}

	public Database(AOConnectionPool pool) {
		if(pool==null) throw new IllegalArgumentException("pool==null");
		this.pool = pool;
		this.dataSource = null;
		this.logger = null;
	}

	public Database(DataSource dataSource, Logger logger) {
		if(dataSource==null) throw new IllegalArgumentException("dataSource==null");
		if(logger==null) throw new IllegalArgumentException("logger==null");
		this.pool = null;
		this.dataSource = dataSource;
		this.logger = logger;
	}

	public DatabaseConnection createDatabaseConnection() {
		return new DatabaseConnection(this);
	}

	/**
	 * Gets the pool or <code>null</code> if using a <code>DataSource</code>.
	 */
	public AOConnectionPool getConnectionPool() {
		return pool;
	}

	/**
	 * Gets the data source or <code>null</code> if using an <code>AOConnectionPool</code>.
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * The custom types discovered via {@link ServiceLoader}.
	 */
	private volatile Map<String,Class<?>> sqlDataTypes;

	/**
	 * Loads the custom types when first needed and caches the results.
	 */
	private Map<String,Class<?>> getSqlDataTypes() throws SQLException {
		if(sqlDataTypes == null) {
			// Load custom types from ServiceLoader
			Map<String,Class<?>> newMap = new LinkedHashMap<>();
			Iterator<SQLData> iter = ServiceLoader.load(SQLData.class).iterator();
			while(iter.hasNext()) {
				SQLData sqlData = iter.next();
				newMap.put(sqlData.getSQLTypeName(), sqlData.getClass());
			}
			sqlDataTypes = newMap;
		}
		return sqlDataTypes;
	}

	private final Map<Connection,Map<String,Class<?>>> oldTypeMaps = new IdentityHashMap<>();

	public Connection getConnection(int isolationLevel, boolean readOnly, int maxConnections) throws SQLException {
		Connection conn;
		if(pool != null) {
			// From pool
			conn = pool.getConnection(isolationLevel, readOnly, maxConnections);
			boolean successful = false;
			try {
				initConnection(conn);
				successful = true;
			} finally {
				if(!successful) pool.releaseConnection(conn);
			}
		} else {
			// From dataSource
			conn = dataSource.getConnection();
			boolean successful = false;
			try {
				if(isolationLevel!=conn.getTransactionIsolation()) conn.setTransactionIsolation(isolationLevel);
				if(readOnly!=conn.isReadOnly()) conn.setReadOnly(readOnly);
				initConnection(conn);
				successful = true;
			} finally {
				if(!successful) conn.close();
			}
		}
		// Load custom types from ServiceLoader
		Map<String,Class<?>> newTypes = getSqlDataTypes();
		int size = newTypes.size();
		if(size != 0) {
			Map<String,Class<?>> typeMap = conn.getTypeMap();
			// Note: We get "null" back from PostgreSQL driver, despite documentation of returning empty
			if(typeMap == null) typeMap = new LinkedHashMap<>(size*4/3+1);
			oldTypeMaps.put(conn, new LinkedHashMap<>(typeMap));
			typeMap.putAll(newTypes);
			conn.setTypeMap(typeMap);
		}
		return conn;
	}

	public void releaseConnection(Connection conn) throws SQLException {
		// Restore custom types
		// TODO: Do not remove on release and avoid re-adding for performance?
		Map<String,Class<?>> oldTypeMap = oldTypeMaps.remove(conn);
		if(oldTypeMap != null) conn.setTypeMap(oldTypeMap);
		if(pool != null) {
			// From pool
			pool.releaseConnection(conn);
		} else {
			// From dataSource
			if(!conn.isClosed()) conn.close();
		}
	}

	public Logger getLogger() {
		if(pool!=null) {
			// From pool
			return pool.getLogger();
		} else {
			// From dataSource
			return logger;
		}
	}

	@Override
	public boolean executeBooleanQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeBooleanQuery(isolationLevel, readOnly, rowRequired, sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				boolean value=conn.executeBooleanQuery(isolationLevel, readOnly, rowRequired, sql, params);
				if(!readOnly) conn.commit();
				return value;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public IntList executeIntListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeIntListQuery(isolationLevel, readOnly, sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				IntList value=conn.executeIntListQuery(isolationLevel, readOnly, sql, params);
				if(!readOnly) conn.commit();
				return value;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public int executeIntQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeIntQuery(isolationLevel, readOnly, rowRequired, sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				int value=conn.executeIntQuery(isolationLevel, readOnly, rowRequired, sql, params);
				if(!readOnly) conn.commit();
				return value;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public LongList executeLongListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeLongListQuery(isolationLevel, readOnly, sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				LongList value=conn.executeLongListQuery(isolationLevel, readOnly, sql, params);
				if(!readOnly) conn.commit();
				return value;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public long executeLongQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeLongQuery(isolationLevel, readOnly, rowRequired, sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				long value=conn.executeLongQuery(isolationLevel, readOnly, rowRequired, sql, params);
				if(!readOnly) conn.commit();
				return value;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public <T,E extends Exception> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws NoRowException, SQLException, E {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeObjectQuery(isolationLevel, readOnly, rowRequired, eClass, objectFactory, sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} catch(Exception e) {
				conn.rollback();
				if(eClass.isInstance(e)) throw eClass.cast(e);
				throw new SQLException(e);
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				T value=conn.executeObjectQuery(isolationLevel, readOnly, rowRequired, eClass, objectFactory, sql, params);
				if(!readOnly) conn.commit();
				return value;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} catch(Exception e) {
				conn.rollback();
				if(eClass.isInstance(e)) throw eClass.cast(e);
				throw new SQLException(e);
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public <T,C extends Collection<? super T>,E extends Exception> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<E> eClass, ObjectFactoryE<T,E> objectFactory, String sql, Object ... params) throws SQLException, E {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeObjectCollectionQuery(isolationLevel, readOnly, collection, eClass, objectFactory, sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} catch(Exception e) {
				conn.rollback();
				if(eClass.isInstance(e)) throw eClass.cast(e);
				throw new SQLException(e);
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				C value=conn.executeObjectCollectionQuery(isolationLevel, readOnly, collection, eClass, objectFactory, sql, params);
				if(!readOnly) conn.commit();
				return value;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} catch(Exception e) {
				conn.rollback();
				if(eClass.isInstance(e)) throw eClass.cast(e);
				throw new SQLException(e);
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public <T,E extends Exception> T executeQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeQuery(isolationLevel, readOnly, eClass, resultSetHandler, sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} catch(Exception e) {
				conn.rollback();
				if(eClass.isInstance(e)) throw eClass.cast(e);
				throw new SQLException(e);
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				T value = conn.executeQuery(isolationLevel, readOnly, eClass, resultSetHandler, sql, params);
				if(!readOnly) conn.commit();
				return value;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} catch(Exception e) {
				conn.rollback();
				if(eClass.isInstance(e)) throw eClass.cast(e);
				throw new SQLException(e);
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public short executeShortQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, SQLException {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeShortQuery(isolationLevel, readOnly, rowRequired, sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				short value=conn.executeShortQuery(isolationLevel, readOnly, rowRequired, sql, params);
				if(!readOnly) conn.commit();
				return value;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public int executeUpdate(String sql, Object ... params) throws SQLException {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return conn.executeUpdate(sql, params);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				int updateCount = conn.executeUpdate(sql, params);
				conn.commit();
				return updateCount;
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} finally {
				conn.releaseConnection();
			}
		}
	}

	private final ThreadLocal<DatabaseConnection> transactionConnection = new ThreadLocal<>();

	/**
	 * Checks if currently in a transaction.
	 *
	 * @see #executeTransaction(com.aoindustries.dbc.DatabaseCallable)
	 * @see #executeTransaction(java.lang.Class, com.aoindustries.dbc.DatabaseCallableE)
	 * @see #executeTransaction(com.aoindustries.dbc.DatabaseRunnable)
	 * @see #executeTransaction(java.lang.Class, com.aoindustries.dbc.DatabaseRunnableE)
	 */
	public boolean isInTransaction() {
		return transactionConnection.get()!=null;
	}

	/**
	 * @see #executeTransaction(java.lang.Class, com.aoindustries.dbc.DatabaseCallableE)
	 *
	 * @see #isInTransaction()
	 */
	public void executeTransaction(final DatabaseRunnable runnable) throws SQLException {
		executeTransaction(
			RuntimeException.class,
			new DatabaseCallableE<Void,RuntimeException>() {
				@Override
				public Void call(DatabaseConnection db) throws SQLException {
					runnable.run(db);
					return null;
				}
			}
		);
	}

	/**
	 * @see #executeTransaction(java.lang.Class, com.aoindustries.dbc.DatabaseCallableE)
	 *
	 * @see #isInTransaction()
	 */
	public <E extends Exception> void executeTransaction(
		Class<E> eClass,
		final DatabaseRunnableE<E> runnable
	) throws SQLException, E {
		executeTransaction(
			eClass,
			new DatabaseCallableE<Void,E>() {
				@Override
				public Void call(DatabaseConnection db) throws SQLException, E {
					runnable.run(db);
					return null;
				}
			}
		);
	}

   /**
	 * @see #executeTransaction(java.lang.Class, com.aoindustries.dbc.DatabaseCallableE)
	 *
	 * @see #isInTransaction()
	 */
	public <V> V executeTransaction(final DatabaseCallable<V> callable) throws SQLException {
		return executeTransaction(
			RuntimeException.class,
			new DatabaseCallableE<V,RuntimeException>() {
				@Override
				public V call(DatabaseConnection db) throws SQLException {
					return callable.call(db);
				}
			}
		);
	}

	/**
	 * <p>
	 * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
	 * Rolls-back the transaction on RuntimeException.
	 * Rolls-back the transaction on NoRowException on the outer-most transaction only.
	 * Rolls-back and closes the connection on all SQLException except NoRowException.
	 * Rolls-back the transaction on E.
	 * </p>
	 * <p>
	 * The connection allocated is stored as a ThreadLocal and will be automatically reused if
	 * another transaction is performed within this transaction.  Any nested transaction will automatically
	 * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
	 * entire transaction on any exception.
	 * </p>
	 *
	 * @see #isInTransaction()
	 */
	public <V,E extends Exception> V executeTransaction(
		Class<E> eClass,
		DatabaseCallableE<V,E> callable
	) throws SQLException, E {
		DatabaseConnection conn = transactionConnection.get();
		if(conn!=null) {
			// Reuse existing connection
			try {
				return callable.call(conn);
			} catch(RuntimeException err) {
				conn.rollback();
				throw err;
			} catch(NoRowException err) {
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} catch(Exception e) {
				conn.rollback();
				if(eClass.isInstance(e)) throw eClass.cast(e);
				throw new SQLException(e);
			}
		} else {
			// Create new connection
			conn=createDatabaseConnection();
			try {
				transactionConnection.set(conn);
				try {
					V result = callable.call(conn);
					conn.commit();
					return result;
				} finally {
					transactionConnection.remove();
				}
			} catch(RuntimeException | NoRowException err) {
				conn.rollback();
				throw err;
			} catch(SQLException err) {
				conn.rollbackAndClose();
				throw err;
			} catch(Exception e) {
				conn.rollback();
				if(eClass.isInstance(e)) throw eClass.cast(e);
				throw new SQLException(e);
			} finally {
				conn.releaseConnection();
			}
		}
	}

	@Override
	public String toString() {
		return "Database("+(pool!=null ? pool.toString() : dataSource.toString())+")";
	}

	/**
	 * Whenever a new connection is obtained from the pool or the dataSource,
	 * it is passed here for any initialization routine.
	 * This default implementation does nothing.
	 */
	protected void initConnection(Connection conn) throws SQLException {
		// Do nothing
	}
}
