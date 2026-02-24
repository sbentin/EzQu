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
 * 22/02/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu;

/**
 * @author shai
 *
 */
class ConditionBetween<A> implements Token {

	Object key;
    A y;
    A  z;

	public ConditionBetween(Object x, A y, A z) {
		if (x instanceof GenericMask mask) {
			this.key = mask.orig();
		}
		else
			this.key = x;
		this.y = y;
		this.z = z;
	}

	@Override
	public void appendSQL(SQLStatement stat, Query<?> query) {
		query.appendSQL(stat, key, false, null);
		stat.appendSQL(" BETWEEN ");
		query.appendSQL(stat, y, false, null);
		stat.appendSQL(" AND ");
		query.appendSQL(stat, z, false, null);
	}
}
