/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2018, 2019, 2020, 2021, 2022  AO Industries, Inc.
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
 * along with ao-dbc.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aoapps.dbc;

import com.aoapps.collections.AoCollections;
import com.aoapps.concurrent.Executors;
import com.aoapps.lang.AutoCloseables;
import com.aoapps.lang.RunnableE;
import com.aoapps.lang.Throwables;
import com.aoapps.lang.concurrent.CallableE;
import com.aoapps.sql.Connections;
import com.aoapps.sql.failfast.FailFastConnection;
import com.aoapps.sql.failfast.FailFastConnectionImpl;
import com.aoapps.sql.pool.AOConnectionPool;
import com.aoapps.sql.tracker.ConnectionTracker;
import com.aoapps.sql.tracker.ConnectionTrackerImpl;
import java.sql.Connection;
import java.sql.SQLData;
import java.sql.SQLException;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.logging.Level;
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
// TODO: Could we leverage savepoints for nested transactions?
public class Database implements DatabaseAccess {

  /**
   * The number of seconds to wait while validating a connection that was closed with a non-terminal fail-fast state.
   */
  private static final int VALIDATION_TIMEOUT_SECONDS = 10;

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
    if (pool == null) {
      throw new IllegalArgumentException("pool == null");
    }
    this.pool = pool;
    this.dataSource = null;
    this.logger = null;
  }

  public Database(DataSource dataSource, Logger logger) {
    if (dataSource == null) {
      throw new IllegalArgumentException("dataSource == null");
    }
    if (logger == null) {
      throw new IllegalArgumentException("logger == null");
    }
    this.pool = null;
    this.dataSource = dataSource;
    this.logger = logger;
  }

  /**
   * @deprecated  Please use either {@link #connect()} or one of the various call/run methods, which enforce
   *              the transaction semantics.
   */
  @Deprecated(forRemoval = true)
  public final DatabaseConnection createDatabaseConnection() {
    return connect();
  }

  /**
   * Creates a new {@link DatabaseConnection} instance.  The instance must be closed
   * via {@link DatabaseConnection#close()} or {@link DatabaseConnection#close(java.lang.Throwable)}, typically in a
   * try/catch or try-with-resources block.
   * <p>
   * Note that the close methods will rollback any transaction in progress, so it is up to the caller to
   * perform any necessary {@link DatabaseConnection#commit()}.
   * </p>
   *
   * @see #transactionCall(com.aoapps.lang.concurrent.CallableE)
   * @see #transactionCall(java.lang.Class, com.aoapps.lang.concurrent.CallableE)
   * @see #transactionCall(com.aoapps.dbc.DatabaseCallable)
   * @see #transactionCall(java.lang.Class, com.aoapps.dbc.DatabaseCallableE)
   * @see #transactionRun(com.aoapps.lang.RunnableE)
   * @see #transactionRun(java.lang.Class, com.aoapps.lang.RunnableE)
   * @see #transactionRun(com.aoapps.dbc.DatabaseRunnable)
   * @see #transactionRun(java.lang.Class, com.aoapps.dbc.DatabaseRunnableE)
   */
  public DatabaseConnection connect() {
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
   * Gets the executors for this database.  Is {@linkplain Executors#close() closed} on {@link #close()}.
   */
  Executors getExecutors() {
    return executors;
  }

  /**
   * Closes the database.
   *
   * @see  AOConnectionPool#close()
   * @see  Executors#close()
   * @see  CloseableDatabase#close()
   */
  protected void close() {
    if (pool != null) {
      pool.close();
    }
    executors.close();
  }

  /**
   * The custom types discovered via {@link ServiceLoader}.
   */
  private volatile Map<String, Class<?>> sqlDataTypes;

  /**
   * Loads the custom types when first needed and caches the results.
   */
  @SuppressWarnings("ReturnOfCollectionOrArrayField") // Only used within this class
  private Map<String, Class<?>> getSqlDataTypes() throws SQLException {
    if (sqlDataTypes == null) {
      // Load custom types from ServiceLoader
      Map<String, Class<?>> newMap = new LinkedHashMap<>();
      for (SQLData sqlData : ServiceLoader.load(SQLData.class)) {
        newMap.put(sqlData.getSQLTypeName(), sqlData.getClass());
      }
      sqlDataTypes = newMap;
    }
    return sqlDataTypes;
  }

  private final Map<Connection, Map<String, Class<?>>> oldTypeMaps = new IdentityHashMap<>();

  /**
   * Whenever a new connection is obtained from the pool or the dataSource,
   * it is passed here for initialization of {@link #getSqlDataTypes()}.
   *
   * @see  #deinitSqlDataTypes(java.sql.Connection)
   * @see  #getConnection(int, boolean, int, boolean)
   */
  protected void initSqlDataTypes(Connection conn) throws SQLException {
    // Load custom types from ServiceLoader
    Map<String, Class<?>> newTypes = getSqlDataTypes();
    int size = newTypes.size();
    if (size != 0) {
      Map<String, Class<?>> typeMap = conn.getTypeMap();
      // Note: We get "null" back from PostgreSQL driver, despite documentation of returning empty
      if (typeMap == null) {
        typeMap = AoCollections.newLinkedHashMap(size);
      }
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
   * @see  #release(com.aoapps.sql.failfast.FailFastConnection)
   */
  protected void deinitSqlDataTypes(Connection conn) throws SQLException {
    // TODO: Do not remove on release and avoid re-adding for performance?
    Map<String, Class<?>> oldTypeMap = oldTypeMaps.remove(conn);
    if (oldTypeMap != null && !conn.isClosed()) {
      conn.setTypeMap(oldTypeMap);
    }
  }

  /**
   * Whenever a new connection is obtained from the pool or the dataSource,
   * it is passed here for any custom initialization routine.
   * <p>
   * This default implementation does nothing.
   * </p>
   *
   * @see  #deinitConnection(java.sql.Connection)
   * @see  #getConnection(int, boolean, int, boolean)
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
   * @see  #release(com.aoapps.sql.failfast.FailFastConnection)
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
   * <p>
   * The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.
   * </p>
   * <p>
   * The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.
   * </p>
   *
   * @return  The read/write connection to the database
   *
   * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
   *
   * @see  #getConnection(int, boolean, int, boolean)
   * @see  DatabaseConnection#getConnection()
   * @see  Connection#close()
   */
  // Note: Matches AOPool.getConnection()
  // Note: Matches AOConnectionPool.getConnection()
  // Note:      Is Database.getConnection()
  // Note: Matches DatabaseConnection.getConnection()
  public Connection getConnection() throws SQLException {
    return getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, 1, true);
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
   * <p>
   * The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.
   * </p>
   * <p>
   * The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.
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
   * @see  #getConnection(int, boolean, int, boolean)
   * @see  DatabaseConnection#getConnection(int)
   * @see  Connection#close()
   */
  // Note: Matches AOPool.getConnection(int)
  // Note: Matches AOConnectionPool.getConnection(int)
  // Note:      Is Database.getConnection(int)
  // Note: Matches DatabaseConnection.getConnection(int)
  public Connection getConnection(int maxConnections) throws SQLException {
    return getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, maxConnections, true);
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
   * <p>
   * The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.
   * </p>
   * <p>
   * The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.
   * </p>
   *
   * @param  readOnly  The {@link Connection#setReadOnly(boolean) read-only flag}
   *
   * @return  The connection to the database
   *
   * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
   *
   * @see  #getConnection(int, boolean, int, boolean)
   * @see  DatabaseConnection#getConnection(boolean)
   * @see  Connection#close()
   */
  // Note: Matches AOConnectionPool.getConnection(boolean)
  // Note:      Is Database.getConnection(boolean)
  // Note: Matches DatabaseConnection.getConnection(boolean)
  public Connection getConnection(boolean readOnly) throws SQLException {
    return getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, readOnly, 1, true);
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
   * <p>
   * The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.
   * </p>
   * <p>
   * The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.
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
   * @see  #getConnection(int, boolean, int, boolean)
   * @see  DatabaseConnection#getConnection(int, boolean)
   * @see  Connection#close()
   */
  // Note: Matches AOConnectionPool.getConnection(int, boolean)
  // Note:      Is Database.getConnection(int, boolean)
  // Note: Matches DatabaseConnection.getConnection(int, boolean)
  public Connection getConnection(int isolationLevel, boolean readOnly) throws SQLException {
    return getConnection(isolationLevel, readOnly, 1, true);
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
   * <p>
   * The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.
   * </p>
   * <p>
   * The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.
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
  public Connection getConnection(int isolationLevel, boolean readOnly, int maxConnections) throws SQLException {
    return getConnection(isolationLevel, readOnly, maxConnections, true);
  }

  @SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
  protected FailFastConnection getConnection(int isolationLevel, boolean readOnly, int maxConnections, boolean allowClose) throws SQLException {
    Connection conn;
    if (pool != null) {
      // From pool, which already internally uses tracker
      conn = pool.getConnection(isolationLevel, readOnly, maxConnections);
      try {
        assert conn.getAutoCommit();
        assert conn.isReadOnly() == readOnly;
        assert conn.getTransactionIsolation() == isolationLevel;
        initSqlDataTypes(conn);
        initConnection(conn);
      } catch (Throwable t) {
        throw AutoCloseables.closeAndWrap(t, SQLException.class, SQLException::new, conn);
      }
    } else {
      // From dataSource, adding tracker   TODO: Does DBCP do the full equivalent of ConnectionTracker already?
      conn = new ConnectionTrackerImpl(dataSource.getConnection());
      try {
        if (!conn.getAutoCommit()) {
          logger.warning("Rolling-back and setting auto-commit on Connection from DataSource that is not in auto-commit mode");
          conn.rollback();
          conn.setAutoCommit(true);
        }
        if (conn.isReadOnly() != readOnly) {
          conn.setReadOnly(readOnly);
        }
        if (conn.getTransactionIsolation() != isolationLevel) {
          conn.setTransactionIsolation(isolationLevel);
        }
        initSqlDataTypes(conn);
        initConnection(conn);
      } catch (Throwable t) {
        throw AutoCloseables.closeAndWrap(t, SQLException.class, SQLException::new, conn);
      }
    }
    return new PooledConnectionImpl(this, conn, allowClose);
  }

  private static interface PooledConnection extends FailFastConnection {
    Database getDatabase();
  }

  private static class PooledConnectionImpl extends FailFastConnectionImpl
      implements PooledConnection {

    private final Database database;
    private final boolean allowClose;

    private PooledConnectionImpl(Database database, Connection wrapped, boolean allowClose) {
      super(wrapped);
      this.database = database;
      this.allowClose = allowClose;
    }

    @Override
    public Database getDatabase() {
      return database;
    }

    /**
     * Conditionally ignores close for use in try-with-resources from {@link DatabaseConnection}.
     */
    @Override
    public void close() throws SQLException {
      if (allowClose) {
        database.release(this);
      }
    }

    @Override
    protected void doClose(Throwable failFastCause) {
      throw new AssertionError("Should not be called since close() intercepted");
    }

    @Override
    @SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
    public void abort(Executor executor) throws SQLException {
      Throwable t0 = null;
      try {
        getWrapped().abort(executor);
      } catch (Throwable t) {
        t0 = Throwables.addSuppressed(t0, t);
      }
      try {
        database.release(this);
      } catch (Throwable t) {
        t0 = Throwables.addSuppressed(t0, t);
      }
      if (t0 != null) {
        throw Throwables.wrap(t0, SQLException.class, SQLException::new);
      }
    }

    @Override
    protected void doAbort(Throwable failFastCause, Executor executor) {
      throw new AssertionError("Should not be called since abort(Executor) intercepted");
    }
  }

  @SuppressWarnings("null")
  private Connection unwrap(FailFastConnection conn) throws SQLException {
    PooledConnection wrapper;
    if (conn instanceof PooledConnection) {
      wrapper = (PooledConnection) conn;
    } else {
      wrapper = conn.unwrap(PooledConnection.class);
    }
    if (wrapper.getDatabase() == this) {
      return wrapper.getWrapped();
    } else {
      throw new SQLException("Connection from a different database, cannot unwrap");
    }
  }

  /**
   * @deprecated  Please release to the pool by {@linkplain Connection#close() closing the connection},
   *              preferably via try-with-resources.
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("null")
  public final void releaseConnection(Connection conn) throws SQLException {
    FailFastConnection failFastConnection;
    if (conn instanceof FailFastConnection) {
      failFastConnection = (FailFastConnection) conn;
    } else {
      failFastConnection = conn.unwrap(FailFastConnection.class);
    }
    release(failFastConnection);
  }

  /**
   * Closes and/or releases the given connection back to the pool.
   * Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
   * <p>
   * When there is no {@linkplain FailFastConnection#getFailFastCause() fail-fast cause}, the connection is immediately
   * deinit'ed then {@linkplain Connection#close() closed} (which returns the connection to the underlying pool).
   * </p>
   * <p>
   * Otherwise, when there remains an unresolved {@linkplain FailFastConnection#getFailFastCause() fail-fast cause}, the
   * connection is {@linkplain Connection#isValid(int) validated} in the background before being either
   * {@linkplain Connection#abort(java.util.concurrent.Executor) aborted} or deinit'ed then {@linkplain Connection#close() closed}
   * (both of which return the connection to the underlying pool).
   * </p>
   *
   * @see  #getConnection(int, boolean, int, boolean)
   * @see  DatabaseConnection#close()
   * @see  #deinitConnection(java.sql.Connection)
   * @see  #deinitSqlDataTypes(java.sql.Connection)
   */
  @SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
  protected void release(FailFastConnection conn) throws SQLException {
    // Unwrap first, to thrown exception when can't unwrap
    Connection wrapped = unwrap(conn);
    // All exceptions will be combined via addSuppressed
    Throwable t0 = null;
    // Rollback any transaction in-progress first for fail-fast state to have a chance to clear itself
    try {
      if (!conn.isClosed() && !conn.getAutoCommit()) {
        conn.rollback();
        conn.setAutoCommit(true);
      }
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    // Perform appropriate release by fail-fast state
    if (conn.getFailFastState() != FailFastConnection.State.OK) {
      // Validate connection in the background, then abort or close based on if connection still valid
      Executor unbounded = executors.getUnbounded();
      unbounded.execute(() -> {
        Throwable t1 = null;
        boolean valid;
        try {
          valid = wrapped.isValid(VALIDATION_TIMEOUT_SECONDS);
        } catch (Throwable t) {
          t1 = Throwables.addSuppressed(t1, t);
          valid = false;
        }
        if (valid) {
          t1 = closeWrappedConnection(t1, wrapped);
        } else {
          try {
            wrapped.abort(unbounded);
          } catch (Throwable t) {
            t1 = Throwables.addSuppressed(t1, t);
          }
        }
        if (t1 != null) {
          logger.log(Level.WARNING, "Background connection validation failed", t1);
        }
      });
    } else {
      t0 = closeWrappedConnection(t0, wrapped);
    }
    if (t0 != null) {
      throw Throwables.wrap(t0, SQLException.class, SQLException::new);
    }
  }

  /**
   * Performs the deinit and close of the wrapped connection.
   *
   * @see  #release(com.aoapps.sql.failfast.FailFastConnection)
   */
  @SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
  private Throwable closeWrappedConnection(Throwable t0, Connection wrapped) {
    // Perform custom de-initialization
    try {
      deinitConnection(wrapped);
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    // Restore custom types
    try {
      deinitSqlDataTypes(wrapped);
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    if (pool == null) {
      // From dataSource, perform some clean-up consistent with AOConnectionPool
      try {
        if (!wrapped.isClosed()) {
          // Log warnings before release and/or close
          try {
            AOConnectionPool.defaultLogConnection(wrapped, logger);
          } catch (Throwable t) {
            t0 = Throwables.addSuppressed(t0, t);
          }
          // Reset connections as they are released
          try {
            AOConnectionPool.defaultResetConnection(wrapped);
          } catch (Throwable t) {
            t0 = Throwables.addSuppressed(t0, t);
          }
        }
      } catch (Throwable t) {
        // isClosed() failed, fall-through to continue close
        t0 = Throwables.addSuppressed(t0, t);
      }
    }
    t0 = AutoCloseables.closeAndCatch(t0, wrapped);
    return t0;
  }

  public Logger getLogger() {
    if (pool != null) {
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
   * @see #transactionCall(com.aoapps.lang.concurrent.CallableE)
   * @see #transactionCall(java.lang.Class, com.aoapps.lang.concurrent.CallableE)
   * @see #transactionCall(com.aoapps.dbc.DatabaseCallable)
   * @see #transactionCall(java.lang.Class, com.aoapps.dbc.DatabaseCallableE)
   * @see #transactionRun(com.aoapps.lang.RunnableE)
   * @see #transactionRun(java.lang.Class, com.aoapps.lang.RunnableE)
   * @see #transactionRun(com.aoapps.dbc.DatabaseRunnable)
   * @see #transactionRun(java.lang.Class, com.aoapps.dbc.DatabaseRunnableE)
   */
  public boolean isInTransaction() {
    return transactionConnection.get() != null;
  }

  /**
   * <p>
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   * </p>
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
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
  public <V> V transactionCall(CallableE<? extends V, ? extends SQLException> callable) throws SQLException {
    return transactionCall(SQLException.class, callable);
  }

  /**
   * <p>
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   * </p>
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   * <p>
   * The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.
   * </p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see #isInTransaction()
   */
  public <V, Ex extends Throwable> V transactionCall(Class<? extends Ex> exClass, CallableE<? extends V, ? extends Ex> callable) throws SQLException, Ex {
    return transactionCall(exClass, db -> callable.call());
  }

  /**
   * <p>
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   * </p>
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
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
  public <V> V transactionCall(DatabaseCallable<? extends V> callable) throws SQLException {
    return transactionCall(RuntimeException.class, callable::call);
  }

  /**
   * @deprecated  Please use {@link #transactionCall(com.aoapps.dbc.DatabaseCallable)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("overloads")
  public final <V> V executeTransaction(DatabaseCallable<V> callable) throws SQLException {
    return transactionCall(callable);
  }

  /**
   * <p>
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   * </p>
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   * <p>
   * The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.
   * </p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see #isInTransaction()
   */
  @SuppressWarnings("UseSpecificCatch")
  public <V, Ex extends Throwable> V transactionCall(Class<? extends Ex> exClass, DatabaseCallableE<? extends V, ? extends Ex> callable) throws SQLException, Ex {
    Throwable t0 = null;
    DatabaseConnection db = transactionConnection.get();
    if (db != null) {
      // Reuse existing connection
      try {
        return callable.call(db);
      } catch (NoRowException | NullDataException | ExtraRowException e) {
        throw e;
      } catch (Throwable t) {
        t0 = Throwables.addSuppressed(t0, t);
        t0 = db.rollback(t0);
      }
    } else {
      // Create new connection
      try (DatabaseConnection newConn = connect()) {
        try {
          transactionConnection.set(newConn);
          try {
            V result = callable.call(newConn);
            newConn.commit();
            return result;
          } finally {
            transactionConnection.remove();
          }
        } catch (Throwable t) {
          t0 = Throwables.addSuppressed(t0, t);
          t0 = newConn.rollback(t0);
        }
      }
    }
    assert t0 != null;
    if (exClass.isInstance(t0)) {
      throw exClass.cast(t0);
    }
    throw Throwables.wrap(t0, SQLException.class, SQLException::new);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #transactionCall(java.lang.Class, com.aoapps.dbc.DatabaseCallableE)}
   */
  @Deprecated(forRemoval = true)
  public final <V, Ex extends Exception> V executeTransaction(Class<Ex> exClass, DatabaseCallableE<V, Ex> callable) throws SQLException, Ex {
    return transactionCall(exClass, callable);
  }

  /**
   * <p>
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   * </p>
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
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
  public void transactionRun(RunnableE<? extends SQLException> runnable) throws SQLException {
    transactionRun(SQLException.class, runnable);
  }

  /**
   * <p>
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   * </p>
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   * <p>
   * The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.
   * </p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see #isInTransaction()
   */
  public <Ex extends Throwable> void transactionRun(Class<? extends Ex> exClass, RunnableE<? extends Ex> runnable) throws SQLException, Ex {
    transactionRun(exClass, db -> runnable.run());
  }

  /**
   * <p>
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   * </p>
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
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
  public void transactionRun(DatabaseRunnable runnable) throws SQLException {
    transactionRun(RuntimeException.class, runnable::run);
  }

  /**
   * @deprecated  Please use {@link #transactionRun(com.aoapps.dbc.DatabaseRunnable)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("overloads")
  public final void executeTransaction(DatabaseRunnable runnable) throws SQLException {
    transactionRun(runnable);
  }

  /**
   * <p>
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   * </p>
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   * <p>
   * The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.
   * </p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see #isInTransaction()
   */
  public <Ex extends Throwable> void transactionRun(Class<? extends Ex> exClass, DatabaseRunnableE<? extends Ex> runnable) throws SQLException, Ex {
    transactionCall(
        exClass,
        db -> {
          runnable.run(db);
          return null;
        }
    );
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #transactionRun(java.lang.Class, com.aoapps.dbc.DatabaseRunnableE)}
   */
  @Deprecated(forRemoval = true)
  public final <Ex extends Exception> void executeTransaction(Class<Ex> exClass, DatabaseRunnableE<Ex> runnable) throws SQLException, Ex {
    transactionRun(exClass, runnable);
  }

  @Override
  public String toString() {
    return "Database(" + (pool != null ? pool.toString() : dataSource.toString()) + ")";
  }

  @Override
  public DoubleStream doubleStream(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return transactionCall(db -> db.doubleStream(isolationLevel, readOnly, sql, params));
  }

  @Override
  public IntStream intStream(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return transactionCall(db -> db.intStream(isolationLevel, readOnly, sql, params));
  }

  @Override
  public LongStream longStream(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return transactionCall(db -> db.longStream(isolationLevel, readOnly, sql, params));
  }

  /**
   * {@inheritDoc}
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  @Override
  public <T, Ex extends Throwable> Stream<T> stream(
      int isolationLevel,
      boolean readOnly,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return transactionCall(
        exClass,
        db -> db.stream(isolationLevel, readOnly, exClass, objectFactory, sql, params)
    );
  }

  /**
   * {@inheritDoc}
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  @Override
  public <T, Ex extends Throwable> T queryCall(
      int isolationLevel,
      boolean readOnly,
      Class<? extends Ex> exClass,
      ResultSetCallableE<? extends T, ? extends Ex> resultSetCallable,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return transactionCall(
        exClass,
        db -> db.queryCall(isolationLevel, readOnly, exClass, resultSetCallable, sql, params)
    );
  }

  @Override
  public int update(
      String sql,
      Object ... params
  ) throws SQLException {
    return transactionCall(db -> db.update(sql, params));
  }

  @Override
  public long largeUpdate(
      String sql,
      Object ... params
  ) throws SQLException {
    return transactionCall(db -> db.largeUpdate(sql, params));
  }
}
