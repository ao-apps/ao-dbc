/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2010, 2011, 2014, 2015, 2019, 2020, 2021  AO Industries, Inc.
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
package com.aoapps.dbc;

import java.sql.SQLException;

/**
 * Target that may be used by {@link Database#transactionCall(com.aoapps.dbc.DatabaseCallable)}.
 *
 * @see  Database#transactionCall(com.aoapps.dbc.DatabaseCallable)
 *
 * @author  AO Industries, Inc.
 */
@FunctionalInterface
public interface DatabaseCallable<V> extends DatabaseCallableE<V, RuntimeException> {

	@Override
	// TODO: Should these take DatabaseAccess instead?
	//       This would support non-transactional, auto-commit, direct usage through Database,
	//       which might be more optimal for one-off actions.
	//       DatabaseAccess might have to gain some methods, though, such as close()/rollback()/commit(), which would be no-ops?
	V call(DatabaseConnection db) throws SQLException;
}
