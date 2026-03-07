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
 * All DB types supported by Ezqu. Corresponds with the Dialect classes
 *
 * @see SQLDialect
 * @author Shai Bentin
 */
public enum Types {
	NONE,
	INTEGER,
	LONG,
	FLOAT,
	DOUBLE,
	BOOLEAN,
	BIGDECIMAL,
	BIGINTEGER,
	STRING,
	UTIL_DATE,
	SQL_DATE,
	TIMESTAMP,
	BYTE,
	SHORT,
	BLOB,
	CLOB,
	ARRAY,
	TIME,
	COLLECTION,
	FK,
	ENUM,
	ENUM_INT,
	UUID,
	LOCALDATE,
	LOCALDATETIME,
	ZONEDDATETIME,
	LOCALTIME,
	INETADDRESS
}
