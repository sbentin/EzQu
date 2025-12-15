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

/*
 * Update Log
 *
 *  Date			User				Comment
 * ------			-------				--------
 * 29/06/2010		shai				 create
 */
package com.centimia.orm.ezqu;

import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.StringJoiner;
import java.util.UUID;

import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;

/**
 *
 * @author shai
 */
class InCondition<A> implements Token {

	CompareType compareType;
	Object key;
    A[] y;

	InCondition(Object x, A[] y, CompareType compareType) {
		this.compareType = compareType;
		if (x instanceof GenericMask mask) {
        	this.key = mask.orig();
        }
        else {
        	this.key = x;
        }
		this.y = y;
	}

	public void appendSQL(SQLStatement stat, Query<?> query) {
	    // left side
	    query.appendSQL(stat, key, false, null);

	    // operator
	    stat.appendSQL(" ").appendSQL(compareType.getString());

	    // right side – a comma‑separated list
	    StringJoiner sj = new StringJoiner(", ", " (", ")");
	    for (A item : y) {
	    	if (null == item)
		        throw new EzquError("null value in %s statement", compareType.name());
	        sj.add(formatValue(query, item));
	    }
	    stat.appendSQL(sj.toString());
	}
	
	/**
	 * Formats a value according to the rules of this query language
	 * and appends it to {@code sb}.
	 *
	 * @param query the running query (needed for PK look‑ups and dialect)
	 * @param val   the value to format (must not be null)
	 */
	private String formatValue(Query<?> query, Object val) {
	    /* ---------- 1. Primitive / simple types ---------- */
	    if (val instanceof String || val instanceof UUID) {
	        return "'" + val.toString() + "'";
	    }
	    
	    /* ---------- 2. Dates / temporals ---------- */
	    if (val instanceof Date d) {
	        return query.getDb().factory.getDialect().getQueryStyleDate(d);
	    }
	    
	    if (val instanceof TemporalAccessor ta) {
	    	query.getDb().factory.getDialect().getQueryStyleDate(ta);
	    }
	    
	    Class<?> cl = val.getClass();
	    /* ---------- 3. Enums ---------- */
	    if (cl.isEnum()) {
	        FieldDefinition fd = query.getSelectColumn(key).getFieldDefinition();
	        return switch (fd.type) {
	            case ENUM_INT -> String.valueOf(((Enum<?>) val).ordinal());
	            default -> "'" + val.toString() + "'";
	        };
	    }
	    
	    /* ---------- 4. EzQu entities ---------- */
	    if (isEntityOrMappedSuper(cl)) {
	        Object pk = query.getDb().factory.getPrimaryKey(val);
	        if (pk instanceof String pks) {
	            return "'" + pks + "'";
	        }
	        else {
	            return pk.toString();
	        }
	    }
	    
	    /* ---------- 5. Fallback ---------- */
	    return val.toString();
	}

	/**
	 * Helper that decides whether a class is an entity or a mapped‑superclass.
	 */
	private static boolean isEntityOrMappedSuper(Class<?> cl) {
	    return cl.getAnnotation(Entity.class) != null ||
	           cl.getAnnotation(MappedSuperclass.class) != null;
	}
}
