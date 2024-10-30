/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2015, 2018, 2020, 2021, 2022, 2023, 2024  AO Industries, Inc.
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
import com.aoapps.lang.RunnableE;
import com.aoapps.lang.concurrent.CallableE;
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
  boolean isInTransaction();

  /**
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   *
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   *
   * <p>The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.</p>
   *
   * @see #isInTransaction()
   */
  default <V> V transactionCall(CallableE<? extends V, ? extends SQLException> callable) throws SQLException {
    return transactionCall(SQLException.class, callable);
  }

  /**
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   *
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   *
   * <p>The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.</p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see #isInTransaction()
   */
  default <V, Ex extends Throwable> V transactionCall(Class<? extends Ex> exClass, CallableE<? extends V, ? extends Ex> callable) throws SQLException, Ex {
    return transactionCall(exClass, db -> callable.call());
  }

  /**
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   *
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   *
   * <p>The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.</p>
   *
   * @see #isInTransaction()
   */
  default <V> V transactionCall(DatabaseCallable<? extends V> callable) throws SQLException {
    return transactionCall(RuntimeException.class, callable::call);
  }

  /**
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   *
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   *
   * <p>The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.</p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see #isInTransaction()
   */
  <V, Ex extends Throwable> V transactionCall(Class<? extends Ex> exClass, DatabaseCallableE<? extends V, ? extends Ex> callable) throws SQLException, Ex;

  /**
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   *
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   *
   * <p>The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.</p>
   *
   * @see #isInTransaction()
   */
  default void transactionRun(RunnableE<? extends SQLException> runnable) throws SQLException {
    transactionRun(SQLException.class, runnable);
  }

  /**
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   *
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   *
   * <p>The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.</p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see #isInTransaction()
   */
  default <Ex extends Throwable> void transactionRun(Class<? extends Ex> exClass, RunnableE<? extends Ex> runnable) throws SQLException, Ex {
    transactionRun(exClass, db -> runnable.run());
  }

  /**
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   *
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   *
   * <p>The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.</p>
   *
   * @see #isInTransaction()
   */
  default void transactionRun(DatabaseRunnable runnable) throws SQLException {
    transactionRun(RuntimeException.class, runnable::run);
  }

  /**
   * Executes an arbitrary transaction, providing automatic commit, rollback, and connection management.
   *
   * <ol>
   * <li>Rolls-back the transaction on {@link NoRowException}, {@link NullDataException}, or
   *     {@link ExtraRowException} on the outer-most transaction only.</li>
   * <li>Rolls-back the transaction on all other {@link Throwable}.</li>
   * </ol>
   *
   * <p>The connection allocated is stored as a {@link ThreadLocal} and will be automatically reused if
   * another transaction is performed within this transaction.  Any nested transaction will automatically
   * become part of the enclosing transaction.  For safety, a nested transaction will still rollback the
   * entire transaction on any exception.</p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see #isInTransaction()
   */
  default <Ex extends Throwable> void transactionRun(Class<? extends Ex> exClass, DatabaseRunnableE<? extends Ex> runnable) throws SQLException, Ex {
    transactionCall(
        exClass,
        db -> {
          runnable.run(db);
          return null;
        }
    );
  }

  /**
   * Read-only query the database with a required non-null {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default BigDecimal queryBigDecimal(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default BigDecimal queryBigDecimalNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<BigDecimal> queryBigDecimalOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<BigDecimal> queryBigDecimalOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryBigDecimal(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default BigDecimal executeBigDecimalQuery(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryBigDecimal(sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default BigDecimal updateBigDecimal(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default BigDecimal updateBigDecimalNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<BigDecimal> updateBigDecimalOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link BigDecimal} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<BigDecimal> updateBigDecimalOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryBigDecimal(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateBigDecimal(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default BigDecimal executeBigDecimalUpdate(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return updateBigDecimal(sql, params);
  }

  /**
   * Query the database with a {@link BigDecimal} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default BigDecimal queryBigDecimal(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, nullable, ObjectFactories.BigDecimal, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryBigDecimal(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default BigDecimal queryBigDecimal(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryBigDecimal(isolationLevel, readOnly, rowRequired, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryBigDecimal(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default BigDecimal executeBigDecimalQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryBigDecimal(isolationLevel, readOnly, rowRequired, true, sql, params);
  }

  /**
   * Read-only query the database with a required non-null {@code boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default boolean queryBoolean(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link Boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Boolean queryBooleanNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link Boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@code Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Boolean> queryBooleanOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link Boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Boolean> queryBooleanOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryBoolean(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default boolean executeBooleanQuery(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBoolean(sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@code boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default boolean updateBoolean(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link Boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Boolean updateBooleanNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link Boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@code Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Boolean> updateBooleanOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link Boolean} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Boolean> updateBooleanOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryBoolean(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateBoolean(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default boolean executeBooleanUpdate(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return updateBoolean(sql, params);
  }

  /**
   * Query the database with a {@link Boolean} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Boolean queryBoolean(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        nullable,
        ObjectFactories.Boolean,
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryBoolean(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Boolean queryBoolean(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBoolean(isolationLevel, readOnly, rowRequired, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryBoolean(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Boolean executeBooleanQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryBoolean(isolationLevel, readOnly, rowRequired, false, sql, params);
  }

  /**
   * Read-only query the database with a required non-null {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default byte[] queryByteArray(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default byte[] queryByteArrayNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<byte[]> queryByteArrayOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<byte[]> queryByteArrayOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryByteArray(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default byte[] executeByteArrayQuery(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryByteArray(sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default byte[] updateByteArray(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default byte[] updateByteArrayNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<byte[]> updateByteArrayOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@code byte[]} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<byte[]> updateByteArrayOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryByteArray(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateByteArray(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default byte[] executeByteArrayUpdate(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return updateByteArray(sql, params);
  }

  /**
   * Query the database with a {@code byte[]} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default byte[] queryByteArray(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, nullable, ObjectFactories.ByteArray, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryByteArray(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default byte[] queryByteArray(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryByteArray(isolationLevel, readOnly, rowRequired, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryByteArray(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default byte[] executeByteArrayQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryByteArray(isolationLevel, readOnly, rowRequired, true, sql, params);
  }

  /**
   * Read-only query the database with a required non-null {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Date queryDate(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Date queryDateNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Date> queryDateOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Date> queryDateOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryDate(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Date executeDateQuery(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryDate(sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Date updateDate(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Date updateDateNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Date> updateDateOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link Date} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Date> updateDateOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryDate(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateDate(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Date executeDateUpdate(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return updateDate(sql, params);
  }

  /**
   * Query the database with a {@link Date} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Date queryDate(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, nullable, ObjectFactories.Date, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryDate(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Date queryDate(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryDate(isolationLevel, readOnly, rowRequired, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryDate(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Date executeDateQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryDate(isolationLevel, readOnly, rowRequired, true, sql, params);
  }

  /**
   * Read-only query the database with a required non-null {@code double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default double queryDouble(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link Double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Double queryDoubleNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link Double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Double> queryDoubleOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link Double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Double> queryDoubleOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * Read-write query the database with a required non-null {@code double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default double updateDouble(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link Double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Double updateDoubleNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link Double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Double> updateDoubleOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link Double} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Double> updateDoubleOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryDouble(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * Query the database with a {@link Double} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Double queryDouble(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        nullable,
        ObjectFactories.Double,
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryDouble(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Double queryDouble(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryDouble(isolationLevel, readOnly, rowRequired, false, sql, params);
  }

  /**
   * Read-only query the database with a {@link DoubleStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #doubleStreamUpdate(java.lang.String, java.lang.Object...)
   */
  default DoubleStream doubleStream(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return doubleStream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * Read-write query the database with a {@link DoubleStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #doubleStream(java.lang.String, java.lang.Object...)
   */
  default DoubleStream doubleStreamUpdate(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return doubleStream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with a {@link DoubleStream} return type.
   *
   * @throws  NullDataException  When has a SQL NULL value.
   */
  DoubleStream doubleStream(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException;

  /**
   * Read-only query the database with a required non-null {@code float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default float queryFloat(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link Float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Float queryFloatNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link Float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Float> queryFloatOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link Float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Float> queryFloatOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * Read-write query the database with a required non-null {@code float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default float updateFloat(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link Float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Float updateFloatNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link Float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Float> updateFloatOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link Float} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Float> updateFloatOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryFloat(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * Query the database with a {@link Float} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Float queryFloat(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        nullable,
        ObjectFactories.Float,
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryFloat(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Float queryFloat(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryFloat(isolationLevel, readOnly, rowRequired, false, sql, params);
  }

  /**
   * Read-only query the database with a required non-null {@code int} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default int queryInt(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link Integer} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Integer queryIntNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link Integer} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Integer> queryIntOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link Integer} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Integer> queryIntOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryInt(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default int executeIntQuery(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryInt(sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@code int} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default int updateInt(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link Integer} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Integer updateIntNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link Integer} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Integer> updateIntOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link Integer} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Integer> updateIntOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryInt(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateInt(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default int executeIntUpdate(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return updateInt(sql, params);
  }

  /**
   * Query the database with an {@link Integer} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Integer queryInt(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        nullable,
        ObjectFactories.Integer,
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryInt(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Integer queryInt(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryInt(isolationLevel, readOnly, rowRequired, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryInt(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Integer executeIntQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryInt(isolationLevel, readOnly, rowRequired, false, sql, params);
  }

  /**
   * Read-only query the database with an {@link IntList} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #updateIntList(java.lang.String, java.lang.Object...)
   */
  default IntList queryIntList(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return queryIntList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryIntList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default IntList executeIntListQuery(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return queryIntList(sql, params);
  }

  /**
   * Read-write query the database with an {@link IntList} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #queryIntList(java.lang.String, java.lang.Object...)
   */
  default IntList updateIntList(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return queryIntList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateIntList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default IntList executeIntListUpdate(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return updateIntList(sql, params);
  }

  /**
   * Query the database with an {@link IntList} return type.
   *
   * @throws  NullDataException  When has a SQL NULL value.
   */
  default IntList queryIntList(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
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
  default IntList executeIntListQuery(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return queryIntList(isolationLevel, readOnly, sql, params);
  }

  /**
   * Read-only query the database with an {@link IntStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #intStreamUpdate(java.lang.String, java.lang.Object...)
   */
  default IntStream intStream(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return intStream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * Read-write query the database with an {@link IntStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #intStream(java.lang.String, java.lang.Object...)
   */
  default IntStream intStreamUpdate(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return intStream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with an {@link IntStream} return type.
   *
   * @throws  NullDataException  When has a SQL NULL value.
   */
  IntStream intStream(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException;

  /**
   * Read-only query the database with a required non-null {@code long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default long queryLong(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link Long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Long queryLongNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link Long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Long> queryLongOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link Long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Long> queryLongOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryLong(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default long executeLongQuery(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryLong(sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@code long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default long updateLong(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link Long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Long updateLongNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link Long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Long> updateLongOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link Long} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Long> updateLongOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryLong(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateLong(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default long executeLongUpdate(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return updateLong(sql, params);
  }

  /**
   * Query the database with a {@link Long} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Long queryLong(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        nullable,
        ObjectFactories.Long,
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryLong(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Long queryLong(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryLong(isolationLevel, readOnly, rowRequired, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryLong(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Long executeLongQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryLong(isolationLevel, readOnly, rowRequired, false, sql, params);
  }

  /**
   * Read-only query the database with a {@link LongList} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #updateLongList(java.lang.String, java.lang.Object...)
   */
  default LongList queryLongList(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return queryLongList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryLongList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default LongList executeLongListQuery(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return queryLongList(sql, params);
  }

  /**
   * Read-write query the database with a {@link LongList} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #queryLongList(java.lang.String, java.lang.Object...)
   */
  default LongList updateLongList(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return queryLongList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateLongList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default LongList executeLongListUpdate(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return updateLongList(sql, params);
  }

  /**
   * Query the database with a {@link LongList} return type.
   *
   * @throws  NullDataException  When has a SQL NULL value.
   */
  default LongList queryLongList(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
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
  default LongList executeLongListQuery(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return queryLongList(isolationLevel, readOnly, sql, params);
  }

  /**
   * Read-only query the database with a {@link LongStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #longStreamUpdate(java.lang.String, java.lang.Object...)
   */
  default LongStream longStream(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return longStream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * Read-write query the database with a {@link LongStream} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @throws  NullDataException  When has a SQL NULL value.
   *
   * @see  #longStream(java.lang.String, java.lang.Object...)
   */
  default LongStream longStreamUpdate(
      String sql,
      Object ... params
  ) throws NullDataException, SQLException {
    return longStream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with a {@link LongStream} return type.
   *
   * @throws  NullDataException  When has a SQL NULL value.
   */
  LongStream longStream(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws NullDataException, SQLException;

  /**
   * Read-only query the database with a required {@code <T>} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with {@code null} value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   *
   * @see  #executeObjectUpdate(java.lang.Class, java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #queryObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectQuery(
      Class<T> clazz,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return executeObjectQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, clazz, sql, params);
  }

  /**
   * Read-write query the database with a required {@code <T>} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with {@code null} value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   *
   * @see  #executeObjectQuery(java.lang.Class, java.lang.String, java.lang.Object...)
   *
   * @deprecated  Please use {@link #updateObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectUpdate(
      Class<T> clazz,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return executeObjectQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, clazz, sql, params);
  }

  /**
   * Query the database with a {@code <T>} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   *
   * @return  The value or {@code null} when no row and row not required, or when row with {@code null} value.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  ExtraRowException  When has more than one row.
   *
   * @deprecated  Please use {@link #queryObject(int, boolean, boolean, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      Class<T> clazz,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, true, new ObjectFactories.Object<>(clazz), sql, params);
  }

  /**
   * Read-only query the database with a required non-null {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T> T queryObject(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with {@code objectFactory} returning {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T> T queryObjectNullable(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T> Optional<T> queryObjectOptional(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, objectFactory, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with {@code objectFactory} returning {@code null}.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T> Optional<T> queryObjectOptionalNullable(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, objectFactory, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectQuery(
      ObjectFactory<T> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T> T updateObject(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with {@code objectFactory} returning {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T> T updateObjectNullable(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T> Optional<T> updateObjectOptional(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, objectFactory, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with {@code objectFactory} returning {@code null}.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T> Optional<T> updateObjectOptionalNullable(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, objectFactory, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateObject(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectUpdate(
      ObjectFactory<T> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return updateObject(objectFactory, sql, params);
  }

  /**
   * Query the database with a {@code <T>} return type, objects are created with the provided factory.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with {@code objectFactory} returning {@code null} value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null} and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T> T queryObject(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, nullable, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryObject(int, boolean, boolean, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> T queryObject(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, true, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryObject(int, boolean, boolean, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> T executeObjectQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      ObjectFactory<T> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, true, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a required non-null {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T, Ex extends Throwable> T queryObject(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException, Ex {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, exClass, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value or {@code null} when row with {@code objectFactory} returning {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T, Ex extends Throwable> T queryObjectNullable(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, exClass, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T, Ex extends Throwable> Optional<T> queryObjectOptional(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException, Ex {
    return Optional.ofNullable(queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, exClass, objectFactory, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with {@code objectFactory} returning {@code null}.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T, Ex extends Throwable> Optional<T> queryObjectOptionalNullable(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException, Ex {
    return Optional.ofNullable(queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, exClass, objectFactory, sql, params));
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryObject(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> T executeObjectQuery(
      Class<Ex> exClass,
      ObjectFactoryE<T, Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryObject(exClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T, Ex extends Throwable> T updateObject(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException, Ex {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, exClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value or {@code null} when row with {@code objectFactory} returning {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T, Ex extends Throwable> T updateObjectNullable(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, exClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T, Ex extends Throwable> Optional<T> updateObjectOptional(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException, Ex {
    return Optional.ofNullable(queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, exClass, objectFactory, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@code <T>} return type, objects are created with the provided factory.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with {@code objectFactory} returning {@code null}.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default <T, Ex extends Throwable> Optional<T> updateObjectOptionalNullable(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException, Ex {
    return Optional.ofNullable(queryObject(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, exClass, objectFactory, sql, params));
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #updateObject(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> T executeObjectUpdate(
      Class<Ex> exClass,
      ObjectFactoryE<T, Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException, Ex {
    return updateObject(exClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@code <T>} return type, objects are created with the provided factory.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with {@code objectFactory} returning {@code null} value and {@code nullable}.
    *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with {@code objectFactory} returning {@code null} and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
  */
  default <T, Ex extends Throwable> T queryObject(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException, Ex {
    return queryCall(
        isolationLevel,
        readOnly,
        exClass,
        (ResultSet results) -> {
          if (results.next()) {
            T object = objectFactory.createObject(results);
            if (object == null && !nullable) {
              throw new NullDataException(results);
            }
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
   * @deprecated  Please use {@link #queryObject(int, boolean, boolean, boolean, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Throwable> T queryObject(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryObject(isolationLevel, readOnly, rowRequired, true, exClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryObject(int, boolean, boolean, boolean, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> T executeObjectQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      Class<Ex> exClass,
      ObjectFactoryE<T, Ex> objectFactory,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException, Ex {
    return queryObject(isolationLevel, readOnly, rowRequired, true, exClass, objectFactory, sql, params);
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
  default <T> List<T> executeObjectListQuery(
      Class<T> clazz,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T> List<T> executeObjectListUpdate(
      Class<T> clazz,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T> List<T> executeObjectListQuery(
      int isolationLevel,
      boolean readOnly,
      Class<T> clazz,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T> List<T> queryList(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> List<T> executeObjectListQuery(
      ObjectFactory<T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T> List<T> updateList(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateList(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> List<T> executeObjectListUpdate(
      ObjectFactory<T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return updateList(objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link List List&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * @return  An unmodifiable list
   */
  default <T> List<T> queryList(
      int isolationLevel,
      boolean readOnly,
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryList(isolationLevel, readOnly, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryList(int, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T> List<T> executeObjectListQuery(
      int isolationLevel,
      boolean readOnly,
      ObjectFactory<T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T, Ex extends Throwable> List<T> queryList(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, exClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryList(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> List<T> executeObjectListQuery(
      Class<Ex> exClass,
      ObjectFactoryE<T, Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryList(exClass, objectFactory, sql, params);
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
  default <T, Ex extends Throwable> List<T> updateList(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, exClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #updateList(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> List<T> executeObjectListUpdate(
      Class<Ex> exClass,
      ObjectFactoryE<T, Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return updateList(exClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link List List&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @return  An unmodifiable list
   */
  default <T, Ex extends Throwable> List<T> queryList(
      int isolationLevel,
      boolean readOnly,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    List<T> list = queryNewCollection(isolationLevel, readOnly, (Function<Integer, List<T>>) AoCollections::newArrayList, exClass, objectFactory, sql, params);
    return AoCollections.optimalUnmodifiableList(list);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryList(int, boolean, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, Ex extends Exception> List<T> executeObjectListQuery(
      int isolationLevel,
      boolean readOnly,
      Class<Ex> exClass,
      ObjectFactoryE<T, Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryList(isolationLevel, readOnly, exClass, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * <p>When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #streamUpdate(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> Stream<T> stream(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return stream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * <p>When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #stream(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> Stream<T> streamUpdate(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return stream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * <p>When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.</p>
   */
  default <T> Stream<T> stream(
      int isolationLevel,
      boolean readOnly,
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return stream(isolationLevel, readOnly, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * <p>When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #streamUpdate(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> Stream<T> stream(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return stream(Connections.DEFAULT_TRANSACTION_ISOLATION, true, exClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * <p>When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #stream(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> Stream<T> streamUpdate(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return stream(Connections.DEFAULT_TRANSACTION_ISOLATION, false, exClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * <p>When the factory is {@linkplain ObjectFactory#isNullable() nullable}, the stream may contain {@code null} elements.
   * Otherwise, will have the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  <T, Ex extends Throwable> Stream<T> stream(
      int isolationLevel,
      boolean readOnly,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex;

  /**
   * Read-only query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   *
   * <p>Always has the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @see  #streamOptionalUpdate(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> Stream<Optional<T>> streamOptional(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return streamOptional(Connections.DEFAULT_TRANSACTION_ISOLATION, true, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   *
   * <p>Always has the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @see  #streamOptional(com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)
   */
  default <T> Stream<Optional<T>> streamOptionalUpdate(
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return streamOptional(Connections.DEFAULT_TRANSACTION_ISOLATION, false, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   *
   * <p>Always has the characteristic {@link Spliterator#NONNULL}.</p>
   */
  default <T> Stream<Optional<T>> streamOptional(
      int isolationLevel,
      boolean readOnly,
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return streamOptional(isolationLevel, readOnly, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * Read-only query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   *
   * <p>Always has the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #streamOptionalUpdate(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> Stream<Optional<T>> streamOptional(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return streamOptional(Connections.DEFAULT_TRANSACTION_ISOLATION, true, exClass, objectFactory, sql, params);
  }

  /**
   * Read-write query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   *
   * <p>Always has the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   * </ul>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @see  #streamOptional(java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)
   */
  default <T, Ex extends Throwable> Stream<Optional<T>> streamOptionalUpdate(
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return streamOptional(Connections.DEFAULT_TRANSACTION_ISOLATION, false, exClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;Optional&lt;T&gt;&gt;} return type, objects are created with the provided factory.
   *
   * <p>Always has the characteristic {@link Spliterator#NONNULL}.</p>
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  default <T, Ex extends Throwable> Stream<Optional<T>> streamOptional(
      int isolationLevel,
      boolean readOnly,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    boolean isNullable = objectFactory.isNullable();
    return stream(
        isolationLevel,
        readOnly,
        exClass,
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
  default <T, C extends Collection<? super T>> C executeObjectCollectionQuery(
      C collection,
      Class<T> clazz,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T, C extends Collection<? super T>> C executeObjectCollectionUpdate(
      C collection,
      Class<T> clazz,
      String sql,
      Object ... params
  ) throws SQLException {
    return executeObjectCollectionQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, false, collection, clazz, sql, params);
  }

  /**
   * Query the database with a {@link Collection Collection&lt;T&gt;} return type.  Class &lt;T&gt; must have a constructor that takes a single argument of {@link ResultSet}.
   *
   * @deprecated  Please use {@link #queryCollection(int, boolean, java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with a constructor lambda {@code Class::new}.
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionQuery(
      int isolationLevel,
      boolean readOnly,
      C collection,
      Class<T> clazz,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T, C extends Collection<? super T>> C queryCollection(
      C collection,
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, true, collection, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryCollection(java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionQuery(
      C collection,
      ObjectFactory<T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T, C extends Collection<? super T>> C updateCollection(
      C collection,
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, collection, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateCollection(java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionUpdate(
      C collection,
      ObjectFactory<T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return updateCollection(collection, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   */
  default <T, C extends Collection<? super T>> C queryCollection(
      int isolationLevel,
      boolean readOnly,
      C collection,
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryCollection(isolationLevel, readOnly, collection, RuntimeException.class, objectFactory, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryCollection(int, boolean, java.util.Collection, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>> C executeObjectCollectionQuery(
      int isolationLevel,
      boolean readOnly,
      C collection,
      ObjectFactory<T> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T, C extends Collection<? super T>, Ex extends Throwable> C queryCollection(
      C collection,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, true, collection, exClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryCollection(java.util.Collection, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>, Ex extends Exception> C executeObjectCollectionQuery(
      C collection,
      Class<Ex> exClass,
      ObjectFactoryE<T, Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCollection(collection, exClass, objectFactory, sql, params);
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
  default <T, C extends Collection<? super T>, Ex extends Throwable> C updateCollection(
      C collection,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, collection, exClass, objectFactory, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #updateCollection(java.util.Collection, java.lang.Class, com.aoapps.dbc.ObjectFactoryE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default <T, C extends Collection<? super T>, Ex extends Exception> C executeObjectCollectionUpdate(
      C collection,
      Class<Ex> exClass,
      ObjectFactoryE<T, Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return updateCollection(collection, exClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  default <T, C extends Collection<? super T>, Ex extends Throwable> C queryCollection(
      int isolationLevel,
      boolean readOnly,
      C collection,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCall(
        isolationLevel,
        readOnly,
        exClass,
        (ResultSet results) -> {
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
  default <T, C extends Collection<? super T>, Ex extends Exception> C executeObjectCollectionQuery(
      int isolationLevel,
      boolean readOnly,
      C collection,
      Class<Ex> exClass,
      ObjectFactoryE<T, Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCollection(isolationLevel, readOnly, collection, exClass, objectFactory, sql, params);
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
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
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
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
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
      ObjectFactory<? extends T> objectFactory,
      String sql,
      Object ... params
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
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryNewCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, true, newCollection, exClass, objectFactory, sql, params);
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
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryNewCollection(Connections.DEFAULT_TRANSACTION_ISOLATION, false, newCollection, exClass, objectFactory, sql, params);
  }

  /**
   * Query the database with a {@link Collection Collection&lt;T&gt;} return type, objects are created with the provided factory.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   * @param  newCollection  given the row count, or {@code null} when unknown, creates the new collection
   */
  @SuppressWarnings("overloads")
  default <T, C extends Collection<? super T>, Ex extends Throwable> C queryNewCollection(
      int isolationLevel,
      boolean readOnly,
      Function<? super Integer, ? extends C> newCollection,
      Class<? extends Ex> exClass,
      ObjectFactoryE<? extends T, ? extends Ex> objectFactory,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCall(
        isolationLevel,
        readOnly,
        exClass,
        (ResultSet results) -> {
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
  default <T> T queryCall(
      ResultSetCallable<? extends T> resultSetCallable,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryCall(Connections.DEFAULT_TRANSACTION_ISOLATION, true, resultSetCallable, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryCall(com.aoapps.dbc.ResultSetCallable, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T> T executeQuery(
      ResultSetHandler<T> resultSetHandler,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T> T updateCall(
      ResultSetCallable<? extends T> resultSetCallable,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryCall(Connections.DEFAULT_TRANSACTION_ISOLATION, false, resultSetCallable, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateCall(com.aoapps.dbc.ResultSetCallable, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T> T executeUpdate(
      ResultSetHandler<T> resultSetHandler,
      String sql,
      Object ... params
  ) throws SQLException {
    return updateCall(resultSetHandler, sql, params);
  }

  /**
   * Query the database, calling the {@link ResultSetCallable} once.
   */
  default <T> T queryCall(
      int isolationLevel,
      boolean readOnly,
      ResultSetCallable<? extends T> resultSetCallable,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryCall(isolationLevel, readOnly, RuntimeException.class, resultSetCallable, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryCall(int, boolean, com.aoapps.dbc.ResultSetCallable, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T> T executeQuery(
      int isolationLevel,
      boolean readOnly,
      ResultSetHandler<T> resultSetHandler,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <T, Ex extends Throwable> T queryCall(
      Class<? extends Ex> exClass,
      ResultSetCallableE<? extends T, ? extends Ex> resultSetCallable,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCall(Connections.DEFAULT_TRANSACTION_ISOLATION, true, exClass, resultSetCallable, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryCall(java.lang.Class, com.aoapps.dbc.ResultSetCallableE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T, Ex extends Exception> T executeQuery(
      Class<Ex> exClass,
      ResultSetHandlerE<T, Ex> resultSetHandler,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCall(exClass, resultSetHandler, sql, params);
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
  default <T, Ex extends Throwable> T updateCall(
      Class<? extends Ex> exClass,
      ResultSetCallableE<? extends T, ? extends Ex> resultSetCallable,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCall(Connections.DEFAULT_TRANSACTION_ISOLATION, false, exClass, resultSetCallable, sql, params);
  }

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #updateCall(java.lang.Class, com.aoapps.dbc.ResultSetCallableE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T, Ex extends Exception> T executeUpdate(
      Class<Ex> exClass,
      ResultSetHandlerE<T, Ex> resultSetHandler,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return updateCall(exClass, resultSetHandler, sql, params);
  }

  /**
   * Query the database, calling the {@link ResultSetCallableE} once.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  <T, Ex extends Throwable> T queryCall(
      int isolationLevel,
      boolean readOnly,
      Class<? extends Ex> exClass,
      ResultSetCallableE<? extends T, ? extends Ex> resultSetCallable,
      String sql,
      Object ... params
  ) throws SQLException, Ex;

  /**
   * @param  <Ex>  An arbitrary exception type that may be thrown
   *
   * @deprecated  Please use {@link #queryCall(int, boolean, java.lang.Class, com.aoapps.dbc.ResultSetCallableE, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  @SuppressWarnings("removal")
  default <T, Ex extends Exception> T executeQuery(
      int isolationLevel,
      boolean readOnly,
      Class<Ex> exClass,
      ResultSetHandlerE<T, Ex> resultSetHandler,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    return queryCall(isolationLevel, readOnly, exClass, resultSetHandler, sql, params);
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
  default void queryRun(
      ResultSetRunnable resultSetRunnable,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default void updateRun(
      ResultSetRunnable resultSetRunnable,
      String sql,
      Object ... params
  ) throws SQLException {
    queryRun(Connections.DEFAULT_TRANSACTION_ISOLATION, false, resultSetRunnable, sql, params);
  }

  /**
   * Query the database, calling the {@link ResultSetRunnable} once.
   */
  default void queryRun(
      int isolationLevel,
      boolean readOnly,
      ResultSetRunnable resultSetRunnable,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default <Ex extends Throwable> void queryRun(
      Class<? extends Ex> exClass,
      ResultSetRunnableE<? extends Ex> resultSetRunnable,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    queryRun(Connections.DEFAULT_TRANSACTION_ISOLATION, true, exClass, resultSetRunnable, sql, params);
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
  default <Ex extends Throwable> void updateRun(
      Class<? extends Ex> exClass,
      ResultSetRunnableE<? extends Ex> resultSetRunnable,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    queryRun(Connections.DEFAULT_TRANSACTION_ISOLATION, false, exClass, resultSetRunnable, sql, params);
  }

  /**
   * Query the database, calling the {@link ResultSetRunnableE} once.
   *
   * @param  <Ex>  An arbitrary exception type that may be thrown
   */
  default <Ex extends Throwable> void queryRun(
      int isolationLevel,
      boolean readOnly,
      Class<? extends Ex> exClass,
      ResultSetRunnableE<? extends Ex> resultSetRunnable,
      String sql,
      Object ... params
  ) throws SQLException, Ex {
    queryCall(
        isolationLevel,
        readOnly,
        exClass,
        (ResultSet results) -> {
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
   * Read-only query the database with a required non-null {@code short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default short queryShort(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link Short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Short queryShortNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link Short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Short> queryShortOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link Short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Short> queryShortOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryShort(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default short executeShortQuery(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryShort(sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@code short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default short updateShort(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link Short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Short updateShortNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link Short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Short> updateShortOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link Short} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Short> updateShortOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryShort(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateShort(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default short executeShortUpdate(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return updateShort(sql, params);
  }

  /**
   * Query the database with a {@link Short} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Short queryShort(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(
        isolationLevel,
        readOnly,
        rowRequired,
        nullable,
        ObjectFactories.Short,
        sql,
        params
    );
  }

  /**
   * @deprecated  Please use {@link #queryShort(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Short queryShort(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryShort(isolationLevel, readOnly, rowRequired, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryShort(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Short executeShortQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryShort(isolationLevel, readOnly, rowRequired, false, sql, params);
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
  default List<Short> executeShortListQuery(
      String sql,
      Object ... params
  ) throws SQLException {
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
  default List<Short> executeShortListUpdate(
      String sql,
      Object ... params
  ) throws SQLException {
    return executeShortListQuery(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with a {@code List<Short>} return type.
   *
   * @deprecated  Please use {@link #queryList(int, boolean, com.aoapps.dbc.ObjectFactory, java.lang.String, java.lang.Object...)}
   *              with {@link ObjectFactories#Short}
   */
  @Deprecated(forRemoval = true)
  default List<Short> executeShortListQuery(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryList(isolationLevel, readOnly, ObjectFactories.Short, sql, params);
  }

  /**
   * Read-only query the database with a required non-null {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default String queryString(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default String queryStringNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<String> queryStringOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<String> queryStringOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryString(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default String executeStringQuery(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryString(sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default String updateString(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default String updateStringNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<String> updateStringOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link String} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<String> updateStringOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryString(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateString(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default String executeStringUpdate(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return updateString(sql, params);
  }

  /**
   * Query the database with a {@link String} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default String queryString(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, nullable, ObjectFactories.String, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryString(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default String queryString(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryString(isolationLevel, readOnly, rowRequired, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryString(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default String executeStringQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryString(isolationLevel, readOnly, rowRequired, true, sql, params);
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
  default List<String> queryStringList(
      String sql,
      Object ... params
  ) throws SQLException {
    return queryStringList(Connections.DEFAULT_TRANSACTION_ISOLATION, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryStringList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default List<String> executeStringListQuery(
      String sql,
      Object ... params
  ) throws SQLException {
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
  default List<String> updateStringList(
      String sql,
      Object ... params
  ) throws SQLException {
    return queryStringList(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * @deprecated  Please use {@link #updateStringList(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default List<String> executeStringListUpdate(
      String sql,
      Object ... params
  ) throws SQLException {
    return updateStringList(sql, params);
  }

  /**
   * Query the database with a {@link List List&lt;String&gt;} return type.
   */
  default List<String> queryStringList(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws SQLException {
    return queryList(isolationLevel, readOnly, ObjectFactories.String, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryStringList(int, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default List<String> executeStringListQuery(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws SQLException {
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
  default Stream<String> streamString(
      String sql,
      Object ... params
  ) throws SQLException {
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
  default Stream<String> streamStringUpdate(
      String sql,
      Object ... params
  ) throws SQLException {
    return streamString(Connections.DEFAULT_TRANSACTION_ISOLATION, false, sql, params);
  }

  /**
   * Query the database with a {@link Stream Stream&lt;String&gt;} return type.
   */
  default Stream<String> streamString(
      int isolationLevel,
      boolean readOnly,
      String sql,
      Object ... params
  ) throws SQLException {
    return stream(isolationLevel, readOnly, ObjectFactories.String, sql, params);
  }

  /**
   * Read-only query the database with a required non-null {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Timestamp queryTimestamp(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, false, sql, params);
  }

  /**
   * Read-only query the database with a required nullable {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Timestamp queryTimestampNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, true, true, true, sql, params);
  }

  /**
   * Read-only query the database with an optional non-null {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Timestamp> queryTimestampOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, false, sql, params));
  }

  /**
   * Read-only query the database with an optional nullable {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code true}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Timestamp> queryTimestampOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, true, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #queryTimestamp(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Timestamp executeTimestampQuery(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryTimestamp(sql, params);
  }

  /**
   * Read-write query the database with a required non-null {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value, never {@code null}.
   *
   * @throws  NoRowException     When has no row.
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Timestamp updateTimestamp(
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, false, sql, params);
  }

  /**
   * Read-write query the database with a required nullable {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code true}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@code null} when row with SQL NULL value.
   *
   * @throws  NoRowException     When has no row.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Timestamp updateTimestampNullable(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, false, true, true, sql, params);
  }

  /**
   * Read-write query the database with an optional non-null {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code false}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when no row.
   *
   * @throws  NullDataException  When has a row, but with SQL NULL value.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Timestamp> updateTimestampOptional(
      String sql,
      Object ... params
  ) throws NullDataException, ExtraRowException, SQLException {
    return Optional.ofNullable(queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, false, sql, params));
  }

  /**
   * Read-write query the database with an optional nullable {@link Timestamp} return type.
   * <ul>
   *   <li>isolationLevel = {@link Connections#DEFAULT_TRANSACTION_ISOLATION}</li>
   *   <li>readOnly = {@code false}</li>
   *   <li>rowRequired = {@code false}</li>
   *   <li>nullable = {@code true}</li>
   * </ul>
   *
   * @return  The value or {@link Optional#empty()} when either no row or row with SQL NULL value.
   *
   * @throws  ExtraRowException  When has more than one row.
   */
  default Optional<Timestamp> updateTimestampOptionalNullable(
      String sql,
      Object ... params
  ) throws ExtraRowException, SQLException {
    return Optional.ofNullable(queryTimestamp(Connections.DEFAULT_TRANSACTION_ISOLATION, false, false, true, sql, params));
  }

  /**
   * @deprecated  Please use {@link #updateTimestamp(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Timestamp executeTimestampUpdate(
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return updateTimestamp(sql, params);
  }

  /**
   * Query the database with a {@link Timestamp} return type.
   *
   * @return  The value, {@code null} when no row and {@code !rowRequired},
   *          or {@code null} when row with SQL NULL value and {@code nullable}.
   *
   * @throws  NoRowException     When has no row and {@code rowRequired}.
   * @throws  NullDataException  When has a row, but with SQL NULL value and {@code !nullable}.
   * @throws  ExtraRowException  When has more than one row.
   */
  default Timestamp queryTimestamp(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      boolean nullable,
      String sql,
      Object ... params
  ) throws NoRowException, NullDataException, ExtraRowException, SQLException {
    return queryObject(isolationLevel, readOnly, rowRequired, nullable, ObjectFactories.Timestamp, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryTimestamp(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Timestamp queryTimestamp(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryTimestamp(isolationLevel, readOnly, rowRequired, true, sql, params);
  }

  /**
   * @deprecated  Please use {@link #queryTimestamp(int, boolean, boolean, boolean, java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default Timestamp executeTimestampQuery(
      int isolationLevel,
      boolean readOnly,
      boolean rowRequired,
      String sql,
      Object ... params
  ) throws NoRowException, ExtraRowException, SQLException {
    return queryTimestamp(isolationLevel, readOnly, rowRequired, true, sql, params);
  }

  /**
   * Performs an update on the database and returns the number of rows affected.
   */
  // TODO: Variant(s) that check the number of items updated when 1 expected, throws NoRowException, ExtraRowException
  //       Call it update1?  updateSingle?  updateRow?
  //       Implement as a functional interface "UpdateValidator" (int) -> SQLException?
  int update(
      String sql,
      Object ... params
  ) throws SQLException;

  /**
   * Performs an update on the database and returns the number of rows affected.
   */
  long largeUpdate(
      String sql,
      Object ... params
  ) throws SQLException;

  /**
   * @deprecated  Please use {@link #update(java.lang.String, java.lang.Object...)}
   */
  @Deprecated(forRemoval = true)
  default int executeUpdate(
      String sql,
      Object ... params
  ) throws SQLException {
    return update(sql, params);
  }
}
