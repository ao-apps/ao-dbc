/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016, 2019, 2020  AO Industries, Inc.
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
import com.aoindustries.sql.WrappedSQLException;
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
import java.sql.Struct;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;

/**
 * A <code>DatabaseConnection</code> is used to only get actual database connections when needed.
 *
 * @see  Database
 *
 * @author  AO Industries, Inc.
 */
public class DatabaseConnection implements DatabaseAccess, AutoCloseable {

	private static final int FETCH_SIZE = 1000;

	private final Database database;

	private Connection _conn;

	protected DatabaseConnection(Database database) {
	   this.database=database;
	}

	public Database getDatabase() {
		return database;
	}

	/**
	 * @see  #getConnection(int, boolean, int)
	 */
	public Connection getConnection(int isolationLevel, boolean readOnly) throws SQLException {
		return getConnection(isolationLevel, readOnly, 1);
	}

	/**
	 * Gets the connection to the underlying database.
	 * <p>
	 * Uses a deferred connection strategy.  If not previously connected, establishes the connection now.  This allows
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
	 *
	 * @see  Connection#setReadOnly(boolean)
	 * @see  Connection#setTransactionIsolation(int)
	 * @see  Connection#setAutoCommit(boolean)
	 */
	public Connection getConnection(int isolationLevel, boolean readOnly, int maxConnections) throws SQLException {
		Connection c = _conn;
		if(c == null) {
			// New connection
			c = database.getConnection(isolationLevel, readOnly, maxConnections);
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
		if(param==null) pstmt.setNull(pos, Types.VARCHAR);
		else if(param instanceof Null) pstmt.setNull(pos, ((Null)param).getType());
		else if(param instanceof Array) pstmt.setArray(pos, (Array)param);
		else if(param instanceof BigDecimal) pstmt.setBigDecimal(pos, (BigDecimal)param);
		else if(param instanceof BigInteger) pstmt.setBigDecimal(pos, new BigDecimal((BigInteger)param));
		else if(param instanceof Blob) pstmt.setBlob(pos, (Blob)param);
		else if(param instanceof Boolean) pstmt.setBoolean(pos, (Boolean)param);
		else if(param instanceof Byte) pstmt.setByte(pos, (Byte)param);
		else if(param instanceof byte[]) pstmt.setBytes(pos, (byte[])param);
		else if(param instanceof Clob) pstmt.setClob(pos, (Clob)param);
		else if(param instanceof Date) pstmt.setDate(pos, (Date)param);
		else if(param instanceof Double) pstmt.setDouble(pos, (Double)param);
		else if(param instanceof Float) pstmt.setFloat(pos, (Float)param);
		else if(param instanceof Integer) pstmt.setInt(pos, (Integer)param);
		else if(param instanceof InputStream) pstmt.setBinaryStream(pos, (InputStream)param);
		else if(param instanceof Long) pstmt.setLong(pos, (Long)param);
		else if(param instanceof NClob) pstmt.setNClob(pos, (NClob)param);
		else if(param instanceof Reader) pstmt.setCharacterStream(pos, (Reader)param);
		else if(param instanceof Ref) pstmt.setRef(pos, (Ref)param);
		else if(param instanceof RowId) pstmt.setRowId(pos, (RowId)param);
		else if(param instanceof Short) pstmt.setShort(pos, (Short)param);
		else if(param instanceof SQLXML) pstmt.setSQLXML(pos, (SQLXML)param);
		else if(param instanceof String) pstmt.setString(pos, (String)param);
		else if(param instanceof Time) pstmt.setTime(pos, (Time)param);
		else if(param instanceof Timestamp) pstmt.setTimestamp(pos, (Timestamp)param);
		else if(param instanceof URL) pstmt.setURL(pos, (URL)param);
		else if(param instanceof Enum) pstmt.setString(pos, ((Enum)param).name());
		else if(
			(param instanceof SQLData)
			|| (param instanceof Struct)
		) pstmt.setObject(pos, param);
		else if(param instanceof String[]) {
			pstmt.setArray(pos, conn.createArrayOf("text", (Object[])param));
		} else {
			// Defaults to string with object.toString only when the class has a valueOf(String) method that will reconstitute it in AutoObjectFactory
			Class<?> clazz = param.getClass();
			if(AutoObjectFactory.getValueOfStringMethod(clazz)!=null) pstmt.setString(pos, param.toString());
			else {
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

	@Override
	public <T,E extends Exception> T executeQuery(int isolationLevel, boolean readOnly, Class<E> eClass, ResultSetHandlerE<T,E> resultSetHandler, String sql, Object ... params) throws SQLException, E {
		Connection conn = getConnection(isolationLevel, readOnly);
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			try {
				pstmt.setFetchSize(FETCH_SIZE);
				setParams(conn, pstmt, params);
				try (ResultSet results = pstmt.executeQuery()) {
					return resultSetHandler.handleResultSet(results);
				}
			} catch(SQLException err) {
				throw new WrappedSQLException(err, pstmt);
			}
		}
	}

	@Override
	public int executeUpdate(String sql, Object ... params) throws SQLException {
		Connection conn = getConnection(Connection.TRANSACTION_READ_COMMITTED, false);
		try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
			try {
				setParams(conn, pstmt, params);
				return pstmt.executeUpdate();
			} catch(SQLException err) {
				throw new WrappedSQLException(err, pstmt);
			}
		}
	}

	public void commit() throws SQLException {
		Connection c = _conn;
		if(c != null && !c.getAutoCommit()) c.commit();
	}

	public boolean isClosed() throws SQLException {
		Connection c = _conn;
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
	 */
	@Override
	public void close() throws SQLException {
		Connection c = _conn;
		if(c != null) {
			_conn = null;
			database.releaseConnection(c);
		}
	}

	/**
	 * Closes and/or releases the current connection back to the pool.
	 *
	 * @param  t  Any exceptions will be added here via {@link Throwable#addSuppressed(java.lang.Throwable)}
	 */
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	public void close(Throwable t) {
		try {
			close();
		} catch(ThreadDeath td) {
			throw td;
		} catch(Throwable t2) {
			t.addSuppressed(t2);
		}
	}

	/**
	 * Rolls back the current connection, if have connection and is not auto-commit.
	 *
	 * @return  {@code true} when connected and rolled-back (or is auto-commit)
	 */
	public boolean rollback() throws SQLException {
		Connection c = _conn;
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
	 * @param  t  Any exceptions will be added here via {@link Throwable#addSuppressed(java.lang.Throwable)}
	 *
	 * @return  {@code true} when connected and rolled-back (or is auto-commit)
	 */
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	public boolean rollback(Throwable t) {
		boolean rolledBack = false;
		try {
			rolledBack = rollback();
		} catch(ThreadDeath td) {
			throw td;
		} catch(Throwable t2) {
			t.addSuppressed(t2);
		}
		return rolledBack;
	}

	/**
	 * Rolls back the current connection, if have connection and is not auto-commit, and forces the underlying
	 * connection closed.  This close is distinct from {@link #close()}, which is intended for
	 * releasing to the underlying pool.
	 *
	 * @return  {@code true} when connected and rolled-back (or is auto-commit)
	 */
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	public boolean rollbackAndClose() throws SQLException {
		Connection c = _conn;
		if(c != null) {
			_conn = null;
			Throwable t1 = null;
			boolean rollback = false;
			try {
				rollback = !c.isClosed();
				if(rollback) {
					try {
						if(!c.getAutoCommit()) c.rollback();
					} catch(ThreadDeath td) {
						throw td;
					} catch(Throwable t) {
						t1 = t;
					} finally {
						try {
							c.close();
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
			} finally {
				try {
					database.releaseConnection(c);
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
			if(t1 != null) {
				if(t1 instanceof Error) throw (Error)t1;
				if(t1 instanceof RuntimeException) throw (RuntimeException)t1;
				if(t1 instanceof SQLException) throw (SQLException)t1;
				throw new WrappedException(t1);
			}
			return rollback;
		} else {
			return false;
		}
	}

	/**
	 * Rolls back the current connection, if have connection and is not auto-commit, and forces the underlying
	 * connection closed.  This close is distinct from {@link #close(java.lang.Throwable)}, which is intended for
	 * releasing to the underlying pool.
	 *
	 * @param  t  Any exceptions will be added here via {@link Throwable#addSuppressed(java.lang.Throwable)}
	 *
	 * @return  {@code true} when connected and rolled-back (or is auto-commit)
	 */
	@SuppressWarnings({"UseSpecificCatch", "TooBroadCatch"})
	public boolean rollbackAndClose(Throwable t) {
		boolean rolledBack = false;
		try {
			rolledBack = rollbackAndClose();
		} catch(ThreadDeath td) {
			throw td;
		} catch(Throwable t2) {
			t.addSuppressed(t2);
		}
		return rolledBack;
	}
}
