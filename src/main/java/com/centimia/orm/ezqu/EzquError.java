/*
 * Copyright (c) 2007-2010 Centimia Ltd.
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 2.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group, Centimia Inc.
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
 *
 * @author shai
 */
public class EzquError extends RuntimeException {
	private static final long serialVersionUID = 2818663786498065024L;

	EzquError() {
		super();
	}

	/**
	 * @param message
	 * @param cause
	 */
	public EzquError(Throwable cause, String message, Object ... args) {
		super(null != message ? String.format(message, args): "No reason given!", cause);
	}

	/**
	 * @param message
	 */
	public EzquError(String message, Object ... args) {
		super(null != message ? String.format(message, args): "No reason given!");
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
}
