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
Created		   Jul 8, 2013			shai

*/
package com.centimia.orm.ezqu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When you have a two sided relationship with an external join table use this annotation on the many side 
 * so ezqu will know to fetch the parent into the parent into the field.
 * 
 * @author shai
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Many2One {

	/** 
	 * Optional - the class type of the related object. Inferred by the generic type if exists.
	 */
	Class<?> childType() default java.lang.Object.class;
	
	/**
	 * The name of the field in the one side object holding the other side of the relationship.<br>
	 * If null 'this' tableName will be used!<p>
	 * <b>Note:</b> <ol><li>When using a relation table this is the name of the column representing the parent object in the relation table.</li>
	 * </ol></p> 
	 */
	String relationFieldName();
}
