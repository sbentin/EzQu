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
package com.centimia.orm.ezqu.util;

/**
 * The internal logger interface
 * S
 * @author shai
 */
public interface Logger {

	/**
	 * Output to a given log statements relevant as info
	 * @param statement
	 */
	void info(String statement);

	/**
	 * Output to a given log statements relevant as debug
	 * @param statement
	 */
	void debug(String statement);

	/**
	 * Output to a given log statements relevant as error
	 * @param statement
	 */
	void error(String statement);
	
	/**
	 * true if logging set to debug or lower
	 * @return boolean
	 */
	public boolean isDebugEnabled();
}
