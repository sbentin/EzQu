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
 * An expression to order by in a query.
 *
 * @param <T> the query data type
 */
class OrderExpression<T> {
    private Query<T> query;
    private Object expression;
    private boolean desc;
    private boolean nullsFirst;
    private boolean nullsLast;

    OrderExpression(Query<T> query, Object expression, boolean desc, boolean nullsFirst, boolean nullsLast) {
        this.query = query;
        this.expression = expression;
        this.desc = desc;
        this.nullsFirst = nullsFirst;
        this.nullsLast = nullsLast;
    }

	void appendSQL(SQLStatement stat) {
	    boolean simpleDialect = query.getDb().factory.dialect.ordinal() <= 1;
	    
	    if (simpleDialect) {
	    	// 0 = H2, 1 = ORACLE
	        // Straightforward SQL: just ORDER BY expr [ASC|DESC] [NULLS ...]
	        query.appendSQL(stat, expression, false, null);
	        stat.appendSQL(desc ? " DESC" : " ASC");

	        if (desc && nullsFirst) {
	            stat.appendSQL(" NULLS FIRST");
	        }
	        else if (!desc && nullsLast) {
	            stat.appendSQL(" NULLS LAST");
	        }
	        return;
	    }

	    // Complex dialects: emulate NULLS FIRST/LAST with CASE expressions.
	    boolean needsCase = (desc && nullsFirst) || (!desc && nullsLast);
	    if (needsCase) {
	        // NULL-first descending or NULL-last ascending → prepend CASE ordering
	        stat.appendSQL("(CASE WHEN " + expression + " IS NULL THEN 0 ELSE 1 END)");

	        // If this CASE is not the final sort key, add comma + space
	        stat.appendSQL(!desc ? " DESC, " : ", ");
	    }

	    // Now the actual expression ordering
	    query.appendSQL(stat, expression, false, null);
	    stat.appendSQL(desc ? " DESC" : " ASC");
	}
}