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

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLType;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;

/**
 * A {@link ResultSet} from a query that is expected to be read-only.
 *
 * <p>Update methods are marked deprecated to discourage their use, since this is a result set from a query that
 * could be read-only and is only intended for read access.  Calling these methods will still pass-through to the
 * wrapped {@link ResultSet}, though.</p>
 *
 * @author  AO Industries, Inc.
 */
public interface ReadOnlyResultSet extends ResultSet {

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean rowUpdated() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean rowInserted() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean rowDeleted() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNull(int columnIndex) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBoolean(int columnIndex, boolean x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateByte(int columnIndex, byte x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateShort(int columnIndex, short x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateInt(int columnIndex, int x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateLong(int columnIndex, long x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateFloat(int columnIndex, float x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateDouble(int columnIndex, double x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateString(int columnIndex, String x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBytes(int columnIndex, byte[] x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateDate(int columnIndex, Date x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateTime(int columnIndex, Time x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateTimestamp(int columnIndex, Timestamp x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateObject(int columnIndex, Object x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNull(String columnLabel) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBoolean(String columnLabel, boolean x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateByte(String columnLabel, byte x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateShort(String columnLabel, short x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateInt(String columnLabel, int x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateLong(String columnLabel, long x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateFloat(String columnLabel, float x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateDouble(String columnLabel, double x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateString(String columnLabel, String x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBytes(String columnLabel, byte[] x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateDate(String columnLabel, Date x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateTime(String columnLabel, Time x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateTimestamp(String columnLabel, Timestamp x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateObject(String columnLabel, Object x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void insertRow() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateRow() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void deleteRow() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void cancelRowUpdates() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void moveToInsertRow() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void moveToCurrentRow() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateRef(int columnIndex, Ref x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateRef(String columnLabel, Ref x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBlob(int columnIndex, Blob x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBlob(String columnLabel, Blob x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateClob(int columnIndex, Clob x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateClob(String columnLabel, Clob x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateArray(int columnIndex, Array x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateArray(String columnLabel, Array x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateRowId(int columnIndex, RowId x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateRowId(String columnLabel, RowId x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNString(int columnIndex, String nString) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNString(String columnLabel, String nString) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNClob(int columnIndex, NClob nClob) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNClob(String columnLabel, NClob nClob) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateClob(int columnIndex, Reader reader, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateClob(String columnLabel, Reader reader, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNClob(int columnIndex, Reader reader, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNClob(String columnLabel, Reader reader, long length) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNCharacterStream(int columnIndex, Reader x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateAsciiStream(int columnIndex, InputStream x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBinaryStream(int columnIndex, InputStream x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateCharacterStream(int columnIndex, Reader x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateAsciiStream(String columnLabel, InputStream x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBinaryStream(String columnLabel, InputStream x) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateCharacterStream(String columnLabel, Reader reader) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBlob(int columnIndex, InputStream inputStream) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateBlob(String columnLabel, InputStream inputStream) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateClob(int columnIndex, Reader reader) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateClob(String columnLabel, Reader reader) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNClob(int columnIndex, Reader reader) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateNClob(String columnLabel, Reader reader) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateObject(int columnIndex, Object x, SQLType targetSqlType, int scaleOrLength)  throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateObject(String columnLabel, Object x, SQLType targetSqlType, int scaleOrLength) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateObject(int columnIndex, Object x, SQLType targetSqlType) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This result is only intended for read access.
   */
  @Deprecated(forRemoval = false)
  @Override
  void updateObject(String columnLabel, Object x, SQLType targetSqlType) throws SQLException;
}
