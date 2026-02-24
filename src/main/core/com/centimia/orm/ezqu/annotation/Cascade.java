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
package com.centimia.orm.ezqu.annotation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use this annotation on One2One relations when you want cascade delete behavior when deleting the Entity 
 * holding the annotation.
 * 
 * @author shai
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface Cascade {

}
