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
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.Date;

import com.centimia.core.ExceptionMessages;
import com.centimia.core.exception.ResourceDeadLockException;
import com.centimia.orm.ezqu.Db;
import com.centimia.orm.ezqu.EzquError;
import com.centimia.orm.ezqu.SQLDialect;
import com.centimia.orm.ezqu.Types;
import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;

/**
 * 
 * @author Shai Bentin
 *
 */
public class OracleDialect implements SQLDialect {

	@Override
	public String getDataType(Class<?> fieldClass) {
		final String VARCHAR2 = "VARCHAR2";
		final String TIMESTAMP_6 = "TIMESTAMP(6)";
		
		if (fieldClass == Integer.class) {
			return "NUMBER(10)";
		}
		else if (fieldClass == String.class) {
			return VARCHAR2;
		}
		else if (fieldClass == Double.class) {
			return "NUMBER(33,5)";
		}
		else if (fieldClass == java.math.BigDecimal.class) {
			return "NUMBER(33,5)";
		}
		else if (fieldClass == java.math.BigInteger.class) {
			return "NUMBER(38,0)";
		}
		else if (fieldClass == java.util.Date.class) {
			return TIMESTAMP_6;
		}
		else if (fieldClass == java.sql.Date.class) {
			return DATE;
		}
		else if (fieldClass == java.time.LocalDate.class) {
			return DATE;
		}
		else if (fieldClass == java.time.LocalDateTime.class) {
			return TIMESTAMP_6;
		}
		else if (fieldClass == java.time.ZonedDateTime.class) {
			return TIMESTAMP_6;
		}
		else if (fieldClass == java.time.LocalTime.class) {
			return DATE;
		}
		else if (fieldClass == java.sql.Time.class) {
			return DATE;
		}
		else if (fieldClass == java.sql.Timestamp.class) {
			return "TIMESTAMP(9)";
		}
		else if (fieldClass == Byte.class) {
			return "NUMBER(3)";
		}
		else if (fieldClass == Long.class) {
			return "NUMBER(19)";
		}
		else if (fieldClass == Short.class) {
			return "NUMBER(5)";
		}
		else if (fieldClass == Boolean.class) {
			return "NUMBER(1)";
		}
		else if (fieldClass == Float.class) {
			return "NUMBER(38,7)";
		}
		else if (fieldClass == java.sql.Blob.class) {
			return BLOB;
		}
		else if (fieldClass == java.sql.Clob.class) {
			return "CLOB";
		}
		else if (fieldClass == java.util.UUID.class || fieldClass == java.net.InetAddress.class) {
			return "RAW(16)";
		}
		else if (fieldClass.isArray()) {
			// not recommended for real use. Arrays and relational DB don't go well together and don't make much sense!
			Class<?> componentClass = fieldClass.getComponentType();
			if (null != componentClass.getAnnotation(Entity.class) || null != componentClass.getAnnotation(MappedSuperclass.class))
				throw new EzquError("IllegalArgument - Array of type 'com.centimia.orm.ezqu.Entity' are relations. Either mark as transient or use a Collection type instead.");
			return BLOB;
		}
		else if (fieldClass.isEnum()) {
			return VARCHAR2;
		}
		else if (null != fieldClass.getInterfaces() && Arrays.stream(fieldClass.getInterfaces()).anyMatch(i -> i == Serializable.class)) {
			return BLOB;
		}
		return VARCHAR2;
	}

	@Override
	public String createTableString(String tableName) {
		return "CREATE TABLE " + tableName;
	}

	@Override
	public boolean checkTableExists(String tableName, Db db) {
		String query = "SELECT 1 FROM USER_TABLES WHERE TABLE_NAME = '" + tableName.toUpperCase() + "'";
		return db.executeQuery(query, ResultSet::next);
	}
	
	/**
	 * Oracle mapping is not straight forward so we map according to the type
	 * 
	 * @see com.centimia.orm.ezqu.SQLDialect#getValueByType(com.centimia.orm.ezqu.Types, java.sql.ResultSet, java.lang.String)
	 */
	@Override
	public Object getValueByType(Types type, ResultSet rs, String columnName) throws SQLException {
		return switch (type) {
			case INTEGER ->  {
	            int i = rs.getInt(columnName);
	            yield rs.wasNull() ? null : Integer.valueOf(i);
	        }
			case LONG -> {
				long l = rs.getLong(columnName);
				yield rs.wasNull() ? null : Long.valueOf(l);
			}
			case DOUBLE -> {
				double d = rs.getDouble(columnName);
				yield rs.wasNull() ? null : Double.valueOf(d);
			}
			case FLOAT -> {
				float f = rs.getFloat(columnName);
				yield rs.wasNull() ? null : Float.valueOf(f);
			}
			case SHORT -> {
				short s = rs.getShort(columnName);
				yield rs.wasNull() ? null : Short.valueOf(s);
			}
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
	        case CLOB -> rs.getClob(columnName);
	        case TIMESTAMP -> rs.getTimestamp(columnName);
    		case SQL_DATE -> rs.getDate(columnName);
    		case UTIL_DATE -> rs.getTimestamp(columnName);
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
	        case TIME -> rs.getTime(columnName);
	        case FK -> {
    			Object o = rs.getObject(columnName); 
    			if (null != o) {
    				if (o instanceof BigDecimal bd) {
	    				if (bd.scale() == 0) {
	    					if (bd.precision() <= 10)
	    						yield rs.getInt(columnName);
	    					else
	    						yield rs.getLong(columnName);
	    				}
	    				else if (bd.scale() == 14) {
	    					yield rs.getFloat(columnName);
	    				}
	    				else
	    					yield rs.getDouble(columnName);
    				}
    				else
    					yield o;
    			}
    			yield null;
    		}
	        default -> rs.getObject(columnName);
	    };
	}

