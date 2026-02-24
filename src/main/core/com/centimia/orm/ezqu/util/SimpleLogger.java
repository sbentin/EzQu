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
 * @author shai
 *
 */
public class SimpleLogger implements Logger {
	private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("com.centimia.orm.ezqu.logger");

	@Override
	public boolean isDebugEnabled() {
		return logger.isLoggable(java.util.logging.Level.FINEST);
	}

	@Override
	public void debug(String statement) {
		logger.finest(statement);	
	}

	@Override
	public void info(String statement) {
		logger.info(statement);
	}

	@Override
	public void error(String statement) {
		logger.severe(statement);
	} 	
}
