/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2023  AO Industries, Inc.
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
import java.sql.SQLException;

/**
 * When an event handler fails,
 * any {@link Throwable} other than {@link Error}, {@link RuntimeException},
 * or {@link SQLException} will be wrapped in an {@link EventException}.
 *
 * @see DatabaseConnection#onCommit(com.aoapps.lang.RunnableE)
 * @see DatabaseConnection#onRollback(com.aoapps.lang.RunnableE)
 */
public class EventException extends SQLException implements LocalizedException {

  private static final long serialVersionUID = 1L;

  /**
   * See <a href="https://en.wikipedia.org/wiki/SQLSTATE">SQLState - Wikipedia</a>.
   */
  private static final String SQLSTATE = "45000";

  /**
   * See <a href="https://en.wikipedia.org/wiki/SQLSTATE">SQLState - Wikipedia</a>.
   */
  private static final String DEFAULT_REASON = "unhandled user-defined exception";

  protected final Resources resources;
  protected final String key;
  protected final Serializable[] args;

  public EventException() {
    this(DEFAULT_REASON);
  }

  public EventException(String reason) {
    super(reason, SQLSTATE);
    resources = null;
    key = null;
    args = null;
  }

  public EventException(Resources resources, String key) {
    super(resources.getMessage(key), SQLSTATE);
    this.resources = resources;
    this.key = key;
    this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
  }

  public EventException(Resources resources, String key, Serializable... args) {
    super(resources.getMessage(key, (Object[]) args), SQLSTATE);
    this.resources = resources;
    this.key = key;
    this.args = args;
  }

  public EventException(Throwable cause) {
    this(DEFAULT_REASON, cause);
  }

  public EventException(String reason, Throwable cause) {
    super(reason, SQLSTATE, cause);
    resources = null;
    key = null;
    args = null;
  }

  public EventException(Throwable cause, Resources resources, String key) {
    super(resources.getMessage(key), SQLSTATE, cause);
    this.resources = resources;
    this.key = key;
    this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
  }

  public EventException(Throwable cause, Resources resources, String key, Serializable... args) {
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
    Throwables.registerSurrogateFactory(EventException.class, (template, cause) ->
        (template.resources == null)
            ? new EventException(template.getMessage(), cause)
            : new EventException(cause, template.resources, template.key, template.args)
    );
  }
}
