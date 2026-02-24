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
package com.centimia.orm.ezqu;

/**
 * An example option that skips all relationships and only works on then immediate fields.
 * @author shai
 */
public class BasicExampleOptions extends GeneralExampleOptions {

	/**
	 * Creates a {@code BasicExampleOptions} instance for the supplied example.
	 * 
	 * @param example the example object whose fields are inspected
	 * @param db the database session used for field definition lookup
	 */
	public BasicExampleOptions(Object example, Db db) {
		super(null);

		TableDefinition<?> tDef = EzquSessionFactory.define(example.getClass(), db);
		for (FieldDefinition fDef: tDef.getFields()) {
			if (!fDef.isSilent && !fDef.isExtension && fDef.fieldType != FieldType.NORMAL) {
				// isSilent, isExtension -> such field can not be selected upon.
				addExcludeProp(fDef.field.getName());
			}
		}
	}
}