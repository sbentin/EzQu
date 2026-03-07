/*
 * Copyright (c) 2020-2024 Shai Bentin & Centimia Inc..
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
package com.centimia.orm.ezqu.annotation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.centimia.orm.ezqu.InheritedType;

/**
 * Used to mark the inheritance strategy to be used for an object graph when persisting it to the underlying RDBMS.<br>
 * Set the annotation on the root class of the object graph.<br>
 * 
 * If the strategy is TABLE_PER_CLASS, all classes in the object graph must be annotated with {@link Inherited}.<br>
 * If the strategy is DISCRIMINATOR, all child classes in the object graph must be annotated with {@link Discriminator}.<br>
 * 
 * <p>
 * Using this annotation you supply more information as to the inheritance strategy your going to deploy in the underlying relational database.
 * </p>
 * 
 * <ul>
 * <li>
 * <b>Table Per Class</b><br>
 * This is the default inheritance and the easiest to implement. It means that each entity has its own table with all the fields, including the inherited ones, in it.
 * It allows for AUTO incrementors like IDENTITY and SEQUENCE.
 * </li>
 * <br>
 * <li><b>Table with Discriminator</b><br>
 * This strategy, though not the only possible strategy, is also a performent strategy in a relational DB as it requires no joins or unions, and it allows for
 * AUTO incrementors like IDENTITY and SEQUENCE. When using this stratgety you should also supply the DiscriminatorColumn and DiscriminatorValue() value.
 * </li>
 * <p>
 * @author shai
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface Inheritance {
	/** 
	 * Determines how the inheritance is mapped to the persistence store. 
	 * In both cases there is a single table. One with a 'Discriminator', the other assumes only a 
	 * single object is mapped per table (TABLE_PER_CLASS)
	 */
	InheritedType inheritedType() default InheritedType.TABLE_PER_CLASS;
	
	/** 
	 * the column in the DB holding the discriminator. 
	 * The default is 'class'. If this column does not exist it will be created 
	 */
	String discriminatorColumn() default "class";
	
	/**
	 * the name of the table representing the whole object graph when using the discriminator strategy.
	 * If not supplied the name of the table will be the same as the table of the root class in the object graph.
	 * For TABLE_PER_CLASS strategy this parameter is ignored.
	 */
	String discriminatorTableName() default "";
}
