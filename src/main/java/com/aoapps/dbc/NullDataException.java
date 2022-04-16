/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.lang.EmptyArrays;
import com.aoapps.lang.Throwables;
import com.aoapps.lang.exception.LocalizedException;
import com.aoapps.lang.i18n.Resources;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;

/**
 * Thrown when database contains a null value and a non-null value is required.
 */
// TODO: Extend SQLDataException, review all subclasses of SQLException for better base class
public class NullDataException extends SQLDataException implements LocalizedException {

	private static final long serialVersionUID = 1L;

	protected final Resources resources;
	protected final String key;
	protected final Serializable[] args;

	public NullDataException() {
		this("null value not allowed");
	}

	public NullDataException(String reason) {
		super(reason, "22004");
		resources = null;
		key = null;
		args = null;
	}

	public NullDataException(ResultSet result) throws SQLException {
		this(DatabaseUtils.getRow(result));
	}

	public NullDataException(Resources resources, String key) {
		super(resources.getMessage(key), "22004");
		this.resources = resources;
		this.key = key;
		this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
	}

	public NullDataException(Resources resources, String key, Serializable... args) {
		super(resources.getMessage(key, (Object[])args), "22004");
		this.resources = resources;
		this.key = key;
		this.args = args;
	}

	public NullDataException(Throwable cause) {
		this("null value not allowed", cause);
	}

	public NullDataException(String reason, Throwable cause) {
		super(reason, "22004", cause);
		resources = null;
		key = null;
		args = null;
	}

	public NullDataException(ResultSet result, Throwable cause) throws SQLException {
		this(DatabaseUtils.getRow(result), cause);
	}

	public NullDataException(Throwable cause, Resources resources, String key) {
		super(resources.getMessage(key), "22004", cause);
		this.resources = resources;
		this.key = key;
		this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
	}

	public NullDataException(Throwable cause, Resources resources, String key, Serializable... args) {
		super(resources.getMessage(key, (Object[])args), "22004", cause);
		this.resources = resources;
		this.key = key;
		this.args = args;
	}

	@Override
	public String getLocalizedMessage() {
		return (resources == null) ? super.getLocalizedMessage() : resources.getMessage(key, (Object[])args);
	}

	@Override
	public final Resources getResources() {
		return resources;
	}

	@Override
	public final String getKey() {
		return key;
	}

	/**
	 * @return  No defensive copy
	 */
	@Override
	@SuppressWarnings("ReturnOfCollectionOrArrayField")
	public final Serializable[] getArgs() {
		return args;
	}

	static {
		Throwables.registerSurrogateFactory(NullDataException.class, (template, cause) ->
			(template.resources == null)
				? new NullDataException(template.getMessage(), cause)
				: new NullDataException(cause, template.resources, template.key, template.args)
		);
	}
}
