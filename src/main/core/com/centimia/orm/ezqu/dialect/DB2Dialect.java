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
 * 09/02/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu.dialect;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;

import com.centimia.core.ExceptionMessages;
import com.centimia.core.exception.ResourceDeadLockException;
import com.centimia.orm.ezqu.Db;
import com.centimia.orm.ezqu.EzquError;
import com.centimia.orm.ezqu.SQLDialect;
import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;

/**
 * This dialect holds information to connect the Ezqu O/R Mapping to DB/2
 * @author Shai Bentin
 */
public class DB2Dialect implements SQLDialect {

	@Override
	public String createTableString(String tableName) {
		return "IF NOT EXISTS (SELECT NAME FROM SYSIBM.SYSTABLES WHERE NAME='" + tableName + "') CREATE TABLE " + tableName;
	}

	@Override
	public boolean checkTableExists(String tableName, Db db) {
		// if the line above does not do the job, use this method instead...
		return false;
	}
	
	@Override
	public String getDataType(Class<?> fieldClass) {
		final String SMALLINT = "SMALLINT";
		
		if (fieldClass == Integer.class) {
			return "INTEGER";
		}
		else if (fieldClass == String.class) {
			return VARCHAR;
		}
		else if (fieldClass == Double.class) {
			return "DOUBLE";
		}
		else if (fieldClass == java.math.BigDecimal.class) {
			return "DECIMAL(38,15)";
		}
		else if (fieldClass == java.math.BigInteger.class) {
			return "DECIMAL(65,0)";
		}
		else if (fieldClass == java.util.Date.class) {
			return TIMESTAMP;
		}
		else if (fieldClass == java.sql.Date.class) {
			return DATE;
		}
		else if (fieldClass == java.time.LocalDate.class) {
			return DATE;
		}
		else if (fieldClass == java.time.LocalDateTime.class) {
			return TIMESTAMP;
		}
		else if (fieldClass == java.time.ZonedDateTime.class) {
			return TIMESTAMP;
		}
		else if (fieldClass == java.time.LocalTime.class) {
			return TIME;
		}
		else if (fieldClass == java.sql.Time.class) {
			return TIME;
		}
		else if (fieldClass == java.sql.Timestamp.class) {
			return TIMESTAMP;
		}
		else if (fieldClass == Byte.class) {
			return SMALLINT;
		}
		else if (fieldClass == Long.class) {
			return "BIGINT";
		}
		else if (fieldClass == Short.class) {
			return SMALLINT;
		}
		else if (fieldClass == Boolean.class) {
			return SMALLINT;
		}
		else if (fieldClass == Float.class) {
			return "REAL";
		}
		else if (fieldClass == java.sql.Blob.class) {
			return BLOB;
		}
		else if (fieldClass == java.sql.Clob.class) {
			return "CLOB";
		}
		else if (fieldClass.isArray()) {
			// not recommended for real use. Arrays and relational DB don't go well together and don't make much sense!
			Class<?> componentClass = fieldClass.getComponentType();
			if (null != componentClass.getAnnotation(Entity.class) || null != componentClass.getAnnotation(MappedSuperclass.class))
				throw new EzquError("IllegalArgument - Array of type 'com.centimia.orm.ezqu.Entity' are relations. Either mark as transient or use a Collection type instead.");
			return BLOB;
		}
		else if (fieldClass.isEnum()) {
			return VARCHAR;
		}
		else if (null != fieldClass.getInterfaces() && Arrays.stream(fieldClass.getInterfaces()).anyMatch(i -> i == Serializable.class)) {
			return BLOB;
		}
		return VARCHAR;
	}

	@Override
	public String getIdentityType() {
		return "BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1, NO CACHE)";
	}

	@Override
	public boolean checkDiscriminatorExists(String tableName, String discriminatorName, Db db) {
		String query = "SELECT 1 FROM syscat.columns WHERE tabname = '" + tableName + "' and colname = '" + discriminatorName + "'" ;
		return db.executeQuery(query, ResultSet::next);
	}
	
	@Override
	public String getFunction(Functions functionName) {
		return switch (functionName) {
			case IFNULL -> "COALESCE";
			default -> "";
		};
	}

	@Override
	public String createIndexStatement(String name, String tableName, boolean unique, String[] columns) {
		StringBuilder query = new StringBuilder();
		if (name.isEmpty()){
			name = columns[0] + "_" + (Math.random() * 10000) + 1;
		}
		if (unique)
			query.append("CREATE UNIQUE INDEX ");
		else
			query.append("CREATE INDEX ");
		query.append(name).append(" ON ").append(tableName).append(" (");
		for (int i = 0; i < columns.length; i++){
			if (i > 0){
				query.append(",");
			}
			query.append(columns[i]);
		}
		query.append(")");
		return query.toString();
	}

	@Override
	public void handleDeadlockException(SQLException e) {
		String sqlState = e.getSQLState();
		int errorCode = e.getErrorCode();
		if (("40001".equals(sqlState) || "57033".equals(sqlState)) && (errorCode == -911 || errorCode == -913)) {
			// possible deadlock
			throw new ResourceDeadLockException(ExceptionMessages.DEADLOCK, e);
		}
		throw new EzquError(e, e.getMessage());
	}
}
