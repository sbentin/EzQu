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
 * 22/02/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.centimia.orm.ezqu.Types;

/**
 * Use this annotation if column name in underlying storage is different the name of the field.
 * @author shai
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {
	/** the name of the field. If annotation is used, this must have a value */
	String name() default "";
	
	/** Denotes the maximum length of the field. Optional */
	int length() default -1;

	/** Denotes the precision of the field. Optional */
	int precision() default -1;
	
	/** If true then the field will be created as unique in the underlying DB. Has no meaning if the Table is not created by ezqu */
	boolean unique() default false;

	/** If true then this field will be created with not null. */
	boolean notNull() default false;
	
	/** 
	 * When the field is an enum use this annotation if you want the underlying persistence to work with the ordinal value.
	 * To do so set the value to Types.ENUM_INT.<br>
	 * <b>Note</b> that it is more performent to work with the default setting of
	 * Types.ENUM which saves the value as a String, so only use this when it is imperative.
	 */
	Types enumType() default Types.ENUM;
	
	/** 
	 * Sometimes the column in the db and the field don't match. Could be because of existing table being used.
	 * You can use this annotation to force the use of some sql types, ant thus tell
	 * ezqu what is needed for creating, or reading, the column. (e.g. if you want a string to be mapped to
	 * a CLOB (or TEXT equivalent) you can put "java.sql.Clob")
	 */
	Class<?> type() default Object.class;
}
