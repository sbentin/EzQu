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
 ISSUE			DATE			AUTHOR
-------		   ------	       --------
Created		   Feb 12, 2014		shai

*/
package com.centimia.orm.ezqu;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;

import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.constant.StatementType;
import com.centimia.orm.ezqu.util.Utils;

/**
 * Utility to work with prepared statements (especially for batching) independently from regular ezQu work but using the same connection.
 * These utils work only on pojo's and ignore relationships. The update and insert statements use all fields, delete only primary keys.
 * Hense, the pojos must have primary keys for these functions to work.
 *
 * @author shai
 */
public class DbUtils {

	private final Db	db;
	private final HashMap<Class<?>, StatementWithCount> insertStatements = new HashMap<>();
	private final HashMap<Class<?>, StatementWithCount> updateStatements = new HashMap<>();
	private final HashMap<Class<?>, StatementWithCount> deleteStatements = new HashMap<>();

	DbUtils(Db db) {
		this.db = db;
	}

	/**
	 * Insert the array of pojo objects in batch mode.<p>
	 * <b>NOTE: using batch insert only works on pojos or {@link Entity} that has no relationship.<br>
	 * When relationships exist on the {@link Entity} it will insert but relationships are disregarded</b>
	 * <p>
	 * The method returns an array of update counts containing one element for each command in the batch.
	 * Here are possible return values for each element:
	 * <ul>
	 * <li>a number greater than zero -- indicates that the command was processed and changed occurred in the db</li>
	 * <li>zero -- indicates that the command was processed but no changes occurred in the db</li>
	 * <li>Statement.SUCCESS_NO_INFO -- indicates that the command was processed successfully but that the number of rows affected is unknown</li>
	 * <li>Statement.EXECUTE_FAILED -- indicates that the command failed to execute successfully</li>
	 * <li>-100 -- When a batch iteration fails on a technical issue this gives extra information on a specific row that actually 
	 * had the technical problem.</li>
	 * </ul>
	 * 
	 * @param batchSize the size of each batch
	 * @param tArray the objects to insert
	 * @return int[] array of update counts 
	 */
    @SuppressWarnings("unchecked")
	public <T> int[] insertBatch(final int batchSize, T ... tArray) {
    	if (null == tArray || 0 == tArray.length)
    		return new int[0];

    	Class<?> clazz = tArray[0].getClass();
		TableDefinition<?> definition = EzquSessionFactory.define(clazz, db);

		return definition.insertBatch(db, batchSize, tArray);
    }

    /**
     * returns a PreparedStatement backed up by the current Db session. It is the developers responsibility to populate, execute, and close this statement.
     * @param statement
     * @return {@link PreparedStatement}
     */
    public PreparedStatement getPreparedStatement(String statement, String ... idColumnNames) {
    	return db.prepare(statement, idColumnNames);
    }

    /**
     * returns a PreparedStatement backed up by the current Db session. It is the developers responsibility to populate, execute, and close this statement.
     * @param statement
     * @return {@link PreparedStatement}
     */
    public CallableStatement getCallableStatement(String statement) {
    	return db.prepareCallable(statement);
    }
    
    /**
     * Returns a preparedStatement for the "pojo" given based on the statement type (UPDATE, INSERT, DELETE).
     * This method always returns the same {@link PreparedStatement} object when running in the same db session.
     *
     * @param clazz
     * @param type
     * @return {@link PreparedStatement}
     */
    public <T> PreparedStatement getPreparedStatement(Class<T> clazz, StatementType type) {
    	return getPreparedStatement(clazz, type, false);
    }

