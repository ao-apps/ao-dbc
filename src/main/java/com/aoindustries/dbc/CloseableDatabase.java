/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2020  AO Industries, Inc.
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

import com.aoindustries.lang.AutoCloseableE;
import com.aoindustries.sql.pool.AOConnectionPool;
import java.sql.SQLException;
import java.util.logging.Logger;
import javax.sql.DataSource;

/**
 * Allows closing of the underlying pool.  This is useful in application life cycle management, such as shutting down
 * a database as a web application is being destroyed.
 * <p>
 * The component that creates the database should generally be responsible for closing it.  To make it evident that the
 * rest of the application should not close the database, only the {@link Database} base type should be provided.
 * </p>
 */
public class CloseableDatabase extends Database implements AutoCloseableE<SQLException> {

	public CloseableDatabase(String driver, String url, String user, String password, int numConnections, long maxConnectionAge, Logger logger) {
		super(driver, url, user, password, numConnections, maxConnectionAge, logger);
	}

	public CloseableDatabase(AOConnectionPool pool) {
		super(pool);
	}

	public CloseableDatabase(DataSource dataSource, Logger logger) {
		super(dataSource, logger);
	}

	/**
	 * Closes the database.
	 *
	 * @see  AOConnectionPool#close()
	 */
	@Override
	public void close() {
		super.close();
	}
}
