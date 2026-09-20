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
 * @param &lt;T&gt; the query data type
 */
class OrderExpression implements Token {
    private Object expression;
    private boolean desc;
    private Boolean nullsFirst;

    OrderExpression(Object expression, boolean desc, Boolean nullsFirst) {
        this.expression = expression;
        this.desc = desc;
        this.nullsFirst = nullsFirst;
    }

	public void appendSQL(SQLStatement stat, Query<?> query) {
	    boolean simpleDialect = query.getDb().factory.dialect.ordinal() <= 1;
	    
	    if (simpleDialect) {
	    	// 0 = H2, 1 = ORACLE
	        // Straightforward SQL: just ORDER BY expr [ASC|DESC] [NULLS ...]
	        query.appendSQL(stat, expression, false, null);
	        stat.appendSQL(desc ? " DESC" : " ASC");

	        if (null != nullsFirst) {
	        	// if nullsFirst is null then it means we want the default ordering of the underlying db
		        if (Boolean.TRUE.equals(nullsFirst)) {
		            stat.appendSQL(" NULLS FIRST");
		        }
		        else {
		            stat.appendSQL(" NULLS LAST");
		        }
	        }
	        return;
	    }

	    // Complex dialects: emulate NULLS FIRST/LAST with CASE expressions.
	    boolean needsCase = null != nullsFirst;
	    if (needsCase) {
	    	stat.appendSQL("(CASE WHEN ");
	    	query.appendSQL(stat, expression, false, null);
	    	
	    	if (Boolean.TRUE.equals(nullsFirst))
		        // NULL-first ascending
		        stat.appendSQL(" IS NULL THEN 0 ELSE 1 END)");
	    	else
	    		// NULL-last ascending
		        stat.appendSQL(" IS NULL THEN 1 ELSE 0 END)");
	    }

	    // Now the actual expression ordering
	    query.appendSQL(stat, expression, false, null);
	    stat.appendSQL(desc ? " DESC" : " ASC");
	}
}