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
 * @author shai
 */
class UnificationToken implements Token {

	enum UNIFICATION_MODE {UNION, INTERESCT}

	private final UNIFICATION_MODE mode;
	private final String queryString;

	public UnificationToken(String queryString, UNIFICATION_MODE mode) {
		this.queryString = queryString;
		this.mode = mode;
	}

	@Override
	public void appendSQL(SQLStatement stat, Query<?> query) {
		if (null != queryString && !queryString.isEmpty()) {
			stat.appendSQL(" ");
			stat.appendSQL(mode.name());
			stat.appendSQL(" ");
			stat.appendSQL(queryString);
		}
	}
}
