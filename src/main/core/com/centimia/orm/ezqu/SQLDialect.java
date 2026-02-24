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
package com.centimia.orm.ezqu;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Date;
import java.util.StringJoiner;

import com.centimia.core.ExceptionMessages;
import com.centimia.core.exception.ResourceDeadLockException;
import com.centimia.orm.ezqu.dialect.Functions;

/**
 * An interface implemented by all Dialects
 * @author Shai Bentin
 */
public interface SQLDialect {
	static final String DATE = "DATE";
	static final String TIME = "TIME";
	static final String VARCHAR = "VARCHAR";
	static final String TIMESTAMP = "TIMESTAMP";
	static final String BLOB = "BLOB";
	
	/**
	 * Returns the appropriate Data type in the DB jargon fitting the given field java type
	 * @param fieldClass
	 * @return String
	 */
	public abstract String getDataType(Class<?> fieldClass);

	/**
	 * Returns the appropriate string for creating a Table in the specific Dialect jargon
	 * @param tableName
	 * @return String
	 */
	public abstract String createTableString(String tableName);

	/**
	 * Retrieves the value from the result set according to the appropriate java data type.<br>
	 * mapping is very close between DB types and java types so we just return the object at hand!
	 * 
	 * @param type
	 * @param rs
	 * @param columnName
	 * @return Object
	 * @throws SQLException
	 */
	default Object getValueByType(Types type, ResultSet rs, String columnName) throws SQLException {
		return switch (type) {
	        case BOOLEAN -> rs.getBoolean(columnName);
	        case STRING -> rs.getString(columnName);
	        case BYTE ->  {
	            byte b = rs.getByte(columnName);
	            yield rs.wasNull() ? null : b;
	        }
	        case ENUM -> rs.getString(columnName);
	        case ENUM_INT ->  {
	            int i = rs.getInt(columnName);
	            yield rs.wasNull() ? null : Integer.valueOf(i);
	        }
	        case BIGDECIMAL -> rs.getBigDecimal(columnName);
	        case BIGINTEGER -> {
	        	BigDecimal bd = rs.getBigDecimal(columnName);
	        	yield rs.wasNull() ? null : bd.toBigInteger();
	        }
	        case LOCALDATE -> {
	            java.sql.Date d = rs.getDate(columnName);
	            yield d == null ? null : d.toLocalDate();
	        }
	        case LOCALDATETIME -> {
	            java.sql.Timestamp ts = rs.getTimestamp(columnName);
	            yield ts == null ? null : ts.toLocalDateTime();
	        }
	        case ZONEDDATETIME -> {
	            java.sql.Timestamp ts = rs.getTimestamp(columnName);
	            yield ts == null ? null : ts.toInstant().atZone(ZoneId.systemDefault());
	        }
	        case LOCALTIME -> {
	            java.sql.Time t = rs.getTime(columnName);
	            yield t == null ? null : t.toLocalTime();
	        }
	        default -> rs.getObject(columnName);
	    };
	}

	/**
	 * Retrieves the value from the result set according to the appropriate java data type.
	 * mapping is very close between DB types and java types so we just return the object at hand!
	 * 
	 * @param type
	 * @param rs
	 * @param columnNumber
	 * @return Object
	 * @throws SQLException
	 */
	default Object getValueByType(Types type, ResultSet rs, int columnNumber) throws SQLException {
		return switch (type) {
	        case BOOLEAN -> {
	            Boolean v = rs.getBoolean(columnNumber);
	            yield !rs.wasNull() && v;
	        }
	        case BYTE ->  {
	            byte b = rs.getByte(columnNumber);
	            yield rs.wasNull() ? null : b;
	        }
	        case ENUM -> rs.getString(columnNumber);
	        case ENUM_INT ->  {
	            int i = rs.getInt(columnNumber);
	            yield rs.wasNull() ? null : Integer.valueOf(i);
	        }
	        case BIGDECIMAL -> rs.getBigDecimal(columnNumber);
	        case BIGINTEGER -> {
	        	BigDecimal bd = rs.getBigDecimal(columnNumber);
	        	yield rs.wasNull() ? null : bd.toBigInteger();
	        }
	        case LOCALDATE -> {
	            java.sql.Date d = rs.getDate(columnNumber);
	            yield d == null ? null : d.toLocalDate();
	        }
	        case LOCALDATETIME -> {
	            java.sql.Timestamp ts = rs.getTimestamp(columnNumber);
	            yield ts == null ? null : ts.toLocalDateTime();
	        }
	        case ZONEDDATETIME -> {
	            java.sql.Timestamp ts = rs.getTimestamp(columnNumber);
	            yield ts == null ? null : ts.toInstant().atZone(ZoneId.systemDefault());
	        }
	        case LOCALTIME -> {
	            java.sql.Time t = rs.getTime(columnNumber);
	            yield t == null ? null : t.toLocalTime();
	        }
	        default -> rs.getObject(columnNumber);
	    };
	}

