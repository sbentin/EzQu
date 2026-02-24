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
 * 11/02/2010		Shai Bentin			 create
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
 * 
 * @author Shai Bentin
 *
 */
public class PostgresDialect implements SQLDialect {
	@Override
	public boolean checkTableExists(String tableName, Db db) {
		String query = "SELECT 1 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + tableName + "'";
		return db.executeQuery(query, ResultSet::next);
	}

	@Override
	public String createTableString(String tableName) {
		return "CREATE TABLE " + tableName;
	}

	@Override
	public String getDataType(Class<?> fieldClass) {
		final String BINARY = "BINARY";
		if (fieldClass == Integer.class) {
			return "INTEGER";
		}
		else if (fieldClass == String.class) {
			return VARCHAR;
		}
		else if (fieldClass == Double.class) {
			return "FLOAT8";
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
			return "TINYINT";
		}
		else if (fieldClass == Long.class) {
			return "BIGINT";
		}
		else if (fieldClass == Short.class) {
			return "SMALLINT";
		}
		else if (fieldClass == Boolean.class) {
			return "BOOLEAN";
		}
		else if (fieldClass == Float.class) {
			return "REAL";
		}
		else if (fieldClass == java.sql.Blob.class) {
			return BINARY;
		}
		else if (fieldClass == java.sql.Clob.class) {
			return "TEXT";
		}
		else if (fieldClass.isArray()) {
			Class<?> componentClass = fieldClass.getComponentType();
			if (Byte.class.isAssignableFrom(componentClass)) {
				// byte array is mapped to BINARY type
				return BINARY;
			}
			else if (null != componentClass.getAnnotation(Entity.class) || null != componentClass.getAnnotation(MappedSuperclass.class)) {
				throw new EzquError("IllegalArgument - Array of type 'com.centimia.orm.ezqu.Entity' are relations. Either mark as transient or use a Collection type instead.");
			}
		}
		else if (fieldClass.isEnum()) {
			return VARCHAR;
		}
		else if (null != fieldClass.getInterfaces() && Arrays.stream(fieldClass.getInterfaces()).anyMatch(i -> i == Serializable.class)) {
			return BINARY;
		}
		return VARCHAR;
	}

	@Override
	public String getIdentityType() {
		return "GENERATED ALWAYS AS IDENTITY";
	}

	@Override
	public boolean checkDiscriminatorExists(String tableName, String discriminatorName, Db db) {
		String query = "select 1 from information_schema.columns where table_name = '" + tableName + "' and column_name = '" + discriminatorName + "'";
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
		if (name.isEmpty()) {
			name = columns[0] + "_" + (Math.random() * 10000) + 1;
		}
		if (unique)
			query.append("CREATE UNIQUE INDEX IF NOT EXISTS ");
		else
			query.append("CREATE INDEX IF NOT EXISTS ");
		query.append(name).append(" ON ").append(tableName).append("(");
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
	public StringBuilder wrapDeleteQuery(String tableName, String as) {
		return new StringBuilder("DELETE FROM ").append(tableName).append(" ").append(as).append(" ");
	}

	@Override
	public String getSequnceQuery(String seqName) {
		StringBuilder builder = new StringBuilder("SELECT nextval('").append(seqName).append("')");
		return builder.toString();
	}

	@Override
	public void handleDeadlockException(SQLException e) {
		if (e.getSQLState().equals("40P01")) {
			// possible deadlock
			throw new ResourceDeadLockException(ExceptionMessages.DEADLOCK, e);
		}
		throw new EzquError(e, e.getMessage());
	}

	@Override
	public String offset(int limit, int offset) {
		if (limit == -1)
			return "OFFSET " + offset;
		return "LIMIT " + limit + " OFFSET " + offset;
	}
}