    /**
     * Returns a preparedStatement for the pojo given based on the statement type (UPDATE, INSERT, DELETE).
     * This method always returns the same {@link PreparedStatement} object when running in the same db session.
     *
     * @param clazz
     * @param type
     * @parm externalizePk - if true utils assumes the PK is injected by the user externally even if the PK is Identity type
     * @return {@link PreparedStatement} or null if the statement type is not supported or if the update statement is requested but all fields are relationships (hence there is nothing to update).
     */
    public <T> PreparedStatement getPreparedStatement(Class<T> clazz, StatementType type, boolean externalizePk) {
    	if (null == clazz || null == type)
    		return null;
    	switch (type) {
    		case INSERT: return getPreparedInsertStatement(clazz, externalizePk).ps;
    		case UPDATE: {
    			StatementWithCount swc = getPreparedUpdateStatement(clazz);
    			if (null != swc)
					return swc.ps;
				else
					return null;
    		}
    		case DELETE: return getPreparedDeleteStatement(clazz).ps;
    		default : return null;
    	}
    }

    /**
     * Prepares the data on the prepared Statement. If the preparedStatement does not exist it will be created.
     * The statement returned is ready for execution but had not been executed.
     *
     * @param obj
     * @return {@link PreparedStatement}
     * @throws EzquError
     */
    public <T> PreparedStatement prepareStatement(T obj, StatementType type) {
    	return prepareStatement(obj, type, false);
    }

    /**
     * Prepares the data on the prepared Statement. If the preparedStatement does not exist it will be created.
     * The statement returned is ready for execution but had not been executed.
     *
     * @param obj
     * @parm externalizePk - if true utils assumes the PK is injected by the user externally even if the PK is Identity type
     * @return {@link PreparedStatement}
     * @throws EzquError
     */
    public <T> PreparedStatement prepareStatement(T obj, StatementType type, boolean externalizePk) {
    	if (null == obj || null == type)
    		return null;
    	switch (type) {
    		case INSERT: return prepareInsertStatement(obj, externalizePk).ps;
    		case UPDATE: return prepareUpdateStatement(obj).ps;
    		case DELETE: return prepareDeleteStatement(obj).ps;
    		default : return null;
    	}
    }

    /**
     * Adds the object into the batch of executions. If the preparedStatement does not yet exist it will be created.
     * @param obj
     * @param type
     */
    public <T> void addBatch(T obj, StatementType type) {
    	addBatch(obj, type, false);
    }

    /**
     * Adds the object into the batch of executions. If the preparedStatement does not yet exist it will be created.
     * 
     * @param obj
     * @param type
     * @param externalizePk - if true utils assumes the PK is injected by the user externally even if the PK is Identity type
     */
    public <T> void addBatch(T obj, StatementType type, boolean externalizePk) {
    	if (null == obj || null == type)
    		return;
    	StatementWithCount swc;
    	switch (type) {
    		case INSERT: swc = prepareInsertStatement(obj, externalizePk); break;
    		case UPDATE: swc = prepareUpdateStatement(obj); break;
    		case DELETE: swc = prepareDeleteStatement(obj); break;
    		default : swc = null; break;
    	}

    	if (null != swc) {
    		try {
				swc.ps.addBatch();
			}
			catch (SQLException e) {
				throw new EzquError(e, e.getMessage());
			}
    	}
    }

    /**
     * Submits all batch of commands to the database for execution no matter the type. Does not return an array
     *
     * @param clazz
     * @return boolean success if any type update was executed.
     */
    public <T> boolean executeBatch(Class<T> clazz) {
    	boolean success = false;
    	for (StatementType type: StatementType.values()) {
    		int[] result = executeBatch(clazz, type);
    		if (null != result && result.length > 0 )
    			success = true;
    	}
    	return success;
    }

