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

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A {@link ResultSet} from a query that only returns a single row and the cursor is already positioned.
 *
 * <p>Cursor navigation methods are marked deprecated to discourage their use, since this is a result set from a query
 * that only returns a single row and the cursor is already positioned.  Calling these methods will still pass-through
 * to the wrapped {@link ResultSet}, though.</p>
 *
 * @author  AO Industries, Inc.
 */
public interface SingleResult extends ResultSet {

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean next() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @throws ExtraRowException when more rows are retrieved than expected.  {@link ResultSet#next()} will be called
   *                           once upon close.  If it returns {@code true}, it indicates multiple rows were returned
   *                           while only a single one is expected.
   */
  @Override
  void close() throws ExtraRowException, SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean isBeforeFirst() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean isAfterLast() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean isFirst() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean isLast() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  void beforeFirst() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  void afterLast() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean first() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean last() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  int getRow() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean absolute(int row) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean relative(int rows) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  boolean previous() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  void setFetchDirection(int direction) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  int getFetchDirection() throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  void setFetchSize(int rows) throws SQLException;

  /**
   * {@inheritDoc}
   *
   * @deprecated  This query only returns a single row and the cursor is already positioned.
   */
  @Deprecated(forRemoval = false)
  @Override
  int getFetchSize() throws SQLException;
}
