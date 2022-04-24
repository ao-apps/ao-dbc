/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2015, 2018, 2020, 2021, 2022  AO Industries, Inc.
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
import com.aoapps.collections.IntList;
import com.aoapps.collections.LongList;
import com.aoapps.sql.Connections;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * Wraps and simplifies access to a JDBC database.
 *
 * @author  AO Industries, Inc.
 */
// TODO: A set of getConnection methods, as found common between Database and DatabaseConnection?
//       Impact on commit() or rollback on close()?
public interface DatabaseAccess {

  /**
   * These may be used as parameters to represent null values of specific types.
   *
   * @see  Types
   * @see  PreparedStatement#setNull(int, int)
   */
  public enum Null {
    BIT(Types.BIT),
    TINYINT(Types.TINYINT),
    SMALLINT(Types.SMALLINT),
    INTEGER(Types.INTEGER),
    BIGINT(Types.BIGINT),
    FLOAT(Types.FLOAT),
    REAL(Types.REAL),
    DOUBLE(Types.DOUBLE),
    NUMERIC(Types.NUMERIC),
    DECIMAL(Types.DECIMAL),
    CHAR(Types.CHAR),
    VARCHAR(Types.VARCHAR),
    LONGVARCHAR(Types.LONGVARCHAR),
    DATE(Types.DATE),
    TIME(Types.TIME),
    TIMESTAMP(Types.TIMESTAMP),
    BINARY(Types.BINARY),
    VARBINARY(Types.VARBINARY),
    LONGVARBINARY(Types.LONGVARBINARY),
    NULL(Types.NULL),
    OTHER(Types.OTHER),
    JAVA_OBJECT(Types.JAVA_OBJECT),
    DISTINCT(Types.DISTINCT),
    STRUCT(Types.STRUCT),
    ARRAY(Types.ARRAY),
    BLOB(Types.BLOB),
    CLOB(Types.CLOB),
    REF(Types.REF),
    DATALINK(Types.DATALINK),
    BOOLEAN(Types.BOOLEAN),
    /* JDBC 4.0 Types */
    ROWID(Types.ROWID),
    NCHAR(Types.NCHAR),
    NVARCHAR(Types.NVARCHAR),
    LONGNVARCHAR(Types.LONGNVARCHAR),
    NCLOB(Types.NCLOB),
    SQLXML(Types.SQLXML),
    /* JDBC 4.2 Types */
    REF_CURSOR(Types.REF_CURSOR),
    TIME_WITH_TIMEZONE(Types.TIME_WITH_TIMEZONE),
    TIMESTAMP_WITH_TIMEZONE(Types.TIMESTAMP_WITH_TIMEZONE);

    private final int type;

    private Null(int type) {
      this.type = type;
    }

    /**
     * @see  Types
     * @see  PreparedStatement#setNull(int, int)
     */
    public int getType() {
      return type;
    }
  }

