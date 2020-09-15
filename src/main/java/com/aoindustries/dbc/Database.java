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

import com.aoindustries.concurrent.Executors;
import com.aoindustries.lang.AutoCloseables;
import com.aoindustries.lang.Throwables;
import com.aoindustries.sql.AOConnectionPool;
import com.aoindustries.sql.Connections;
import com.aoindustries.sql.IUncloseableConnectionWrapper;
import com.aoindustries.sql.UncloseableConnectionWrapper;
import java.sql.Connection;
import java.sql.SQLData;
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import javax.sql.DataSource;

/**
 * Wraps and simplifies access to a JDBC database.  If used directly as a {@link DatabaseAccess} each individual call is a separate transaction.
 * If the current thread is already in a transaction, the calls will be performed using the connection associated with that transaction.
 * For transactions across multiple statements, use {@link DatabaseConnection}.
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

	private final Executors executors = new Executors();

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

	/**
	 * Creates a new {@link DatabaseConnection} instance.  The instance must be closed
	 * via {@link DatabaseConnection#close()} or {@link DatabaseConnection#close(java.lang.Throwable)}.
	 * <p>
	 * Note that the close methods will rollback any transaction in progress, so it is up to the caller to
	 * perform any necessary {@link DatabaseConnection#commit()}.
	 * </p>
	 *
	 * @deprecated  Direct use of this method is discouraged, as this is intended for instantiation and initialization
	 *              of {@link DatabaseConnection} only.  Please use one of the various transaction methods, which
	 *              enforce the transaction semantics.
	 *
	 * @see #transaction(java.lang.Runnable)
	 * @see #transaction(com.aoindustries.dbc.DatabaseRunnable)
	 * @see #transaction(java.lang.Class, com.aoindustries.dbc.DatabaseRunnableE)
	 * @see #transaction(java.util.concurrent.Callable)
	 * @see #transaction(com.aoindustries.dbc.DatabaseCallable)
	 * @see #transaction(java.lang.Class, com.aoindustries.dbc.DatabaseCallableE)
	 *
	 * @see #newDatabaseConnection()
	 */
	@Deprecated
	final public DatabaseConnection createDatabaseConnection() {
		return newDatabaseConnection();
	}

	/**
	 * Creates a new {@link DatabaseConnection} instance.  The instance must be closed
	 * via {@link DatabaseConnection#close()} or {@link DatabaseConnection#close(java.lang.Throwable)}.
	 * <p>
	 * Note that the close methods will rollback any transaction in progress, so it is up to the caller to
	 * perform any necessary {@link DatabaseConnection#commit()}.
	 * </p>
	 */
	protected DatabaseConnection newDatabaseConnection() {
		return new DatabaseConnection(this);
	}

	/**
	 * Gets the pool or {@code null} if using a {@link DataSource}.
	 *
	 * @see  #getDataSource()
	 */
	public AOConnectionPool getConnectionPool() {
		return pool;
	}

	/**
	 * Gets the data source or {@code null} if using an {@link AOConnectionPool}.
	 *
	 * @see  #getConnectionPool()
	 */
	public DataSource getDataSource() {
		return dataSource;
	}

	/**
	 * Gets the executors for this database.  Is {@linkplain Executors#dispose() disposed} on {@link #close()}.
	 */
	Executors getExecutors() {
		return executors;
	}

	/**
	 * Closes the database.
	 *
	 * @see  AOConnectionPool#close()
	 * @see  Executors#dispose()
	 * @see  CloseableDatabase#close()
	 */
	protected void close() {
		if(pool != null) pool.close();
		executors.dispose();
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
			for(SQLData sqlData : ServiceLoader.load(SQLData.class)) {
				newMap.put(sqlData.getSQLTypeName(), sqlData.getClass());
			}
			sqlDataTypes = newMap;
		}
		return sqlDataTypes;
	}

	private final Map<Connection,Map<String,Class<?>>> oldTypeMaps = new IdentityHashMap<>();

	/**
	 * Whenever a new connection is obtained from the pool or the dataSource,
	 * it is passed here for initialization of {@link #getSqlDataTypes()}.
	 *
	 * @see  #deinitSqlDataTypes(java.sql.Connection)
	 * @see  #getConnection(int, boolean, int)
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
	 * Before a connection is release back to the pool or the dataSource,
	 * it is passed here for de-initialization of {@link #getSqlDataTypes()}.
	 *
	 * @see  #initSqlDataTypes(java.sql.Connection)
	 * @see  #releaseConnection(java.sql.Connection)
	 */
	protected void deinitSqlDataTypes(Connection conn) throws SQLException {
		// TODO: Do not remove on release and avoid re-adding for performance?
		Map<String,Class<?>> oldTypeMap = oldTypeMaps.remove(conn);
		if(oldTypeMap != null && !conn.isClosed()) conn.setTypeMap(oldTypeMap);
	}

	/**
	 * Whenever a new connection is obtained from the pool or the dataSource,
	 * it is passed here for any custom initialization routine.
	 * <p>
	 * This default implementation does nothing.
	 * </p>
	 *
	 * @see  #deinitConnection(java.sql.Connection)
	 * @see  #getConnection(int, boolean, int)
	 */
	protected void initConnection(Connection conn) throws SQLException {
		// Do nothing
	}

	/**
	 * Before a connection is released back to the pool or the dataSource,
	 * it is passed here for any custom de-initialization routine.
	 * <p>
	 * This default implementation does nothing.
	 * </p>
	 *
	 * @see  #initConnection(java.sql.Connection)
	 * @see  #releaseConnection(java.sql.Connection)
	 */
	protected void deinitConnection(Connection conn) throws SQLException {
		// Do nothing
	}

	/**
	 * Gets a read/write connection to the database with a transaction level of
	 * {@link Connections#DEFAULT_TRANSACTION_ISOLATION},
	 * warning when a connection is already used by this thread.
	 * <p>
	 * The connection will be in auto-commit mode, as configured by {@link AOConnectionPool#resetConnection(java.sql.Connection)}
	 * (or compatible {@link DataSource} implementation via {@link AOConnectionPool#defaultResetConnection(java.sql.Connection)}).
	 * </p>
	 * <p>
	 * When obtaining a connection from a {@link DataSource}, if the connection is not in
	 * {@linkplain Connection#getAutoCommit() auto-commit} mode, a warning will be logged, then the connection will
	 * be rolled-back and set to auto-commit.
	 * </p>
	 * <p>
	 * If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
	 * available.
	 * </p>
	 *
	 * @return  The read/write connection to the database
	 *
	 * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
	 *
	 * @see  #getConnection(int, boolean, int)
	 * @see  DatabaseConnection#getConnection()
	 * @see  Connection#close()
	 */
	// Note: Matches AOPool.getConnection()
	// Note: Matches AOConnectionPool.getConnection()
	// Note:      Is Database.getConnection()
	// Note: Matches DatabaseConnection.getConnection()
	public Connection getConnection() throws SQLException {
		return getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, 1);
	}

	/**
	 * Gets a read/write connection to the database with a transaction level of
	 * {@link Connections#DEFAULT_TRANSACTION_ISOLATION}.
	 * <p>
	 * The connection will be in auto-commit mode, as configured by {@link AOConnectionPool#resetConnection(java.sql.Connection)}
	 * (or compatible {@link DataSource} implementation via {@link AOConnectionPool#defaultResetConnection(java.sql.Connection)}).
	 * </p>
	 * <p>
	 * When obtaining a connection from a {@link DataSource}, if the connection is not in
	 * {@linkplain Connection#getAutoCommit() auto-commit} mode, a warning will be logged, then the connection will
	 * be rolled-back and set to auto-commit.
	 * </p>
	 * <p>
	 * If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
	 * available.
	 * </p>
	 *
	 * @param  maxConnections  The maximum number of connections expected to be used by the current thread.
	 *                         This should normally be one to avoid potential deadlock.
	 *                         <p>
	 *                         The connection will continue to be considered used by the allocating thread until
	 *                         released (via {@link Connection#close()}, even if the connection is shared by another
	 *                         thread.
	 *                         </p>
	 *
	 * @return  The read/write connection to the database
	 *
	 * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
	 *
	 * @see  #getConnection(int, boolean, int)
	 * @see  DatabaseConnection#getConnection(int)
	 * @see  Connection#close()
	 */
	// Note: Matches AOPool.getConnection(int)
	// Note: Matches AOConnectionPool.getConnection(int)
	// Note:      Is Database.getConnection(int)
	// Note: Matches DatabaseConnection.getConnection(int)
	public Connection getConnection(int maxConnections) throws SQLException {
		return getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, maxConnections);
	}

	/**
	 * Gets a connection to the database with a transaction level of
	 * {@link Connections#DEFAULT_TRANSACTION_ISOLATION},
	 * warning when a connection is already used by this thread.
	 * <p>
	 * The connection will be in auto-commit mode, as configured by {@link AOConnectionPool#resetConnection(java.sql.Connection)}
	 * (or compatible {@link DataSource} implementation via {@link AOConnectionPool#defaultResetConnection(java.sql.Connection)}).
	 * </p>
	 * <p>
	 * When obtaining a connection from a {@link DataSource}, if the connection is not in
	 * {@linkplain Connection#getAutoCommit() auto-commit} mode, a warning will be logged, then the connection will
	 * be rolled-back and set to auto-commit.
	 * </p>
	 * <p>
	 * If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
	 * available.
	 * </p>
	 *
	 * @param  readOnly  The {@link Connection#setReadOnly(boolean) read-only flag}
	 *
	 * @return  The connection to the database
	 *
	 * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
	 *
	 * @see  #getConnection(int, boolean, int)
	 * @see  DatabaseConnection#getConnection(boolean)
	 * @see  Connection#close()
	 */
	// Note: Matches AOConnectionPool.getConnection(boolean)
	// Note:      Is Database.getConnection(boolean)
	// Note: Matches DatabaseConnection.getConnection(boolean)
	public Connection getConnection(boolean readOnly) throws SQLException {
		return getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, readOnly, 1);
	}

	/**
	 * Gets a connection to the database,
	 * warning when a connection is already used by this thread.
	 * <p>
	 * The connection will be in auto-commit mode, as configured by {@link AOConnectionPool#resetConnection(java.sql.Connection)}
	 * (or compatible {@link DataSource} implementation via {@link AOConnectionPool#defaultResetConnection(java.sql.Connection)}).
	 * </p>
	 * <p>
	 * When obtaining a connection from a {@link DataSource}, if the connection is not in
	 * {@linkplain Connection#getAutoCommit() auto-commit} mode, a warning will be logged, then the connection will
	 * be rolled-back and set to auto-commit.
	 * </p>
	 * <p>
	 * If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
	 * available.
	 * </p>
	 *
	 * @param  isolationLevel  The {@link Connection#setTransactionIsolation(int) transaction isolation level}
	 *
	 * @param  readOnly        The {@link Connection#setReadOnly(boolean) read-only flag}
	 *
	 * @return  The connection to the database
	 *
	 * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
	 *
	 * @see  #getConnection(int, boolean, int)
	 * @see  DatabaseConnection#getConnection(int, boolean)
	 * @see  Connection#close()
	 */
	// Note: Matches AOConnectionPool.getConnection(int, boolean)
	// Note:      Is Database.getConnection(int, boolean)
	// Note: Matches DatabaseConnection.getConnection(int, boolean)
	public Connection getConnection(int isolationLevel, boolean readOnly) throws SQLException {
		return getConnection(isolationLevel, readOnly, 1);
	}

	/**
	 * Gets a connection to the database.
	 * <p>
	 * The connection will be in auto-commit mode, as configured by {@link AOConnectionPool#resetConnection(java.sql.Connection)}
	 * (or compatible {@link DataSource} implementation via {@link AOConnectionPool#defaultResetConnection(java.sql.Connection)}).
	 * </p>
	 * <p>
	 * When obtaining a connection from a {@link DataSource}, if the connection is not in
	 * {@linkplain Connection#getAutoCommit() auto-commit} mode, a warning will be logged, then the connection will
	 * be rolled-back and set to auto-commit.
	 * </p>
	 * <p>
	 * If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
	 * available.
	 * </p>
	 *
	 * @param  isolationLevel  The {@link Connection#setTransactionIsolation(int) transaction isolation level}
	 *
	 * @param  readOnly        The {@link Connection#setReadOnly(boolean) read-only flag}
	 *
	 * @param  maxConnections  The maximum number of connections expected to be used by the current thread.
	 *                         This should normally be one to avoid potential deadlock.
	 *                         <p>
	 *                         The connection will continue to be considered used by the allocating thread until
	 *                         released (via {@link Connection#close()}, even if the connection is shared by another
	 *                         thread.
	 *                         </p>
	 *
	 * @return  The connection to the database
	 *
	 * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
	 *
	 * @see  DatabaseConnection#getConnection(int, boolean, int)
	 * @see  Connection#close()
	 */
	// Note: Matches AOConnectionPool.getConnection(int, boolean, int)
	// Note:      Is Database.getConnection(int, boolean, int)
	// Note: Matches DatabaseConnection.getConnection(int, boolean, int)
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	public Connection getConnection(int isolationLevel, boolean readOnly, int maxConnections) throws SQLException {
		DatabaseConnectionWrapper conn;
		if(pool != null) {
			// From pool
			conn = new DatabaseConnectionWrapper(this, pool.getConnection(isolationLevel, readOnly, maxConnections));
			try {
				assert conn.getAutoCommit();
				assert conn.isReadOnly() == readOnly;
				assert conn.getTransactionIsolation() == isolationLevel;
				initSqlDataTypes(conn);
				initConnection(conn);
			} catch(Throwable t) {
				throw AutoCloseables.closeAndWrap(t, SQLException.class, SQLException::new, conn);
			}
		} else {
			// From dataSource
			conn = new DatabaseConnectionWrapper(this, dataSource.getConnection());
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
			} catch(Throwable t) {
				throw AutoCloseables.closeAndWrap(t, SQLException.class, SQLException::new, conn);
			}
		}
		return conn;
	}

	private static interface IDatabaseConnectionWrapper extends IUncloseableConnectionWrapper {
		Database getDatabase();
	}

	private static class DatabaseConnectionWrapper extends UncloseableConnectionWrapper
		implements IDatabaseConnectionWrapper {

		private final Database database;

		private DatabaseConnectionWrapper(Database database, Connection wrapped) {
			super(wrapped);
			this.database = database;
		}

		@Override
		public Database getDatabase() {
			return database;
		}

		@Override
		@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
		public void onAbort(Executor executor) throws SQLException {
			Throwable t0 = null;
			try {
				super.onAbort(executor);
			} catch(Throwable t) {
				t0 = Throwables.addSuppressed(t0, t);
			}
			try {
				database.release(this);
			} catch(Throwable t) {
				t0 = Throwables.addSuppressed(t0, t);
			}
			if(t0 != null) {
				throw Throwables.wrap(t0, SQLException.class, SQLException::new);
			}
		}

		@Override
		public void onClose() throws SQLException {
			database.release(this);
		}
	}

	@SuppressWarnings("null")
	private Connection unwrap(Connection conn) throws SQLException {
		IDatabaseConnectionWrapper wrapper;
		if(conn instanceof IDatabaseConnectionWrapper) {
			wrapper = (IDatabaseConnectionWrapper)conn;
		} else {
			wrapper = conn.unwrap(IDatabaseConnectionWrapper.class);
		}
		if(wrapper.getDatabase() == this) {
			return wrapper.getWrappedConnection();
		} else {
			throw new SQLException("Connection from a different database, cannot unwrap");
		}
	}

	/**
	 * @deprecated  Please release to the pool by {@linkplain Connection#close() closing the connection},
	 *              preferably via try-with-resources.
	 */
	@Deprecated
	final public void releaseConnection(Connection conn) throws SQLException {
		release(conn);
	}

	/**
	 * Closes and/or releases the given connection back to the pool.
	 * Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
	 *
	 * @see  #getConnection(int, boolean, int)
	 * @see  DatabaseConnection#close()
	 * @see  #deinitConnection(java.sql.Connection)
	 * @see  #deinitSqlDataTypes(java.sql.Connection)
	 */
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	protected void release(Connection conn) throws SQLException {
		Throwable t0 = null;
		// Perform custom de-initialization
		try {
			deinitConnection(conn);
		} catch(Throwable t) {
			t0 = Throwables.addSuppressed(t0, t);
		}
		// Restore custom types
		try {
			deinitSqlDataTypes(conn);
		} catch(Throwable t) {
			t0 = Throwables.addSuppressed(t0, t);
		}
		// Unwrap
		try {
			conn = unwrap(conn);
		} catch(Throwable t) {
			t0 = Throwables.addSuppressed(t0, t);
		}
		if(pool == null) {
			// From dataSource, perform some clean-up consistent with AOConnectionPool
			try {
				if(!conn.isClosed()) {
					// Log warnings before release and/or close
					try {
						AOConnectionPool.defaultLogConnection(conn, logger);
					} catch(Throwable t) {
						t0 = Throwables.addSuppressed(t0, t);
					}
					// Reset connections as they are released
					try {
						AOConnectionPool.defaultResetConnection(conn);
					} catch(Throwable t) {
						t0 = Throwables.addSuppressed(t0, t);
					}
				}
			} catch(Throwable t) {
				// isClosed() failed, fall-through to continue close
				t0 = Throwables.addSuppressed(t0, t);
			}
		}
		AutoCloseables.closeAndThrow(t0, SQLException.class, SQLException::new, conn);
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

	private final ThreadLocal<DatabaseConnection> transactionConnection = new ThreadLocal<>();

	/**
	 * Checks if the current thread is in a transaction.
	 *
	 * @see #transaction(java.lang.Runnable)
	 * @see #transaction(com.aoindustries.dbc.DatabaseRunnable)
	 * @see #transaction(java.lang.Class, com.aoindustries.dbc.DatabaseRunnableE)
	 * @see #transaction(java.util.concurrent.Callable)
	 * @see #transaction(com.aoindustries.dbc.DatabaseCallable)
	 * @see #transaction(java.lang.Class, com.aoindustries.dbc.DatabaseCallableE)
	 */
	public boolean isInTransaction() {
		return transactionConnection.get()!=null;
	}

	/**
	 * <p>
	 * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
	 * </p>
	 * <ol>
	 * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
	 *     {@link ExtraRowException} on the outer-most transaction only.</li>
	 * <li>Rolls-back and closes the connection on all {@link SQLException} except {@link NoRowException},
	 *     {@link NullDataException}, or {@link ExtraRowException}.</li>
	 * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
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
	@SuppressWarnings("overloads")
	public void transaction(Runnable runnable) throws SQLException {
		transaction((DatabaseConnection db) -> runnable.run());
	}

	// TODO: RunnableE

	// TODO: transactionR and transactionC or run/runE/call/callE/supply like ThreadLocale

	/**
	 * <p>
	 * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
	 * </p>
	 * <ol>
	 * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
	 *     {@link ExtraRowException} on the outer-most transaction only.</li>
	 * <li>Rolls-back and closes the connection on all {@link SQLException} except {@link NoRowException},
	 *     {@link NullDataException}, or {@link ExtraRowException}.</li>
	 * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
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
	@SuppressWarnings("overloads")
	public void transaction(DatabaseRunnable runnable) throws SQLException {
		transaction(RuntimeException.class, runnable::run);
	}

	/**
	 * @deprecated  Please use {@link #transaction(com.aoindustries.dbc.DatabaseRunnable)}
	 */
	@Deprecated
	@SuppressWarnings("overloads")
	final public void executeTransaction(DatabaseRunnable runnable) throws SQLException {
		transaction(runnable);
	}

	/**
	 * <p>
	 * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
	 * </p>
	 * <ol>
	 * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
	 *     {@link ExtraRowException} on the outer-most transaction only.</li>
	 * <li>Rolls-back and closes the connection on all {@link SQLException} except {@link NoRowException},
	 *     {@link NullDataException}, or {@link ExtraRowException}.</li>
	 * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
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
	@SuppressWarnings("overloads")
	public <E extends Throwable> void transaction(Class<? extends E> eClass, DatabaseRunnableE<? extends E> runnable) throws SQLException, E {
		transaction(
			eClass,
			(DatabaseConnection db) -> {
				runnable.run(db);
				return null;
			}
		);
	}

	/**
	 * @deprecated  Please use {@link #transaction(java.lang.Class, com.aoindustries.dbc.DatabaseRunnableE)}
	 */
	@Deprecated
	@SuppressWarnings("overloads")
	final public <E extends Exception> void executeTransaction(Class<E> eClass, DatabaseRunnableE<E> runnable) throws SQLException, E {
		transaction(eClass, runnable);
	}

	/**
	 * <p>
	 * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
	 * </p>
	 * <ol>
	 * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
	 *     {@link ExtraRowException} on the outer-most transaction only.</li>
	 * <li>Rolls-back and closes the connection on all {@link SQLException} except {@link NoRowException},
	 *     {@link NullDataException}, or {@link ExtraRowException}.</li>
	 * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
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
	@SuppressWarnings({"overloads", "UseSpecificCatch", "TooBroadCatch"})
	public <V> V transaction(Callable<? extends V> callable) throws SQLException {
		return transaction((DatabaseConnection db) -> {
			try {
				return callable.call();
			} catch(Throwable t) {
				throw Throwables.wrap(t, SQLException.class, SQLException::new);
			}
		});
	}

	// TODO: CallableE

	/**
	 * <p>
	 * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
	 * </p>
	 * <ol>
	 * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
	 *     {@link ExtraRowException} on the outer-most transaction only.</li>
	 * <li>Rolls-back and closes the connection on all {@link SQLException} except {@link NoRowException},
	 *     {@link NullDataException}, or {@link ExtraRowException}.</li>
	 * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
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
	@SuppressWarnings("overloads")
	public <V> V transaction(DatabaseCallable<? extends V> callable) throws SQLException {
		return transaction(RuntimeException.class, callable::call);
	}

	/**
	 * @deprecated  Please use {@link #transaction(com.aoindustries.dbc.DatabaseCallable)}
	 */
	@Deprecated
	@SuppressWarnings("overloads")
	final public <V> V executeTransaction(DatabaseCallable<V> callable) throws SQLException {
		return transaction(callable);
	}

	/**
	 * <p>
	 * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
	 * </p>
	 * <ol>
	 * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
	 *     {@link ExtraRowException} on the outer-most transaction only.</li>
	 * <li>Rolls-back and closes the connection on all {@link SQLException} except {@link NoRowException},
	 *     {@link NullDataException}, or {@link ExtraRowException}.</li>
	 * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
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
	@SuppressWarnings({"UseSpecificCatch", "overloads"})
	public <V,E extends Throwable> V transaction(Class<? extends E> eClass, DatabaseCallableE<? extends V,? extends E> callable) throws SQLException, E {
		Throwable t0 = null;
		DatabaseConnection conn = transactionConnection.get();
		if(conn != null) {
			// Reuse existing connection
			try {
				return callable.call(conn);
			} catch(NoRowException | NullDataException | ExtraRowException e) {
				throw e;
			} catch(SQLException e) {
				t0 = Throwables.addSuppressed(t0, e);
				t0 = conn.rollbackAndClose(t0);
			} catch(Throwable t) {
				t0 = Throwables.addSuppressed(t0, t);
				t0 = conn.rollback(t0);
			}
		} else {
			// Create new connection
			try (DatabaseConnection newConn = newDatabaseConnection()) {
				try {
					transactionConnection.set(newConn);
					try {
						V result = callable.call(newConn);
						newConn.commit();
						return result;
					} finally {
						transactionConnection.remove();
					}
				} catch(NoRowException | NullDataException | ExtraRowException e) {
					t0 = Throwables.addSuppressed(t0, e);
					t0 = newConn.rollback(t0);
				} catch(SQLException e) {
					t0 = Throwables.addSuppressed(t0, e);
					t0 = newConn.rollbackAndClose(t0);
				} catch(Throwable t) {
					t0 = Throwables.addSuppressed(t0, t);
					t0 = newConn.rollback(t0);
				}
			}
		}
		assert t0 != null;
		if(eClass.isInstance(t0)) throw eClass.cast(t0);
		throw Throwables.wrap(t0, SQLException.class, SQLException::new);
	}

	/**
	 * @deprecated  Please use {@link #transaction(java.lang.Class, com.aoindustries.dbc.DatabaseCallableE)}
	 */
	@Deprecated
	@SuppressWarnings("overloads")
	final public <V,E extends Exception> V executeTransaction(Class<E> eClass, DatabaseCallableE<V,E> callable) throws SQLException, E {
		return transaction(eClass, callable);
	}

	@Override
	public String toString() {
		return "Database("+(pool!=null ? pool.toString() : dataSource.toString())+")";
	}

	@Override
	public DoubleStream doubleStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
		return transaction((DatabaseConnection conn) ->
			conn.doubleStream(isolationLevel, readOnly, sql, params)
		);
	}

	@Override
	public IntStream intStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
		return transaction((DatabaseConnection conn) ->
			conn.intStream(isolationLevel, readOnly, sql, params)
		);
	}

	@Override
	public LongStream longStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
		return transaction((DatabaseConnection conn) ->
			conn.longStream(isolationLevel, readOnly, sql, params)
		);
	}

	@Override
	public <T,E extends Throwable> Stream<T> stream(int isolationLevel, boolean readOnly, Class<? extends E> eClass, ObjectFactoryE<? extends T,? extends E> objectFactory, String sql, Object ... params) throws SQLException, E {
		return transaction(eClass, (DatabaseConnection conn) ->
			conn.stream(isolationLevel, readOnly, eClass, objectFactory, sql, params)
		);
	}

	@Override
	public <T,E extends Throwable> T query(int isolationLevel, boolean readOnly, Class<? extends E> eClass, ResultSetCallableE<? extends T,? extends E> resultSetCallable, String sql, Object ... params) throws SQLException, E {
		return transaction(eClass, (DatabaseConnection conn) ->
			conn.query(isolationLevel, readOnly, eClass, resultSetCallable, sql, params)
		);
	}

	@Override
	public int update(String sql, Object ... params) throws SQLException {
		return transaction((DatabaseConnection conn) ->
			conn.update(sql, params)
		);
	}
}
