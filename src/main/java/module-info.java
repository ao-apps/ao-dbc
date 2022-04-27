/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2021, 2022  AO Industries, Inc.
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
module com.aoapps.dbc {
  exports com.aoapps.dbc;
  exports com.aoapps.dbc.meta;
  uses java.sql.SQLData; // TODO: This is in different module, can we use it?  Make our own interface that loads it?  "SQLDataFactory"?
  // Direct
  requires com.aoapps.collections; // <groupId>com.aoapps</groupId><artifactId>ao-collections</artifactId>
  requires com.aoapps.concurrent; // <groupId>com.aoapps</groupId><artifactId>ao-concurrent</artifactId>
  requires com.aoapps.encoding; // <groupId>com.aoapps</groupId><artifactId>ao-encoding</artifactId>
  requires com.aoapps.hodgepodge; // <groupId>com.aoapps</groupId><artifactId>ao-hodgepodge</artifactId>
  requires com.aoapps.lang; // <groupId>com.aoapps</groupId><artifactId>ao-lang</artifactId>
  requires com.aoapps.sql; // <groupId>com.aoapps</groupId><artifactId>ao-sql</artifactId>
  requires com.aoapps.sql.failfast; // <groupId>com.aoapps</groupId><artifactId>ao-sql-failfast</artifactId>
  requires com.aoapps.sql.pool; // <groupId>com.aoapps</groupId><artifactId>ao-sql-pool</artifactId>
  requires com.aoapps.sql.tracker; // <groupId>com.aoapps</groupId><artifactId>ao-sql-tracker</artifactId>
  // Java SE
  requires java.logging;
  requires java.sql;
} // TODO: Avoiding rewrite-maven-plugin-4.22.2 truncation
