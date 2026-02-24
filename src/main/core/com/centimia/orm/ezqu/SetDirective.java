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
 * Update Log
 *
 *  Date			User				Comment
 * ------			-------				--------
 * 29/01/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu;

import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;

/**
 * Use for Update Set directive in an update query.
 *
 * @author Shai Bentin
 */
class SetDirective<A> implements Token {

	A x;
	A value;

	SetDirective(A x, A value) {
		this.x = x;
		this.value = value;
	}

	@Override
	@SuppressWarnings({ "rawtypes" })
	public void appendSQL(SQLStatement stat, Query<?> query) {
		query.appendSQL(stat, x, false, null);
		stat.appendSQL(" = ");
		// for relationship support
		if (value != null && (null != value.getClass().getAnnotation(Entity.class) || null != value.getClass().getAnnotation(MappedSuperclass.class))) {
			query.getDb().merge(value);
			query.appendSQL(stat, query.getDb().factory.getPrimaryKey(value), false, null);
		}
		else if (value != null && (value.getClass().isEnum() || value.getClass().getSuperclass().isEnum())) {
			switch (query.getSelectColumn(x).getFieldDefinition().type) {
        		case ENUM: query.appendSQL(stat, value.toString(), false, null); break;
        		case ENUM_INT: query.appendSQL(stat, ((Enum)value).ordinal(), false, null); break;
           		case UUID: query.appendSQL(stat, value.toString(), false, null); break;
        		default: query.appendSQL(stat, value, false, null); break;
        	}
		}
		else
			query.appendSQL(stat, value, false, null);
	}
}
