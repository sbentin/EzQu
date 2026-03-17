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
 * 15/07/2011		shai				 create
 */
package com.centimia.orm.ezqu;

/**
 * The main exception thrown on ezqu events
 * 
 * @author shai
 */
public class EzquError extends RuntimeException {
	private static final long serialVersionUID = 2818663786498065024L;

	/**
	 * @param message
	 * @param cause
	 */
	public EzquError(Throwable cause, String message, Object ... args) {
		super(prepareMessage(message, args), cause);
	}

	/**
	 * @param message
	 */
	public EzquError(String message, Object ... args) {
		super(prepareMessage(message, args));
	}

	/**
	 * @param cause
	 */
	public EzquError(Throwable cause) {
		super(cause);
	}

	/**
	 * @return true if this error is a deadlock error
	 */
	public boolean isDeadLockError() {
		return null != this.getMessage() && this.getMessage().indexOf("Deadlock") != -1;
	}
	
	protected static String prepareMessage(String message, Object[] args) {
		if (null != message) {
			message = String.format(message, args);
			StatementLogger.error(message);
			return message;
		}
		return "No reason given!";
	}
}
