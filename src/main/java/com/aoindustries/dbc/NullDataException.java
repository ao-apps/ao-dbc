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

import com.aoindustries.lang.Throwables;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Thrown when database contains a null value and a non-null value is required.
 */
public class NullDataException extends SQLException {

	private static final long serialVersionUID = 1L;

	public NullDataException() {
		super("null value not allowed", "22004");
	}

	public NullDataException(String reason) {
		super(reason, "22004");
	}

	public NullDataException(ResultSet result) throws SQLException {
		super(DatabaseUtils.getRow(result), "22004");
	}

	public NullDataException(Throwable cause) {
		super("null value not allowed", "22004", cause);
	}

	public NullDataException(String reason, Throwable cause) {
		super(reason, "22004", cause);
	}

	public NullDataException(ResultSet result, Throwable cause) throws SQLException {
		super(DatabaseUtils.getRow(result), "22004", cause);
	}

	static {
		Throwables.registerSurrogateFactory(NullDataException.class, (template, cause) ->
			new NullDataException(template.getMessage(), cause)
		);
	}
}
