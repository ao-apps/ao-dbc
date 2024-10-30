/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2014, 2015, 2016, 2018, 2020, 2021, 2022, 2023, 2024  AO Industries, Inc.
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

import static com.aoapps.encoding.TextInPsqlEncoder.textInPsqlEncoder;

import java.io.IOException;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Types;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation utilities.
 *
 * @author  AO Industries, Inc.
 */
public final class DatabaseUtils {

  /** Make no instances. */
  private DatabaseUtils() {
    throw new AssertionError();
  }

  private static final Logger logger = Logger.getLogger(DatabaseUtils.class.getName());

  private static final int AUTO_ELLIPSIS_LEN = 32;

  private static void encodeWithEllipsis(StringBuilder sb, String value) {
    try {
      textInPsqlEncoder.writePrefixTo(sb);
      for (int i = 0, chars = 0, len = value.length(), codePoint, charCount;
          i < len;
          i += charCount, chars++
      ) {
        codePoint = value.codePointAt(i);
        charCount = Character.charCount(codePoint);
        if (chars >= AUTO_ELLIPSIS_LEN) {
          sb.append('…');
          break;
        }
        // PostgreSQL encoder does not support NULL, handle it here instead of exception
        if (codePoint == 0) {
          sb.append("\\x00");
        } else if (charCount == 2) {
          if (Character.isValidCodePoint(codePoint)) {
            textInPsqlEncoder.append(Character.highSurrogate(codePoint), sb);
            textInPsqlEncoder.append(Character.lowSurrogate(codePoint), sb);
          } else {
            throw new IOException(String.format("Invalid code point: 0x%X", codePoint));
          }
        } else {
          assert charCount == 1;
          textInPsqlEncoder.append((char) codePoint, sb);
        }
      }
      textInPsqlEncoder.writeSuffixTo(sb, false);
    } catch (IOException e) {
      // TODO: This pattern is used may places, define in ao-lang?
      throw new AssertionError("IOException should not occur on StringBuilder", e);
    }
  }

