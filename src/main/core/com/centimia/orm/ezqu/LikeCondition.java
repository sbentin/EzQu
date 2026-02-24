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

/*
 ISSUE			DATE			AUTHOR
-------		   ------	       --------
Created		   Nov 1, 2012			shai

*/
package com.centimia.orm.ezqu;

import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;

/**
 * Special to deal with cases where the user wants to explicitly say which kind of like he needs without stating it himself on the input.
 * @author shai
 */
class LikeCondition<A> implements Token {

	A y;
	Object key;
	LikeMode	mode;

	public LikeCondition(Object x, A y, LikeMode mode) {
		if (x instanceof GenericMask mask) {
        	this.key = mask.orig();
        }
        else {
        	this.key = x;
        }
		this.y = y;
		this.mode = mode;
	}

	@Override
	public void appendSQL(SQLStatement stat, Query<?> query) {
		// 1. Append left side
        appendProperValue(stat, query, key, false);
        
        // 2. Separator & comparison operator
		stat.appendSQL(" LIKE ");
		
		// 3. Append right side
		appendProperValue(stat, query, y, true);
	}
	
	private void appendProperValue(SQLStatement stat, Query<?> q, Object obj, boolean checkMode) {
        if (obj == null) {
            q.appendSQL(stat, obj, false, null);
            return;
        }

        Class<?> cl = obj.getClass();
        boolean isEntity = cl.getAnnotation(Entity.class) != null ||
                           cl.getAnnotation(MappedSuperclass.class) != null;

        if (isEntity) {
            Object pk = q.getDb().factory.getPrimaryKey(obj);
            q.appendSQL(stat, pk != null ? pk : obj, false, null);
            return;
        }

        if (checkMode) {
	        switch(mode) {
				case ANYWHERE -> {stat.appendSQL("'%"); stat.appendSQL((String)y); stat.appendSQL("%'");}
				case END -> {stat.appendSQL("'" + (String)y); stat.appendSQL("%'");}
				case START -> {stat.appendSQL("'%"); stat.appendSQL((String)y + "'");}
			}
        }
        else
        	q.appendSQL(stat, obj, false, null);
    }
}
