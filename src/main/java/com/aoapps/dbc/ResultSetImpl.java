/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2026  AO Industries, Inc.
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

import com.aoapps.lang.util.ErrorPrinter;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
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
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;

/**
 * The result of a query or update in-progress that must be {@linkplain #close() closed}.  Without being closed,
 * resources could not be freed in a timely manner, and some additional required actions might not be taken.
 *
 * <p>This is the behind-the-scenes implementation.  This implements {@link ReadOnlySingleResult} so that it
 * can be safely cast to any of {@link ResultSet}, {@link ReadOnlyResultSet}, {@link SingleResult}, or
 * {@link ReadOnlySingleResult}.  Various methods return different interfaces matching their usage pattern,
 * but they all share this as their concrete implementation.</p>
 *
 * @author  AO Industries, Inc.
 */
abstract class ResultSetImpl implements ReadOnlySingleResult {

  protected final ResultSet wrapped;

  ResultSetImpl(ResultSet wrapped) {
    this.wrapped = wrapped;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final <T> T unwrap(Class<T> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return iface.cast(this);
    }
    return wrapped.unwrap(iface);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isWrapperFor(Class<?> iface) throws SQLException {
    if (iface.isInstance(this)) {
      return true;
    }
    return wrapped.isWrapperFor(iface);
  }

  @Override
  @SuppressWarnings("deprecation")
  public final boolean next() throws SQLException {
    return wrapped.next();
  }

  /**
   * Unlike typical implementations of {@link ResultSet#close()}, the implementations of this method perform
   * additional clean-up, depending on the usage.  Examples include:
   *
   * <ol>
   * <li>Verifying no unexpected additional results to enforce {@link ExtraRowException}</li>
   * <li>Registering specific SQL statement in {@link ErrorPrinter#addSql(java.lang.Throwable, java.sql.PreparedStatement)}
   *     during exception handling</li>
   * <li>Closing the related {@link PreparedStatement}</li>
   * <li>Managing transaction boundaries</li>
   * <li>Closing the related {@link Connection}, which will typically release it back to the pool.</li>
   * </ol>
   */
  @Override
  public abstract void close() throws SQLException;

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean wasNull() throws SQLException {
    return wrapped.wasNull();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getString(int columnIndex) throws SQLException {
    return wrapped.getString(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean getBoolean(int columnIndex) throws SQLException {
    return wrapped.getBoolean(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final byte getByte(int columnIndex) throws SQLException {
    return wrapped.getByte(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final short getShort(int columnIndex) throws SQLException {
    return wrapped.getShort(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int getInt(int columnIndex) throws SQLException {
    return wrapped.getInt(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final long getLong(int columnIndex) throws SQLException {
    return wrapped.getLong(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final float getFloat(int columnIndex) throws SQLException {
    return wrapped.getFloat(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final double getDouble(int columnIndex) throws SQLException {
    return wrapped.getDouble(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated(since = "1.2")
  public final BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
    return wrapped.getBigDecimal(columnIndex, scale);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final byte[] getBytes(int columnIndex) throws SQLException {
    return wrapped.getBytes(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Date getDate(int columnIndex) throws SQLException {
    return wrapped.getDate(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Time getTime(int columnIndex) throws SQLException {
    return wrapped.getTime(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Timestamp getTimestamp(int columnIndex) throws SQLException {
    return wrapped.getTimestamp(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final InputStream getAsciiStream(int columnIndex) throws SQLException {
    return wrapped.getAsciiStream(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated(since = "1.2")
  public final InputStream getUnicodeStream(int columnIndex) throws SQLException {
    return wrapped.getUnicodeStream(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final InputStream getBinaryStream(int columnIndex) throws SQLException {
    return wrapped.getBinaryStream(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getString(String columnLabel) throws SQLException {
    return wrapped.getString(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean getBoolean(String columnLabel) throws SQLException {
    return wrapped.getBoolean(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final byte getByte(String columnLabel) throws SQLException {
    return wrapped.getByte(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final short getShort(String columnLabel) throws SQLException {
    return wrapped.getShort(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int getInt(String columnLabel) throws SQLException {
    return wrapped.getInt(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final long getLong(String columnLabel) throws SQLException {
    return wrapped.getLong(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final float getFloat(String columnLabel) throws SQLException {
    return wrapped.getFloat(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final double getDouble(String columnLabel) throws SQLException {
    return wrapped.getDouble(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated(since = "1.2")
  public final BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
    return wrapped.getBigDecimal(columnLabel, scale);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final byte[] getBytes(String columnLabel) throws SQLException {
    return wrapped.getBytes(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Date getDate(String columnLabel) throws SQLException {
    return wrapped.getDate(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Time getTime(String columnLabel) throws SQLException {
    return wrapped.getTime(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Timestamp getTimestamp(String columnLabel) throws SQLException {
    return wrapped.getTimestamp(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final InputStream getAsciiStream(String columnLabel) throws SQLException {
    return wrapped.getAsciiStream(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Deprecated(since = "1.2")
  public final InputStream getUnicodeStream(String columnLabel) throws SQLException {
    return wrapped.getUnicodeStream(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final InputStream getBinaryStream(String columnLabel) throws SQLException {
    return wrapped.getBinaryStream(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final SQLWarning getWarnings() throws SQLException {
    return wrapped.getWarnings();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void clearWarnings() throws SQLException {
    wrapped.clearWarnings();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getCursorName() throws SQLException {
    return wrapped.getCursorName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final ResultSetMetaData getMetaData() throws SQLException {
    return wrapped.getMetaData();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object getObject(int columnIndex) throws SQLException {
    return wrapped.getObject(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object getObject(String columnLabel) throws SQLException {
    return wrapped.getObject(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int findColumn(String columnLabel) throws SQLException {
    return wrapped.findColumn(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Reader getCharacterStream(int columnIndex) throws SQLException {
    return wrapped.getCharacterStream(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Reader getCharacterStream(String columnLabel) throws SQLException {
    return wrapped.getCharacterStream(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    return wrapped.getBigDecimal(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final BigDecimal getBigDecimal(String columnLabel) throws SQLException {
    return wrapped.getBigDecimal(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean isBeforeFirst() throws SQLException {
    return wrapped.isBeforeFirst();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean isAfterLast() throws SQLException {
    return wrapped.isAfterLast();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean isFirst() throws SQLException {
    return wrapped.isFirst();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean isLast() throws SQLException {
    return wrapped.isLast();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void beforeFirst() throws SQLException {
    wrapped.beforeFirst();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void afterLast() throws SQLException {
    wrapped.afterLast();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean first() throws SQLException {
    return wrapped.first();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean last() throws SQLException {
    return wrapped.last();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final int getRow() throws SQLException {
    return wrapped.getRow();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean absolute(int row) throws SQLException {
    return wrapped.absolute(row);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean relative(int rows) throws SQLException {
    return wrapped.relative(rows);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean previous() throws SQLException {
    return wrapped.previous();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void setFetchDirection(int direction) throws SQLException {
    wrapped.setFetchDirection(direction);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final int getFetchDirection() throws SQLException {
    return wrapped.getFetchDirection();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void setFetchSize(int rows) throws SQLException {
    wrapped.setFetchSize(rows);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final int getFetchSize() throws SQLException {
    return wrapped.getFetchSize();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int getType() throws SQLException {
    return wrapped.getType();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int getConcurrency() throws SQLException {
    return wrapped.getConcurrency();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean rowUpdated() throws SQLException {
    return wrapped.rowUpdated();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean rowInserted() throws SQLException {
    return wrapped.rowInserted();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final boolean rowDeleted() throws SQLException {
    return wrapped.rowDeleted();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNull(int columnIndex) throws SQLException {
    wrapped.updateNull(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBoolean(int columnIndex, boolean x) throws SQLException {
    wrapped.updateBoolean(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateByte(int columnIndex, byte x) throws SQLException {
    wrapped.updateByte(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateShort(int columnIndex, short x) throws SQLException {
    wrapped.updateShort(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateInt(int columnIndex, int x) throws SQLException {
    wrapped.updateInt(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateLong(int columnIndex, long x) throws SQLException {
    wrapped.updateLong(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateFloat(int columnIndex, float x) throws SQLException {
    wrapped.updateFloat(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateDouble(int columnIndex, double x) throws SQLException {
    wrapped.updateDouble(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
    wrapped.updateBigDecimal(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateString(int columnIndex, String x) throws SQLException {
    wrapped.updateString(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBytes(int columnIndex, byte[] x) throws SQLException {
    wrapped.updateBytes(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateDate(int columnIndex, Date x) throws SQLException {
    wrapped.updateDate(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateTime(int columnIndex, Time x) throws SQLException {
    wrapped.updateTime(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateTimestamp(int columnIndex, Timestamp x) throws SQLException {
    wrapped.updateTimestamp(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException {
    wrapped.updateAsciiStream(columnIndex, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {
    wrapped.updateBinaryStream(columnIndex, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException {
    wrapped.updateCharacterStream(columnIndex, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException {
    wrapped.updateObject(columnIndex, x, scaleOrLength);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateObject(int columnIndex, Object x) throws SQLException {
    wrapped.updateObject(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNull(String columnLabel) throws SQLException {
    wrapped.updateNull(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBoolean(String columnLabel, boolean x) throws SQLException {
    wrapped.updateBoolean(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateByte(String columnLabel, byte x) throws SQLException {
    wrapped.updateByte(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateShort(String columnLabel, short x) throws SQLException {
    wrapped.updateShort(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateInt(String columnLabel, int x) throws SQLException {
    wrapped.updateInt(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateLong(String columnLabel, long x) throws SQLException {
    wrapped.updateLong(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateFloat(String columnLabel, float x) throws SQLException {
    wrapped.updateFloat(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateDouble(String columnLabel, double x) throws SQLException {
    wrapped.updateDouble(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException {
    wrapped.updateBigDecimal(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateString(String columnLabel, String x) throws SQLException {
    wrapped.updateString(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBytes(String columnLabel, byte[] x) throws SQLException {
    wrapped.updateBytes(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateDate(String columnLabel, Date x) throws SQLException {
    wrapped.updateDate(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateTime(String columnLabel, Time x) throws SQLException {
    wrapped.updateTime(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateTimestamp(String columnLabel, Timestamp x) throws SQLException {
    wrapped.updateTimestamp(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException {
    wrapped.updateAsciiStream(columnLabel, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException {
    wrapped.updateBinaryStream(columnLabel, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException {
    wrapped.updateCharacterStream(columnLabel, reader, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException {
    wrapped.updateObject(columnLabel, x, scaleOrLength);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateObject(String columnLabel, Object x) throws SQLException {
    wrapped.updateObject(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void insertRow() throws SQLException {
    wrapped.insertRow();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateRow() throws SQLException {
    wrapped.updateRow();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void deleteRow() throws SQLException {
    wrapped.deleteRow();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final void refreshRow() throws SQLException {
    wrapped.refreshRow();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void cancelRowUpdates() throws SQLException {
    wrapped.cancelRowUpdates();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void moveToInsertRow() throws SQLException {
    wrapped.moveToInsertRow();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void moveToCurrentRow() throws SQLException {
    wrapped.moveToCurrentRow();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Statement getStatement() throws SQLException {
    return wrapped.getStatement();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
    return wrapped.getObject(columnIndex, map);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Ref getRef(int columnIndex) throws SQLException {
    return wrapped.getRef(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Blob getBlob(int columnIndex) throws SQLException {
    return wrapped.getBlob(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Clob getClob(int columnIndex) throws SQLException {
    return wrapped.getClob(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Array getArray(int columnIndex) throws SQLException {
    return wrapped.getArray(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
    return wrapped.getObject(columnLabel, map);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Ref getRef(String columnLabel) throws SQLException {
    return wrapped.getRef(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Blob getBlob(String columnLabel) throws SQLException {
    return wrapped.getBlob(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Clob getClob(String columnLabel) throws SQLException {
    return wrapped.getClob(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Array getArray(String columnLabel) throws SQLException {
    return wrapped.getArray(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Date getDate(int columnIndex, Calendar cal) throws SQLException {
    return wrapped.getDate(columnIndex, cal);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Date getDate(String columnLabel, Calendar cal) throws SQLException {
    return wrapped.getDate(columnLabel, cal);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Time getTime(int columnIndex, Calendar cal) throws SQLException {
    return wrapped.getTime(columnIndex, cal);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Time getTime(String columnLabel, Calendar cal) throws SQLException {
    return wrapped.getTime(columnLabel, cal);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
    return wrapped.getTimestamp(columnIndex, cal);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
    return wrapped.getTimestamp(columnLabel, cal);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final URL getURL(int columnIndex) throws SQLException {
    return wrapped.getURL(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final URL getURL(String columnLabel) throws SQLException {
    return wrapped.getURL(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateRef(int columnIndex, Ref x) throws SQLException {
    wrapped.updateRef(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateRef(String columnLabel, Ref x) throws SQLException {
    wrapped.updateRef(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBlob(int columnIndex, Blob x) throws SQLException {
    wrapped.updateBlob(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBlob(String columnLabel, Blob x) throws SQLException {
    wrapped.updateBlob(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateClob(int columnIndex, Clob x) throws SQLException {
    wrapped.updateClob(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateClob(String columnLabel, Clob x) throws SQLException {
    wrapped.updateClob(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateArray(int columnIndex, Array x) throws SQLException {
    wrapped.updateArray(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateArray(String columnLabel, Array x) throws SQLException {
    wrapped.updateArray(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final RowId getRowId(int columnIndex) throws SQLException {
    return wrapped.getRowId(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final RowId getRowId(String columnLabel) throws SQLException {
    return wrapped.getRowId(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateRowId(int columnIndex, RowId x) throws SQLException {
    wrapped.updateRowId(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateRowId(String columnLabel, RowId x) throws SQLException {
    wrapped.updateRowId(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final int getHoldability() throws SQLException {
    return wrapped.getHoldability();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final boolean isClosed() throws SQLException {
    return wrapped.isClosed();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNString(int columnIndex, String nString) throws SQLException {
    wrapped.updateNString(columnIndex, nString);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNString(String columnLabel, String nString) throws SQLException {
    wrapped.updateNString(columnLabel, nString);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    wrapped.updateNClob(columnIndex, nClob);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNClob(String columnLabel, NClob nClob) throws SQLException {
    wrapped.updateNClob(columnLabel, nClob);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final NClob getNClob(int columnIndex) throws SQLException {
    return wrapped.getNClob(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final NClob getNClob(String columnLabel) throws SQLException {
    return wrapped.getNClob(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final SQLXML getSQLXML(int columnIndex) throws SQLException {
    return wrapped.getSQLXML(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final SQLXML getSQLXML(String columnLabel) throws SQLException {
    return wrapped.getSQLXML(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
    wrapped.updateSQLXML(columnIndex, xmlObject);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
    wrapped.updateSQLXML(columnLabel, xmlObject);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getNString(int columnIndex) throws SQLException {
    return wrapped.getNString(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final String getNString(String columnLabel) throws SQLException {
    return wrapped.getNString(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Reader getNCharacterStream(int columnIndex) throws SQLException {
    return wrapped.getNCharacterStream(columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final Reader getNCharacterStream(String columnLabel) throws SQLException {
    return wrapped.getNCharacterStream(columnLabel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    wrapped.updateNCharacterStream(columnIndex, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    wrapped.updateNCharacterStream(columnLabel, reader, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
    wrapped.updateAsciiStream(columnIndex, x, columnIndex);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
    wrapped.updateBinaryStream(columnIndex, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    wrapped.updateCharacterStream(columnIndex, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
    wrapped.updateAsciiStream(columnLabel, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
    wrapped.updateBinaryStream(columnLabel, x, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    wrapped.updateCharacterStream(columnLabel, reader, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
    wrapped.updateBlob(columnIndex, inputStream, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
    wrapped.updateBlob(columnLabel, inputStream, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
    wrapped.updateClob(columnIndex, reader, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
    wrapped.updateClob(columnLabel, reader, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
    wrapped.updateNClob(columnIndex, reader, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
    wrapped.updateNClob(columnLabel, reader, length);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
    wrapped.updateNCharacterStream(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
    wrapped.updateNCharacterStream(columnLabel, reader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
    wrapped.updateAsciiStream(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
    wrapped.updateBinaryStream(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
    wrapped.updateCharacterStream(columnIndex, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
    wrapped.updateAsciiStream(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
    wrapped.updateBinaryStream(columnLabel, x);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
    wrapped.updateCharacterStream(columnLabel, reader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
    wrapped.updateBlob(columnIndex, inputStream);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
    wrapped.updateBlob(columnLabel, inputStream);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateClob(int columnIndex, Reader reader) throws SQLException {
    wrapped.updateClob(columnIndex, reader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateClob(String columnLabel, Reader reader) throws SQLException {
    wrapped.updateClob(columnLabel, reader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNClob(int columnIndex, Reader reader) throws SQLException {
    wrapped.updateNClob(columnIndex, reader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateNClob(String columnLabel, Reader reader) throws SQLException {
    wrapped.updateNClob(columnLabel, reader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
    return wrapped.getObject(columnIndex, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
    return wrapped.getObject(columnLabel, type);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateObject(int columnIndex, Object x, SQLType targetSqlType, int scaleOrLength)  throws SQLException {
    wrapped.updateObject(columnIndex, x, targetSqlType, scaleOrLength);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateObject(String columnLabel, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException {
    wrapped.updateObject(columnLabel, x, targetSqlType, scaleOrLength);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateObject(int columnIndex, Object x, SQLType targetSqlType) throws SQLException {
    wrapped.updateObject(columnIndex, x, targetSqlType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("deprecation")
  public final void updateObject(String columnLabel, Object x, SQLType targetSqlType) throws SQLException {
    wrapped.updateObject(columnLabel, x, targetSqlType);
  }
}
