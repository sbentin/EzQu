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
package com.centimia.orm.ezqu.util;

/**
 * Implement this interface to create a custom conversion from field to Db
 * 
 * @param <I> - the type used in DB
 * @param <O> - the type used in object
 * @author shai
 */
public interface EzquConverter<I, O> {

	/**
	 * Converts value coming from db to value in object
	 * @param value
	 * @return O
	 */
	public O fromDb(I value);
	
	/**
	 * Converts value from object to the value in the Db
	 * @param value
	 * @return I
	 */
	public I toDb(O value);
}
