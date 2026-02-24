/*
 * Copyright (c) 2025-2030 Shai Bentin & Centimia Ltd..
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
 * @author shai
 */
public class OffsetToken implements Token {

	private final int limit;
	private final int offset;

	OffsetToken(int limitNum, int offsetNum) {
		this.limit = limitNum;
		this.offset = offsetNum;
	}

	@Override
	public void appendSQL(SQLStatement stat, Query<?> query) {		
		stat.appendSQL(query.getDb().factory.getDialect().theDialect.offset(limit, offset));
	}
}
