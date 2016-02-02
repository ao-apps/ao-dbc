/*
 * ao-dbc - Simplified JDBC access for simplified code.
 * Copyright (C) 2011, 2013, 2015, 2016  AO Industries, Inc.
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
package com.aoindustries.dbc.meta;

import com.aoindustries.table.IndexType;
import com.aoindustries.util.AoCollections;
import com.aoindustries.util.StringUtility;
import java.util.ArrayList;
import java.util.List;

/**
 * A friendly wrapper around database meta data.
 *
 * @author  AO Industries, Inc.
 */
public class Index {

    private final Table table;
    private final String name;
    private final IndexType type;
    private final List<Column> columns;

    protected Index(
        Table table,
        String name,
        IndexType type,
        List<Column> columns
    ) {
        this.table = table;
        this.name = name;
        this.type = type;
        if(columns.isEmpty()) throw new IllegalArgumentException("columns.isEmpty()");
        this.columns = AoCollections.optimalUnmodifiableList(new ArrayList<>(columns));
    }

    @Override
    public String toString() {
        if(name==null) return type + " (" + StringUtility.join(columns, ", ") + ")";
        else return name + " " + type + " (" + StringUtility.join(columns, ", ") + ")";
    }

    public Table getTable() {
        return table;
    }

    public String getName() {
        return name;
    }

    public IndexType getIndexType() {
        return type;
    }

    public List<Column> getColumns() {
        return columns;
    }
}