	/**
	 * Oracle mapping is not straight forward so we map according to the type
	 * 
	 * @see com.centimia.orm.ezqu.SQLDialect#getValueByType(com.centimia.orm.ezqu.Types, java.sql.ResultSet, java.lang.String)
	 */
	@Override
	public Object getValueByType(Types type, ResultSet rs, int columnNumber) throws SQLException {
		return switch (type) {
			case INTEGER ->  {
	            int i = rs.getInt(columnNumber);
	            yield rs.wasNull() ? null : Integer.valueOf(i);
	        }
			case LONG -> {
				long l = rs.getLong(columnNumber);
				yield rs.wasNull() ? null : Long.valueOf(l);
			}
			case DOUBLE -> {
				double d = rs.getDouble(columnNumber);
				yield rs.wasNull() ? null : Double.valueOf(d);
			}
			case FLOAT -> {
				float f = rs.getFloat(columnNumber);
				yield rs.wasNull() ? null : Float.valueOf(f);
			}
			case SHORT -> {
				short s = rs.getShort(columnNumber);
				yield rs.wasNull() ? null : Short.valueOf(s);
			}
			case BOOLEAN -> rs.getBoolean(columnNumber);
	        case STRING -> rs.getString(columnNumber);
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
	        case CLOB -> rs.getClob(columnNumber);
	        case TIMESTAMP -> rs.getTimestamp(columnNumber);
			case SQL_DATE -> rs.getDate(columnNumber);
			case UTIL_DATE -> rs.getTimestamp(columnNumber);
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
	        case TIME -> rs.getTime(columnNumber);
	        case FK -> {
				Object o = rs.getObject(columnNumber); 
				if (null != o) {
					if (o instanceof BigDecimal bd) {
	    				if (bd.scale() == 0) {
	    					if (bd.precision() <= 10)
	    						yield rs.getInt(columnNumber);
	    					else
	    						yield rs.getLong(columnNumber);
	    				}
	    				else if (bd.scale() == 14) {
	    					yield rs.getFloat(columnNumber);
	    				}
	    				else
	    					yield rs.getDouble(columnNumber);
					}
					else
						yield o;
				}
				yield null;
			}
	        default -> rs.getObject(columnNumber);
	    };
	}
	
	@Override
	public String getIdentityType() {
		return "NUMBER(19) GENERATED ALWAYS AS IDENTITY";
	}

	@Override
	public String getSequnceQuery(String seqName) {
		return "SELECT " + seqName + ".nextval from dual";
	}
	
	@Override
	public boolean checkDiscriminatorExists(String tableName, String discriminatorName, Db db) {
		String query = "Select 1 from user_tab_columns c where c.table_name = '" + tableName + "' and c.column_name = '" + discriminatorName + "'";
		return db.executeQuery(query, ResultSet::next);
	}

	@Override
	public String getFunction(Functions functionName) {
		return switch (functionName){
			case IFNULL -> "NVL";
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
			query.append("CREATE UNIQUE INDEX ");
		else
			query.append("CREATE INDEX ");
		query.append(tableName).append(".").append(name).append(" ON (");
		for (int i = 0; i < columns.length; i++){
			if (i > 0){
				query.append(",");
			}
			query.append(columns[i]).append(" ASC");
		}
		query.append(")");
		return query.toString();
	}
	
	@Override
	public String getQueryStyleDate(Date date) {
		if (null == date)
			return "null";
		else {
			if (java.sql.Date.class.isAssignableFrom(date.getClass()))
				return "toDate('yyyy-MM-dd','" + new SimpleDateFormat("yyyy-MM-dd").format(date) + "')";
			else
				return "toDate(yyyy-MM-dd HH:mm:ss','" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date) + "')";
		}
	}
	
	@Override
	public String getQueryStyleDate(TemporalAccessor temporal) {
		if (null == temporal)
			return "null";
		else {
			if (LocalDate.class.isAssignableFrom(temporal.getClass()))
				return "toDate('yyyy-MM-dd','" + DateTimeFormatter.ISO_LOCAL_DATE.format(temporal) + "')";
			if (LocalDateTime.class.isAssignableFrom(temporal.getClass()))
				return "toDate('yyyy-MM-dd HH:mm:ss','" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(temporal) + "')";
		}
		return "null";
	}

	@Override
	public void handleDeadlockException(SQLException e) {
		if (e.getErrorCode() == 60 || e.getErrorCode() == 6100) {
			// Oracle deadlock
			throw new ResourceDeadLockException(ExceptionMessages.DEADLOCK, e);
		}
		throw new EzquError(e, e.getMessage());
	}
}