    /**
     * Submits a batch of commands to the database for execution and if all commands execute successfully, returns an array of update counts.
     * The int elements of the array that is returned are ordered to correspond to the commands in the batch,
     * which are ordered according to the order in which they were added to the batch.
     *
     * @param clazz
     * @param type
     * @return int[]
     */
    public <T> int[] executeBatch(Class<T> clazz, StatementType type) {
    	if (null == clazz)
    		return new int[0];
    	StatementWithCount ps;
    	switch (type) {
    		case INSERT: ps = insertStatements.get(clazz); break;
    		case UPDATE: ps = updateStatements.get(clazz); break;
    		case DELETE: ps = deleteStatements.get(clazz); break;
    		default: ps = null; break;
    	}

    	if (null == ps)
    		return new int[0];

    	try {
			return ps.ps.executeBatch();
		}
		catch (SQLException e) {
			if (e instanceof java.sql.BatchUpdateException bue) {
				// this means that the batch failed on a technical issue. 
				// We return an array with -100 for the batch that failed and all other batches 
				// that were not executed because of this failure.
				int[] updateCounts = bue.getUpdateCounts();
				int[] result = new int[ps.count];
				Arrays.fill(result, -100);
				
				for (int i = 0; i < updateCounts.length; i++) {
					if (updateCounts[i] != java.sql.Statement.EXECUTE_FAILED) {
						result[i] = updateCounts[i];
					}
				}
				return result;
			}
			return new int[0];
		}
    }

    /*
     * prepares the insertStatement for this object with data from the given object
     */
     @SuppressWarnings("unchecked")
 	private <T> StatementWithCount prepareInsertStatement(T obj, boolean externalizePk) {
     	Class<T> tClazz = (Class<T>) obj.getClass();
     	StatementWithCount swc = insertStatements.get(tClazz);
     	if (null == swc) {
     		swc = getPreparedInsertStatement(tClazz, externalizePk);
     	}
     	swc.count++;
 		TableDefinition<?> definition = EzquSessionFactory.define(tClazz, db);
 		int i = 1;
 		for (FieldDefinition field : definition.getFields()) {
 			if ((!externalizePk && field.isPrimaryKey && GeneratorType.IDENTITY == field.genType)
 					|| field.isSilent || field.isExtension || field.fieldType != FieldType.NORMAL)
 				// skip identity types because these are auto incremented
 				// skip silent fields because they don't really exist.
 				// skip everything which is not a plain field (i.e any type of relationship)
         		continue;

 			if (field.fieldType == FieldType.FK) {
 				setValue(swc.ps, i, getFkValue(obj, field));
 				i++;
 			}
 			else {
 	        	Object value = field.getValue(obj);
 	        	setValue(swc.ps, i, value);
 	        	i++;
 			}
 		}
     	return swc;
     }
     
     /*
      * prepares the updateStatement for this object with data from the given object
      */
     @SuppressWarnings("unchecked")
	private <T> StatementWithCount prepareUpdateStatement(T obj) {
		Class<T> tClazz = (Class<T>) obj.getClass();
		StatementWithCount swc = updateStatements.get(tClazz);
		if (null == swc) {
			swc = getPreparedUpdateStatement(tClazz);
		}
		swc.count++;
		TableDefinition<?> definition = EzquSessionFactory.define(tClazz, db);
		int i = 1;
		for (FieldDefinition field : definition.getFields()) {
			if (field.isExtension)
				continue;
			if (!field.isPrimaryKey) {
				if (field.fieldType == FieldType.FK) {
					setValue(swc.ps, i, getFkValue(obj, field));
					i++;
				}
				else if (!field.isSilent) {
					Object value = field.getValue(obj);
					setValue(swc.ps, i, value);
					i++;
				}
			}
		}

		for (FieldDefinition field : definition.getPrimaryKeyFields()) {
			Object value = field.getValue(obj);
			setValue(swc.ps, i, value);
			i++;
		}
		return swc;
     }
     
     /*
      * prepares the deleteStatement for this object with data from the given object
      */
     @SuppressWarnings("unchecked")
 	private <T> StatementWithCount prepareDeleteStatement(T obj) {
     	Class<T> tClazz = (Class<T>) obj.getClass();
     	StatementWithCount swc = deleteStatements.get(tClazz);
     	if (null == swc) {
     		swc = getPreparedDeleteStatement(tClazz);
     	}
     	swc.count++;
     	TableDefinition<?> definition = EzquSessionFactory.define(tClazz, db);
 		int i = 1;
 		for (FieldDefinition field : definition.getPrimaryKeyFields()) {
 			Object value = field.getValue(obj);
         	setValue(swc.ps, i, value);
         	i++;
 		}
     	return swc;
     }
     
