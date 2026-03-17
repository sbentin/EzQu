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
 * Internal Logger implementation based on slf4j
 * 
 * @author shai
 */
public class Slf4jLogger implements Logger {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger("com.centimia.orm.ezqu.log");

	@Override
	public void debug(String statement) {
		logger.debug(statement);		
	}

	@Override
	public void info(String statement) {
		logger.info(statement);
	}

	@Override
	public boolean isDebugEnabled(){
		return logger.isDebugEnabled();
	}

	@Override
	public void error(String statement) {
		logger.error(statement);		
	}
}
