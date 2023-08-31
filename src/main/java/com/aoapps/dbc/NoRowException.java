/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2010, 2011, 2015, 2020, 2021, 2022, 2023  AO Industries, Inc.
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
import java.sql.SQLNonTransientException;

/**
 * Thrown when no row available and a row is required.
 */
public class NoRowException extends SQLNonTransientException implements LocalizedException {

  private static final long serialVersionUID = 5397878995581459678L;

  /**
   * See <a href="https://en.wikipedia.org/wiki/SQLSTATE">SQLState - Wikipedia</a>.
   */
  private static final String SQLSTATE = "02000";

  /**
   * See <a href="https://en.wikipedia.org/wiki/SQLSTATE">SQLState - Wikipedia</a>.
   */
  private static final String DEFAULT_REASON = "no data";

  protected final Resources resources;
  protected final String key;
  protected final Serializable[] args;

  public NoRowException() {
    this(DEFAULT_REASON);
  }

  public NoRowException(String reason) {
    super(reason, SQLSTATE);
    resources = null;
    key = null;
    args = null;
  }

  public NoRowException(Resources resources, String key) {
    super(resources.getMessage(key), SQLSTATE);
    this.resources = resources;
    this.key = key;
    this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
  }

  public NoRowException(Resources resources, String key, Serializable... args) {
    super(resources.getMessage(key, (Object[]) args), SQLSTATE);
    this.resources = resources;
    this.key = key;
    this.args = args;
  }

  public NoRowException(Throwable cause) {
    this(DEFAULT_REASON, cause);
  }

  public NoRowException(String reason, Throwable cause) {
    super(reason, SQLSTATE, cause);
    resources = null;
    key = null;
    args = null;
  }

  public NoRowException(Throwable cause, Resources resources, String key) {
    super(resources.getMessage(key), SQLSTATE, cause);
    this.resources = resources;
    this.key = key;
    this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
  }

  public NoRowException(Throwable cause, Resources resources, String key, Serializable... args) {
    super(resources.getMessage(key, (Object[]) args), SQLSTATE, cause);
    this.resources = resources;
    this.key = key;
    this.args = args;
  }

  @Override
  public String getLocalizedMessage() {
    return (resources == null) ? super.getLocalizedMessage() : resources.getMessage(key, (Object[]) args);
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
   * {@inheritDoc}
   *
   * @return  No defensive copy
   */
  @Override
  @SuppressWarnings("ReturnOfCollectionOrArrayField")
  public final Serializable[] getArgs() {
    return args;
  }

  static {
    Throwables.registerSurrogateFactory(NoRowException.class, (template, cause) ->
        (template.resources == null)
            ? new NoRowException(template.getMessage(), cause)
            : new NoRowException(cause, template.resources, template.key, template.args)
    );
  }
}