    /**
     * Returns an insert preparedStatement for the pojo given. This method always returns the same {@link PreparedStatement} object when running in the same db session.
     * @param clazz
     * @return {@link PreparedStatement}
     */
    private <T> StatementWithCount getPreparedInsertStatement(Class<T> clazz, boolean externalizePk) {
    	StatementWithCount ps = insertStatements.get(clazz);
    	if (null == ps) {
    		TableDefinition<?> definition = EzquSessionFactory.define(clazz, db);
    		String[] idColumnNames = null != definition.getPrimaryKeyFields() ? definition.getPrimaryKeyFields().stream().map(fd -> fd.columnName).toArray(String[]::new) : new String[0];
     		PreparedStatement prs = db.prepare(getInsertStatement(definition, externalizePk).toString(), idColumnNames);
     		ps = new StatementWithCount(prs);
     		insertStatements.put(clazz, ps);
    	}
    	return ps;
    }

    /**
     * Returns an update preparedStatement which updates all the fields (accept the primaryKey) for the pojo given.
     * This method always returns the same {@link PreparedStatement} object when running in the same db session.
     *
     * @param clazz
     * @return {@link PreparedStatement}
     */
    private <T> StatementWithCount getPreparedUpdateStatement(Class<T> clazz) {
    	StatementWithCount ps = updateStatements.get(clazz);
    	if (null == ps) {
    		TableDefinition<?> definition = EzquSessionFactory.define(clazz, db);
    		String[] idColumnNames = null != definition.getPrimaryKeyFields() ? definition.getPrimaryKeyFields().stream().map(fd -> fd.columnName).toArray(String[]::new) : new String[0];
    		StringBuilder builder = getUpdateStatement(definition, clazz);
    		if (null != builder) {
    			PreparedStatement prs = db.prepare(builder.toString(), idColumnNames);
    			ps = new StatementWithCount(prs);
    			updateStatements.put(clazz, ps);
    		}
    		else
    			return null;
    	}
    	return ps;
    }

    /**
     * Returns an update preparedStatement which updates all the fields (accept the primaryKey) for the pojo given. This method always returns the same {@link PreparedStatement}
     * object when running in the same db session.
     *
     * @param clazz
     * @return {@link PreparedStatement}
     */
    private <T> StatementWithCount getPreparedDeleteStatement(Class<T> clazz) {
    	StatementWithCount ps = deleteStatements.get(clazz);
    	if (null == ps) {
    		TableDefinition<?> definition = EzquSessionFactory.define(clazz, db);
    		String[] idColumnNames = null != definition.getPrimaryKeyFields() ? definition.getPrimaryKeyFields().stream().map(fd -> fd.columnName).toArray(String[]::new) : new String[0];
   			PreparedStatement prs = db.prepare(getDeleteStatement(definition, clazz).toString(), idColumnNames);
   			ps = new StatementWithCount(prs);
    		deleteStatements.put(clazz, ps);
    	}
    	return ps;
    }

	/**
	 * Return the primary key of the Fk object value.
	 * @param obj
	 * @param field
	 */
	private <T> Object getFkValue(T obj, FieldDefinition field) {
		// try to get the value from the FK
		Object value = field.getValue(obj);
		TableDefinition<?> fKDefinition = db.define(value.getClass());
		List<FieldDefinition> pks = fKDefinition.getPrimaryKeyFields();
		if (pks.size() != 1) {
			return null;
		}
		else {
			return pks.get(0).getValue(value);
		}
	}

	private void setValue(PreparedStatement prep, int parameterIndex, Object x) {
        try {
        	if (x instanceof java.util.Date xDate)
        		x = new Timestamp(xDate.getTime());
			prep.setObject(parameterIndex, x);
        }
        catch (SQLException e) {
            throw new EzquError(e, e.getMessage());
        }
    }

