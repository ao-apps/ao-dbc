/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2019, 2020, 2021  AO Industries, Inc.
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
package com.aoapps.dbc;

import com.aoapps.lang.AutoCloseables;
import com.aoapps.lang.Throwables;
import com.aoapps.lang.exception.WrappedException;
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
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
	 * <p>
	 * We've been using a value of {@code 1000} for nearly two decades.  As of the year 2020, we've bumped this up to
	 * {@code 10000}.
	 * </p>
	 */
	public static final int FETCH_SIZE = 10000;

	private final Database database;

	private FailFastConnection _conn;

	protected DatabaseConnection(Database database) {
	   this.database = database;
	}

	public Database getDatabase() {
		return database;
	}

	/**
	 * Gets the read/write connection to the database with a transaction level of
	 * {@link Connections#DEFAULT_TRANSACTION_ISOLATION},
	 * warning when a connection is already used by this thread.
	 * <p>
	 * Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
	 * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
	 * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
	 * thereof) of the database is unknown.
	 * </p>
	 * <p>
	 * The default auto-commit state depends on the read-only and isolation levels.  Upon initial connection,
	 * auto-commit is enabled.  It then remains unchanged while is read-only and at an isolation level of
	 * {@link Connection#TRANSACTION_READ_COMMITTED} or below.  This means, conversely, that auto-commit is disabled
	 * when is either read-write or at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ} or above.
	 * </p>
	 * <p>
	 * When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
	 * has auto-commit disabled (which typically means it was either already read-write or at an isolation level of
	 * {@link Connection#TRANSACTION_REPEATABLE_READ} or above).
	 * </p>
	 * <p>
	 * With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
	 * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
	 * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.
	 * </p>
	 * <p>
	 * Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
	 * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
	 * potentially being within the scope of a larger read-write transaction.</strong>
	 * </p>
	 * <p>
	 * When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
	 * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
	 * highest isolation level that will be required at the beginning of the transaction.
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
	 * @return  The read/write connection to the database.
	 *          <p>
	 *          This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
	 *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
	 *          {@linkplain #close() closed}.
	 *          </p>
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
	 * <p>
	 * Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
	 * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
	 * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
	 * thereof) of the database is unknown.
	 * </p>
	 * <p>
	 * The default auto-commit state depends on the read-only and isolation levels.  Upon initial connection,
	 * auto-commit is enabled.  It then remains unchanged while is read-only and at an isolation level of
	 * {@link Connection#TRANSACTION_READ_COMMITTED} or below.  This means, conversely, that auto-commit is disabled
	 * when is either read-write or at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ} or above.
	 * </p>
	 * <p>
	 * When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
	 * has auto-commit disabled (which typically means it was either already read-write or at an isolation level of
	 * {@link Connection#TRANSACTION_REPEATABLE_READ} or above).
	 * </p>
	 * <p>
	 * With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
	 * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
	 * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.
	 * </p>
	 * <p>
	 * Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
	 * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
	 * potentially being within the scope of a larger read-write transaction.</strong>
	 * </p>
	 * <p>
	 * When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
	 * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
	 * highest isolation level that will be required at the beginning of the transaction.
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
	 * @return  The read/write connection to the database.
	 *          <p>
	 *          This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
	 *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
	 *          {@linkplain #close() closed}.
	 *          </p>
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
	 * <p>
	 * Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
	 * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
	 * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
	 * thereof) of the database is unknown.
	 * </p>
	 * <p>
	 * The default auto-commit state depends on the read-only and isolation levels.  Upon initial connection,
	 * auto-commit is enabled.  It then remains unchanged while is read-only and at an isolation level of
	 * {@link Connection#TRANSACTION_READ_COMMITTED} or below.  This means, conversely, that auto-commit is disabled
	 * when is either read-write or at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ} or above.
	 * </p>
	 * <p>
	 * When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
	 * has auto-commit disabled (which typically means it was either already read-write or at an isolation level of
	 * {@link Connection#TRANSACTION_REPEATABLE_READ} or above).
	 * </p>
	 * <p>
	 * With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
	 * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
	 * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.
	 * </p>
	 * <p>
	 * Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
	 * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
	 * potentially being within the scope of a larger read-write transaction.</strong>
	 * </p>
	 * <p>
	 * When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
	 * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
	 * highest isolation level that will be required at the beginning of the transaction.
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
	 * @param  readOnly  The {@link Connection#setReadOnly(boolean) read-only flag}.  Please note: a read-write connection
	 *                   will always be returned while already in the scope of an overall read-write transaction.
	 *
	 * @return  The connection to the database.
	 *          <p>
	 *          This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
	 *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
	 *          {@linkplain #close() closed}.
	 *          </p>
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
	 * <p>
	 * Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
	 * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
	 * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
	 * thereof) of the database is unknown.
	 * </p>
	 * <p>
	 * The default auto-commit state depends on the read-only and isolation levels.  Upon initial connection,
	 * auto-commit is enabled.  It then remains unchanged while is read-only and at an isolation level of
	 * {@link Connection#TRANSACTION_READ_COMMITTED} or below.  This means, conversely, that auto-commit is disabled
	 * when is either read-write or at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ} or above.
	 * </p>
	 * <p>
	 * When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
	 * has auto-commit disabled (which typically means it was either already read-write or at an isolation level of
	 * {@link Connection#TRANSACTION_REPEATABLE_READ} or above).
	 * </p>
	 * <p>
	 * With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
	 * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
	 * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.
	 * </p>
	 * <p>
	 * Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
	 * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
	 * potentially being within the scope of a larger read-write transaction.</strong>
	 * </p>
	 * <p>
	 * When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
	 * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
	 * highest isolation level that will be required at the beginning of the transaction.
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
	 * @param  isolationLevel  The {@link Connection#setTransactionIsolation(int) transaction isolation level}.  Please
	 *                         note: a connection of a higher transaction isolation level may be returned while already
	 *                         in the scope of an overall transaction.
	 *
	 * @param  readOnly        The {@link Connection#setReadOnly(boolean) read-only flag}.  Please note: a read-write
	 *                         connection will always be returned while already in the scope of an overall read-write
	 *                         transaction.
	 *
	 * @return  The connection to the database.
	 *          <p>
	 *          This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
	 *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
	 *          {@linkplain #close() closed}.
	 *          </p>
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
	 * <p>
	 * Uses a deferred connection strategy.  If not previously connected, allocates the connection now.  This allows
	 * applications to create {@link DatabaseConnection} at no cost, only connecting to the database when first needed.
	 * This is helpful for when a transaction scope is established at a high level, where the actual use (or lack
	 * thereof) of the database is unknown.
	 * </p>
	 * <p>
	 * The default auto-commit state depends on the read-only and isolation levels.  Upon initial connection,
	 * auto-commit is enabled.  It then remains unchanged while is read-only and at an isolation level of
	 * {@link Connection#TRANSACTION_READ_COMMITTED} or below.  This means, conversely, that auto-commit is disabled
	 * when is either read-write or at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ} or above.
	 * </p>
	 * <p>
	 * When a connection already exists, its read-only mode may be changed, but may not be changed on a connection that
	 * has auto-commit disabled (which typically means it was either already read-write or at an isolation level of
	 * {@link Connection#TRANSACTION_REPEATABLE_READ} or above).
	 * </p>
	 * <p>
	 * With the default auto-commit behavior (auto-commit not disabled by application), <strong>it is an error to try to
	 * change from read-only to read-write while at an isolation level of {@link Connection#TRANSACTION_REPEATABLE_READ}
	 * or above,</strong> as the necessary actions to make the change would break the repeatable-read guarantee.
	 * </p>
	 * <p>
	 * Read-write connections will not be set back to read-only mode when the connection has auto-commit disabled, thus
	 * <strong>the read-only flag is an optimization and an extra level of protection, but cannot be relied upon due to
	 * potentially being within the scope of a larger read-write transaction.</strong>
	 * </p>
	 * <p>
	 * When a connection already exists, its isolation level may be increased, but will never be decreased.  However,
	 * the ability to change the isolation level within a transaction is driver dependent.  It is best to set the
	 * highest isolation level that will be required at the beginning of the transaction.
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
	 *                         <p>
	 *                         The connection will continue to be considered used by the allocating thread until
	 *                         released (via {@link Connection#close()}, even if the connection is shared by another
	 *                         thread.
	 *                         </p>
	 *
	 * @return  The connection to the database.
	 *          <p>
	 *          This connection may be used in try-with-resources, but any calls to {@link Connection#close()} are
	 *          ignored.  Instead, the connection is released and/or closed when this {@link DatabaseConnection} is
	 *          {@linkplain #close() closed}.
	 *          </p>
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
		FailFastConnection c = _conn;
		if(c == null) {
			// New connection
			c = database.getConnection(isolationLevel, readOnly, maxConnections, false);
			assert c.getAutoCommit();
			assert c.isReadOnly() == readOnly;
			assert c.getTransactionIsolation() == isolationLevel;
			_conn = c;
		} else {
			// Existing connection
			if(c.isReadOnly() != readOnly) {
				if(readOnly) {
					// Set back to read-only when auto-commit enabled, otherwise leave read-write
					if(c.getAutoCommit()) c.setReadOnly(true);
				} else {
					// Let driver try to set read-write (which may fail if already in transaction)
					c.setReadOnly(false);
				}
			}
			if(c.getTransactionIsolation() < isolationLevel) {
				// Let driver try to set isolation level (which may fail if already in transaction)
				c.setTransactionIsolation(isolationLevel);
			}
		}
		if(!readOnly || isolationLevel >= Connection.TRANSACTION_REPEATABLE_READ) {
			c.setAutoCommit(false);
		}
		return c;
	}

	protected static void setParam(Connection conn, PreparedStatement pstmt, int pos, Object param) throws SQLException {
		if(param == null)                     pstmt.setNull           (pos, Types.VARCHAR);
		else if(param instanceof Null)        pstmt.setNull           (pos, ((Null)param).getType());
		else if(param instanceof Array)       pstmt.setArray          (pos, (Array)param);
		else if(param instanceof BigDecimal)  pstmt.setBigDecimal     (pos, (BigDecimal)param);
		else if(param instanceof BigInteger)  pstmt.setBigDecimal     (pos, new BigDecimal((BigInteger)param));
		else if(param instanceof Blob)        pstmt.setBlob           (pos, (Blob)param);
		else if(param instanceof Boolean)     pstmt.setBoolean        (pos, (Boolean)param);
		else if(param instanceof Byte)        pstmt.setByte           (pos, (Byte)param);
		else if(param instanceof byte[])      pstmt.setBytes          (pos, (byte[])param);
		else if(param instanceof Clob)        pstmt.setClob           (pos, (Clob)param);
		else if(param instanceof Date)        pstmt.setDate           (pos, (Date)param);
		else if(param instanceof Double)      pstmt.setDouble         (pos, (Double)param);
		else if(param instanceof Float)       pstmt.setFloat          (pos, (Float)param);
		else if(param instanceof Integer)     pstmt.setInt            (pos, (Integer)param);
		else if(param instanceof InputStream) pstmt.setBinaryStream   (pos, (InputStream)param);
		else if(param instanceof Long)        pstmt.setLong           (pos, (Long)param);
		else if(param instanceof NClob)       pstmt.setNClob          (pos, (NClob)param);
		else if(param instanceof Reader)      pstmt.setCharacterStream(pos, (Reader)param);
		else if(param instanceof Ref)         pstmt.setRef            (pos, (Ref)param);
		else if(param instanceof RowId)       pstmt.setRowId          (pos, (RowId)param);
		else if(param instanceof Short)       pstmt.setShort          (pos, (Short)param);
		else if(param instanceof SQLXML)      pstmt.setSQLXML         (pos, (SQLXML)param);
		else if(param instanceof String)      pstmt.setString         (pos, (String)param);
		else if(param instanceof Time)        pstmt.setTime           (pos, (Time)param);
		else if(param instanceof Timestamp)   pstmt.setTimestamp      (pos, (Timestamp)param);
		else if(param instanceof URL)         pstmt.setURL            (pos, (URL)param);
		else if(param instanceof Enum)        pstmt.setString         (pos, ((Enum)param).name());
		else if(
			(param instanceof SQLData)
			|| (param instanceof Struct)
		) {
			pstmt.setObject(pos, param);
		} else if(param instanceof String[]) {
			pstmt.setArray(pos, conn.createArrayOf("text", (Object[])param));
		} else {
			// Defaults to string with object.toString only when the class has a valueOf(String) method that will reconstitute it in AutoObjectFactory
			Class<?> clazz = param.getClass();
			if(AutoObjectFactory.getValueOfStringMethod(clazz) != null) {
				pstmt.setString(pos, param.toString());
			} else {
				// Call setObject here, to give the database driver a chance to decide what to do
				pstmt.setObject(pos, param);
				//throw new SQLException("Unexpected parameter class: "+clazz.getName());
			}
		}
	}

	public static void setParams(Connection conn, PreparedStatement pstmt, Object ... params) throws SQLException {
		int pos = 1;
		for(Object param : params) {
			setParam(conn, pstmt, pos++, param);
		}
	}

	// TODO: Restore default isolation levels and read-only state on commit and rollback?
	public void commit() throws SQLException {
		FailFastConnection c = _conn;
		if(c != null && !c.getAutoCommit()) c.commit();
	}

	public boolean isClosed() throws SQLException {
		FailFastConnection c = _conn;
		return c == null || c.isClosed();
	}

	/**
	 * @deprecated  Please use {@link #close()}
	 */
	@Deprecated
	public void releaseConnection() throws SQLException {
		close();
	}

	/**
	 * Closes and/or releases the current connection back to the pool.
	 * Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
	 *
	 * @see  Database#release(com.aoapps.sql.failfast.FailFastConnection)
	 * @see  #close(java.lang.Throwable)
	 */
	@Override
	public void close() throws SQLException {
		FailFastConnection c = _conn;
		if(c != null) {
			_conn = null;
			database.release(c);
		}
	}

	/**
	 * Closes and/or releases the current connection back to the pool.
	 * Any {@linkplain Connection#getAutoCommit() transaction in-progress} is {@linkplain Connection#rollback() rolled-back}.
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
	 * @return  {@code true} when connected and rolled-back (or is auto-commit)
	 */
	// TODO: Rollback to the savepoint of the current sub-transaction?
	public boolean rollback() throws SQLException {
		FailFastConnection c = _conn;
		if(c != null && !c.isClosed()) {
			if(!c.getAutoCommit()) c.rollback();
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Rolls back the current connection, if have connection and is not auto-commit.
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
		} catch(Throwable t) {
			t0 = Throwables.addSuppressed(t0, t);
		}
		return t0;
	}

	/**
	 * Rolls back the current connection, if have connection and is not auto-commit, and forces the underlying
	 * connection closed via {@link Connection#abort(java.util.concurrent.Executor)}.  This close is distinct
	 * from {@link #close()}, which is intended for releasing to the underlying pool via {@link Connection#close()}.
	 *
	 * @return  {@code true} when connected and rolled-back (or is auto-commit)
	 *
	 * @see  #rollback()
	 * @see  #rollbackAndClose(java.lang.Throwable)
	 *
	 * @deprecated  Please use {@link #rollback()}.  The connection will be {@linkplain Connection#isValid(int) validated}
	 *              during {@link #close()} when there is any unresolved {@linkplain FailFastConnection#getFailFastState() fail-fast state}.
	 */
	@Deprecated
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	public boolean rollbackAndClose() throws SQLException {
		FailFastConnection c = _conn;
		if(c != null) {
			_conn = null;
			Throwable t0 = null;
			boolean rollback = false;
			try {
				rollback = !c.isClosed();
				if(rollback) {
					try {
						if(!c.getAutoCommit()) c.rollback();
					} catch(Throwable t) {
						t0 = Throwables.addSuppressed(t0, t);
					}
				}
			} finally {
				try {
					c.abort(database.getExecutors().getUnbounded());
				} catch(Throwable t) {
					t0 = Throwables.addSuppressed(t0, t);
					// Abort failed, try to close the wrapped connection directly
					t0 = AutoCloseables.closeAndCatch(t0, c.getWrapped());
					try {
						database.release(c);
					} catch(Throwable t2) {
						t0 = Throwables.addSuppressed(t0, t2);
					}
				}
			}
			if(t0 != null) {
				throw Throwables.wrap(t0, SQLException.class, SQLException::new);
			} else {
				return rollback;
			}
		} else {
			return false;
		}
	}

	/**
	 * Rolls back the current connection, if have connection and is not auto-commit, and forces the underlying
	 * connection closed via {@link Connection#abort(java.util.concurrent.Executor)}.  This close is distinct
	 * from {@link #close()}, which is intended for releasing to the underlying pool via {@link Connection#close()}.
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
	@Deprecated
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	public Throwable rollbackAndClose(Throwable t0) {
		try {
			rollbackAndClose();
		} catch(Throwable t) {
			t0 = Throwables.addSuppressed(t0, t);
		}
		return t0;
	}

	private static class StreamCloser implements Runnable {

		private final Statement stmt;
		private final ResultSet results;

		private StreamCloser(Statement stmt, ResultSet results) {
			this.stmt = stmt;
			this.results = results;
		}

		@Override
		public void run() {
			AutoCloseables.closeAndThrow(WrappedException.class, WrappedException::new, results, stmt);
		}
	}

	@Override
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch", "AssignmentToCatchBlockParameter"})
	public DoubleStream doubleStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
		Connection conn = getConnection(isolationLevel, readOnly);
		PreparedStatement pstmt = conn.prepareStatement(sql); // TODO: Use regular statements when there are no parameters?  Here and entire api?
		try {
			try {
				pstmt.setFetchSize(FETCH_SIZE);
				setParams(conn, pstmt, params);
				ResultSet results = pstmt.executeQuery();
				try {
					return StreamSupport.doubleStream(
						Spliterators.spliteratorUnknownSize(
							new PrimitiveIterator.OfDouble() {
								private double next;
								private boolean nextSet; // next may be null, so extra flag

								@Override
								public boolean hasNext() {
									try {
										if(!nextSet && results.next()) {
											next = results.getDouble(1);
											if(results.wasNull()) throw new NullDataException(results);
											nextSet = true;
										}
										return nextSet;
									} catch(Throwable t) {
										throw Throwables.wrap(t, WrappedException.class, WrappedException::new);
									}
								}

								@Override
								public double nextDouble() {
									try {
										if(nextSet) {
											nextSet = false;
											return next;
										} else if(results.next()) {
											double d = results.getDouble(1);
											if(results.wasNull()) throw new NullDataException(results);
											return d;
										} else {
											throw new NoSuchElementException();
										}
									} catch(Throwable t) {
										throw Throwables.wrap(t, WrappedException.class, WrappedException::new);
									}
								}
							},
							Spliterator.ORDERED | Spliterator.NONNULL
						),
						false
					).onClose(new StreamCloser(pstmt, results));
				} catch(Throwable t) {
					throw AutoCloseables.closeAndCatch(t, results);
				}
			} catch(Error | RuntimeException | SQLException e) {
				ErrorPrinter.addSQL(e, pstmt);
				throw e;
			}
		} catch(Throwable t) {
			throw AutoCloseables.closeAndWrap(t, SQLException.class, SQLException::new, pstmt);
		}
	}

	@Override
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch", "AssignmentToCatchBlockParameter"})
	public IntStream intStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
		Connection conn = getConnection(isolationLevel, readOnly);
		PreparedStatement pstmt = conn.prepareStatement(sql);
		try {
			try {
				pstmt.setFetchSize(FETCH_SIZE);
				setParams(conn, pstmt, params);
				ResultSet results = pstmt.executeQuery();
				try {
					return StreamSupport.intStream(
						Spliterators.spliteratorUnknownSize(
							new PrimitiveIterator.OfInt() {
								private int next;
								private boolean nextSet; // next may be null, so extra flag

								@Override
								public boolean hasNext() {
									try {
										if(!nextSet && results.next()) {
											next = results.getInt(1);
											if(results.wasNull()) throw new NullDataException(results);
											nextSet = true;
										}
										return nextSet;
									} catch(Throwable t) {
										throw Throwables.wrap(t, WrappedException.class, WrappedException::new);
									}
								}

								@Override
								public int nextInt() {
									try {
										if(nextSet) {
											nextSet = false;
											return next;
										} else if(results.next()) {
											int i = results.getInt(1);
											if(results.wasNull()) throw new NullDataException(results);
											return i;
										} else {
											throw new NoSuchElementException();
										}
									} catch(Throwable t) {
										throw Throwables.wrap(t, WrappedException.class, WrappedException::new);
									}
								}
							},
							Spliterator.ORDERED | Spliterator.NONNULL
						),
						false
					).onClose(new StreamCloser(pstmt, results));
				} catch(Throwable t) {
					throw AutoCloseables.closeAndCatch(t, results);
				}
			} catch(Error | RuntimeException | SQLException e) {
				ErrorPrinter.addSQL(e, pstmt);
				throw e;
			}
		} catch(Throwable t) {
			throw AutoCloseables.closeAndWrap(t, SQLException.class, SQLException::new, pstmt);
		}
	}

	@Override
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch", "AssignmentToCatchBlockParameter"})
	public LongStream longStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
		Connection conn = getConnection(isolationLevel, readOnly);
		PreparedStatement pstmt = conn.prepareStatement(sql);
		try {
			try {
				pstmt.setFetchSize(FETCH_SIZE);
				setParams(conn, pstmt, params);
				ResultSet results = pstmt.executeQuery();
				try {
					return StreamSupport.longStream(
						Spliterators.spliteratorUnknownSize(
							new PrimitiveIterator.OfLong() {
								private long next;
								private boolean nextSet; // next may be null, so extra flag

								@Override
								public boolean hasNext() {
									try {
										if(!nextSet && results.next()) {
											next = results.getLong(1);
											if(results.wasNull()) throw new NullDataException(results);
											nextSet = true;
										}
										return nextSet;
									} catch(Throwable t) {
										throw Throwables.wrap(t, WrappedException.class, WrappedException::new);
									}
								}

								@Override
								public long nextLong() {
									try {
										if(nextSet) {
											nextSet = false;
											return next;
										} else if(results.next()) {
											long l = results.getLong(1);
											if(results.wasNull()) throw new NullDataException(results);
											return l;
										} else {
											throw new NoSuchElementException();
										}
									} catch(Throwable t) {
										throw Throwables.wrap(t, WrappedException.class, WrappedException::new);
									}
								}
							},
							Spliterator.ORDERED | Spliterator.NONNULL
						),
						false
					).onClose(new StreamCloser(pstmt, results));
				} catch(Throwable t) {
					throw AutoCloseables.closeAndCatch(t, results);
				}
			} catch(Error | RuntimeException | SQLException e) {
				ErrorPrinter.addSQL(e, pstmt);
				throw e;
			}
		} catch(Throwable t) {
			throw AutoCloseables.closeAndWrap(t, SQLException.class, SQLException::new, pstmt);
		}
	}

	/**
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 */
	private static class ResultSetIterator<T, Ex extends Throwable> implements Iterator<T> {

		private final ObjectFactoryE<? extends T, ? extends Ex> objectFactory;
		private final ResultSet results;
		private final boolean isNullable;

		private T next;
		private boolean nextSet; // next may be null, so extra flag

		private ResultSetIterator(ObjectFactoryE<? extends T, ? extends Ex> objectFactory, boolean isNullable, ResultSet results) {
			this.objectFactory = objectFactory;
			this.results = results;
			this.isNullable = isNullable;
		}

		private T createAndCheckNullable(ResultSet results) throws SQLException, Ex {
			T t = objectFactory.createObject(results);
			if(t == null && !isNullable) throw new NullDataException(results);
			return t;
		}

		@Override
		@SuppressWarnings("UseSpecificCatch")
		public boolean hasNext() {
			try {
				if(!nextSet && results.next()) {
					next = createAndCheckNullable(results);
					nextSet = true;
				}
				return nextSet;
			} catch(Throwable t) {
				throw Throwables.wrap(t, WrappedException.class, WrappedException::new);
			}
		}

		@Override
		@SuppressWarnings("UseSpecificCatch")
		public T next() {
			try {
				if(nextSet) {
					T t = next;
					next = null;
					nextSet = false;
					return t;
				} else if(results.next()) {
					return createAndCheckNullable(results);
				} else {
					throw new NoSuchElementException();
				}
			} catch(Throwable t) {
				throw Throwables.wrap(t, WrappedException.class, WrappedException::new);
			}
		}
	}

	/**
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 */
	@Override
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch", "AssignmentToCatchBlockParameter", "fallthrough"})
	// TODO: Take an optional int for additional characteristics?  Might be useful for DISTINCT and SORTED, in particular.
	public <T, Ex extends Throwable> Stream<T> stream(int isolationLevel, boolean readOnly, Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
		Connection conn = getConnection(isolationLevel, readOnly);
		PreparedStatement pstmt = conn.prepareStatement(sql);
		try {
			try {
				pstmt.setFetchSize(FETCH_SIZE);
				setParams(conn, pstmt, params);
				ResultSet results = pstmt.executeQuery();
				try {
					Spliterator<T> spliterator;
					{
						int characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
						boolean isNullable = objectFactory.isNullable();
						if(!isNullable) characteristics |= Spliterator.NONNULL;
						int resultType = results.getType();
						switch(resultType) {
							case ResultSet.TYPE_FORWARD_ONLY :
								spliterator = Spliterators.spliteratorUnknownSize(
									new ResultSetIterator<T, Ex>(objectFactory, isNullable, results),
									characteristics
								);
								break;
							case ResultSet.TYPE_SCROLL_INSENSITIVE :
								characteristics |= Spliterator.SIZED;
								// Fall-through
							case ResultSet.TYPE_SCROLL_SENSITIVE :
								int rowCount = 0;
								if(results.last()) {
									rowCount = results.getRow();
									results.beforeFirst();
								}
								spliterator = Spliterators.spliterator(
									new ResultSetIterator<T, Ex>(objectFactory, isNullable, results),
									rowCount,
									characteristics
								);
								break;
							default :
								throw new AssertionError(resultType);
						}
					}
					return StreamSupport.stream(spliterator, false).onClose(new StreamCloser(pstmt, results));
				} catch(Throwable t) {
					throw AutoCloseables.closeAndCatch(t, results);
				}
			} catch(Error | RuntimeException | SQLException e) {
				ErrorPrinter.addSQL(e, pstmt);
				throw e;
			}
		} catch(Throwable t) {
			t = AutoCloseables.closeAndCatch(t, pstmt);
			if(eClass.isInstance(t)) throw eClass.cast(t);
			throw Throwables.wrap(t, SQLException.class, SQLException::new);
		}
	}

	/**
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 */
	@Override
	public <T, Ex extends Throwable> T queryCall(int isolationLevel, boolean readOnly, Class<? extends Ex> eClass, ResultSetCallableE<? extends T, ? extends Ex> resultSetCallable, String sql, Object ... params) throws SQLException, Ex {
		Connection conn = getConnection(isolationLevel, readOnly);
		// TODO: Use regular Statement when there are no params?  Interaction with PostgreSQL prepared statement caching?
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			try {
				pstmt.setFetchSize(FETCH_SIZE);
				setParams(conn, pstmt, params);
				try (ResultSet results = pstmt.executeQuery()) {
					return resultSetCallable.call(results);
				}
			} catch(Error | RuntimeException | SQLException e) {
				ErrorPrinter.addSQL(e, pstmt);
				throw e;
			}
		}
	}

	@Override
	public int update(String sql, Object ... params) throws SQLException {
		Connection conn = getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false);
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			try {
				setParams(conn, pstmt, params);
				return pstmt.executeUpdate();
			} catch(Error | RuntimeException | SQLException e) {
				ErrorPrinter.addSQL(e, pstmt);
				throw e;
			}
		}
	}

	@Override
	public long largeUpdate(String sql, Object ... params) throws SQLException {
		Connection conn = getConnection(Connections.DEFAULT_TRANSACTION_ISOLATION, false);
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			try {
				setParams(conn, pstmt, params);
				return pstmt.executeLargeUpdate();
			} catch(Error | RuntimeException | SQLException e) {
				ErrorPrinter.addSQL(e, pstmt);
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
