/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2019, 2020, 2021, 2022, 2023, 2024, 2025, 2026  AO Industries, Inc.
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

import com.aoapps.lang.AutoCloseables;
import com.aoapps.lang.RunnableE;
import com.aoapps.lang.Throwables;
import com.aoapps.lang.concurrent.CallableE;
import com.aoapps.lang.util.ErrorPrinter;
import com.aoapps.sql.Connections;
import com.aoapps.sql.failfast.FailFastConnection;
import com.aoapps.sql.tracker.ConnectionTracker;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Date;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * A {@link DatabaseConnection} represents the scope of an overall transaction.
 * It is used to only get actual database connections when needed.
 *
 * @see  Database
 *
 * @author  AO Industries, Inc.
 */
// TODO: We need abstract (or interface) parent, with two implementations:
//       TransactionDatabaseConnection, that is the outer-most transaction,
//       and SavepointDatabaseConnection, that uses a Savepoint to establish
//       a sub-transaction.  Also Tracker associate all things possible with Savepoint,
//       so that rollback/(and release?) of that Savepoint performs a thorough close of all
//       associated objects, much like overall rollback and/or close?
public class DatabaseConnection implements DatabaseAccess, AutoCloseable {

  /**
   * The fetch size given to {@link Statement#setFetchSize(int)}.  This value may be increased over time as the amount
   * of typical system memory increases.  As this may affect the number of round trips, and the speed of light is not
   * likely to change, future increases may be of benefit.
   *
   * <p>We've been using a value of {@code 1000} for nearly two decades.  As of the year 2020, we've bumped this up to
   * {@code 10000}.</p>
   */
  public static final int FETCH_SIZE = 10000;

  private final Database database;

  private FailFastConnection conn;

  protected DatabaseConnection(Database database) {
    this.database = database;
  }

  public Database getDatabase() {
    return database;
  }

  /**
   * @return {@code true} since {@link DatabaseConnection} is already within a transaction.
   *
   * @deprecated {@link DatabaseConnection} is already within a transaction.
   */
  @Override
  @Deprecated
  public boolean isInTransaction() {
    return true;
  }

  /**
   * @deprecated {@link DatabaseConnection} is already within a transaction.
   */
  @Override
  @Deprecated
  public <V> V transactionCall(CallableE<? extends V, ? extends SQLException> callable) throws SQLException {
    return DatabaseAccess.super.transactionCall(callable);
  }

  /**
   * @deprecated {@link DatabaseConnection} is already within a transaction.
   */
  @Override
  @Deprecated
  public <V, Ex extends Throwable> V transactionCall(Class<? extends Ex> exClass, CallableE<? extends V, ? extends Ex> callable) throws SQLException, Ex {
    return DatabaseAccess.super.transactionCall(exClass, callable);
  }

  /**
   * @deprecated {@link DatabaseConnection} is already within a transaction.
   */
  @Override
  @Deprecated
  public <V> V transactionCall(DatabaseCallable<? extends V> callable) throws SQLException {
    return DatabaseAccess.super.transactionCall(callable);
  }

  /**
   * @deprecated {@link DatabaseConnection} is already within a transaction.
   */
  @Override
  @Deprecated
  @SuppressWarnings("UseSpecificCatch")
  public <V, Ex extends Throwable> V transactionCall(Class<? extends Ex> exClass, DatabaseCallableE<? extends V, ? extends Ex> callable) throws SQLException, Ex {
    Throwable t0 = null;
    try {
      return callable.call(this);
    } catch (NoRowException | NullDataException | ExtraRowException e) {
      throw e;
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
      t0 = rollback(t0);
    }
    assert t0 != null;
    if (exClass.isInstance(t0)) {
      throw exClass.cast(t0);
    }
    throw Throwables.wrap(t0, SQLException.class, SQLException::new);
  }

  /**
   * @deprecated {@link DatabaseConnection} is already within a transaction.
   */
  @Override
  @Deprecated
  public void transactionRun(RunnableE<? extends SQLException> runnable) throws SQLException {
    DatabaseAccess.super.transactionRun(runnable);
  }

  /**
   * @deprecated {@link DatabaseConnection} is already within a transaction.
   */
  @Override
  @Deprecated
  public <Ex extends Throwable> void transactionRun(Class<? extends Ex> exClass, RunnableE<? extends Ex> runnable) throws SQLException, Ex {
    DatabaseAccess.super.transactionRun(exClass, runnable);
  }

  /**
   * @deprecated {@link DatabaseConnection} is already within a transaction.
   */
  @Override
  @Deprecated
  public void transactionRun(DatabaseRunnable runnable) throws SQLException {
    DatabaseAccess.super.transactionRun(runnable);
  }

  /**
   * @deprecated {@link DatabaseConnection} is already within a transaction.
   */
  @Override
  @Deprecated
  public <Ex extends Throwable> void transactionRun(Class<? extends Ex> exClass, DatabaseRunnableE<? extends Ex> runnable) throws SQLException, Ex {
    DatabaseAccess.super.transactionRun(exClass, runnable);
  }