	private StringBuilder getInsertStatement(TableDefinition<?> def, boolean externalizePk) {
    	StringBuilder buff = new StringBuilder("INSERT INTO ");
		StringJoiner fieldTypes = new StringJoiner(", ");
		StringJoiner  valueTypes = new StringJoiner(", ");
		buff.append(def.tableName).append('(');
		if (InheritedType.DISCRIMINATOR == def.inheritedType) {
			// the inheritance is based on a single table with a discriminator
			fieldTypes.add(def.discriminatorColumn);
			valueTypes.add("'" + def.discriminatorValue + "'");
		}
		for (FieldDefinition field : def.getFields()) {
			if ((!externalizePk && field.isPrimaryKey && GeneratorType.IDENTITY == field.genType)
				|| field.isSilent || field.isExtension || (field.fieldType != FieldType.FK && field.fieldType != FieldType.NORMAL))
				// skip identity types because these are auto incremented
				// skip silent fields because they don't really exist.
				// skip everything which is not a plain field (i.e any type of relationship)
        		continue;

        	fieldTypes.add(field.columnName);
        	valueTypes.add("?");
        }
		buff.append(fieldTypes.toString()).append(") VALUES(").append(valueTypes.toString()).append(')');
		if (db.factory.isShowSQL())
			StatementLogger.info(buff.toString());
		return buff;
    }

	private StringBuilder getUpdateStatement(TableDefinition<?> def, Class<?> clazz) {
		Object alias = Utils.newObject(clazz);
		Query<Object> query = Query.from(db, alias);
		String as = query.getSelectTable().getAs();

		boolean hasNoSilent = false;		
		StringJoiner innerUpdate = new StringJoiner(", ");
		for (FieldDefinition field : def.getFields()) {
			if (!field.isPrimaryKey && (field.fieldType == FieldType.FK || !field.isSilent || !field.isExtension)) {
				innerUpdate.add(as + "." + field.columnName + " = ?");
				hasNoSilent = true;
			}
		}		
		if (hasNoSilent) {
			StringBuilder buff = def.dialect.wrapUpdateQuery(innerUpdate, def.tableName, as);
			buff.append(" WHERE ");
			// if all fields were silent there would be noting to update here so we disregard the whole thing
			// and we don't do the update.
			StringJoiner sj = new StringJoiner(" AND ");
			for (FieldDefinition field : def.getPrimaryKeyFields()) {
				sj.add(field.columnName + " = ?");				
			}
			buff.append(sj.toString());
			if (db.factory.isShowSQL())
				StatementLogger.info(buff.toString());
			return buff;
		}
		else {
			return null;
		}
	}

	/*
	 * Creates the delete statement String
	 */
	private StringBuilder getDeleteStatement(TableDefinition<?> def, Class<?> clazz) {
		Object alias = Utils.newObject(clazz);
		Query<Object> query = Query.from(db, alias);
		String as = query.getSelectTable().getAs();

		StringBuilder buff = def.dialect.wrapDeleteQuery(def.tableName, as);
		buff.append(" WHERE ");
		boolean firstCondition = true;
		for (FieldDefinition field : def.getPrimaryKeyFields()) {
			if (!firstCondition) {
				buff.append(" AND ");
			}
			buff.append(field.columnName).append(" = ?");
			firstCondition = false;
		}
		if (db.factory.isShowSQL())
			StatementLogger.info(buff.toString());
		return buff;
	}

	/*
	 * Close all the statements and clean the maps.
	 */
	void clean() {
		for (StatementWithCount swc: insertStatements.values()) {
			try {
				swc.ps.close();
			}
			catch (SQLException e) {
				// nothing I can do
			}
		}
		for (StatementWithCount swc: updateStatements.values()) {
			try {
				swc.ps.close();
			}
			catch (SQLException e) {
				// nothing I can do
			}
		}
		for (StatementWithCount swc: deleteStatements.values()) {
			try {
				swc.ps.close();
			}
			catch (SQLException e) {
				// nothing I can do
			}
		}
		insertStatements.clear();
		updateStatements.clear();
		deleteStatements.clear();
	}
	
	private class StatementWithCount {
		PreparedStatement ps;
		int count;

		public StatementWithCount(PreparedStatement ps) {
			this.ps = ps;
		}
	}
}
