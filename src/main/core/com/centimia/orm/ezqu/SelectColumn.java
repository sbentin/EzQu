/*
 * Copyright (c) 2025-2030 Centimia Ltd.
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * Licensed under Eclipse Public License, Version 2.0,
 * Initial Developer: Shai Bentin, Centimia Ltd.
 */
package com.centimia.orm.ezqu;

/**
 * This class represents a column of a table in a query.
 *
 * @param <T> the table data type
 */
class SelectColumn<T> {
    private SelectTable<T> selectTable;
    private FieldDefinition fieldDef;

    SelectColumn(SelectTable<T> table, FieldDefinition fieldDef) {
        this.selectTable = table;
        this.fieldDef = fieldDef;
    }

    void appendSQL(SQLStatement stat, String as) {
        if (selectTable.getQuery().isJoin()) {
            stat.appendSQL(selectTable.getAs() + "." + fieldDef.columnName);
        }
        else {
            stat.appendSQL(as + "." + fieldDef.columnName);
        }
    }

    FieldDefinition getFieldDefinition() {
        return fieldDef;
    }

    SelectTable<T> getSelectTable() {
        return selectTable;
    }
}