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

class RelationDefinition {
	/** The name of the relationship table. On O2M relationships might be null. */
	String relationTableName;
	/**
	 * The name of the column used for the relation. When O2M relationship and no relation table is used this value is the field holding
	 * the other side relation
	 */
	String relationColumnName;
	/** The name of the field used for holding the other side mapping. */
	String relationFieldName;
	/**
	 * When true, related children will be loaded with the object data, otherwise they will load when the getter function is used.
	 * 'false' is default
	 */
	boolean eagerLoad = false;
	/** The data type of the relationship object */
	Class<?>[] dataType;
	/** type of cascade relation with the related child. relevant to O2M relations only. Currently only CascadeType.DELETE is supported */
	CascadeType cascadeType = CascadeType.NONE;
	/** the name of a field in the other side of the relation that determines the order of the response */
	String orderByField = null;
	/** the name of a column in the other side of the relation that determines the order of the response */
	String orderByColumn = null;
	/** the direction of order by. Default is ASC" */
	String direction = "ASC";
}