  /**
   * Gets the read/write connection to the database with a transaction level of
   * {@link Connections#DEFAULT_TRANSACTION_ISOLATION},
   * warning when a connection is already used by this thread.
   *
   * <p>Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
   * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
   * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
   * thereof) of the database is unknown.</p>
   *
   * <p>The default auto-commit state depends on the read-only, isolation levels, and the presence of any
   * {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit} or
   * {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback} event listeners.  Upon initial connection,
   * auto-commit is enabled.  It then remains unchanged while is read-only, at an isolation level of
   * {@link Connection#TRANSACTION_READ_COMMITTED} or below, and has no event listeners.  This means, conversely, that
   * auto-commit is disabled when is read-write, at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above, or has any event listeners.</p>
   *
   * <p>When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
   * has auto-commit disabled (which typically means it was already read-write, at an isolation level of
   * {@link Connection#TRANSACTION_REPEATABLE_READ} or above, or had event listeners).</p>
   *
   * <p>With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
   * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.</p>
   *
   * <p>Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
   * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
   * potentially being within the scope of a larger read-write transaction.</strong></p>
   *
   * <p>When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
   * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
   * highest isolation level that will be required at the beginning of the transaction.</p>
   *
   * <p>If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
   * available.</p>
   *
   * <p>The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.</p>
   *
   * <p>The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.</p>
   *
   * @return  The read/write connection to the database.
   *
   *          <p>This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
   *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
   *          {@linkplain #close() closed}.</p>
   *
   *          <p>{@link Connection#commit()} and {@link Connection#rollback()} should not be used directly, since they will
   *          not fire any {@link #onCommit(com.aoapps.lang.RunnableE)} or {@link #onRollback(com.aoapps.lang.RunnableE)}
   *          events.  Instead, please use {@link #commit()} and {@link #rollback()}.</p>
   *
   * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
   *
   * @see  #getConnection(int, boolean, int)
   * @see  Database#getConnection()
   * @see  Connection#close()
   */
  // Note: Matches AOPool.getConnection()
  // Note: Matches AOConnectionPool.getConnection()
  // Note: Matches Database.getConnection()
  // Note:      Is DatabaseConnection.getConnection()
  public Connection getConnection() throws SQLException {
    return getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, 1);
  }

  /**
   * Gets the read/write connection to the database with a transaction level of
   * {@link Connections#DEFAULT_TRANSACTION_ISOLATION}.
   *
   * <p>Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
   * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
   * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
   * thereof) of the database is unknown.</p>
   *
   * <p>The default auto-commit state depends on the read-only, isolation levels, and the presence of any
   * {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit} or
   * {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback} event listeners.  Upon initial connection,
   * auto-commit is enabled.  It then remains unchanged while is read-only, at an isolation level of
   * {@link Connection#TRANSACTION_READ_COMMITTED} or below, and has no event listeners.  This means, conversely, that
   * auto-commit is disabled when is read-write, at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above, or has any event listeners.</p>
   *
   * <p>When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
   * has auto-commit disabled (which typically means it was already read-write, at an isolation level of
   * {@link Connection#TRANSACTION_REPEATABLE_READ} or above, or had event listeners).</p>
   *
   * <p>With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
   * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.</p>
   *
   * <p>Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
   * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
   * potentially being within the scope of a larger read-write transaction.</strong></p>
   *
   * <p>When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
   * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
   * highest isolation level that will be required at the beginning of the transaction.</p>
   *
   * <p>If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
   * available.</p>
   *
   * <p>The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.</p>
   *
   * <p>The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.</p>
   *
   * @param  maxConnections  The maximum number of connections expected to be used by the current thread.
   *                         This should normally be one to avoid potential deadlock.
   *
   *                         <p>The connection will continue to be considered used by the allocating thread until
   *                         released (via {@link Connection#close()}, even if the connection is shared by another
   *                         thread.</p>
   *
   * @return  The read/write connection to the database.
   *
   *          <p>This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
   *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
   *          {@linkplain #close() closed}.</p>
   *
   *          <p>{@link Connection#commit()} and {@link Connection#rollback()} should not be used directly, since they will
   *          not fire any {@link #onCommit(com.aoapps.lang.RunnableE)} or {@link #onRollback(com.aoapps.lang.RunnableE)}
   *          events.  Instead, please use {@link #commit()} and {@link #rollback()}.</p>
   *
   * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
   *
   * @see  #getConnection(int, boolean, int)
   * @see  Database#getConnection(int)
   * @see  Connection#close()
   */
  // Note: Matches AOPool.getConnection()
  // Note: Matches AOConnectionPool.getConnection()
  // Note: Matches Database.getConnection()
  // Note:      Is DatabaseConnection.getConnection()
  public Connection getConnection(int maxConnections) throws SQLException {
    return getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, maxConnections);
  }

  /**
   * Gets the connection to the database with a transaction level of
   * {@link Connections#DEFAULT_TRANSACTION_ISOLATION},
   * warning when a connection is already used by this thread.
   *
   * <p>Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
   * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
   * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
   * thereof) of the database is unknown.</p>
   *
   * <p>The default auto-commit state depends on the read-only, isolation levels, and the presence of any
   * {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit} or
   * {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback} event listeners.  Upon initial connection,
   * auto-commit is enabled.  It then remains unchanged while is read-only, at an isolation level of
   * {@link Connection#TRANSACTION_READ_COMMITTED} or below, and has no event listeners.  This means, conversely, that
   * auto-commit is disabled when is read-write, at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above, or has any event listeners.</p>
   *
   * <p>When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
   * has auto-commit disabled (which typically means it was already read-write, at an isolation level of
   * {@link Connection#TRANSACTION_REPEATABLE_READ} or above, or had event listeners).</p>
   *
   * <p>With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
   * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.</p>
   *
   * <p>Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
   * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
   * potentially being within the scope of a larger read-write transaction.</strong></p>
   *
   * <p>When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
   * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
   * highest isolation level that will be required at the beginning of the transaction.</p>
   *
   * <p>If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
   * available.</p>
   *
   * <p>The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.</p>
   *
   * <p>The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.</p>
   *
   * @param  readOnly  The {@link Connection#setReadOnly(boolean) read-only flag}.  Please note: a read-write connection
   *                   will always be returned while already in the scope of an overall read-write transaction.
   *
   * @return  The connection to the database.
   *
   *          <p>This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
   *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
   *          {@linkplain #close() closed}.</p>
   *
   *          <p>{@link Connection#commit()} and {@link Connection#rollback()} should not be used directly, since they will
   *          not fire any {@link #onCommit(com.aoapps.lang.RunnableE)} or {@link #onRollback(com.aoapps.lang.RunnableE)}
   *          events.  Instead, please use {@link #commit()} and {@link #rollback()}.</p>
   *
   * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
   *
   * @see  #getConnection(int, boolean, int)
   * @see  Database#getConnection(boolean)
   * @see  Connection#close()
   */
  // Note: Matches AOConnectionPool.getConnection(boolean)
  // Note: Matches Database.getConnection(boolean)
  // Note:      Is DatabaseConnection.getConnection(boolean)
  public Connection getConnection(boolean readOnly) throws SQLException {
    return getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, readOnly, 1);
  }

  /**
   * Gets the connection to the database,
   * warning when a connection is already used by this thread.
   *
   * <p>Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
   * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
   * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
   * thereof) of the database is unknown.</p>
   *
   * <p>The default auto-commit state depends on the read-only, isolation levels, and the presence of any
   * {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit} or
   * {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback} event listeners.  Upon initial connection,
   * auto-commit is enabled.  It then remains unchanged while is read-only, at an isolation level of
   * {@link Connection#TRANSACTION_READ_COMMITTED} or below, and has no event listeners.  This means, conversely, that
   * auto-commit is disabled when is read-write, at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above, or has any event listeners.</p>
   *
   * <p>When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
   * has auto-commit disabled (which typically means it was already read-write, at an isolation level of
   * {@link Connection#TRANSACTION_REPEATABLE_READ} or above, or had event listeners).</p>
   *
   * <p>With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
   * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.</p>
   *
   * <p>Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
   * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
   * potentially being within the scope of a larger read-write transaction.</strong></p>
   *
   * <p>When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
   * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
   * highest isolation level that will be required at the beginning of the transaction.</p>
   *
   * <p>If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
   * available.</p>
   *
   * <p>The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.</p>
   *
   * <p>The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.</p>
   *
   * @param  isolationLevel  The {@link Connection#setTransactionIsolation(int) transaction isolation level}.  Please
   *                         note: a connection of a higher transaction isolation level may be returned while already
   *                         in the scope of an overall transaction.
   *
   * @param  readOnly        The {@link Connection#setReadOnly(boolean) read-only flag}.  Please note: a read-write
   *                         connection will always be returned while already in the scope of an overall read-write
   *                         transaction.
   *
   * @return  The connection to the database.
   *
   *          <p>This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
   *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
   *          {@linkplain #close() closed}.</p>
   *
   *          <p>{@link Connection#commit()} and {@link Connection#rollback()} should not be used directly, since they will
   *          not fire any {@link #onCommit(com.aoapps.lang.RunnableE)} or {@link #onRollback(com.aoapps.lang.RunnableE)}
   *          events.  Instead, please use {@link #commit()} and {@link #rollback()}.</p>
   *
   * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
   *
   * @see  #getConnection(int, boolean, int)
   * @see  Database#getConnection(int, boolean)
   * @see  Connection#close()
   */
  // Note: Matches AOConnectionPool.getConnection(int, boolean)
  // Note: Matches Database.getConnection(int, boolean)
  // Note:      Is DatabaseConnection.getConnection(int, boolean)
  public Connection getConnection(int isolationLevel, boolean readOnly) throws SQLException {
    return getConnection(isolationLevel, readOnly, 1);
  }

  /**
   * Gets the connection to the database.
   *
   * <p>Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
   * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
   * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
   * thereof) of the database is unknown.</p>
   *
   * <p>The default auto-commit state depends on the read-only, isolation levels, and the presence of any
   * {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit} or
   * {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback} event listeners.  Upon initial connection,
   * auto-commit is enabled.  It then remains unchanged while is read-only, at an isolation level of
   * {@link Connection#TRANSACTION_READ_COMMITTED} or below, and has no event listeners.  This means, conversely, that
   * auto-commit is disabled when is read-write, at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above, or has any event listeners.</p>
   *
   * <p>When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
   * has auto-commit disabled (which typically means it was already read-write, at an isolation level of
   * {@link Connection#TRANSACTION_REPEATABLE_READ} or above, or had event listeners).</p>
   *
   * <p>With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
   * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
   * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.</p>
   *
   * <p>Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
   * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
   * potentially being within the scope of a larger read-write transaction.</strong></p>
   *
   * <p>When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
   * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
   * highest isolation level that will be required at the beginning of the transaction.</p>
   *
   * <p>If all the connections in the pool are busy and the pool is at capacity, waits until a connection becomes
   * available.</p>
   *
   * <p>The connection will be a {@link FailFastConnection}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The fail-fast connection is used to determine whether to {@linkplain Connection#rollback() roll-back} or
   * {@linkplain Connection#commit() commit} during automatic transaction management.</p>
   *
   * <p>The connection will also be a {@link ConnectionTracker}, which may be unwrapped via {@link Connection#unwrap(java.lang.Class)}.
   * The connection tracking is used to close/free all objects before returning the connection to the underlying pool.</p>
   *
   * @param  isolationLevel  The {@link Connection#setTransactionIsolation(int) transaction isolation level}.  Please
   *                         note: a connection of a higher transaction isolation level may be returned while already
   *                         in the scope of an overall transaction.
   *
   * @param  readOnly        The {@link Connection#setReadOnly(boolean) read-only flag}.  Please note: a read-write
   *                         connection will always be returned while already in the scope of an overall read-write
   *                         transaction.
   *
   * @param  maxConnections  The maximum number of connections expected to be used by the current thread.
   *                         This should normally be one to avoid potential deadlock.
   *
   *                         <p>The connection will continue to be considered used by the allocating thread until
   *                         released (via {@link Connection#close()}, even if the connection is shared by another
   *                         thread.</p>
   *
   * @return  The connection to the database.
   *
   *          <p>This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
   *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
   *          {@linkplain #close() closed}.</p>
   *
   *          <p>{@link Connection#commit()} and {@link Connection#rollback()} should not be used directly, since they will
   *          not fire any {@link #onCommit(com.aoapps.lang.RunnableE)} or {@link #onRollback(com.aoapps.lang.RunnableE)}
   *          events.  Instead, please use {@link #commit()} and {@link #rollback()}.</p>
   *
   * @throws  SQLException  when an error occurs, or when a thread attempts to allocate more than half the pool
   *
   * @see  Database#getConnection(int, boolean, int, boolean)
   * @see  Connection#close()
   */
  // Note: Matches AOConnectionPool.getConnection(int, boolean, int)
  // Note: Matches Database.getConnection(int, boolean, int)
  // Note:      Is DatabaseConnection.getConnection(int, boolean)
  public Connection getConnection(int isolationLevel, boolean readOnly, int maxConnections) throws SQLException {
    FailFastConnection c = conn;
    if (c == null) {
      // New connection
      c = database.getConnection(isolationLevel, readOnly, maxConnections, false);
      assert c.getAutoCommit();
      assert c.isReadOnly() == readOnly;
      assert c.getTransactionIsolation() == isolationLevel;
      conn = c;
    } else {
      // Existing connection
      if (c.isReadOnly() != readOnly) {
        if (readOnly) {
          // Set back to read-only when auto-commit enabled, otherwise leave read-write
          if (c.getAutoCommit()) {
            c.setReadOnly(true);
          }
        } else {
          // Let driver try to set read-write (which may fail if already in transaction)
          c.setReadOnly(false);
        }
      }
      if (c.getTransactionIsolation() < isolationLevel) {
        // Let driver try to increase isolation level (which may fail if already in transaction)
        c.setTransactionIsolation(isolationLevel);
      }
    }
    if (!readOnly || isolationLevel >= Connection.TRANSACTION_REPEATABLE_READ
        || !onCommits.isEmpty() || !onRollbacks.isEmpty()) {
      c.setAutoCommit(false);
    }
    return c;
  }

  protected static void setParam(Connection conn, PreparedStatement pstmt, int pos, Object param) throws SQLException {
    if (param == null) {
      pstmt.setNull(pos, Types.VARCHAR);
    } else if (param instanceof Null) {
      pstmt.setNull(pos, ((Null) param).getType());
    } else if (param instanceof Array) {
      pstmt.setArray(pos, (Array) param);
    } else if (param instanceof BigDecimal) {
      pstmt.setBigDecimal(pos, (BigDecimal) param);
    } else if (param instanceof BigInteger) {
      pstmt.setBigDecimal(pos, new BigDecimal((BigInteger) param));
    } else if (param instanceof Blob) {
      pstmt.setBlob(pos, (Blob) param);
    } else if (param instanceof Boolean) {
      pstmt.setBoolean(pos, (Boolean) param);
    } else if (param instanceof Byte) {
      pstmt.setByte(pos, (Byte) param);
    } else if (param instanceof byte[]) {
      pstmt.setBytes(pos, (byte[]) param);
    } else if (param instanceof Clob) {
      pstmt.setClob(pos, (Clob) param);
    } else if (param instanceof Date) {
      pstmt.setDate(pos, (Date) param);
    } else if (param instanceof Double) {
      pstmt.setDouble(pos, (Double) param);
    } else if (param instanceof Float) {
      pstmt.setFloat(pos, (Float) param);
    } else if (param instanceof Integer) {
      pstmt.setInt(pos, (Integer) param);
    } else if (param instanceof InputStream) {
      pstmt.setBinaryStream(pos, (InputStream) param);
    } else if (param instanceof Long) {
      pstmt.setLong(pos, (Long) param);
    } else if (param instanceof NClob) {
      pstmt.setNClob(pos, (NClob) param);
    } else if (param instanceof Reader) {
      pstmt.setCharacterStream(pos, (Reader) param);
    } else if (param instanceof Ref) {
      pstmt.setRef(pos, (Ref) param);
    } else if (param instanceof RowId) {
      pstmt.setRowId(pos, (RowId) param);
    } else if (param instanceof Short) {
      pstmt.setShort(pos, (Short) param);
    } else if (param instanceof SQLXML) {
      pstmt.setSQLXML(pos, (SQLXML) param);
    } else if (param instanceof String) {
      pstmt.setString(pos, (String) param);
    } else if (param instanceof Time) {
      pstmt.setTime(pos, (Time) param);
    } else if (param instanceof Timestamp) {
      pstmt.setTimestamp(pos, (Timestamp) param);
    } else if (param instanceof URL) {
      pstmt.setURL(pos, (URL) param);
    } else if (param instanceof Enum) {
      pstmt.setString(pos, ((Enum) param).name());
    } else if (
        (param instanceof SQLData)
            || (param instanceof Struct)
    ) {
      pstmt.setObject(pos, param);
    } else if (param instanceof String[]) {
      pstmt.setArray(pos, conn.createArrayOf("text", (Object[]) param));
    } else {
      // Defaults to string with object.toString only when the class has a valueOf(String) method that will reconstitute it in AutoObjectFactory
      Class<?> clazz = param.getClass();
      if (AutoObjectFactory.getValueOfStringMethod(clazz) != null) {
        pstmt.setString(pos, param.toString());
      } else {
        // Call setObject here, to give the database driver a chance to decide what to do
        pstmt.setObject(pos, param);
        // throw new SQLException("Unexpected parameter class: "+clazz.getName());
      }
    }
  }

  public static void setParams(
      Connection conn,
      PreparedStatement pstmt,
      Object... params
  ) throws SQLException {
    int pos = 1;
    for (Object param : params) {
      setParam(conn, pstmt, pos++, param);
    }
  }

  private final List<RunnableE<Throwable>> onCommits = new ArrayList<>();

  private final List<RunnableE<Throwable>> onRollbacks = new ArrayList<>();

  /**
   * Adds an on-commit event handler, which are called in order just after the transaction has been
   * successfully committed.
   *
   * <p>Exceptions of any on-commit event handler will be propagated.
   * When handler throws {@link Error}, {@link RuntimeException}, or {@link SQLException}, the exception is
   * propagated directly.  Any other {@link Throwable} is wrapped in {@link EventException}.</p>
   *
   * <p>When an on-commit event handler throws any exception, remaining on-commit event handlers registered after the
   * failed one will not be fired.</p>
   *
   * <p>When adding the first non-null event handler (either of {@link #onCommit(com.aoapps.lang.RunnableE)} or
   * {@link #onRollback(com.aoapps.lang.RunnableE)}), if the connection exists and currently has
   * {@linkplain Connection#getAutoCommit() auto-commit} enabled, auto-commit will be disabled.  This is so the event
   * scope can match the transaction scope of the underlying database.</p>
   *
   * @param onCommit The event handler, may be {@code null} which will simply be ignored.
   *
   * @see EventException
   * @see #commit()
   * @see #onRollback(com.aoapps.lang.RunnableE)
   */
  @SuppressWarnings("unchecked")
  public DatabaseConnection onCommit(RunnableE<?> onCommit) throws SQLException {
    if (onCommit != null) {
      FailFastConnection c = conn;
      if (c != null && onCommits.isEmpty() && onRollbacks.isEmpty() && c.getAutoCommit()) {
        c.setAutoCommit(false);
      }
      onCommits.add((RunnableE) onCommit);
    }
    return this;
  }

  /**
   * Adds an on-rollback event handler, which are called in reverse order just before the transaction will be
   * rolled-back.
   *
   * <p>Exceptions of any on-rollback event handler will be propagated.
   * When handler throws {@link Error}, {@link RuntimeException}, or {@link SQLException}, the exception is
   * propagated directly.  Any other {@link Throwable} is wrapped in {@link EventException}.</p>
   *
   * <p>When an on-rollback event handler throws any exception, remaining on-rollback event handlers registered before the
   * failed one will not be fired.</p>
   *
   * <p>When adding the first non-null event handler (either of {@link #onCommit(com.aoapps.lang.RunnableE)} or
   * {@link #onRollback(com.aoapps.lang.RunnableE)}), if the connection exists and currently has
   * {@linkplain Connection#getAutoCommit() auto-commit} enabled, auto-commit will be disabled.  This is so the event
   * scope can match the transaction scope of the underlying database.</p>
   *
   * @param onRollback The event handler, may be {@code null} which will simply be ignored.
   *
   * @see EventException
   * @see #rollback()
   * @see #rollback(java.lang.Throwable)
   * @see #close()
   * @see #close(java.lang.Throwable)
   * @see #onCommit(com.aoapps.lang.RunnableE)
   */
  @SuppressWarnings("unchecked")
  public DatabaseConnection onRollback(RunnableE<?> onRollback) throws SQLException {
    if (onRollback != null) {
      FailFastConnection c = conn;
      if (c != null && onCommits.isEmpty() && onRollbacks.isEmpty() && c.getAutoCommit()) {
        c.setAutoCommit(false);
      }
      onRollbacks.add((RunnableE) onRollback);
    }
    return this;
  }

  /**
   * Clears {@link #onRollbacks}, copies then clears {@link #onCommits}, then calls each on-commit handler
   * in order.
   *
   * @throws SQLException   Any {@link SQLException} thrown by the event handler is propagated
   *
   * @throws EventException When an {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit event handler} fails,
   *                        any {@link Throwable} other than {@link Error}, {@link RuntimeException},
   *                        or {@link SQLException} will be wrapped in an {@link EventException}.
   *
   * @see #fireOnRollbacks()
   */
  @SuppressWarnings("UseSpecificCatch")
  private void fireOnCommits() throws SQLException, EventException {
    onRollbacks.clear();
    @SuppressWarnings("unchecked")
    RunnableE<Throwable>[] copy = onCommits.toArray(RunnableE[]::new);
    onCommits.clear();
    try {
      for (RunnableE<Throwable> onCommit : copy) {
        onCommit.run();
      }
    } catch (Error | RuntimeException | SQLException e) {
      throw e;
    } catch (Throwable t) {
      throw new EventException(t);
    }
  }

  /**
   * Clears {@link #onCommits}, copies then clears {@link #onRollbacks}, then calls each on-rollback handler
   * in reverse order.
   *
   * @throws SQLException   Any {@link SQLException} thrown by the event handler is propagated
   *
   * @throws EventException When an {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback event handler} fails,
   *                        any {@link Throwable} other than {@link Error}, {@link RuntimeException},
   *                        or {@link SQLException} will be wrapped in an {@link EventException}.
   *
   * @see #fireOnCommits()
   */
  @SuppressWarnings("UseSpecificCatch")
  private void fireOnRollbacks() throws SQLException, EventException {
    onCommits.clear();
    @SuppressWarnings("unchecked")
    RunnableE<Throwable>[] copy = onRollbacks.toArray(RunnableE[]::new);
    onRollbacks.clear();
    try {
      for (int i = copy.length - 1; i >= 0; i--) {
        copy[i].run();
      }
    } catch (Error | RuntimeException | SQLException e) {
      throw e;
    } catch (Throwable t) {
      throw new EventException(t);
    }
  }

  /**
   * Commits the current transaction.
   *
   * <p>Once the underlying transaction (if any) is successfully committed, any
   * {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback event handlers} are discarded, then all
   * {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit event handlers} are fired in order.
   * When an on-commit event handler throws any exception, remaining on-commit event handlers registered after the
   * failed one will not be fired.</p>
   */
  // TODO: Restore default isolation levels and read-only state on commit and rollback?
  public void commit() throws SQLException {
    FailFastConnection c = conn;
    if (c != null && !c.getAutoCommit()) {
      c.commit();
    }
    fireOnCommits();
  }

  public boolean isClosed() throws SQLException {
    FailFastConnection c = conn;
    return c == null || c.isClosed();
  }

  /**
   * @deprecated  Please use {@link #close()}
   */
  @Deprecated(forRemoval = true)
  public void releaseConnection() throws SQLException {
    close();
  }

  /**
   * Closes and/or releases the current connection back to the pool.
   *
   * <p>Any {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit event handlers} are discarded.</p>
   *
   * <p>All {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback event handlers} are fired in reverse order.
   * When an on-rollback event handler throws any exception, remaining on-rollback event handlers registered before the
   * failed one will not be fired.</p>
   *
   * <p>Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
   * This will still be done even when an on-rollback event handler has thrown an exception.</p>
   *
   * @see  Database#release(com.aoapps.sql.failfast.FailFastConnection)
   * @see  #close(java.lang.Throwable)
   */
  @Override
  @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch", "UseSpecificCatch"})
  public void close() throws SQLException {
    Throwable t0 = null;
    try {
      fireOnRollbacks();
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    try {
      FailFastConnection c = conn;
      if (c != null) {
        conn = null;
        database.release(c);
      }
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    if (t0 != null) {
      throw Throwables.wrap(t0, SQLException.class, SQLException::new);
    }
  }

  /**
   * Closes and/or releases the current connection back to the pool.
   *
   * <p>Any {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit event handlers} are discarded.</p>
   *
   * <p>All {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback event handlers} are fired in reverse order.
   * When an on-rollback event handler throws any exception, remaining on-rollback event handlers registered before the
   * failed one will not be fired.</p>
   *
   * <p>Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
   * This will still be done even when an on-rollback event handler has thrown an exception.</p>
   *
   * @param  t0  Any exceptions will be added here via {@link Throwables#addSuppressed(java.lang.Throwable, java.lang.Throwable)}
   *
   * @return  The given exception, or new exception, or {@code null} when none given and none new
   *
   * @see  #close()
   * @see  AutoCloseables#closeAndCatch(java.lang.Throwable, java.lang.AutoCloseable)
   */
  public Throwable close(Throwable t0) {
    return AutoCloseables.closeAndCatch(t0, this);
  }

  /**
   * Rolls back the current connection, if have connection and is not auto-commit.
   *
   * <p>Any {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit event handlers} are discarded.</p>
   *
   * <p>All {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback event handlers} are fired in reverse order.
   * When an on-rollback event handler throws any exception, remaining on-rollback event handlers registered before the
   * failed one will not be fired.</p>
   *
   * <p>Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
   * This will still be done even when an on-rollback event handler has thrown an exception.</p>
   *
   * @return  {@code true} when connected and rolled-back (or is auto-commit)
   */
  // TODO: Rollback to the savepoint of the current sub-transaction?
  @SuppressWarnings({"UseSpecificCatch", "BroadCatchBlock", "TooBroadCatch"})
  public boolean rollback() throws SQLException {
    Throwable t0 = null;
    try {
      fireOnRollbacks();
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    boolean rollback = false;
    try {
      FailFastConnection c = conn;
      if (c != null && !c.isClosed()) {
        rollback = true;
        if (!c.getAutoCommit()) {
          c.rollback();
        }
      }
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    if (t0 != null) {
      throw Throwables.wrap(t0, SQLException.class, SQLException::new);
    } else {
      return rollback;
    }
  }

  /**
   * Rolls back the current connection, if have connection and is not auto-commit.
   *
   * <p>Any {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit event handlers} are discarded.</p>
   *
   * <p>All {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback event handlers} are fired in reverse order.
   * When an on-rollback event handler throws any exception, remaining on-rollback event handlers registered before the
   * failed one will not be fired.</p>
   *
   * <p>Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
   * This will still be done even when an on-rollback event handler has thrown an exception.</p>
   *
   * @param  t0  Any exceptions will be added here via {@link Throwables#addSuppressed(java.lang.Throwable, java.lang.Throwable)}
   *
   * @return  The given exception, or new exception, or {@code null} when none given and none new
   *
   * @see  #rollback()
   * @see  Throwables#addSuppressed(java.lang.Throwable, java.lang.Throwable)
   */
  // TODO: Rollback to the savepoint of the current sub-transaction?
  @SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
  public Throwable rollback(Throwable t0) {
    try {
      rollback();
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    return t0;
  }

  /**
   * Rolls back the current connection, if have connection and is not auto-commit, and forces the underlying
   * connection closed via {@link Connection#abort(java.util.concurrent.Executor)}.  This close is distinct
   * from {@link #close()}, which is intended for releasing to the underlying pool via {@link Connection#close()}.
   *
   * <p>Any {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit event handlers} are discarded.</p>
   *
   * <p>All {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback event handlers} are fired in reverse order.
   * When an on-rollback event handler throws any exception, remaining on-rollback event handlers registered before the
   * failed one will not be fired.</p>
   *
   * <p>Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
   * This will still be done even when an on-rollback event handler has thrown an exception.</p>
   *
   * @return  {@code true} when connected and rolled-back (or is auto-commit)
   *
   * @see  #rollback()
   * @see  #rollbackAndClose(java.lang.Throwable)
   *
   * @deprecated  Please use {@link #rollback()}.  The connection will be {@linkplain Connection#isValid(int) validated}
   *              during {@link #close()} when there is any unresolved {@linkplain FailFastConnection#getFailFastState() fail-fast state}.
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
  public boolean rollbackAndClose() throws SQLException {
    Throwable t0 = null;
    try {
      fireOnRollbacks();
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    boolean rollback = false;
    FailFastConnection c = conn;
    if (c != null) {
      conn = null;
      // Rollback
      try {
        if (!c.isClosed()) {
          rollback = true;
          if (!c.getAutoCommit()) {
            c.rollback();
          }
        }
      } catch (Throwable t) {
        t0 = Throwables.addSuppressed(t0, t);
      }
      // Abort/close
      try {
        c.abort(database.getExecutors().getUnbounded());
      } catch (Throwable t) {
        t0 = Throwables.addSuppressed(t0, t);
        // Abort failed, try to close the wrapped connection directly
        t0 = AutoCloseables.closeAndCatch(t0, c.getWrapped());
        try {
          database.release(c);
        } catch (Throwable t2) {
          t0 = Throwables.addSuppressed(t0, t2);
        }
      }
    }
    if (t0 != null) {
      throw Throwables.wrap(t0, SQLException.class, SQLException::new);
    } else {
      return rollback;
    }
  }

  /**
   * Rolls back the current connection, if have connection and is not auto-commit, and forces the underlying
   * connection closed via {@link Connection#abort(java.util.concurrent.Executor)}.  This close is distinct
   * from {@link #close()}, which is intended for releasing to the underlying pool via {@link Connection#close()}.
   *
   * <p>Any {@linkplain #onCommit(com.aoapps.lang.RunnableE) on-commit event handlers} are discarded.</p>
   *
   * <p>All {@linkplain #onRollback(com.aoapps.lang.RunnableE) on-rollback event handlers} are fired in reverse order.
   * When an on-rollback event handler throws any exception, remaining on-rollback event handlers registered before the
   * failed one will not be fired.</p>
   *
   * <p>Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
   * This will still be done even when an on-rollback event handler has thrown an exception.</p>
   *
   * @param  t0  Any exceptions will be added here via {@link Throwables#addSuppressed(java.lang.Throwable, java.lang.Throwable)}
   *
   * @return  The given exception, or new exception, or {@code null} when none given and none new
   *
   * @see  #rollback(java.lang.Throwable)
   * @see  #rollbackAndClose()
   * @see  Throwables#addSuppressed(java.lang.Throwable, java.lang.Throwable)
   *
   * @deprecated  Please use {@link #rollback(java.lang.Throwable)}.  The connection will be {@linkplain Connection#isValid(int) validated}
   *              during {@link #close(java.lang.Throwable)} when there is any unresolved {@linkplain FailFastConnection#getFailFastState() fail-fast state}.
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
  public Throwable rollbackAndClose(Throwable t0) {
    try {
      rollbackAndClose();
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
    }
    return t0;
  }

  @Override
  @SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
  public ResultSet queryResultSet(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean allowExtraRows,
      String sql,
      Object... params
  ) throws NoRowException, SQLException {
    Connection c = getConnection(isolationLevel, readOnly);
    Throwable t0 = null;
    PreparedStatement pstmt = c.prepareStatement(sql);
    try {
      try {
        pstmt.setFetchSize(FETCH_SIZE);
        setParams(c, pstmt, params);
        ResultSet results = pstmt.executeQuery();
        try {
          if (rowRequired && !results.next()) {
            throw new NoRowException();
          }
          return new ResultSetImpl(results) {
            @Override
            public void close() throws ExtraRowException, SQLException {
              try {
                try {
                  try {
                    if (!allowExtraRows && wrapped.next()) {
                      throw new ExtraRowException(wrapped);
                    }
                  } finally {
                    wrapped.close();
                  }
                } catch (Error | RuntimeException | SQLException e) {
                  ErrorPrinter.addSql(e, pstmt);
                  throw e;
                }
              } finally {
                pstmt.close();
              }
            }
          };
        } catch (Throwable t) {
          throw AutoCloseables.closeAndWrap(t, SQLException.class, SQLException::new, results);
        }
      } catch (Error | RuntimeException | SQLException e) {
        ErrorPrinter.addSql(e, pstmt);
        throw e;
      }
    } catch (Throwable t) {
      t0 = Throwables.addSuppressed(t0, t);
      t0 = AutoCloseables.closeAndCatch(t0, pstmt);
    }
    assert t0 != null;
    throw Throwables.wrap(t0, SQLException.class, SQLException::new);
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
      Object... params
  ) throws SQLException, Ex {
    Connection c = getConnection(isolationLevel, readOnly);
    // TODO: Use regular Statement when there are no params?  Interaction with PostgreSQL prepared statement caching?
    try (PreparedStatement pstmt = c.prepareStatement(sql)) {
      try {
        pstmt.setFetchSize(FETCH_SIZE);
        setParams(c, pstmt, params);
        try (ResultSet results = pstmt.executeQuery()) {
          return resultSetCallable.call(results);
        }
      } catch (Error | RuntimeException | SQLException e) {
        ErrorPrinter.addSql(e, pstmt);
        throw e;
      }
    }
  }

  @Override
  public int update(
      String sql,
      Object... params
  ) throws SQLException {
    Connection c = getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false);
    try (PreparedStatement pstmt = c.prepareStatement(sql)) {
      try {
        setParams(c, pstmt, params);
        return pstmt.executeUpdate();
      } catch (Error | RuntimeException | SQLException e) {
        ErrorPrinter.addSql(e, pstmt);
        throw e;
      }
    }
  }

  @Override
  public long largeUpdate(
      String sql,
      Object... params
  ) throws SQLException {
    Connection c = getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false);
    try (PreparedStatement pstmt = c.prepareStatement(sql)) {
      try {
        setParams(c, pstmt, params);
        return pstmt.executeLargeUpdate();
      } catch (Error | RuntimeException | SQLException e) {
        ErrorPrinter.addSql(e, pstmt);
        throw e;
      }
    }
  }

  // TODO: variants of update method that passes Iterable<Object[]> params (or Iterable<Iterable<?>>, or Object[][])
  //       that would perform a batch update.  Do not even prepare statement when iterable is empty.
  //
  //       There might also be room for the opposite of ObjectFactory, maybe PreparedStatementFactory, which would
  //       set the prepared statement values.  This might be what is iterated.
  //       Expose default Object[] -> pstmt as a helper?  A set of PreparedStatementFactories?
  //
  //       How would streaming fit into this?
}
