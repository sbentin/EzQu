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
 ISSUE			DATE			AUTHOR
-------		   ------	       --------
Created		   May 6, 2012			shai

*/
package com.centimia.orm.ezqu.ext.gradle;

/**
 * @author shai
 *
 */
public class LocationExtension {

	public String outputDir = null;
	
	public LocationExtension() {
		
	}
	
	public LocationExtension(String dir) {
		this.outputDir = dir;
	}
}
