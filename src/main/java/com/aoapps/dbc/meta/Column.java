/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2011, 2015, 2021, 2022  AO Industries, Inc.
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

package com.aoapps.dbc.meta;

/**
 * A friendly wrapper around database meta data.
 *
 * @author  AO Industries, Inc.
 */
public class Column {

  private final Table table;
  private final String name;
  private final int dataType;
  private final String typeName;
  private final Integer columnSize;
  private final Integer decimalDigits;
  private final int nullable;
  private final String columnDef;
  private final Integer charOctetLength;
  private final int ordinalPosition;
  private final String isNullable;
  private final String isAutoincrement;
  private final int hashCode;

  protected Column(
    Table table,
    String name,
    int dataType,
    String typeName,
    Integer columnSize,
    Integer decimalDigits,
    int nullable,
    String columnDef,
    Integer charOctetLength,
    int ordinalPosition,
    String isNullable,
    String isAutoincrement
  ) {
    this.table = table;
    if (name.indexOf('"') != -1) {
      throw new IllegalArgumentException();
    }
    this.name = name;
    this.dataType = dataType;
    this.typeName = typeName;
    this.columnSize = columnSize;
    this.decimalDigits = decimalDigits;
    this.nullable = nullable;
    this.columnDef = columnDef;
    this.charOctetLength = charOctetLength;
    this.ordinalPosition = ordinalPosition;
    this.isNullable = isNullable;
    this.isAutoincrement = isAutoincrement;
    this.hashCode = table.hashCode() * 31 + name.hashCode();
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Two columns are equal if they have the same schema name, table name, and column name.
   */
  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Column)) {
      return false;
    }
    Column other = (Column)obj;
    return
      hashCode == other.hashCode
      && name.equals(other.name)
      && table.equals(other.table)
    ;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public Table getTable() {
    return table;
  }

  public String getName() {
    return name;
  }

  public int getDataType() {
    return dataType;
  }

  public String getTypeName() {
    return typeName;
  }

  public Integer getColumnSize() {
    return columnSize;
  }

  public Integer getDecimalDigits() {
    return decimalDigits;
  }

  public int getNullable() {
    return nullable;
  }

  public String getColumnDef() {
    return columnDef;
  }

  public Integer getCharOctetLength() {
    return charOctetLength;
  }

  public int getOrdinalPosition() {
    return ordinalPosition;
  }

  public String getIsNullable() {
    return isNullable;
  }

  public String getIsAutoincrement() {
    return isAutoincrement;
  }
}
