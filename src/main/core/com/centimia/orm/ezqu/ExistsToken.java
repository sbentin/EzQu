/*
 * Copyright (c) 2025-2030 Shai Bentin & Centimia Inc..
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * THIS SOFTWARE CONTAINS CONFIDENTIAL INFORMATION AND TRADE
 * SECRETS OF Shai Bentin USE, DISCLOSURE, OR
 * REPRODUCTION IS PROHIBITED WITHOUT THE PRIOR EXPRESS
 * WRITTEN PERMISSION OF Shai Bentin & CENTIMIA, INC.
 */
package com.centimia.orm.ezqu;

/**
 * Implementation of the EXISTS operator.
 */
public class ExistsToken implements Token {

	private final QueryWhere<?> subQuery;

	public ExistsToken(QueryWhere<?> subQuery) {
		this.subQuery = subQuery;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void appendSQL(SQLStatement stat, Query<?> query) {
		subQuery.query.aliasMap().putAll(query.aliasMap());
		stat.appendSQL("EXISTS (").appendSQL(subQuery.getSQL()).appendSQL(")");
	}
}
