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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * When applying this annotation ezQu will not update the target Object only the ID of the object on ForeignKeys and nothing on O2M, M2M relations.
 * <p>
 * <b>Note:</b> This effects all Insert/Update operations. When you apply this to a field it is assumed that the other side Object/s already exist in the DB and have ID.
 * @author shai
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NoUpdateOnSave {

}