  /**
   * Read-only query the database with a {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #updateBigDecimal(java.lang.String, java.lang.Object...)
   */
  default BigDecimal queryBigDecimal(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryBigDecimal(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default BigDecimal executeBigDecimalQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryBigDecimal(sql, params);
  }

  /**
   * Read-write query the database with a {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #queryBigDecimal(java.lang.String, java.lang.Object...)
   */
  default BigDecimal updateBigDecimal(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateBigDecimal(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default BigDecimal executeBigDecimalUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return updateBigDecimal(sql, params);
  }

  /**
   * Query the database with a {@link BigDecimal} return type.
   *
   * @return  The value or {@code null} when no row and row not required, or when row with null value.
   */
  default BigDecimal queryBigDecimal(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, ObjectFactories.BigDecimal, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryBigDecimal(int, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default BigDecimal executeBigDecimalQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryBigDecimal(isolationLevel, readOnly, rowRequired, sql, params);
  }

  /**
   * Read-only query the database with a {@code boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #updateBoolean(java.lang.String, java.lang.Object...)
   */
  default boolean queryBoolean(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryBoolean(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default boolean executeBooleanQuery(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBoolean(sql, params);
  }

  /**
   * Read-write query the database with a {@code boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #queryBoolean(java.lang.String, java.lang.Object...)
   */
  default boolean updateBoolean(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateBoolean(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default boolean executeBooleanUpdate(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return updateBoolean(sql, params);
  }

  /**
   * Query the database with a {@link Boolean} return type.
   *
   * @return  The value or {@code null} when no row and row not required.
   *
   * @throws  NullDataException  When has a row, but with NULL value.
   */
  default Boolean queryBoolean(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        ObjectFactories.notNull(ObjectFactories.Boolean),
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryBoolean(int, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Boolean executeBooleanQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBoolean(isolationLevel, readOnly, rowRequired, sql, params);
  }

  /**
   * Read-only query the database with a {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #updateByteArray(java.lang.String, java.lang.Object...)
   */
  default byte[] queryByteArray(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryByteArray(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default byte[] executeByteArrayQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryByteArray(sql, params);
  }

  /**
   * Read-write query the database with a {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #queryByteArray(java.lang.String, java.lang.Object...)
   */
  default byte[] updateByteArray(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateByteArray(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default byte[] executeByteArrayUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return updateByteArray(sql, params);
  }

  /**
   * Query the database with a {@code byte[]} return type.
   *
   * @return  The value or {@code null} when no row and row not required, or when row with null value.
   */
  default byte[] queryByteArray(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, ObjectFactories.ByteArray, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryByteArray(int, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default byte[] executeByteArrayQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryByteArray(isolationLevel, readOnly, rowRequired, sql, params);
  }

  /**
   * Read-only query the database with a {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #updateDate(java.lang.String, java.lang.Object...)
   */
  default Date queryDate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryDate(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Date executeDateQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryDate(sql, params);
  }

  /**
   * Read-write query the database with a {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #queryDate(java.lang.String, java.lang.Object...)
   */
  default Date updateDate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateDate(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Date executeDateUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return updateDate(sql, params);
  }

  /**
   * Query the database with a {@link Date} return type.
   *
   * @return  The value or {@code null} when no row and row not required, or when row with null value.
   */
  default Date queryDate(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, ObjectFactories.Date, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryDate(int, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Date executeDateQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryDate(isolationLevel, readOnly, rowRequired, sql, params);
  }

  /**
   * Read-only query the database with a {@code double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #updateDouble(java.lang.String, java.lang.Object...)
   */
  default double queryDouble(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * Read-write query the database with a {@code double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #queryDouble(java.lang.String, java.lang.Object...)
   */
  default double updateDouble(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * Query the database with a {@link Double} return type.
   *
   * @return  The value or {@code null} when no row and row not required.
   *
   * @throws  NullDataException  When has a row, but with NULL value.
   */
  default Double queryDouble(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        ObjectFactories.notNull(ObjectFactories.Double),
        sql,
        params
    );
  }

  /**
   * Read-only query the database with a {@link DoubleStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #doubleStreamUpdate(java.lang.String, java.lang.Object...)
   */
  default DoubleStream doubleStream(String sql, Object ... params) throws NullDataException, SQLException {
    return doubleStream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * Read-write query the database with a {@link DoubleStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #doubleStream(java.lang.String, java.lang.Object...)
   */
  default DoubleStream doubleStreamUpdate(String sql, Object ... params) throws NullDataException, SQLException {
    return doubleStream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with a {@link DoubleStream} return type.
   */
  DoubleStream doubleStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException;

  /**
   * Read-only query the database with a {@code float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #updateFloat(java.lang.String, java.lang.Object...)
   */
  default float queryFloat(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * Read-write query the database with a {@code float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #queryFloat(java.lang.String, java.lang.Object...)
   */
  default float updateFloat(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * Query the database with a {@link Float} return type.
   *
   * @return  The value or {@code null} when no row and row not required.
   *
   * @throws  NullDataException  When has a row, but with NULL value.
   */
  default Float queryFloat(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        ObjectFactories.notNull(ObjectFactories.Float),
        sql,
        params
    );
  }

  /**
   * Read-only query the database with an {@code int} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #updateInt(java.lang.String, java.lang.Object...)
   */
  default int queryInt(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryInt(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default int executeIntQuery(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryInt(sql, params);
  }

  /**
   * Read-write query the database with an {@code int} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #queryInt(java.lang.String, java.lang.Object...)
   */
  default int updateInt(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateInt(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default int executeIntUpdate(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return updateInt(sql, params);
  }

  /**
   * Query the database with an {@link Integer} return type.
   *
   * @return  The value or {@code null} when no row and row not required.
   *
   * @throws  NullDataException  When has a row, but with NULL value.
   */
  default Integer queryInt(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        ObjectFactories.notNull(ObjectFactories.Integer),
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryInt(int, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Integer executeIntQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryInt(isolationLevel, readOnly, rowRequired, sql, params);
  }

  /**
   * Read-only query the database with an {@link IntList} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #updateIntList(java.lang.String, java.lang.Object...)
   */
  default IntList queryIntList(String sql, Object ... params) throws NullDataException, SQLException {
    return queryIntList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryIntList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default IntList executeIntListQuery(String sql, Object ... params) throws NullDataException, SQLException {
    return queryIntList(sql, params);
  }

  /**
   * Read-write query the database with an {@link IntList} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #queryIntList(java.lang.String, java.lang.Object...)
   */
  default IntList updateIntList(String sql, Object ... params) throws NullDataException, SQLException {
    return queryIntList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateIntList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default IntList executeIntListUpdate(String sql, Object ... params) throws NullDataException, SQLException {
    return updateIntList(sql, params);
  }

  /**
   * Query the database with an {@link IntList} return type.
   */
  default IntList queryIntList(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
    return queryCall(
        isolationLevel,
        readOnly,
        results -> {
          IntList list = AoCollections.newIntArrayList(DatabaseUtils.getRowCount(results));
          while (results.next()) {
            int i = results.getInt(1);
            if (results.wasNull()) {
              throw new NullDataException(results);
            }
            list.add(i);
          }
          return list;
        },
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryIntList(int, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default IntList executeIntListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
    return queryIntList(isolationLevel, readOnly, sql, params);
  }

  /**
   * Read-only query the database with an {@link IntStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #intStreamUpdate(java.lang.String, java.lang.Object...)
   */
  default IntStream intStream(String sql, Object ... params) throws NullDataException, SQLException {
    return intStream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * Read-write query the database with an {@link IntStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #intStream(java.lang.String, java.lang.Object...)
   */
  default IntStream intStreamUpdate(String sql, Object ... params) throws NullDataException, SQLException {
    return intStream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with an {@link IntStream} return type.
   */
  IntStream intStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException;

  /**
   * Read-only query the database with a {@code long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #updateLong(java.lang.String, java.lang.Object...)
   */
  default long queryLong(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryLong(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default long executeLongQuery(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryLong(sql, params);
  }

  /**
   * Read-write query the database with a {@code long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #queryLong(java.lang.String, java.lang.Object...)
   */
  default long updateLong(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateLong(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default long executeLongUpdate(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return updateLong(sql, params);
  }

  /**
   * Query the database with a {@link Long} return type.
   *
   * @return  The value or {@code null} when no row and row not required.
   *
   * @throws  NullDataException  When has a row, but with NULL value.
   */
  default Long queryLong(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        ObjectFactories.notNull(ObjectFactories.Long),
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryLong(int, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Long executeLongQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryLong(isolationLevel, readOnly, rowRequired, sql, params);
  }

  /**
   * Read-only query the database with a {@link LongList} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #updateLongList(java.lang.String, java.lang.Object...)
   */
  default LongList queryLongList(String sql, Object ... params) throws NullDataException, SQLException {
    return queryLongList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryLongList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default LongList executeLongListQuery(String sql, Object ... params) throws NullDataException, SQLException {
    return queryLongList(sql, params);
  }

  /**
   * Read-write query the database with a {@link LongList} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #queryLongList(java.lang.String, java.lang.Object...)
   */
  default LongList updateLongList(String sql, Object ... params) throws NullDataException, SQLException {
    return queryLongList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateLongList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default LongList executeLongListUpdate(String sql, Object ... params) throws NullDataException, SQLException {
    return updateLongList(sql, params);
  }

  /**
   * Query the database with a {@link LongList} return type.
   */
  default LongList queryLongList(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
    return queryCall(
        isolationLevel,
        readOnly,
        results -> {
          LongList list = AoCollections.newLongArrayList(DatabaseUtils.getRowCount(results));
          while (results.next()) {
            long l = results.getLong(1);
            if (results.wasNull()) {
              throw new NullDataException(results);
            }
            list.add(l);
          }
          return list;
        },
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryLongList(int, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default LongList executeLongListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException {
    return queryLongList(isolationLevel, readOnly, sql, params);
  }

  /**
   * Read-only query the database with a {@link LongStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #longStreamUpdate(java.lang.String, java.lang.Object...)
   */
  default LongStream longStream(String sql, Object ... params) throws NullDataException, SQLException {
    return longStream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * Read-write query the database with a {@link LongStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #longStream(java.lang.String, java.lang.Object...)
   */
  default LongStream longStreamUpdate(String sql, Object ... params) throws NullDataException, SQLException {
    return longStream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with a {@link LongStream} return type.
   */
  LongStream longStream(int isolationLevel, boolean readOnly, String sql, Object ... params) throws NullDataException, SQLException;

  /**
   * Read-only query the database with a {@code <T>} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #executeObjectUpdate(java.lang.Class, java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #queryObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectQuery(Class<T> clazz, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return executeObjectQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, clazz, sql, params);
  }

  /**
   * Read-write query the database with a {@code <T>} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #executeObjectQuery(java.lang.Class, java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #updateObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectUpdate(Class<T> clazz, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return executeObjectQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, clazz, sql, params);
  }

  /**
   * Query the database with a {@code <T>} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   *
   * @return  The value or {@code null} when no row and row not required, or when row with null value.
   *
   * @deprecated  Please use {@link #queryObject(int, boolean, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<T> clazz, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, new ObjectFactories.Object<>(clazz), sql, params);
  }

  /**
   * Read-only query the database with a {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #updateObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> T queryObject(ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #queryObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> T updateObject(ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return updateObject(objectFactory, sql, params);
  }

  /**
   * Query the database with a {@code <T>} return type, objects are created with the provided factory.
   *
   * @return  The value or {@code null} when no row and row not required, or when row with null value.
   */
  default <T> T queryObject(int isolationLevel, boolean readOnly, boolean rowRequired, ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryObject(int, boolean, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, ObjectFactory<T> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #updateObject(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> T queryObject(Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, eClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryObject(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> T executeObjectQuery(Class<Ex> eClass, ObjectFactoryE<T, Ex> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryObject(eClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #queryObject(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> T updateObject(Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, eClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #updateObject(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> T executeObjectUpdate(Class<Ex> eClass, ObjectFactoryE<T, Ex> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException, Ex {
    return updateObject(eClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@code <T>} return type, objects are created with the provided factory.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value or {@code null} when no row and row not required, or when row with null value.
   */
  default <T, Ex extends Throwable> T queryObject(int isolationLevel, boolean readOnly, boolean rowRequired, Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryCall(
        isolationLevel,
        readOnly,
        eClass,
        results -> {
          if (results.next()) {
            T object = objectFactory.createObject(results);
            if (results.next()) {
              throw new ExtraRowException(results);
            }
            return object;
          }
          if (rowRequired) {
            throw new NoRowException();
          }
          return null;
        },
        sql,
        params
    );
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryObject(int, boolean, boolean, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> T executeObjectQuery(int isolationLevel, boolean readOnly, boolean rowRequired, Class<Ex> eClass, ObjectFactoryE<T, Ex> objectFactory, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryObject(isolationLevel, readOnly, rowRequired, eClass, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link List List&lt;T&gt;} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @return  An unmodifiable list
   *
   * @see  #executeObjectListUpdate(java.lang.Class, java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #queryList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T> List<T> executeObjectListQuery(Class<T> clazz, String sql, Object ... params) throws SQLException {
    return executeObjectListQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, true, clazz, sql, params);
  }

  /**
   * Read-write query the database with a {@link List List&lt;T&gt;} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @return  An unmodifiable list
   *
   * @see  #executeObjectListQuery(java.lang.Class, java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #updateList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T> List<T> executeObjectListUpdate(Class<T> clazz, String sql, Object ... params) throws SQLException {
    return executeObjectListQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, false, clazz, sql, params);
  }

  /**
   * Query the database with a {@link List List&lt;T&gt;} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   *
   * @return  An unmodifiable list
   *
   * @deprecated  Please use {@link #queryList(int, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<T> clazz, String sql, Object ... params) throws SQLException {
    return queryList(isolationLevel, readOnly, new ObjectFactories.Object<>(clazz), sql, params);
  }

  /**
   * Read-only query the database with a {@link List List&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @return  An unmodifiable list
   *
   * @see  #updateList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> List<T> queryList(ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> List<T> executeObjectListQuery(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryList(objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link List List&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @return  An unmodifiable list
   *
   * @see  #queryList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> List<T> updateList(ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> List<T> executeObjectListUpdate(ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
    return updateList(objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link List List&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * @return  An unmodifiable list
   */
  default <T> List<T> queryList(int isolationLevel, boolean readOnly, ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryList(isolationLevel, readOnly, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryList(int, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryList(isolationLevel, readOnly, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link List List&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  An unmodifiable list
   *
   * @see  #updateList(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> List<T> queryList(Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return queryList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, eClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryList(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> List<T> executeObjectListQuery(Class<Ex> eClass, ObjectFactoryE<T, Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return queryList(eClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link List List&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  An unmodifiable list
   *
   * @see  #queryList(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> List<T> updateList(Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return queryList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, eClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #updateList(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> List<T> executeObjectListUpdate(Class<Ex> eClass, ObjectFactoryE<T, Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return updateList(eClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link List List&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  An unmodifiable list
   */
  default <T, Ex extends Throwable> List<T> queryList(int isolationLevel, boolean readOnly, Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    List<T> list = queryNewCollection(isolationLevel, readOnly, (Function<Integer, List<T>>) AoCollections::newArrayList, eClass, objectFactory, sql, params);
    return AoCollections.optimalUnmodifiableList(list);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryList(int, boolean, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> List<T> executeObjectListQuery(int isolationLevel, boolean readOnly, Class<Ex> eClass, ObjectFactoryE<T, Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return queryList(isolationLevel, readOnly, eClass, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   * <p>
   * When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.
   * </p>
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #streamUpdate(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> Stream<T> stream(ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return stream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   * <p>
   * When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.
   * </p>
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #stream(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> Stream<T> streamUpdate(ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return stream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   * <p>
   * When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.
   * </p>
   */
  default <T> Stream<T> stream(int isolationLevel, boolean readOnly, ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return stream(isolationLevel, readOnly, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   * <p>
   * When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.
   * </p>
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #streamUpdate(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> Stream<T> stream(Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return stream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, eClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   * <p>
   * When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.
   * </p>
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #stream(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> Stream<T> streamUpdate(Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return stream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, eClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   * <p>
   * When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.
   * </p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  <T, Ex extends Throwable> Stream<T> stream(int isolationLevel, boolean readOnly, Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex;

  /**
   * Read-only query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   * <p>
   * Always has the characteristic {@link Spliterator#NONNULL}.
   * </p>
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #streamOptionalUpdate(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> Stream<Optional<T>> streamOptional(ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return streamOptional(Connections.DEFAULT_TRANSACTION_ISOLATION, true, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   * <p>
   * Always has the characteristic {@link Spliterator#NONNULL}.
   * </p>
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #streamOptional(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> Stream<Optional<T>> streamOptionalUpdate(ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return streamOptional(Connections.DEFAULT_TRANSACTION_ISOLATION, false, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   * <p>
   * Always has the characteristic {@link Spliterator#NONNULL}.
   * </p>
   */
  default <T> Stream<Optional<T>> streamOptional(int isolationLevel, boolean readOnly, ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return streamOptional(isolationLevel, readOnly, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   * <p>
   * Always has the characteristic {@link Spliterator#NONNULL}.
   * </p>
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #streamOptionalUpdate(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> Stream<Optional<T>> streamOptional(Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return streamOptional(Connections.DEFAULT_TRANSACTION_ISOLATION, true, eClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   * <p>
   * Always has the characteristic {@link Spliterator#NONNULL}.
   * </p>
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #streamOptional(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> Stream<Optional<T>> streamOptionalUpdate(Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return streamOptional(Connections.DEFAULT_TRANSACTION_ISOLATION, false, eClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   * <p>
   * Always has the characteristic {@link Spliterator#NONNULL}.
   * </p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  default <T, Ex extends Throwable> Stream<Optional<T>> streamOptional(int isolationLevel, boolean readOnly, Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    boolean isNullable = objectFactory.isNullable();
    return stream(
        isolationLevel,
        readOnly,
        eClass,
        new ObjectFactoryE<Optional<T>, Ex>() {
          @Override
          public Optional<T> createObject(ResultSet result) throws SQLException, Ex {
            T t = objectFactory.createObject(result);
            if (isNullable) {
              return Optional.ofNullable(t);
            } else {
              if (t == null) {
                throw new NullDataException(result);
              }
              return Optional.of(t);
            }
          }
          @Override
          public boolean isNullable() {
            return false;
          }
        },
        sql,
        params
    );
  }

  /**
   * Read-only query the database with a {@link Collection Collection&lt;T&gt;} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #executeObjectCollectionUpdate(java.util.Collection, java.lang.Class, java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #queryCollection(java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
    return executeObjectCollectionQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, true, collection, clazz, sql, params);
  }

  /**
   * Read-write query the database with a {@link Collection Collection&lt;T&gt;} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #executeObjectCollectionQuery(java.util.Collection, java.lang.Class, java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #updateCollection(java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
    return executeObjectCollectionQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, false, collection, clazz, sql, params);
  }

  /**
   * Query the database with a {@link Collection Collection&lt;T&gt;} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   *
   * @deprecated  Please use {@link #queryCollection(int, boolean, java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<T> clazz, String sql, Object ... params) throws SQLException {
    return queryCollection(isolationLevel, readOnly, collection, new ObjectFactories.Object<>(clazz), sql, params);
  }

  /**
   * Read-only query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #updateCollection(java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T, C extends Collection<? super T>> C queryCollection(C collection, ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, true, collection, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryCollection(java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionQuery(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryCollection(collection, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #queryCollection(java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T, C extends Collection<? super T>> C updateCollection(C collection, ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, collection, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateCollection(java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionUpdate(C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
    return updateCollection(collection, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   */
  default <T, C extends Collection<? super T>> C queryCollection(int isolationLevel, boolean readOnly, C collection, ObjectFactory<? extends T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryCollection(isolationLevel, readOnly, collection, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryCollection(int, boolean, java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, ObjectFactory<T> objectFactory, String sql, Object ... params) throws SQLException {
    return queryCollection(isolationLevel, readOnly, collection, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #updateCollection(java.util.Collection, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, C extends Collection<? super T>, Ex extends Throwable> C queryCollection(C collection, Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return queryCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, true, collection, eClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryCollection(java.util.Collection, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>, Ex extends Exception> C executeObjectCollectionQuery(C collection, Class<Ex> eClass, ObjectFactoryE<T, Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return queryCollection(collection, eClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #queryCollection(java.util.Collection, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, C extends Collection<? super T>, Ex extends Throwable> C updateCollection(C collection, Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return queryCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, collection, eClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #updateCollection(java.util.Collection, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>, Ex extends Exception> C executeObjectCollectionUpdate(C collection, Class<Ex> eClass, ObjectFactoryE<T, Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return updateCollection(collection, eClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  default <T, C extends Collection<? super T>, Ex extends Throwable> C queryCollection(int isolationLevel, boolean readOnly, C collection, Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return queryCall(
        isolationLevel,
        readOnly,
        eClass,
        results -> {
          while (results.next()) {
            T newObj = objectFactory.createObject(results);
            if (!collection.add(newObj)) {
              throw new SQLException("Duplicate row in results: " + DatabaseUtils.getRow(results));
            }
          }
          return collection;
        },
        sql,
        params
    );
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryCollection(int, boolean, java.util.Collection, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>, Ex extends Exception> C executeObjectCollectionQuery(int isolationLevel, boolean readOnly, C collection, Class<Ex> eClass, ObjectFactoryE<T, Ex> objectFactory, String sql, Object ... params) throws SQLException, Ex {
    return queryCollection(isolationLevel, readOnly, collection, eClass, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  newCollection  given the row count, or {@code null} when unknown, creates the new collection
   *
   * @see  #updateNewCollection(java.util.function.Function, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  @SuppressWarnings("overloads")
  default <T, C extends Collection<? super T>> C queryNewCollection(
      Function<? super Integer, ? extends C> newCollection,
      ObjectFactory<? extends T> objectFactory, String sql, Object ... params
  ) throws SQLException {
    return queryNewCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, true, newCollection, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  newCollection  given the row count, or {@code null} when unknown, creates the new collection
   *
   * @see  #queryNewCollection(java.util.function.Function, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object[])
   */
  @SuppressWarnings("overloads")
  default <T, C extends Collection<? super T>> C updateNewCollection(
      Function<? super Integer, ? extends C> newCollection,
      ObjectFactory<? extends T> objectFactory, String sql, Object ... params
  ) throws SQLException {
    return queryNewCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, newCollection, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * @param  newCollection  given the row count, or {@code null} when unknown, creates the new collection
   */
  @SuppressWarnings("overloads")
  default <T, C extends Collection<? super T>> C queryNewCollection(
      int isolationLevel, boolean readOnly,
      Function<? super Integer, ? extends C> newCollection,
      ObjectFactory<? extends T> objectFactory, String sql, Object ... params
  ) throws SQLException {
    return queryNewCollection(isolationLevel, readOnly, newCollection, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   * @param  newCollection  given the row count, or {@code null} when unknown, creates the new collection
   *
   * @see  #updateNewCollection(java.util.function.Function, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  @SuppressWarnings("overloads")
  default <T, C extends Collection<? super T>, Ex extends Throwable> C queryNewCollection(
      Function<? super Integer, ? extends C> newCollection,
      Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params
  ) throws SQLException, Ex {
    return queryNewCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, true, newCollection, eClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   * @param  newCollection  given the row count, or {@code null} when unknown, creates the new collection
   *
   * @see  #queryNewCollection(java.util.function.Function, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object[])
   */
  @SuppressWarnings("overloads")
  default <T, C extends Collection<? super T>, Ex extends Throwable> C updateNewCollection(
      Function<? super Integer, ? extends C> newCollection,
      Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params
  ) throws SQLException, Ex {
    return queryNewCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, newCollection, eClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   * @param  newCollection  given the row count, or {@code null} when unknown, creates the new collection
   */
  @SuppressWarnings("overloads")
  default <T, C extends Collection<? super T>, Ex extends Throwable> C queryNewCollection(
      int isolationLevel, boolean readOnly,
      Function<? super Integer, ? extends C> newCollection,
      Class<? extends Ex> eClass, ObjectFactoryE<? extends T, ? extends Ex> objectFactory, String sql, Object ... params
  ) throws SQLException, Ex {
    return queryCall(
        isolationLevel,
        readOnly,
        eClass,
        results -> {
          int rowCount = DatabaseUtils.getRowCount(results);
          C collection = newCollection.apply(rowCount == -1 ? null : rowCount);
          while (results.next()) {
            T newObj = objectFactory.createObject(results);
            if (!collection.add(newObj)) {
              throw new SQLException("Duplicate row in results: " + DatabaseUtils.getRow(results));
            }
          }
          return collection;
        },
        sql,
        params
    );
  }

  /**
   * Read-only query the database, calling the {@link ResultSetCallable} once.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #updateCall(com.aoapps.dbc.ResultSetCallable, java.lang.String, java.lang.Object[])
   */
  default <T> T queryCall(ResultSetCallable<? extends T> resultSetCallable, String sql, Object ... params) throws SQLException {
    return queryCall(Connections.DEFAULT_TRANSACTION_ISOLATION, true, resultSetCallable, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryCall(com.aoapps.dbc.ResultSetCallable, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T> T executeQuery(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
    return queryCall(resultSetHandler, sql, params);
  }

  /**
   * Read-write query the database, calling the {@link ResultSetCallable} once.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #queryCall(com.aoapps.dbc.ResultSetCallable, java.lang.String, java.lang.Object[])
   */
  default <T> T updateCall(ResultSetCallable<? extends T> resultSetCallable, String sql, Object ... params) throws SQLException {
    return queryCall(Connections.DEFAULT_TRANSACTION_ISOLATION, false, resultSetCallable, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateCall(com.aoapps.dbc.ResultSetCallable, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T> T executeUpdate(ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
    return updateCall(resultSetHandler, sql, params);
  }

  /**
   * Query the database, calling the {@link ResultSetCallable} once.
   */
  default <T> T queryCall(int isolationLevel, boolean readOnly, ResultSetCallable<? extends T> resultSetCallable, String sql, Object ... params) throws SQLException {
    return queryCall(isolationLevel, readOnly, RuntimeException.class, resultSetCallable, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryCall(int, boolean, com.aoapps.dbc.ResultSetCallable, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T> T executeQuery(int isolationLevel, boolean readOnly, ResultSetHandler<T> resultSetHandler, String sql, Object ... params) throws SQLException {
    return queryCall(isolationLevel, readOnly, resultSetHandler, sql, params);
  }

  /**
   * Read-only query the database, calling the {@link ResultSetCallableE} once.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #updateCall(java.lang.Class, com.aoapps.dbc.ResultSetCallableE, java.lang.String, java.lang.Object[])
   */
  default <T, Ex extends Throwable> T queryCall(Class<? extends Ex> eClass, ResultSetCallableE<? extends T, ? extends Ex> resultSetCallable, String sql, Object ... params) throws SQLException, Ex {
    return queryCall(Connections.DEFAULT_TRANSACTION_ISOLATION, true, eClass, resultSetCallable, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryCall(java.lang.Class, com.aoapps.dbc.ResultSetCallableE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T, Ex extends Exception> T executeQuery(Class<Ex> eClass, ResultSetHandlerE<T, Ex> resultSetHandler, String sql, Object ... params) throws SQLException, Ex {
    return queryCall(eClass, resultSetHandler, sql, params);
  }

  /**
   * Read-write query the database, calling the {@link ResultSetCallableE} once.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #queryCall(java.lang.Class, com.aoapps.dbc.ResultSetCallableE, java.lang.String, java.lang.Object[])
   */
  default <T, Ex extends Throwable> T updateCall(Class<? extends Ex> eClass, ResultSetCallableE<? extends T, ? extends Ex> resultSetCallable, String sql, Object ... params) throws SQLException, Ex {
    return queryCall(Connections.DEFAULT_TRANSACTION_ISOLATION, false, eClass, resultSetCallable, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #updateCall(java.lang.Class, com.aoapps.dbc.ResultSetCallableE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T, Ex extends Exception> T executeUpdate(Class<Ex> eClass, ResultSetHandlerE<T, Ex> resultSetHandler, String sql, Object ... params) throws SQLException, Ex {
    return updateCall(eClass, resultSetHandler, sql, params);
  }

  /**
   * Query the database, calling the {@link ResultSetCallableE} once.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  <T, Ex extends Throwable> T queryCall(int isolationLevel, boolean readOnly, Class<? extends Ex> eClass, ResultSetCallableE<? extends T, ? extends Ex> resultSetCallable, String sql, Object ... params) throws SQLException, Ex;

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryCall(int, boolean, java.lang.Class, com.aoapps.dbc.ResultSetCallableE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T, Ex extends Exception> T executeQuery(int isolationLevel, boolean readOnly, Class<Ex> eClass, ResultSetHandlerE<T, Ex> resultSetHandler, String sql, Object ... params) throws SQLException, Ex {
    return queryCall(isolationLevel, readOnly, eClass, resultSetHandler, sql, params);
  }

  /**
   * Read-only query the database, calling the {@link ResultSetRunnable} once.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #updateRun(com.aoapps.dbc.ResultSetRunnable, java.lang.String, java.lang.Object[])
   */
  default void queryRun(ResultSetRunnable resultSetRunnable, String sql, Object ... params) throws SQLException {
    queryRun(Connections.DEFAULT_TRANSACTION_ISOLATION, true, resultSetRunnable, sql, params);
  }

  /**
   * Read-write query the database, calling the {@link ResultSetRunnable} once.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #queryRun(com.aoapps.dbc.ResultSetRunnable, java.lang.String, java.lang.Object[])
   */
  default void updateRun(ResultSetRunnable resultSetRunnable, String sql, Object ... params) throws SQLException {
    queryRun(Connections.DEFAULT_TRANSACTION_ISOLATION, false, resultSetRunnable, sql, params);
  }

  /**
   * Query the database, calling the {@link ResultSetRunnable} once.
   */
  default void queryRun(int isolationLevel, boolean readOnly, ResultSetRunnable resultSetRunnable, String sql, Object ... params) throws SQLException {
    queryRun(isolationLevel, readOnly, RuntimeException.class, resultSetRunnable, sql, params);
  }

  /**
   * Read-only query the database, calling the {@link ResultSetRunnableE} once.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #updateRun(java.lang.Class, com.aoapps.dbc.ResultSetRunnableE, java.lang.String, java.lang.Object[])
   */
  default <Ex extends Throwable> void queryRun(Class<? extends Ex> eClass, ResultSetRunnableE<? extends Ex> resultSetRunnable, String sql, Object ... params) throws SQLException, Ex {
    queryRun(Connections.DEFAULT_TRANSACTION_ISOLATION, true, eClass, resultSetRunnable, sql, params);
  }

  /**
   * Read-write query the database, calling the {@link ResultSetRunnableE} once.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #queryRun(java.lang.Class, com.aoapps.dbc.ResultSetRunnableE, java.lang.String, java.lang.Object[])
   */
  default <Ex extends Throwable> void updateRun(Class<? extends Ex> eClass, ResultSetRunnableE<? extends Ex> resultSetRunnable, String sql, Object ... params) throws SQLException, Ex {
    queryRun(Connections.DEFAULT_TRANSACTION_ISOLATION, false, eClass, resultSetRunnable, sql, params);
  }

  /**
   * Query the database, calling the {@link ResultSetRunnableE} once.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  default <Ex extends Throwable> void queryRun(int isolationLevel, boolean readOnly, Class<? extends Ex> eClass, ResultSetRunnableE<? extends Ex> resultSetRunnable, String sql, Object ... params) throws SQLException, Ex {
    queryCall(
        isolationLevel,
        readOnly,
        eClass,
        results -> {
          resultSetRunnable.run(results);
          return null;
        },
        sql,
        params
    );
  }

  // TODO: Variants that return the ResultSet?
  //       Closing the ResultSet would also close its associated PreparedStatement along with all the other usual cleanup
  //       This variant could then be used as the basis for the stream* implementations.
  //       This would be consistent with the proposed transaction() methods that would return the DatabaseConnection

  /**
   * Read-only query the database with a {@code short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #updateShort(java.lang.String, java.lang.Object...)
   */
  default short queryShort(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryShort(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default short executeShortQuery(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryShort(sql, params);
  }

  /**
   * Read-write query the database with a {@code short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @see  #queryShort(java.lang.String, java.lang.Object...)
   */
  default short updateShort(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateShort(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default short executeShortUpdate(String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return updateShort(sql, params);
  }

  /**
   * Query the database with a {@link Short} return type.
   *
   * @return  The value or {@code null} when no row and row not required.
   *
   * @throws  NullDataException  When has a row, but with NULL value.
   */
  default Short queryShort(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        ObjectFactories.notNull(ObjectFactories.Short),
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryShort(int, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Short executeShortQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryShort(isolationLevel, readOnly, rowRequired, sql, params);
  }

  /**
   * Read-only query the database with a {@code List<Short>} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #executeShortListUpdate(java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #queryList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with {@link ObjectFactories#Short}
   */
  @Deprecated(forRemoval = true)
  default List<Short> executeShortListQuery(String sql, Object ... params) throws SQLException {
    return executeShortListQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * Read-write query the database with a {@code List<Short>} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #executeShortListQuery(java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #updateList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with {@link ObjectFactories#Short}
   */
  @Deprecated(forRemoval = true)
  default List<Short> executeShortListUpdate(String sql, Object ... params) throws SQLException {
    return executeShortListQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with a {@code List<Short>} return type.
   *
   * @deprecated  Please use {@link #queryList(int, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with {@link ObjectFactories#Short}
   */
  @Deprecated(forRemoval = true)
  default List<Short> executeShortListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
    return queryList(isolationLevel, readOnly, ObjectFactories.Short, sql, params);
  }

  /**
   * Read-only query the database with a {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #updateString(java.lang.String, java.lang.Object...)
   */
  default String queryString(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryString(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default String executeStringQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryString(sql, params);
  }

  /**
   * Read-write query the database with a {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #queryString(java.lang.String, java.lang.Object...)
   */
  default String updateString(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateString(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default String executeStringUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return updateString(sql, params);
  }

  /**
   * Query the database with a {@link String} return type.
   *
   * @return  The value or {@code null} when no row and row not required, or when row with null value.
   */
  default String queryString(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, ObjectFactories.String, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryString(int, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default String executeStringQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryString(isolationLevel, readOnly, rowRequired, sql, params);
  }

  /**
   * Read-only query the database with a {@link List List&lt;String&gt;} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #updateStringList(java.lang.String, java.lang.Object...)
   */
  default List<String> queryStringList(String sql, Object ... params) throws SQLException {
    return queryStringList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryStringList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default List<String> executeStringListQuery(String sql, Object ... params) throws SQLException {
    return queryStringList(sql, params);
  }

  /**
   * Read-write query the database with a {@link List List&lt;String&gt;} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #queryStringList(java.lang.String, java.lang.Object...)
   */
  default List<String> updateStringList(String sql, Object ... params) throws SQLException {
    return queryStringList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateStringList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default List<String> executeStringListUpdate(String sql, Object ... params) throws SQLException {
    return updateStringList(sql, params);
  }

  /**
   * Query the database with a {@link List List&lt;String&gt;} return type.
   */
  default List<String> queryStringList(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
    return queryList(isolationLevel, readOnly, ObjectFactories.String, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryStringList(int, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default List<String> executeStringListQuery(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
    return queryStringList(isolationLevel, readOnly, sql, params);
  }

  /**
   * Read-only query the database with a {@link Stream Stream&lt;String&gt;} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #streamStringUpdate(java.lang.String, java.lang.Object...)
   */
  default Stream<String> streamString(String sql, Object ... params) throws SQLException {
    return streamString(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * Read-write query the database with a {@link Stream Stream&lt;String&gt;} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #streamString(java.lang.String, java.lang.Object...)
   */
  default Stream<String> streamStringUpdate(String sql, Object ... params) throws SQLException {
    return streamString(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;String&gt;} return type.
   */
  default Stream<String> streamString(int isolationLevel, boolean readOnly, String sql, Object ... params) throws SQLException {
    return stream(isolationLevel, readOnly, ObjectFactories.String, sql, params);
  }

  /**
   * Read-only query the database with a {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #updateTimestamp(java.lang.String, java.lang.Object...)
   */
  default Timestamp queryTimestamp(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryTimestamp(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Timestamp executeTimestampQuery(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryTimestamp(sql, params);
  }

  /**
   * Read-write query the database with a {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with null value.
   *
   * @see  #queryTimestamp(java.lang.String, java.lang.Object...)
   */
  default Timestamp updateTimestamp(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateTimestamp(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Timestamp executeTimestampUpdate(String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return updateTimestamp(sql, params);
  }

  /**
   * Query the database with a {@link Timestamp} return type.
   *
   * @return  The value or {@code null} when no row and row not required, or when row with null value.
   */
  default Timestamp queryTimestamp(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, ObjectFactories.Timestamp, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryTimestamp(int, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Timestamp executeTimestampQuery(int isolationLevel, boolean readOnly, boolean rowRequired, String sql, Object ... params) throws NoRowException, ExtraRowException, SQLException {
    return queryTimestamp(isolationLevel, readOnly, rowRequired, sql, params);
  }

  /**
   * Performs an update on the database and returns the number of rows affected.
   */
  // TODO: Variant(s) that check the number of items updated when 1 expected, throws NoRowException, ExtraRowException
  //       Call it update1?  updateSingle?  updateRow?
  //       Implement as a functional interface "UpdateValidator" (int) -> SQLException?
  int update(String sql, Object ... params) throws SQLException;

  /**
   * Performs an update on the database and returns the number of rows affected.
   */
  long largeUpdate(String sql, Object ... params) throws SQLException;

  /**
   * @deprecated  Please use {@link #update(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default int executeUpdate(String sql, Object ... params) throws SQLException {
    return update(sql, params);
  }
}
