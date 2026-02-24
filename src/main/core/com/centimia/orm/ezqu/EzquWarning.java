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
 * Same as ezqu error but does not print the message to log
 */
public class EzquWarning extends RuntimeException {
	private static final long serialVersionUID = -4135260455221394087L;

	/**
	 * 
	 * @param message
	 * @param args
	 */
	public EzquWarning(String message, Object... args) {
		super(prepareMessage(message, args));
	}

	/**
	 * @param cause
	 * @param message
	 * @param args
	 */
	public EzquWarning(Throwable cause, String message, Object... args) {
		super(prepareMessage(message, args), cause);
	}

	/**
	 * 
	 * @param cause
	 */
	public EzquWarning(Throwable cause) {
		super(cause);
	}

	/**
	 * @return true if this error is a deadlock error
	 */
	public boolean isDeadLockError() {
		return null != this.getMessage() && this.getMessage().indexOf("Deadlock") != -1;
	}
	
	private static String prepareMessage(String message, Object[] args) {
		return null != message ? String.format(message, args) : "No reason given!";
	}
}
