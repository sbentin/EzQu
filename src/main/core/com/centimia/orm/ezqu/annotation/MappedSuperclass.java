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
 * 24/02/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.centimia.orm.ezqu.InheritedType;

/**
 * Use this annotation on a super class that should be mapped with it's children to a single table. Super classes without
 * this annotation will not be mapped to the DB.<p>
 * <b>If a super class is used in a relation, FK,O2M,M2M then:
 * <ol>
 * <li>The Inherited type must be of {@link InheritedType} DISCRIMINATOR. Otherwise ezQu will not know which table to join.</li>
 * <li>If the table name is different then the the super class type name then the super type must also take the table name using the Table annotation</li>
 * </ol>
 * 
 * @author Shai Bentin
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MappedSuperclass {}
