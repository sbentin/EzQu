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
 * 28/02/2010		shai				 create
 */
package com.centimia.orm.ezqu;

/**
 * The type of inheritance supported by Ezqu fro entities.<br>
 * <ol>
 * 	<li>TABLE_PER_CLASS - each child class has its own table with all the fields from the parent</li>
 * 	<li>DISCRIMINATOR - Single table for all children and parent, each discriminated using its own discriminator letter. </li>
 * </ol>
 *
 * @author shai
 */
public enum InheritedType {
	NONE,
	TABLE_PER_CLASS,
	DISCRIMINATOR
}