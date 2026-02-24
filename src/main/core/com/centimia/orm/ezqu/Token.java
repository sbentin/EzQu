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
 * Classes implementing this interface can be used as a token in a statement.
 */
@FunctionalInterface
interface Token {
    /**
     * Append the SQL to the given statement using the given query.
     *
     * @param stat the statement to append the SQL to
     * @param query the query to use
     */
    void appendSQL(SQLStatement stat, Query<?> query);
}