  /**
   * Gets a user-friendly description of the provided result in a string formatted like
   * {@code ('value', 'value', 'long_value_cutoff_at_32_characte…', int_value, NULL, …)}.
   * This must not be used generate SQL statements - it is just to provide user display.
   */
  @SuppressWarnings("fallthrough")
  public static String getRow(ResultSet result) throws SQLException {
    StringBuilder sb = new StringBuilder();
    sb.append('(');
    ResultSetMetaData metaData = result.getMetaData();
    int colCount = metaData.getColumnCount();
    for (int c = 1; c <= colCount; c++) {
      if (c > 1) {
        sb.append(", ");
      }
      int colType = metaData.getColumnType(c);
      switch (colType) {
        case Types.BINARY:
          {
            byte[] value = result.getBytes(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("BINARY(length = ").append(value.length).append(')');
            }
            break;
          }
        case Types.VARBINARY:
          {
            byte[] value = result.getBytes(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("VARBINARY(length = ").append(value.length).append(')');
            }
            break;
          }
        case Types.LONGVARBINARY:
          {
            byte[] value = result.getBytes(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("LONGVARBINARY(length = ").append(value.length).append(')');
            }
            break;
          }
        case Types.OTHER:
          {
            Object value = result.getObject(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("OTHER(");
              encodeWithEllipsis(sb, value.toString());
              sb.append(')');
            }
            break;
          }
        case Types.JAVA_OBJECT:
          {
            Object value = result.getObject(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("JAVA_OBJECT(");
              encodeWithEllipsis(sb, value.toString());
              sb.append(')');
            }
            break;
          }
        case Types.DISTINCT:
          {
            Object value = result.getObject(c);
            sb.append((value == null) ? "NULL" : "DISTINCT");
            break;
          }
        case Types.STRUCT:
          {
            Object value = result.getObject(c);
            sb.append((value == null) ? "NULL" : "STRUCT");
            break;
          }
        case Types.ARRAY:
          {
            Array value = result.getArray(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("ARRAY");
              value.free();
            }
            break;
          }
        case Types.BLOB:
          {
            Blob value = result.getBlob(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("BLOB(length = ").append(value.length()).append(')');
              value.free();
            }
            break;
          }
        case Types.CLOB:
          {
            Clob value = result.getClob(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("CLOB(length = ").append(value.length()).append(')');
              value.free();
            }
            break;
          }
        case Types.REF:
          {
            Ref value = result.getRef(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("REF(baseTypeName = ");
              encodeWithEllipsis(sb, value.getBaseTypeName());
              sb.append(')');
            }
            break;
          }
        case Types.DATALINK:
          {
            Object value = result.getObject(c);
            sb.append((value == null) ? "NULL" : "DATALINK");
            break;
          }
        case Types.ROWID: // JDBC 4.0 Types
          {
            RowId value = result.getRowId(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("ROWID(bytes.length = ").append(value.getBytes().length).append(')');
            }
            break;
          }
        case Types.NCLOB: // JDBC 4.0 Types
          {
            NClob value = result.getNClob(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("NCLOB(length = ").append(value.length()).append(')');
            }
            break;
          }
        case Types.SQLXML: // JDBC 4.0 Types
          {
            SQLXML value = result.getSQLXML(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              sb.append("SQLXML(");
              encodeWithEllipsis(sb, value.getString());
              sb.append(')');
              value.free();
            }
            break;
          }
        case Types.REF_CURSOR: // JDBC 4.2 Types
          {
            Object value = result.getObject(c);
            sb.append((value == null) ? "NULL" : "REF_CURSOR");
            break;
          }
        // Types without quotes
        // Note: Matches JdbcResourceSynchronizer
        case Types.BIGINT:
        case Types.BIT:
        case Types.BOOLEAN:
        case Types.DECIMAL:
        case Types.DOUBLE:
        case Types.FLOAT:
        case Types.INTEGER:
        case Types.NULL:
        case Types.NUMERIC:
        case Types.REAL:
        case Types.SMALLINT:
        case Types.TINYINT:
          sb.append(Objects.toString(result.getObject(c), "NULL"));
          break;
        default:
          if (logger.isLoggable(Level.WARNING)) {
            logger.log(Level.WARNING, "Unexpected column type: {0}", colType);
          }
        // fall-through to quoted
        case Types.CHAR:
        case Types.DATE:
        case Types.LONGNVARCHAR: // JDBC 4.0 Types
        case Types.LONGVARCHAR:
        case Types.NCHAR: // JDBC 4.0 Types
        case Types.NVARCHAR: // JDBC 4.0 Types
        case Types.TIME:
        case Types.TIME_WITH_TIMEZONE: // JDBC 4.2 Types
        case Types.TIMESTAMP:
        case Types.TIMESTAMP_WITH_TIMEZONE: // JDBC 4.2 Types
        case Types.VARCHAR:
          {
            String value = result.getString(c);
            if (value == null) {
              sb.append("NULL");
            } else {
              encodeWithEllipsis(sb, value);
            }
            break;
          }
      }
    }
    sb.append(')');
    return sb.toString();
  }

  /**
   * Gets the number of rows or {@code -1} when unknown.
   */
  public static int getRowCount(ResultSet results) throws SQLException {
    int resultType = results.getType();
    switch (resultType) {
      case ResultSet.TYPE_FORWARD_ONLY:
        return -1;
      case ResultSet.TYPE_SCROLL_INSENSITIVE:
      case ResultSet.TYPE_SCROLL_SENSITIVE:
        int rowCount = 0;
        if (results.last()) {
          rowCount = results.getRow();
          results.beforeFirst();
        }
        return rowCount;
      default:
        throw new AssertionError(resultType);
    }
  }
}
