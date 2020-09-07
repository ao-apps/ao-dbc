/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2018, 2019, 2020  AO Industries, Inc.
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

import com.aoindustries.exception.WrappedException;
import com.aoindustries.sql.AOConnectionPool;
import java.sql.Connection;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.logging.Level;
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
public class Database implements DatabaseAccess {

	/**
	 * Only one connection pool is made to the database.
	 */
	private final AOConnectionPool pool;

	private final DataSource dataSource;
	@SuppressWarnings("NonConstantLogger")
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
	 * Closes the database.
	 *
	 * @see  AOConnectionPool#close()
	 * @see  CloseableDatabase#close()
	 */
	protected void close() {
		if(pool != null) pool.close();
	}

	/**
	 * The custom types discovered via {@link ServiceLoader}.
	 */
	private volatile Map<String,Class<?>> sqlDataTypes;

	/**
	 * Loads the custom types when first needed and caches the results.
	 */
	@SuppressWarnings("ReturnOfCollectionOrArrayField") // Only used within this class
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

	/**
	 * Gets a connection to the database.
	 * The connection will be in {@linkplain Connection#getAutoCommit() auto-commit} mode, and have the given
	 * {@linkplain Connection#getTransactionIsolation() isolation level} and
	 * {@linkplain Connection#isReadOnly() read-only mode}.
	 * <p>
	 * When obtaining a connection from a {@link DataSource}, if the connection is not in
	 * {@linkplain Connection#getAutoCommit() auto-commit} mode, a warning will be logged, then the connection will
	 * be rolled-back and set to auto-commit.
	 * </p>
	 */
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	public Connection getConnection(int isolationLevel, boolean readOnly, int maxConnections) throws SQLException {
		Connection conn;
		Throwable t1 = null;
		if(pool != null) {
			// From pool
			conn = pool.getConnection(isolationLevel, readOnly, maxConnections);
			try {
				assert conn.getAutoCommit();
				assert conn.isReadOnly() == readOnly;
				assert conn.getTransactionIsolation() == isolationLevel;
				initSqlDataTypes(conn);
				initConnection(conn);
			} catch(ThreadDeath td) {
				throw td;
			} catch(Throwable t) {
				t1 = t;
			} finally {
				if(t1 != null) {
					try {
						pool.releaseConnection(conn);
					} catch(ThreadDeath td) {
						throw td;
					} catch(Throwable t) {
						t1.addSuppressed(t);
					}
				}
			}
		} else {
			// From dataSource
			conn = dataSource.getConnection();
			try {
				if(!conn.getAutoCommit()) {
					logger.warning("Rolling-back and setting auto-commit on Connection from DataSource that is not in auto-commit mode");
					conn.rollback();
					conn.setAutoCommit(true);
				}
				if(conn.isReadOnly() != readOnly) conn.setReadOnly(readOnly);
				if(conn.getTransactionIsolation() != isolationLevel) conn.setTransactionIsolation(isolationLevel);
				initSqlDataTypes(conn);
				initConnection(conn);
			} catch(ThreadDeath td) {
				throw td;
			} catch(Throwable t) {
				t1 = t;
			} finally {
				if(t1 != null) {
					try {
						conn.close();
					} catch(ThreadDeath td) {
						throw td;
					} catch(Throwable t) {
						t1.addSuppressed(t);
					}
				}
			}
		}
		if(t1 != null) {
			if(t1 instanceof Error) throw (Error)t1;
			if(t1 instanceof RuntimeException) throw (RuntimeException)t1;
			if(t1 instanceof SQLException) throw (SQLException)t1;
			throw new WrappedException(t1);
		}
		return conn;
	}

	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	public void releaseConnection(Connection conn) throws SQLException {
		Throwable t1 = null;
		// Restore custom types
		Boolean closed = null; // Only call conn.isClosed() once
		try {
			// TODO: Do not remove on release and avoid re-adding for performance?
			Map<String,Class<?>> oldTypeMap = oldTypeMaps.remove(conn);
			if(oldTypeMap != null) {
				if(closed == null) closed = conn.isClosed();
				if(!closed) conn.setTypeMap(oldTypeMap);
			}
		} catch(ThreadDeath td) {
			throw td;
		} catch(Throwable t) {
			if(t1 == null) {
				t1 = t;
			} else {
				t1.addSuppressed(t);
			}
		}
		if(pool != null) {
			// From pool
			pool.releaseConnection(conn);
		} else {
			// From dataSource
			try {
				// Log warnings here, like done by AOConnectionPool.logConnection(Connection)
				if(closed == null) closed = conn.isClosed();
				if(!closed) {
					if(logger.isLoggable(Level.WARNING)) {
						SQLWarning warning = conn.getWarnings();
						if(warning != null) logger.log(Level.WARNING, null, warning);
					}
					conn.clearWarnings();
				}
			} catch(ThreadDeath td) {
				throw td;
			} catch(Throwable t) {
				if(t1 == null) {
					t1 = t;
				} else {
					t1.addSuppressed(t);
				}
			} finally {
				try {
					conn.close();
				} catch(ThreadDeath td) {
					throw td;
				} catch(Throwable t) {
					if(t1 == null) {
						t1 = t;
					} else {
						t1.addSuppressed(t);
					}
				}
			}
		}
		if(t1 != null) {
			if(t1 instanceof Error) throw (Error)t1;
			if(t1 instanceof RuntimeException) throw (RuntimeException)t1;
			if(t1 instanceof SQLException) throw (SQLException)t1;
			throw new SQLException(t1);
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
	public <T,E extends Exception> T executeQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E {
		return executeTransaction(eClass, (DatabaseConnection conn) ->
			conn.executeQuery(isolationLevel, readOnly, eClass, resultSetHandler, sql, params)
		);
	}

	@Override
	public int executeUpdate(String sql, Object ... params) throws SQLException {
		return executeTransaction((DatabaseConnection conn) ->
			conn.executeUpdate(sql, params)
		);
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
	@SuppressWarnings("overloads")
	public void executeTransaction(DatabaseRunnable runnable) throws SQLException {
		executeTransaction(
			RuntimeException.class,
			(DatabaseConnection db) -> {
				runnable.run(db);
				return null;
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
		DatabaseRunnableE<E> runnable
	) throws SQLException, E {
		executeTransaction(
			eClass,
			(DatabaseConnection db) -> {
				runnable.run(db);
				return null;
			}
		);
	}

   /**
	 * @see #executeTransaction(java.lang.Class, com.aoindustries.dbc.DatabaseCallableE)
	 *
	 * @see #isInTransaction()
	 */
	@SuppressWarnings("overloads")
	public <V> V executeTransaction(DatabaseCallable<V> callable) throws SQLException {
		return executeTransaction(
			RuntimeException.class,
			(DatabaseConnection db) -> callable.call(db)
		);
	}

	/**
	 * <p>
	 * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
	 * </p>
	 * <ol>
	 * <li>Returns immediately on {@link ThreadDeath}.</li>
	 * <li>Rolls-back the transaction on {@link Error} or {@link RuntimeException}.</li>
	 * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
	 *     {@link ExtraRowException} on the outer-most transaction only.</li>
	 * <li>Rolls-back and closes the connection on all {@link SQLException} except {@link NoRowException},
	 *     {@link NullDataException}, or {@link ExtraRowException}.</li>
	 * <li>Rolls-back the transaction on {@code E}.</li>
	 * </ol>
	 * <p>
	 * The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
	 * another transaction is performed within this transaction.  Any nested transaction will automatically
	 * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
	 * entire transaction on any exception.
	 * </p>
	 *
	 * @see #isInTransaction()
	 */
	@SuppressWarnings("UseSpecificCatch")
	public <V,E extends Exception> V executeTransaction(
		Class<E> eClass,
		DatabaseCallableE<V,E> callable
	) throws SQLException, E {
		DatabaseConnection conn = transactionConnection.get();
		if(conn != null) {
			// Reuse existing connection
			try {
				return callable.call(conn);
			} catch(ThreadDeath | NoRowException | NullDataException | ExtraRowException e) {
				throw e;
			} catch(Error | RuntimeException e) {
				conn.rollback(e);
				throw e;
			} catch(SQLException err) {
				conn.rollbackAndClose(err);
				throw err;
			} catch(Throwable t) {
				conn.rollback(t);
				if(eClass.isInstance(t)) throw eClass.cast(t);
				throw new WrappedException(t);
			}
		} else {
			// Create new connection
			try (DatabaseConnection newConn = createDatabaseConnection()) {
				try {
					transactionConnection.set(newConn);
					try {
						V result = callable.call(newConn);
						newConn.commit();
						return result;
					} finally {
						transactionConnection.remove();
					}
				} catch(ThreadDeath td) {
					throw td;
				} catch(Error | RuntimeException | NoRowException | NullDataException | ExtraRowException e) {
					newConn.rollback(e);
					throw e;
				} catch(SQLException err) {
					newConn.rollbackAndClose(err);
					throw err;
				} catch(Throwable t) {
					newConn.rollback(t);
					if(eClass.isInstance(t)) throw eClass.cast(t);
					throw new WrappedException(t);
				}
			}
		}
	}

	@Override
	public String toString() {
		return "Database("+(pool!=null ? pool.toString() : dataSource.toString())+")";
	}

	/**
	 * Whenever a new connection is obtained from the pool or the dataSource,
	 * it is passed here for initialization of {@link #getSqlDataTypes()}.
	 */
	protected void initSqlDataTypes(Connection conn) throws SQLException {
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
