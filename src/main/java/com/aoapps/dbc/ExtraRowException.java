/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2013, 2015, 2020, 2021  AO Industries, Inc.
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

import com.aoapps.lang.EmptyArrays;
import com.aoapps.lang.Throwables;
import com.aoapps.lang.i18n.Resources;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;

/**
 * Thrown when more rows are retrieved than expected.
 */
public class ExtraRowException extends SQLNonTransientException {

	private static final long serialVersionUID = 1L;

	protected final Resources resources;
	protected final String key;
	protected final Serializable[] args;

	public ExtraRowException() {
		this("additional result sets returned");
	}

	public ExtraRowException(String reason) {
		super(reason, "0100D");
		resources = null;
		key = null;
		args = null;
	}

	public ExtraRowException(ResultSet result) throws SQLException {
		this(DatabaseUtils.getRow(result));
	}

	public ExtraRowException(Resources resources, String key) {
		super(resources.getMessage(key), "0100D");
		this.resources = resources;
		this.key = key;
		this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
	}

	public ExtraRowException(Resources resources, String key, Serializable... args) {
		super(resources.getMessage(key, (Object[])args), "0100D");
		this.resources = resources;
		this.key = key;
		this.args = args;
	}

	public ExtraRowException(Throwable cause) {
		this("additional result sets returned", cause);
	}

	public ExtraRowException(String reason, Throwable cause) {
		super(reason, "0100D", cause);
		resources = null;
		key = null;
		args = null;
	}

	public ExtraRowException(ResultSet result, Throwable cause) throws SQLException {
		this(DatabaseUtils.getRow(result), cause);
	}

	public ExtraRowException(Throwable cause, Resources resources, String key) {
		super(resources.getMessage(key), "0100D", cause);
		this.resources = resources;
		this.key = key;
		this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
	}

	public ExtraRowException(Throwable cause, Resources resources, String key, Serializable... args) {
		super(resources.getMessage(key, (Object[])args), "0100D", cause);
		this.resources = resources;
		this.key = key;
		this.args = args;
	}

	@Override
	public String getLocalizedMessage() {
		return (resources == null) ? super.getLocalizedMessage() : resources.getMessage(key, (Object[])args);
	}

	static {
		Throwables.registerSurrogateFactory(ExtraRowException.class, (template, cause) ->
			(template.resources == null)
				? new ExtraRowException(template.getMessage(), cause)
				: new ExtraRowException(cause, template.resources, template.key, template.args)
		);
	}
}
