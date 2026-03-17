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
 * Implementation of the "having" sql syntax
 * 
 * @author shai
 */
public class HavingToken implements Token {

	@Override
	public void appendSQL(SQLStatement stat, Query<?> query) {
		stat.appendSQL("having");
	}

}