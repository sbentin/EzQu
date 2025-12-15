/*
 * Copyright (c) 2007-2010 Centimia Ltd.
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 2.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group, Centimia Inc.
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

//	void appendSQL(SQLStatement stat) {
//		if (1 >= query.getDb().factory.dialect.ordinal()) {
//			// case when the dialect is H2, ORACLE
//			query.appendSQL(stat, expression, false, null);			
//			if (desc) {
//				stat.appendSQL(" DESC");
//				// default null ordering in descending is null last so we only need to check null first
//				if (nullsFirst) {
//					stat.appendSQL(" NULLS FIRST");
//				}
//			}
//			else {
//				stat.appendSQL(" ASC");
//				// default null ordering in ascending is null first so we only need to check null last
//				if (nullsLast) {
//					stat.appendSQL(" NULLS LAST");
//				}
//			}
//		}
//		else {
//			// the others will use something a bit more complex. (CASE WHEN ate.user_id IS NULL THEN 0 ELSE 1 END)
//			if (desc) {
//				if (nullsFirst)
//					stat.appendSQL("(CASE WHEN " + expression + " IS NULL THEN 0 ELSE 1 END), ");
//
//				query.appendSQL(stat, expression, false, null);
//				stat.appendSQL(" DESC");
//			}
//			else {
//				if (nullsLast)
//					stat.appendSQL("(CASE WHEN " + expression + " IS NULL THEN 0 ELSE 1 END) DESC, ");
//
//				query.appendSQL(stat, expression, false, null);
//				stat.appendSQL(" ASC");
//			}
//		}
//	}
	
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