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
import java.util.UUID;

import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;
import com.centimia.orm.ezqu.util.StatementBuilder;

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

	@Override
	@SuppressWarnings({ "rawtypes" })
	public <T> void appendSQL(SQLStatement stat, Query<T> query) {
		query.appendSQL(stat, key, false, null);
		stat.appendSQL(" ");
        stat.appendSQL(compareType.getString());
        StatementBuilder buff = new StatementBuilder(" (");
        for (A item: y) {
        	if (null == item)
        		throw new EzquError("can not have a 'null' value in an %s statement value", compareType.name());
        	buff.appendExceptFirst(", ");
        	if ((item instanceof String) || (item instanceof UUID)) {
        		buff.append("'" + item.toString() + "'");
        	}
        	else if (item.getClass().isEnum()) {
        		switch (query.getSelectColumn(key).getFieldDefinition().type) {
            		case ENUM: buff.append("'" + item.toString() + "'"); break;
            		case ENUM_INT: buff.append(((Enum)item).ordinal()); break;
            		default: buff.append("'" + item.toString() + "'"); break;
            	}
        	}
        	else if (item instanceof Date d) {
        		query.getDb().factory.getDialect().getQueryStyleDate(d);
        	}
        	else if (TemporalAccessor.class.isAssignableFrom(item.getClass()))
        		query.getDb().factory.getDialect().getQueryStyleDate((TemporalAccessor)item);
        	else if (null != item.getClass().getAnnotation(Entity.class) || null != item.getClass().getAnnotation(MappedSuperclass.class)) {
        		Object o = query.getDb().factory.getPrimaryKey(item);
        		if (String.class.isAssignableFrom(o.getClass()))
        			buff.append("'" + o.toString() + "'");
        		else
        			buff.append(o.toString());
        	}
        	else {
        		buff.append(item.toString());
        	}
        }
        buff.append(")");
        stat.appendSQL(buff.toString());
	}

}
