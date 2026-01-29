/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2014, 2015, 2019, 2020, 2021, 2022, 2024  AO Industries, Inc.
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
import java.util.Spliterator;

/**
 * Creates instances of objects of the generics type from a result set.
 *
 * @param  <Ex>  An arbitrary exception type that may be thrown
 *
 * @author  AO Industries, Inc.
 */
@FunctionalInterface
public interface ObjectFactoryE<T, Ex extends Throwable> {

  /**
   * Creates one object from the current values in the ResultSet.
   */
  T createObject(ResultSet result) throws SQLException, Ex;

  /**
   * Gets if this object factory can return {@code null} from {@link ObjectFactoryE#createObject(java.sql.ResultSet)}.
   *
   * <p>Note: This is used to enable the {@link Spliterator#NONNULL} on streams.</p>
   *
   * @return  {@code true} by default
   */
  default boolean isNullable() {
    return true;
  }
}
