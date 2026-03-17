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
 * 26/02/2011		shai				 create
 */
package com.centimia.orm.ezqu.constant;

import java.util.regex.Pattern;

/**
 * General constants used
 * @author shai
 */
public class Constants {
	private Constants() {}
	
	public static final Pattern LENGTH_AND_PRECISION = Pattern.compile("\\(\\d{1,2},\\d{1,2}\\)");
	public static final int IO_BUFFER_SIZE = 4 * 1024;
	public static final String IS_LAZY = "isLazy";
	public static final String TO_GET_A_SUBSET = "To get a subset of the fields or a mix of the fields using mapping use the"
    		+ " 'select(Z)' or 'selectFirst(Z)' or 'selectDistinct(Z)' methods";
}