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
 * SessionOptions is a helper class you should use when executing in a session.
 * 
 * Here are the options:
 * <ul>
 * <li>SCOPED - regular scoped session with commit off</li>
 * <li>CURRENT - current session or exception</li>
 * <li>SCOPED, CURRENT - current session scoped with commit off</li>
 * <li>NEW - new local session</li>
 * <li>SCOPED, NEW - new local session scoped with commit off</li>
 * <li>SCOPED, COMMIT - regular scoped session with commit on</li>
 * <li>SCOPED, CURRENT, COMMIT - current scoped session  with commit on</li>
 * <li>SCOPED, NEW, COMMIT - new local scoped session with commit on</li>
 * </ul>
 * 
 * To use in EzquSessionFactory:
 * <pre>
 * sessionFactory.runInSession(db -> {...}, SessionOptions.SCOPED, SessionOptions.CURRENT, SessionOptions.COMMIT); // or any of the combinations above
 * T result = sessionFactory.getFromSession(db -> {...}, SessionOptions.SCOPED, SessionOptions.NEW); // or any of the combinations above
 * </pre>
 * Using any combination not from the above combinations will cause an exception
 */
public class SessionOptions {

	public static final byte SCOPED = 1;
	public static final byte CURRENT = 2;
	public static final byte NEW = 4;
	public static final byte COMMIT = 8;
	
	private SessionOptions() {}
}