	/**
	 * Checks if the table already exists. Some databases don't have this feature incorporated in their SQL language.
	 *
	 * @param tableName
	 * @param db
	 * @return boolean
	 */
	public abstract boolean checkTableExists(String tableName, Db db);

	/**
	 * The type of field type used for Identity in the Dialect Jargon
	 * @return String
	 */
	public abstract String getIdentityType();

	/**
	 * Because Alter table is different in every dialect we used this method to add a discriminator column
	 * @param tableName
	 * @param discriminatorName
	 * @return String
	 */
	default String createDiscrimantorColumn(String tableName, String discriminatorName) {
		return "ALTER TABLE " + tableName + " ADD " + discriminatorName + " VARCHAR(2)";
	}

	/**
	 * Add a column to an existing table
	 * @param tableName
	 * @param columnName
	 * @param pkType
	 * @return String
	 */
	default String alterTableAddColumn(String tableName, String columnName, Class<?> pkType) {
		return "ALTER TABLE " + tableName + " ADD " + columnName + " " + getDataType(pkType);
	}
	
	/**
	 * Check if the discriminator column exists
	 *
	 * @param tableName
	 * @param discriminatorName
	 * @param db
	 * @return boolean
	 */
	public boolean checkDiscriminatorExists(String tableName, String discriminatorName, Db db);

	/**
	 * returns the function that is the right function syntax for this dialect
	 *
	 * @param functionName
	 * @return String
	 */
	public abstract String getFunction(Functions functionName);

	/**
	 * Creates and returns the proper Alter statement that will create the index given in this dialect.
	 *
	 * @param name
	 * @param unique
	 * @param columns
	 * @return String
	 */
	public abstract String createIndexStatement(String name, String tableName, boolean unique, String[] columns);

	/**
	 * wraps the correct form for writing the update statement for this dialect
	 * 
	 * @param innerUpdate
	 * @param tableName
	 * @param as
	 * @return StringBuilder
	 */
	default StringBuilder wrapUpdateQuery(StringJoiner innerUpdate, String tableName, String as) {
		return new StringBuilder("UPDATE ").append(tableName).append(" ").append(as).append(" SET ").append(innerUpdate.toString());
	}

	/**
	 * wraps the correct form for writing the delete statement for this dialect
	 * 
	 * @param tableName
	 * @param as
	 * @return StringBuilder
	 */
	default StringBuilder wrapDeleteQuery(String tableName, String as) {
		return new StringBuilder("DELETE FROM ").append(tableName).append(" ").append(as).append(" ");
	}

	/**
	 * Allows a dialect to handle a deadlock exception. Default behavior is to re-throw it as a EzQu Error
	 * @param e
	 */
	default void handleDeadlockException(SQLException e) {
		if ("40001".equals(e.getSQLState()) || e.getErrorCode() == 40001) {
			// possible deadlock
			throw new ResourceDeadLockException(ExceptionMessages.DEADLOCK, e);
		}
		throw new EzquError(e, e.getMessage());
	}

	/**
	 * returns a String representation of the date to be embedded in the dialect query
	 * @param temporal
	 * @return String
	 */
	default String getQueryStyleDate(TemporalAccessor temporal) {
		if (null == temporal)
			return "null";
		else {
			if (LocalDate.class.isAssignableFrom(temporal.getClass()))
				return "'" + DateTimeFormatter.ISO_LOCAL_DATE.format(temporal) + "'";
			if (LocalDateTime.class.isAssignableFrom(temporal.getClass()))
				return "'" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(temporal) + "'";
		}
		return "null";
	}

	/**
	 * returns a String representation of the date to be embedded in the dialect query
	 * @param temporal
	 * @return String
	 */
	default String getQueryStyleDate(Date date) {
		if (null == date)
			return "null";
		else {
			if (java.sql.Date.class.isAssignableFrom(date.getClass()))
				return "'" + new SimpleDateFormat("yyyy-MM-dd").format(date) + "'";
			else
				return "'" +  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date) + "'";
		}
	}

	/**
	 * get the appropriate query for selecting a value from a sequence
	 * 
	 * @param seqName
	 * @return String
	 */
	default String getSequnceQuery(String seqName) {
		return "SELECT NEXT VALUE FOR " + seqName;
	}
	
	/**
	 * get the appropriate limit query for this dialect
	 *
	 * @param limit
	 * @return String
	 */
	default String limitQuery(int limit) {
		return "FETCH FIRST " + limit + " ROWS ONLY";
	}
	
	/**
	 * get the appropriate limit query for this dialect
	 *
	 * @param limit
	 * @return String
	 */
	default String offset(int limit, int offset) {
		if (limit == -1)
			return "OFFSET " + offset + " ROWS";
		return "OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
	}
}
