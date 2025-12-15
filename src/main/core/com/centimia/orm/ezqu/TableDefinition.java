/*
 * Copyright (c) 2007-2010 Centimia Ltd.
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 2.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group, Centimia Inc.
 */
package com.centimia.orm.ezqu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.sql.BatchUpdateException;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.regex.Pattern;

import com.centimia.orm.ezqu.annotation.Cascade;
import com.centimia.orm.ezqu.annotation.Column;
import com.centimia.orm.ezqu.annotation.Converter;
import com.centimia.orm.ezqu.annotation.Discriminator;
import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.Event;
import com.centimia.orm.ezqu.annotation.Extension;
import com.centimia.orm.ezqu.annotation.Generated;
import com.centimia.orm.ezqu.annotation.Immutable;
import com.centimia.orm.ezqu.annotation.Index;
import com.centimia.orm.ezqu.annotation.Indices;
import com.centimia.orm.ezqu.annotation.Inherited;
import com.centimia.orm.ezqu.annotation.Interceptor;
import com.centimia.orm.ezqu.annotation.Lazy;
import com.centimia.orm.ezqu.annotation.Many2Many;
import com.centimia.orm.ezqu.annotation.Many2One;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;
import com.centimia.orm.ezqu.annotation.NoUpdateOnSave;
import com.centimia.orm.ezqu.annotation.One2Many;
import com.centimia.orm.ezqu.annotation.PrimaryKey;
import com.centimia.orm.ezqu.annotation.Transient;
import com.centimia.orm.ezqu.annotation.Version;
import com.centimia.orm.ezqu.constant.Constants;
import com.centimia.orm.ezqu.util.ClassUtils;
import com.centimia.orm.ezqu.util.EzquConverter;
import com.centimia.orm.ezqu.util.StringUtils;
import com.centimia.orm.ezqu.util.Utils;

/**
 * A table definition contains the index definitions of a table, the field definitions, the table name, and other meta data.
 *
 * @param <T> the table type
 */
class TableDefinition<T> {
	private static final String TO_DB = "toDb";	
	private final Class<T> clazz;
	private HashMap<String, FieldDefinition> fieldMap = new HashMap<>();
	private List<FieldDefinition> fields = new ArrayList<>();
	private List<FieldDefinition> primaryKeyColumnNames;
	private List<FieldDefinition> oneToOneRelations;	
	private GeneratorType genType = GeneratorType.NONE;
	private String sequenceQuery = null;
	
	@SuppressWarnings("rawtypes")
	private CRUDInterceptor	intercepter;
	private Event[]	interceptorEvents;
	
	FieldDefinition version = null;	
	boolean isAggregateParent = false;
	InheritedType inheritedType = InheritedType.NONE;
	char discriminatorValue;
	String discriminatorColumn;	

	final Dialect dialect;
	final String tableName;
	
	TableDefinition(Class<T> clazz, Dialect dialect) {
		this.dialect = dialect;
		this.clazz = clazz;
		String nameOfTable = clazz.getSimpleName();
		
		// Handle table annotation if entity and Table annotations exist
		com.centimia.orm.ezqu.annotation.Table tableAnnotation = clazz.getAnnotation(com.centimia.orm.ezqu.annotation.Table.class);
		if (tableAnnotation != null && tableAnnotation.name() != null && !"".equals(tableAnnotation.name())) {
			nameOfTable = tableAnnotation.name();
		}
		if (clazz.getAnnotation(Entity.class) != null || (!Modifier.isAbstract(clazz.getModifiers()) && null != clazz.getAnnotation(MappedSuperclass.class))) {
			// we must have primary keys
			primaryKeyColumnNames = new ArrayList<>();
		}
		this.tableName = nameOfTable;
		Inherited inherited = clazz.getAnnotation(Inherited.class);
		if (inherited != null) {
			this.inheritedType = inherited.inheritedType();
			this.discriminatorValue = inherited.discriminatorValue();
			this.discriminatorColumn = inherited.discriminatorColumn();
		}
		else {
			Discriminator discriminator = clazz.getAnnotation(Discriminator.class);
			if (null != discriminator) {
				this.inheritedType = InheritedType.DISCRIMINATOR;
				this.discriminatorValue = discriminator.discriminatorValue();
				this.discriminatorColumn = discriminator.discriminatorColumn();
			}
		}
		com.centimia.orm.ezqu.annotation.Interceptor interceptorAnnot = getInterceptorAnnotation(clazz);
		if (null != interceptorAnnot) {
			this.interceptorEvents = interceptorAnnot.event();
			try {
				intercepter = interceptorAnnot.Class().getConstructor().newInstance();
			}
			catch (Exception e) {
				throw new EzquError("Expected an Interceptor class for Table/ Entity %s. Unable to invoke!!", nameOfTable);
			}
		}
	}

	List<FieldDefinition> getFields() {
		return fields;
	}

	void addManyToMany(FieldDefinition fieldDefinition, Many2Many many2Many, Db db) {
		Class<?>[] childType = many2Many.childType();
		if (Object.class.equals(childType[0])) {
			try {
				childType = new Class<?>[] {(Class<?>) ((ParameterizedType)fieldDefinition.field.getGenericType()).getActualTypeArguments()[0]};
			}
			catch (Exception e) {
				throw new EzquError(e, "Tried to figure out the type of the child relation but couldn't. Try setting the 'childType' " +
						"annotation parameter on @Many2Many annotation");
			}
		}

		String relationFieldName = many2Many.relationFieldName();
		if (relationFieldName == null || "".equals(relationFieldName))
			relationFieldName = tableName;

		String relationColumnName = many2Many.relationColumnName();
		if (null == relationColumnName || "".equals(relationColumnName)) {
			// this can only be where there is only one child type
			relationColumnName = childType[0].getSimpleName();
		}

		RelationDefinition def = new RelationDefinition();
		def.eagerLoad = false;
		def.relationTableName = many2Many.joinTableName();

		if (!"".equals(many2Many.orderBy())) {
			def.orderByField = many2Many.orderBy();
			try {
				Column columnAnnotation = ClassUtils.findField(childType[0], def.orderByField).getAnnotation(Column.class);
				def.orderByColumn = columnAnnotation.name();
			}
			catch (NoSuchFieldException | SecurityException e) {
				throw new EzquError("When using orderBy on a relation the field must exist on child and must have a 'Column' annotation");
			}
		}
		def.direction = many2Many.direction();

		if (fieldDefinition != null) {
			fieldDefinition.relationDefinition = def;
			fieldDefinition.fieldType = FieldType.M2M;
			def.dataType = childType;
			if (def.dataType == null || (null == def.dataType[0].getAnnotation(Entity.class) && null == def.dataType[0].getAnnotation(MappedSuperclass.class)))
				throw new EzquError("IllegalState - field %s "
						+ "was marked as a relationship, but does not point to a TABLE Type", fieldDefinition.columnName);
			def.relationColumnName = relationColumnName;
			def.relationFieldName = relationFieldName;
		}
		if (db.factory.createTable) {
			// Try to get the primary key of the relationship
			Class<?> childPkType = many2Many.childPkType();
			if (Object.class.equals(childPkType)) {
				try {
					childPkType = extractPrimaryKeyFromClass(childType[0]); // all child types must have the same type of primary key
				}
				catch (Exception e) {
					throw new EzquError(
							e,
							"You declared a join table, but EzQu was not able to find your child Primary Key. Try setting the 'childPkType' property on the @one2Many annotation");
				}
			}
			createRelationTable(childType[0], many2Many.joinTableName(), relationFieldName, childPkType, def.relationColumnName, db);
		}
	}

	void addOneToMany(FieldDefinition fieldDefinition, One2Many one2ManyAnnotation, Db db) {
		Class<?>[] childType = one2ManyAnnotation.childType();
		if (Object.class.equals(childType[0])) {
			try {
				childType = new Class<?>[] {(Class<?>) ((ParameterizedType)fieldDefinition.field.getGenericType()).getActualTypeArguments()[0]};
			}
			catch (Exception e) {
				throw new EzquError(e, "Tried to figure out the type of the child relation but couldn't. Try setting the 'childType' annotation parameter on @One2Many annotation");
			}
		}

		String relationFieldName = one2ManyAnnotation.relationFieldName();
		if (relationFieldName == null || "".equals(relationFieldName))
			relationFieldName = tableName;

		String relationColumnName = one2ManyAnnotation.relationColumnName();
		if (null == relationColumnName || "".equals(relationColumnName)) {
			relationColumnName = childType[0].getSimpleName();
		}

		RelationDefinition def = new RelationDefinition();
		def.eagerLoad = one2ManyAnnotation.eagerLoad();
		if (!"".equals(one2ManyAnnotation.joinTableName())) {
			def.relationTableName = one2ManyAnnotation.joinTableName();
		}
		if (!"".equals(one2ManyAnnotation.orderBy())) {
			def.orderByField = one2ManyAnnotation.orderBy();
			try {
				Column columnAnnotation = ClassUtils.findField(childType[0], def.orderByField).getAnnotation(Column.class);
				def.orderByColumn = columnAnnotation.name();
			}
			catch (NoSuchFieldException | SecurityException e) {
				throw new EzquError("When using orderBy on a relation the field must exist on child and must have a 'Column' annotation");
			}
		}
		def.direction = one2ManyAnnotation.direction();
		if (one2ManyAnnotation.cascadeType() != null)
			def.cascadeType = one2ManyAnnotation.cascadeType();

		if (fieldDefinition != null) {
			fieldDefinition.relationDefinition = def;
			fieldDefinition.fieldType = FieldType.O2M;
			def.dataType = childType;
			if (null == def.dataType[0].getAnnotation(Entity.class) && null == def.dataType[0].getAnnotation(MappedSuperclass.class))
				throw new EzquError("IllegalState - field %s was marked as a relationship, but does not point to an Entity Type", fieldDefinition.columnName);
			def.relationColumnName = relationColumnName;
			def.relationFieldName = relationFieldName;
		}

		if (db.factory.createTable && one2ManyAnnotation.joinTableName() != null && !"".equals(one2ManyAnnotation.joinTableName())) {
			// add relation table creation
			// Try to get the primary key of the relationship
			Class<?> childPkType = one2ManyAnnotation.childPkType();
			if (Object.class.equals(childPkType)) {
				try {
					childPkType = extractPrimaryKeyFromClass(childType[0]); // all child types must share the same kind of primary key
				}
				catch (Exception e) {
					throw new EzquError(e, "You declared a join table, but EzQu was not able to find your child Primary Key. Try setting the 'childPkType' property on the @one2Many annotation");
				}
			}
			createRelationTable(childType[0], one2ManyAnnotation.joinTableName(), relationFieldName, childPkType, def.relationColumnName, db);
		}
	}

	FieldDefinition getDefinitionForField(String fieldName) {
		return fieldMap.get(fieldName);
	}

	void mapFields(Db db) {
		Field[] classFields = getAllFields(clazz);
		for (Field f : classFields) {
			if (Modifier.isTransient(f.getModifiers()) || Modifier.isFinal(f.getModifiers()) || Modifier.isStatic(f.getModifiers()) || Constants.IS_LAZY.equals(f.getName()))
				// we ignore this specially generated field for marking lazy O2O relations
				continue;

			// don't persist ignored fields.
			if (null != f.getAnnotation(Transient.class))
				continue;

			Class<?> classType = f.getType();
			if (classType.isPrimitive()) {
				throw new EzquError("Ezqu does not allow primitive types. See documentation! Field %s was decalred %s", f.getName(), f.getType());
			}
			Converter converter = f.getAnnotation(Converter.class);
			if (null != converter) {
				Method[] methods = converter.value().getMethods();
				if (0 < methods.length) {
					for (Method m: methods) {
						if (TableDefinition.TO_DB.equals(m.getName())) {
							classType = m.getReturnType();
							break;
						}
					}
				}
			}
			f.setAccessible(true);
			FieldDefinition fieldDef = new FieldDefinition();
			if (null != f.getAnnotation(Extension.class))
				fieldDef.isExtension = true;
			fieldDef.field = f;
			fieldDef.columnName = f.getName();
			fields.add(fieldDef);
			fieldMap.put(f.getName(), fieldDef);
			if (null != fieldDef.field.getAnnotation(NoUpdateOnSave.class) ||
					null != fieldDef.field.getType().getAnnotation(Immutable.class))
				// if this field is marked as NoUpdateOnSave we mark it here
				fieldDef.noUpdateField = true;

			if (java.time.temporal.Temporal.class.isAssignableFrom(classType) || java.util.Date.class.isAssignableFrom(classType) || java.lang.Number.class.isAssignableFrom(classType)
					|| String.class.isAssignableFrom(classType) || Boolean.class.isAssignableFrom(classType)
					|| Blob.class.isAssignableFrom(classType) || Clob.class.isAssignableFrom(classType)
					|| UUID.class.isAssignableFrom(classType) || classType.isEnum()) {

				if (null != f.getAnnotation(Version.class)) {
					if (null == this.version) {
						if (Long.class != f.getType() && Integer.class != f.getType())
							throw new EzquError("Annotated Version field must be either Integer or Long");
						fieldDef.isVersion = true;
						this.version = fieldDef;
					}
					else
						throw new EzquError("Too many version fields defined in this class: %s - %s", tableName, clazz);
				}
				// handle column name on annotation
				Column columnAnnotation = f.getAnnotation(Column.class);
				fieldDef.type = getTypes(classType);
				if (String.class.isAssignableFrom(classType) || classType.isEnum()) {
					// strings have a default size
					fieldDef.maxLength = 256;
				}
				if (null != columnAnnotation) {
					if (!StringUtils.isNullOrEmpty(columnAnnotation.name()))
						fieldDef.columnName = columnAnnotation.name();
					int length = columnAnnotation.length();
					if (length != -1)
						fieldDef.maxLength = length;
					fieldDef.unique = columnAnnotation.unique();
					fieldDef.notNull = columnAnnotation.notNull();
				}

				if (!getEnumType(fieldDef, columnAnnotation)) {
					if (null != columnAnnotation && Object.class != columnAnnotation.type())
						fieldDef.dataType = getDataType(columnAnnotation.type());
					else
						fieldDef.dataType = getDataType(f.getType());
				}

				PrimaryKey pkAnnotation = f.getAnnotation(PrimaryKey.class);
				if (pkAnnotation != null) {
					if (null == primaryKeyColumnNames)
						primaryKeyColumnNames = new ArrayList<>();
					fieldDef.isPrimaryKey = true;
					primaryKeyColumnNames.add(fieldDef);
					this.genType = pkAnnotation.generatorType();
					if (this.genType != null){
						// check if allowed
						if (this.primaryKeyColumnNames.size() > 1){
							throw new EzquError("Too many primary keys with an auto increment field. Using an auto increment " +
									"field requires a single column primary key. For uniqueness consider unique indexs in your table");
						}
						if (genType == GeneratorType.IDENTITY)
							fieldDef.dataType = dialect.getIdentityType();
						else if (this.genType == GeneratorType.SEQUENCE) {
							if (pkAnnotation.seqName() == null)
								throw new EzquError("IllegalArgument - GeneratorType.SEQUENCE must supply a sequence name!!!");
							this.sequenceQuery = db.factory.getDialect().getSequenceQuery(pkAnnotation.seqName());
						}
					}
				}
				Generated genAnnotation = f.getAnnotation(Generated.class);
				if (null != genAnnotation) {
					fieldDef.genType = genAnnotation.generatorType();
					if (this.genType != null){
						if (genType == GeneratorType.IDENTITY)
							fieldDef.dataType = dialect.getIdentityType();
						else if (this.genType == GeneratorType.SEQUENCE) {
							if (null == genAnnotation.seqName())
								throw new EzquError("IllegalArgument - GeneratorType.SEQUENCE must supply a sequence name!!!");
							fieldDef.sequenceQuery = db.factory.getDialect().getSequenceQuery(genAnnotation.seqName());
						}
					}
				}
			}
			else if (Collection.class.isAssignableFrom(classType)) {
				// these are one to many or many to many relations
				fieldDef.dataType = null;
				fieldDef.type = Types.COLLECTION;
				// only entities have relations
				if (clazz.getAnnotation(Entity.class) != null || clazz.getAnnotation(MappedSuperclass.class) != null) {
					this.isAggregateParent = true;
					fieldDef.isSilent = true;
					// find the method...
					String methodName = Pattern.compile(f.getName().substring(0, 1)).matcher(f.getName()).replaceFirst(f.getName().substring(0, 1).toUpperCase());
					try {
						Method m = null;
						if (null == this.inheritedType || InheritedType.NONE == this.inheritedType)
							m = clazz.getDeclaredMethod("get" + methodName);
						else
							m = clazz.getMethod("get" + methodName);
						fieldDef.getter = m;
					}
					catch (NoSuchMethodException nsme) {
						throw new EzquError(nsme, "Relation fields must have a getter in the form of get %s in class %s", methodName, clazz.getName());
					}
					One2Many one2ManyAnnotation = f.getAnnotation(One2Many.class);
					if (one2ManyAnnotation != null) {
						addOneToMany(fieldDef, one2ManyAnnotation, db);
						continue;
					}
					Many2Many many2ManyAnnotation = f.getAnnotation(Many2Many.class);
					if (many2ManyAnnotation != null) {
						addManyToMany(fieldDef, many2ManyAnnotation, db);
					}
				}
			}
			else {
				// this is some kind of Object, we check if it is a table
				Entity entity = classType.getAnnotation(Entity.class);
				MappedSuperclass mapped = classType.getAnnotation(MappedSuperclass.class);
				if (null != entity || null != mapped) {
					// this class is a table
					Column columnAnnotation = f.getAnnotation(Column.class);
					if (columnAnnotation != null && !StringUtils.isNullOrEmpty(columnAnnotation.name())) {
						fieldDef.columnName = columnAnnotation.name();
					}
					fieldDef.type = Types.FK;
					Many2One many2one = f.getAnnotation(Many2One.class);
					if (null == many2one) {
						// its a foreign key
						fieldDef.fieldType = FieldType.FK;
						if (null == this.oneToOneRelations)
							this.oneToOneRelations = new ArrayList<>();
						this.oneToOneRelations.add(fieldDef);
					}
					else {
						// this is the other side of a O2M relationship which is handled by a join table
						fieldDef.fieldType = FieldType.M2O;
						fieldDef.isSilent = true;
						RelationDefinition def = new RelationDefinition();
						def.eagerLoad = true;
						Class<?> otherSide = f.getType();
						if (!Object.class.equals(many2one.childType()))
							otherSide = many2one.childType();

						One2Many otherSideAnnotation;
						try {
							otherSideAnnotation = ClassUtils.findField(otherSide, many2one.relationFieldName()).getAnnotation(One2Many.class);
						}
						catch (SecurityException | NoSuchFieldException e) {
							throw new EzquError("Field {%s} in class {%s} is anntoated with M2O and declares a parent field {%s} which does not exist!!!", f.getName(), clazz, many2one.relationFieldName());
						}
						def.relationTableName = otherSideAnnotation.joinTableName();
						def.dataType = new Class<?>[] {otherSide};

						def.relationColumnName = otherSideAnnotation.relationFieldName();
						if ("".equals(def.relationColumnName))
							def.relationColumnName = f.getName();
						def.relationFieldName = otherSideAnnotation.relationColumnName();
						if ("".equals(def.relationFieldName))
							def.relationFieldName = clazz.getSimpleName();

						fieldDef.relationDefinition = def;
					}
				}
				else {
					// this will be stored as a blob...
					fieldDef.dataType = dialect.getDataType(Blob.class);
					fieldDef.type = Types.BLOB;

					// handle column name on annotation
					Column columnAnnotation = f.getAnnotation(Column.class);
					if (null != columnAnnotation) {
						if (!StringUtils.isNullOrEmpty(columnAnnotation.name()))
							fieldDef.columnName = columnAnnotation.name();
						fieldDef.unique = columnAnnotation.unique();
						fieldDef.notNull = columnAnnotation.notNull();
						if (Object.class != columnAnnotation.type())
							fieldDef.dataType = getDataType(columnAnnotation.type());
					}
				}
			}
		}
		// make sure the list of fields is sorted according to field type. we want the list to return the normal simple fields first then the
		// FK fields and then O2M and M2M. This way we make sure we have the primary key of the object before we try checking for reentrant.
		Collections.sort(fields);
	}

	void mapOneToOneFields(Db db) {
		if (oneToOneRelations == null)
			return;
		for (FieldDefinition fdef : oneToOneRelations) {
			Class<?> classType = fdef.field.getType();
			TableDefinition<?> def;
			if (null != classType.getAnnotation(Entity.class))
				def = EzquSessionFactory.define(classType, db);
			else
				def = EzquSessionFactory.define(classType, db, false);
			if (def.primaryKeyColumnNames == null || def.primaryKeyColumnNames.isEmpty())
				// no primary keys defined we can't make a DB relation although we expected such a relation to exist.
				throw new EzquError("IllegalState - No primary key columns defined for table %s - no relationship possible", classType);
			else if (def.primaryKeyColumnNames.size() > 1)
				throw new EzquError("UnsupportedOperation - Entity relationship is not supported for complex primary keys. Found in %s", classType);
			else {
				// Single primary key. Here we find out the type and move on....
				for (FieldDefinition innerDef : def.fields) {
					if (innerDef.isPrimaryKey) {
						fdef.dataType = getDataType(innerDef.field.getType());
						fdef.maxLength = innerDef.maxLength;
						break;
					}
				}				
			}
		}
	}

	/**
	 * Insert a batch of entities or pojos without O2M, M2M entity relationships. 
	 * <br><b>O2O and M2O relationships are allowed but must be annotated with {@link NoUpdateOnSave}</b>
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
	 * @param db the database connection
	 * @param batchSize the size of each batch
	 * @param objs the objects to insert
	 * @return int[] array of update counts 
	 */
	int[] insertBatch(Db db, final int batchSize, Object ... objs) {
		// first we build an insert statement
		SQLStatement stat = new SQLStatement(db);
		StringBuilder buff = new StringBuilder("INSERT INTO ");
		StringJoiner fieldTypes = new StringJoiner(", ");
		StringJoiner valueTypes = new StringJoiner(", ");
		buff.append(tableName).append('(');
		if (InheritedType.DISCRIMINATOR == this.inheritedType) {
			// the inheritance is based on a single table with a discriminator
			fieldTypes.add(this.discriminatorColumn);
			valueTypes.add("'" + this.discriminatorValue + "'");
		}
		
		final EnumSet<FieldType> rejectedFieldTypes = EnumSet.of(FieldType.M2M, FieldType.O2M);		
		for (FieldDefinition field : fields) {
			if (field.isSilent || field.isExtension || rejectedFieldTypes.contains(field.fieldType))
        		// skip everything which is not a plain field (i.e any type of relationship)
        		continue;

        	fieldTypes.add(field.columnName);
        	valueTypes.add("?");
        }
		buff.append(fieldTypes.toString()).append(") VALUES(").append(valueTypes.toString()).append(')');
		stat.setSQL(buff.toString());
		List<Integer> results = new ArrayList<>();
		int count = 0;
		for (Object o: objs) {
			// add the parameters
			for (FieldDefinition field : fields) {
				if (field.isSilent || field.isExtension || rejectedFieldTypes.contains(field.fieldType))
	        		// skip everything which is not a plain field (i.e any type of relationship)
	        		continue;

				handleValue(db, o, stat, field, -1);
			}
			stat.prepareBatch();

			if (++count % batchSize == 0) {
				// we reduce by one because error counting starts from the beginning of batch
		        int[] counts = executeBatch(db, stat, batchSize, false);
		        results.addAll(Arrays.stream(counts).boxed().toList());
		    }
		}
		// here we have not completed a full batch so we don't need to reduce by one
		int[] counts = executeBatch(db, stat, ++count % batchSize, true);
		results.addAll(Arrays.stream(counts).boxed().toList());
		return results.stream().mapToInt(Integer::intValue).toArray();
	}

	void insert(Db db, Object obj, int depth) {
		if (db.reEntrantCache.checkReEntrent(obj))
			return;
		SQLStatement stat = new SQLStatement(db);
		StringBuilder buff = new StringBuilder("INSERT INTO ");
		StringJoiner fieldTypes = new StringJoiner(", ");
		StringJoiner valueTypes = new StringJoiner(", ");
		buff.append(tableName).append('(');
		if (InheritedType.DISCRIMINATOR == this.inheritedType) {
			// the inheritance is based on a single table with a discriminator
			fieldTypes.add(this.discriminatorColumn);
			valueTypes.add("'" + this.discriminatorValue + "'");
		}

		FieldDefinition identityField = null;
		for (FieldDefinition field : fields) {
			if (null == field.getValue(obj) && (GeneratorType.IDENTITY == field.genType || (field.isPrimaryKey && GeneratorType.IDENTITY == genType))) {
				// only one identity field can exist, usually it is the pk
				if (null != identityField) {
					String msg = String.format("Error you can not have two identity fields in a row [%s, %s]", identityField.field.getName(), field.field.getName());
					StatementLogger.error(msg);
					throw new EzquError(msg, obj.getClass());
				}
				identityField = field;
				// skip identity types because these are auto incremented
				continue;
			}
			if (field.isExtension || field.isSilent || (field.fieldType != FieldType.NORMAL))
        		// skip everything which is not a plain field (i.e any type of relationship)
        		// its value will be handled in the following update statement
        		continue;

        	if (field.isVersion) {
        		field.field.setAccessible(true);
        		try {
        			Class<?> fieldType = field.field.getType();
        			if (Long.class == fieldType)
        				field.field.set(obj, 0L);
        			else
        				field.field.set(obj, 0);
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
					// Nothing to do here
					StatementLogger.debug("problem in reflection setting field " + field.field.getName());
					throw new EzquError("unable to set a declared version field on insert.", e);
				}
        	}
        	fieldTypes.add(field.columnName);
        	valueTypes.add("?");
            handleValue(db, obj, stat, field, 0);
        }
		buff.append(fieldTypes.toString()).append(") VALUES(").append(valueTypes.toString()).append(')');
		stat.setSQL(buff.toString());
		if (db.factory.isShowSQL())
			StatementLogger.insert(stat.logSQL());

		if (null != identityField) {
			// we insert first basically to get the generated primary key on Identity fields
			// note that unlike Identity, Sequence is generated in 'handleValue'
			updateWithId(obj, stat, identityField);
		}
		else
			stat.executeUpdate();
		if (null != primaryKeyColumnNames && !primaryKeyColumnNames.isEmpty()) {
			// only an object with primary key fields can have relationships or silent fields so we do an update
			update(db, obj, depth);
		}
	}

	void merge(Db db, Object obj, int depth) {
		if (db.reEntrantCache.checkReEntrent(obj))
			return;
		if (primaryKeyColumnNames == null || primaryKeyColumnNames.isEmpty()) {
			throw new EzquError("IllegalState - No primary key columns defined for table %s - no merge possible", obj.getClass());
		}

		if (null == db.factory.getPrimaryKey(obj)) {
			// no primary key so we can only do insert
			db.insert(obj, depth);
			return;
		}
		// check if object exists in the DB
		SQLStatement stat = new SQLStatement(db);
		StringBuilder buff = new StringBuilder("SELECT * FROM ");
		buff.append(tableName).append(" WHERE ");

		StringJoiner sj = new StringJoiner(" AND ");
		for (FieldDefinition field : primaryKeyColumnNames) {
			sj.add(field.columnName + " = ?");
			handleValue(db, obj, stat, field, 0);
		}
		buff.append(sj.toString());
		
		stat.setSQL(buff.toString());
		if (db.factory.isShowSQL())
			StatementLogger.merge(stat.logSQL());
		stat.executeQuery(rs -> {
			if (rs.next()) {
				// such a row exists do an update
				db.update(obj, depth);
			}
			else
				db.insert(obj, depth);
			return null;
		});
	}

	void update(Db db, Object obj, int depth) {
		if (db.reEntrantCache.checkReEntrent(obj))
			return;
		if (null == primaryKeyColumnNames || primaryKeyColumnNames.isEmpty()) {
			throw new EzquError("IllegalState - No primary key columns defined for table %s - can't locate row", obj.getClass());
		}
		SQLStatement stat = new SQLStatement(db);
		Object alias = Utils.newObject(obj.getClass());
		Query<Object> query = Query.from(db, alias);
		String as = query.getSelectTable().getAs();

		StringJoiner innerUpdate = new StringJoiner(", ");
		boolean hasNoneSilent = false;
		for (FieldDefinition field : fields) {
			if (field.isExtension)
				continue;
			if (!field.isPrimaryKey) {
				if (null != field.field.getAnnotation(Lazy.class)) {
					try {
						Object value = field.getValue(obj);
						if (null != value) {
							Field lazyField = value.getClass().getField(Constants.IS_LAZY);
							boolean isLazy = lazyField.getBoolean(value);
							if (isLazy)
								continue;
						}
						else
							continue; // FIXME we have a problem here when the user actually wants to delete the relation between objects
					}
					catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
						StatementLogger.log("Unable to interrogate Lazy relation " + field.columnName + " " + e.getMessage());
					}
				}
				if (field.isVersion) {
					innerUpdate.add(as + "." + field.columnName + " = " + as + "." + field.columnName + " + 1");
					hasNoneSilent = true;
					continue;
				}
				if (!field.isSilent && (field.fieldType == FieldType.NORMAL || depth != 0)) {
					innerUpdate.add(as + "." + field.columnName + " = ?");
					hasNoneSilent = true;
				}
				handleValue(db, obj, stat, field, depth);
			}
		}
		if (hasNoneSilent) {
			// if all fields were silent they were handled in handleValue and there would be nothing to do here
			// so we don't do the update.
			boolean firstCondition = true;
			Object primaryKey = null;
			for (FieldDefinition field : primaryKeyColumnNames) {
				Object aliasValue = field.getValue(alias);
				primaryKey = field.getValue(obj);
				if (!firstCondition) {
					query.addConditionToken(ConditionAndOr.AND);
				}
				firstCondition = false;
				query.addConditionToken(new Condition<>(aliasValue, primaryKey, CompareType.EQUAL));
			}
			stat.setSQL(dialect.wrapUpdateQuery(innerUpdate, tableName, as).toString());
			Number aliasValue = null;
			Number lVersion = null;
			if (null != this.version) {
				// if this ezqu is maintaining version we must find a row that matches our current
				query.addConditionToken(ConditionAndOr.AND);
				aliasValue = (Number)this.version.getValue(alias);
				lVersion = (Number)this.version.getValue(obj);
				query.addConditionToken(new Condition<>(aliasValue, lVersion, CompareType.EQUAL));
			}
			query.appendWhere(stat);
			if (db.factory.isShowSQL())
				StatementLogger.update(stat.logSQL());

			int numOfResults = stat.executeUpdate();
			if (0 == numOfResults) {
				// No update was done. This is probably because of a concurrency error
				// an sql error would be a -1 and a successful update will have a number higher than 0
				if (null != this.version) {
					db.rollback();
					throw new EzquConcurrencyException(tableName, obj.getClass(), primaryKey, lVersion);
				}
			}
			else if (null != this.version) {
				// we need to update the instance with the new version
				try {
					if (Long.class == this.version.field.getType())
						this.version.field.set(obj, lVersion.longValue() + 1);
					else
						this.version.field.set(obj, lVersion.intValue() + 1);
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
					StatementLogger.debug("problem setting version field " + this.version.field.getName());
				}
			}
		}
		// if the object inserted successfully and is a Table add the session to it.
		db.addSession(obj);
	}

	@SuppressWarnings("unchecked")
	void delete(Db db, Object obj) {
		db.reEntrantCache.prepareReEntrent(obj);
		if (this.isAggregateParent) {
    		// we have aggregate children
    		for (FieldDefinition fdef: this.getFields()) {
    			if (fdef.fieldType.isCollectionRelation()) { // either O2M or M2M relationship
    				db.deleteParentRelation(fdef, obj); // if it has relations it must be a Table type by design
    			}
    		}
    	}
		if (null != this.oneToOneRelations && !this.oneToOneRelations.isEmpty()) {
			// check for cascade delete on o2o relations
			for (FieldDefinition fdef: this.oneToOneRelations) {
				if (fdef.field.getAnnotation(Cascade.class) != null) {
					// this relation should be deleted as well
					fdef.field.setAccessible(true);
					try {
						Object o2o = fdef.field.get(obj);
						if (null == o2o)
							// attempt to get the relation from the db if exists
							o2o = getOne2OneFromDb(db, obj, primaryKeyColumnNames, fdef);
						if (null != o2o && !db.reEntrantCache.checkReEntrent(o2o))
							db.delete(o2o);
					}
					catch (IllegalArgumentException | IllegalAccessException e) {
						StatementLogger.log("Unable to delete child relation --> " + fdef.field.getName());
					}
				}
			}
		}
        if (null != this.getInterceptor())
        	this.getInterceptor().onDelete(obj);

        // after dealing with the children we delete the object
		if (primaryKeyColumnNames == null || primaryKeyColumnNames.isEmpty()) {
			throw new EzquError("IllegalState - No primary key columns defined for table %s - no update possible", obj.getClass());
		}
		SQLStatement stat = new SQLStatement(db);
		Object alias = Utils.newObject(obj.getClass());
		Query<Object> query = Query.from(db, alias);
		String as = query.getSelectTable().getAs();

		boolean firstCondition = true;
		for (FieldDefinition field : primaryKeyColumnNames) {
			Object value = field.getValue(obj);
			if (null == value) {
				// I don't have a primary key so I can't delete from the underlying db
				return;
			}
			Object aliasValue = field.getValue(alias);
			if (!firstCondition) {
				query.addConditionToken(ConditionAndOr.AND);
			}
			firstCondition = false;
			query.addConditionToken(new Condition<>(aliasValue, value, CompareType.EQUAL));
		}		
		stat.setSQL(dialect.wrapDeleteQuery(tableName, as).toString());
		query.appendWhere(stat);
		if (db.factory.isShowSQL())
			StatementLogger.delete(stat.logSQL());
		stat.executeUpdate();
		// multi cache is responsible for multi calls to the session thus if removed from the underlying db
		// this object should be removed from the cache.
		db.multiCallCache.removeReEntrent(obj);
	}

	int deleteAll(Db db) {
		SQLStatement stat = new SQLStatement(db);
		Object alias = Utils.newObject(this.clazz);
		Query<Object> query = Query.from(db, alias);
		String as = query.getSelectTable().getAs();

		stat.setSQL(dialect.wrapDeleteQuery(tableName, as).toString());
		if (db.factory.isShowSQL())
			StatementLogger.delete(stat.logSQL());
		return stat.executeUpdate();
	}

	TableDefinition<T> createTableIfRequired(Db db) {
		if (dialect.checkTableExists(tableName, db))
			return this;
		SQLStatement stat = new SQLStatement(db);
		StringBuilder buff = new StringBuilder(dialect.getCreateTableStatment(tableName));
		buff.append('(');
		StringJoiner sj = new StringJoiner(", ");
		for (FieldDefinition field : fields) {
			if (!field.isSilent && !field.isExtension) {
				String typeDef = field.columnName + " " + field.dataType;
				if (field.genType == GeneratorType.NONE && field.maxLength != 0) {
					typeDef += "(" + field.maxLength + ")";
				}
				sj.add(typeDef);
			}
		}
		if (0 < sj.length())
			buff.append(sj.toString());
			
		if (primaryKeyColumnNames != null && !primaryKeyColumnNames.isEmpty()) {
			sj = new StringJoiner(", ", ", PRIMARY KEY(", ")");
			for (FieldDefinition n : primaryKeyColumnNames) {
				sj.add(n.columnName);
			}
			buff.append(sj.toString());
		}
		buff.append(')');
		stat.setSQL(buff.toString());
		if (db.factory.isShowSQL())
			StatementLogger.create(stat.logSQL());
		stat.executeUpdate();
		try {
			createIndices(db);
		}
		catch (Exception any){
			StatementLogger.log("Unable to create indices [May be they exist]. " + any.getMessage());
		}
		alterTableDiscriminatorIfRequired(db);
		return this;
	}

	void initSelectObject(SelectTable<T> table, Object obj, Map<Object, SelectColumn<T>> map) {
		for (FieldDefinition def : fields) {
			Object o = def.initWithNewObject(obj);
			SelectColumn<T> column = new SelectColumn<>(table, def);
			map.put(o, column);
		}
	}

	@SuppressWarnings("unchecked")
	T readRow(ResultSet rs, Db db) {
		T item = Utils.newObject(clazz);
		if (null != primaryKeyColumnNames && !primaryKeyColumnNames.isEmpty()) {
			// this class has a primary key
			// 1. get the primaryKey value, 2. check if we have an object with such value in cache, 3. if so return it
			// if not continue.			
			for (FieldDefinition def: primaryKeyColumnNames) {
				Object key = def.read(rs, dialect);
				Object o = db.multiCallCache.checkReEntrent(clazz, key);
				if (null == o)
					o = db.reEntrantCache.checkReEntrent(clazz, key);
				if (null != o) {
					try {
						return (T)o;
					}
					catch (ClassCastException cce) {
						// This error should not happen. For now we just ignore it.
					}
				}
				else {
					doRead(rs, db, item, def);
				}
			}
		}
		for (FieldDefinition def: fields) {
			if (!def.isPrimaryKey) {
				db.reEntrantCache.prepareReEntrent(item);
				doRead(rs, db, item, def);
				db.reEntrantCache.removeReEntrent(item);
			}
		}
		if (null != primaryKeyColumnNames && !primaryKeyColumnNames.isEmpty()) {
			db.multiCallCache.prepareReEntrent(item);			
		}
		return item;
	}

	SQLStatement getSelectList(Db db, String as) {
		SQLStatement selectList = new SQLStatement(db);
		int i = 0;
		for (FieldDefinition def: fields) {
			if (!def.isSilent && !def.isExtension) {
				if (i > 0) {
					selectList.appendSQL(", ");
				}
				selectList.appendSQL(as + "." + def.columnName);
			}
			i++;
		}
		return selectList;
	}

	<Y, X> SQLStatement getSelectList(Query<Y> query, X x) {
		SQLStatement selectList = new SQLStatement(query.getDb());
		int i = 0;
		for (FieldDefinition def: fields) {
			if (def.isSilent)
				continue;
			if (i > 0) {
				selectList.appendSQL(", ");
			}
			Object obj = def.getValue(x);
			query.appendSQL(selectList, obj, def.field.getType().isEnum(), def.field.getType());

			// since the 'X' type object does not necessarily have field types as the queried objects in the result set we add a column name
			// that conforms with the 'X' type
			selectList.appendSQL(" AS " + def.columnName);
			i++;
		}
		return selectList;
	}

	/**
	 * Returns a list of primary key definitions
	 *
	 * @return List<FieldDefinition>
	 */
	List<FieldDefinition> getPrimaryKeyFields() {
		return primaryKeyColumnNames;
	}

	/**
	 * Return the intercepter instance for this table.
	 * @return Intercepter
	 */
	@SuppressWarnings("rawtypes" )
	CRUDInterceptor getInterceptor() {
		return intercepter;
	}

	/**
	 * returns true if the intercepter handles the given event.
	 *
	 * @param update
	 * @return boolean
	 */
	boolean hasInterceptEvent(Event event) {
		for (Event interceptorEvent: interceptorEvents) {
			if (Event.ALL == interceptorEvent || interceptorEvent == event)
				return true;
		}
		return false;
	}

	GeneratorType getGenerationtype() {
		return this.genType;
	}
	
	/*
	 * get the most "forward" "Intercepter Annotation" in the hierarchy tree.
	 */
	private Interceptor getInterceptorAnnotation(Class<?> clazz) {
		if (null == clazz || clazz.equals(Object.class))
			return null;
		Interceptor interceptorAnnot = getInterceptorAnnotation(clazz.getSuperclass());

		Interceptor lInterceptor = clazz.getAnnotation(Interceptor.class);
		return null == lInterceptor ? interceptorAnnot : lInterceptor;
	}
	
	private boolean getEnumType(FieldDefinition fieldDef, Column columnAnnotation) {
		if (Types.ENUM == fieldDef.type) {
			 if (null != columnAnnotation && Types.ENUM_INT == columnAnnotation.enumType()) {
				 fieldDef.type = Types.ENUM_INT;
				 fieldDef.dataType = dialect.getDataType(Integer.class);
				 fieldDef.maxLength = 0; // make sure that integer type does not get a length value when creating tables
			 }
			 else {
				 fieldDef.dataType = dialect.getDataType(String.class);
			 }
			 return true;
		}
		return false;
	}

	/**
	 * @return Field[]
	 */
	private <A> Field[] getAllFields(Class<A> clazz) {
		Field[] classFields;
		Inherited inherited = clazz.getAnnotation(Inherited.class);
		if (inherited != null) {
			Field[] superFields = null;
			Class<? super A> superClazz = clazz.getSuperclass();
			superFields = addSuperClassFields(superClazz);
			if (superFields.length > 0) {
				Field[] childFields = clazz.getDeclaredFields();
				classFields = new Field[superFields.length + childFields.length];
				System.arraycopy(superFields, 0, classFields, 0, superFields.length);
				System.arraycopy(childFields, 0, classFields, superFields.length, childFields.length);
			}
			else {
				classFields = clazz.getDeclaredFields();
			}
		}
		else {
			classFields = clazz.getDeclaredFields();
		}
		return classFields;
	}

	private <A> Field[] addSuperClassFields(Class<? super A> superClazz) {
		if (superClazz == null || superClazz.equals(Object.class))
			return new Field[0];
		Field[] superSuperFields = addSuperClassFields(superClazz.getSuperclass());

		boolean shouldMap = (superClazz.getAnnotation(MappedSuperclass.class) != null || superClazz.getAnnotation(Entity.class) != null);
		if (!shouldMap)
			return superSuperFields;

		if (superSuperFields.length == 0) {
			// super class is Object.class or not mapped. However this class is mapped so we can get its fields
			return superClazz.getDeclaredFields();
		}
		Field[] declaredFields = superClazz.getDeclaredFields();
		Field[] allFields = new Field[superSuperFields.length + declaredFields.length];
		System.arraycopy(superSuperFields, 0, allFields, 0, superSuperFields.length);
		System.arraycopy(declaredFields, 0, allFields, superSuperFields.length, declaredFields.length);
		return allFields;
	}

	private Types getTypes(Class<?> classType) {
		try {
			if (classType.isEnum())
				return Types.ENUM;
			return Types.valueOf(classType.getSimpleName().toUpperCase());
		}
		catch (RuntimeException e) {
			if (java.util.Date.class.equals(classType))
				return Types.UTIL_DATE;
			else if (java.sql.Date.class.equals(classType))
				return Types.SQL_DATE;
			else
				throw new EzquError(e, e.getMessage());
		}
	}
	
	private String getDataType(Class<?> fieldClass) {
		if (fieldClass.isPrimitive())
			fieldClass = ClassUtils.getWrapperClass(fieldClass);
		else if (fieldClass == java.util.UUID.class)
			fieldClass = java.lang.String.class;
		return dialect.getDataType(fieldClass);
	}
	
	/*
	 * Execute a batch and handle exceptions
	 */
	private int[] executeBatch(Db db, SQLStatement stat, int batchSize, boolean clean) {
		try {
			int[] results = stat.executeBatch(clean);
			db.commit();
			return results;
		}
		catch (BatchUpdateException bue) {
			db.rollback();
			int[] results = new int[batchSize];
			int[] counts = bue.getUpdateCounts();
			
			Arrays.fill(results, Statement.EXECUTE_FAILED);
			for (int i = 0; i < counts.length; i++) {
				if (counts[i] == Statement.EXECUTE_FAILED)
					results[i] = -100; // mark as the cause of the batch failure
			}
			return results;
		}
		catch (Exception e) {
			db.rollback();
			int[] results = new int[batchSize];
			Arrays.fill(results, Statement.EXECUTE_FAILED);
			return results;
		}
	}
	
	/*
	 * The last identity called using this connection would be the one that inserted the parameter 'obj'. we use it to set the value
	 */
	private void updateWithId(Object obj, SQLStatement stat, FieldDefinition identityField) {
		if (null != primaryKeyColumnNames) {
			String[] idColumnNames =  new String[] {identityField.columnName};
			Long generatedId = stat.executeUpdateWithId(idColumnNames);
			if (null != generatedId) {
				try {
					primaryKeyColumnNames.get(0).field.set(obj, generatedId);
				}
				catch (Exception e) {
					throw new EzquError(e, e.getMessage());
				}
			}
			else {
				throw new EzquError("Expected a generated Id but received None. Maybe your DB is not supported. Check supported Db list");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void handleValue(Db db, Object obj, SQLStatement stat, FieldDefinition field, int depth) {
		Object value = field.getValue(obj);
		// Deal with null primary keys (if object is sequence do a sequence query and update object... if identity you need to query the
		// object on the way out).
		if (field.isPrimaryKey && null == value) {
			if (genType == GeneratorType.SEQUENCE) {
				value = db.executeQuery(sequenceQuery, rs -> {
					if (rs.next()) {
						return rs.getLong(1);
					}
					return null;
				});
				try {
					field.field.set(obj, value); // add the new id to the object
				}
				catch (Exception e) {
					throw new EzquError(e, e.getMessage());
				}
			 }
			else if (genType == GeneratorType.UUID || UUID.class.isAssignableFrom(field.field.getType())) {
				try {
					UUID pk = UUID.randomUUID();
					// add the new id to the object
					if (String.class.isAssignableFrom(field.field.getType()))
						field.field.set(obj, pk.toString());
					else if (UUID.class.isAssignableFrom(field.field.getType()))
							field.field.set(obj, pk);

					value = pk.toString(); // the value int underlying Db is varchar
				}
				catch (Exception e) {
					throw new EzquError(e, e.getMessage());
				}
			}
		}
		switch (field.fieldType) {
			case NORMAL:
				Converter converter = field.field.getAnnotation(Converter.class);
				if (null != converter) {
					@SuppressWarnings("rawtypes")
					EzquConverter convert = Utils.newObject(converter.value());
					value = convert.toDb(value);
				}
				else if (null == value && GeneratorType.NONE != field.genType) {
					// this field has no value the value needs to be generated
					if (GeneratorType.SEQUENCE == field.genType) {
						value = db.executeQuery(field.sequenceQuery, rs -> {
							if (rs.next()) {
								return rs.getLong(1);
							}
							return null;
						});
						try {
							field.field.set(obj, value); // add the new id to the object
						}
						catch (Exception e) {
							throw new EzquError(e, e.getMessage());
						}
					}
					else if (GeneratorType.UUID == field.genType) {
						try {
							UUID pk = UUID.randomUUID();
							// add the new id to the object
							if (String.class.isAssignableFrom(field.field.getType()))
								field.field.set(obj, pk.toString());
							else if (UUID.class.isAssignableFrom(field.field.getType()))
									field.field.set(obj, pk);
	
							value = pk.toString(); // the value int underlying Db is varchar
						}
						catch (Exception e) {
							throw new EzquError(e, e.getMessage());
						}
					}
				}
				
				stat.addParameter(value);
				break;
			case FK: {
				// value is a table
				if (depth != 0) {
					if (value != null) {
						// if this object exists it updates if not it is inserted. ForeignKeys are always table
						Object pk = db.factory.getPrimaryKey(value);
						if (null == pk || !field.noUpdateField) {
							db.reEntrantCache.prepareReEntrent(obj);
							db.merge(value, depth - 1); // we reduce depth here
						}
						if (null == pk)
							// now after the merge the object has a primary key value
							stat.addParameter(db.factory.getPrimaryKey(value));
						else
							stat.addParameter(pk);
					}
					else
						stat.addParameter(null);
				}
				break;
			}
			case O2M, M2M: {
				// value is a list of tables (we got here only when merge was called from db by outside user
				if (depth != 0 && value != null && !((Collection<?>) value).isEmpty()) {
					// value is a Collection type
					for (Object table : (Collection<?>) value) {
						db.reEntrantCache.prepareReEntrent(obj);
						Object pk = db.factory.getPrimaryKey(table);
						if (null == pk || !field.noUpdateField)
							db.merge(table, depth - 1); // we reduce depth here
						db.updateRelationship(field, table, obj); // here object can only be a entity
					}
					if (!(value instanceof AbstractEzquCollection)) {
						try {
							if (value instanceof List l) {
								EzquList<?> list = new EzquList<>(l, db, field, db.getPrimaryKey(obj));
								field.field.set(obj, list);
							}
							else if (value instanceof Set s) {
								EzquSet<?> set = new EzquSet<>(s, db, field, db.getPrimaryKey(obj));
								field.field.set(obj, set);
							}
						}
						catch (IllegalArgumentException | IllegalAccessException e) {
							StatementLogger.log("Unable to set Jaqu Collection on field " + field.field.getName());
						}
					}
				}
				break;
			}
			case M2O: {
				// this is the many side of a join table managed O2M relationship.
				if (depth != 0 && value != null) {
					db.reEntrantCache.prepareReEntrent(obj);
					Object pk = db.factory.getPrimaryKey(value);
					if (null == pk || !field.noUpdateField)
						db.merge(value, depth - 1); // we reduce depth here
					db.updateRelationship(field, obj, value); // the parent(value) is still the one side as 'obj' is the many side
				}
				break;
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object getOne2OneFromDb(Db db, Object obj, List<FieldDefinition> primaryKeyColumnNames, FieldDefinition fdef) throws IllegalArgumentException, IllegalAccessException {
		Object parent = Utils.newObject(obj.getClass());
		Object desc = Utils.newObject(fdef.field.getType());

		Query query = (Query) db.from(desc);
		QueryJoinWhere queryJoin = query.innerJoin(parent).on(fdef.field.get(parent)).is(desc);

		boolean firstCondition = true;
		for (FieldDefinition field : primaryKeyColumnNames) {
			Object value = field.getValue(obj);
			if (null == value) {
				// I don't have a primary key so I can't delete from the underlying db
				return null;
			}
			Object aliasValue = field.getValue(parent);
			if (!firstCondition) {
				query.addConditionToken(ConditionAndOr.AND);
			}
			firstCondition = false;
			query.addConditionToken(new Condition<>(aliasValue, value, CompareType.EQUAL));
		}
		SQLStatement stat = new SQLStatement(db);
		query.appendWhere(stat);
		return queryJoin.selectFirst();
	}
	
	private void createIndices(Db db) {
		// Handle index/ constraint definition
		Indices indices = this.clazz.getAnnotation(Indices.class);
		if (null != indices){
			for (Index index: indices.value()){
				String query = dialect.getIndexStatement(index.name(), this.tableName, index.unique(), index.columns());
				SQLStatement stat = new SQLStatement(db);
				stat.setSQL(query);
				if (db.factory.isShowSQL())
					StatementLogger.alter(stat.logSQL());
				stat.executeUpdate();
			}
		}
	}

	private void alterTableDiscriminatorIfRequired(Db db) {
		if (inheritedType == InheritedType.DISCRIMINATOR && !dialect.checkDiscriminatorExists(tableName, discriminatorColumn, db)) {
			SQLStatement stat = new SQLStatement(db);
			stat.setSQL(dialect.getDiscriminatorStatment(tableName, discriminatorColumn));
			if (db.factory.isShowSQL())
				StatementLogger.alter(stat.logSQL());
			stat.executeUpdate();
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void doRead(ResultSet rs, Db db, T item, FieldDefinition def) {
		if (StatementLogger.isDebugEnabled())
			StatementLogger.debug("Working on Field: " + def.field.getName());
		if (!def.isSilent) {
			Object o;
			try {
				o = def.read(rs, dialect);
			}
			catch (EzquError sqle) {
				if (def.isExtension)
					return;
				throw sqle;
			}
			Converter converter = def.field.getAnnotation(Converter.class);
			if (null != converter) {				
				EzquConverter convert = Utils.newObject(converter.value());
				o = convert.fromDb(o);
			}
			def.setValue(item, o, db);
		}
		else
			// probably a relation is loaded
			def.setValue(item, null, db);
	}
	
	private Class<?> extractPrimaryKeyFromClass(Class<?> childType) {
		Field[] lFields = getAllFields(childType);
		for (Field field: lFields) {
			if (null != field.getAnnotation(PrimaryKey.class))
				return field.getType();
		}
		return null;
	}

	/*
	 * Create the join table if does not exist
	 *
	 * @param childType - The class of the other side of relation.
	 * @param joinTableName
	 * @param myColumnNameInRelation
	 * @param relationPkName
	 * @param relationColumnName
	 */
	private void createRelationTable(Class<?> childType, String joinTableName, String myColumnNameInRelation, Class<?> relationPkClass,
			String relationColumnName, Db db) {
		try {
			if (dialect.checkTableExists(joinTableName, db))
				return;
			FieldDefinition myPkDef = this.getPrimaryKeyFields().get(0);
			String myPkLength = myPkDef.maxLength != 0 ? "(" + myPkDef.maxLength + ")" : "";

			// Locate the pk field of the target relation
			Field[] lFields = getAllFields(childType);
			String relationPkLength = "";
			for (Field f : lFields) {
				if (f.getAnnotation(PrimaryKey.class) != null) {
					// this is the primary key
					Column columnAnnotation = f.getAnnotation(Column.class);
					if (columnAnnotation != null) {
						relationPkLength = columnAnnotation.length() != -1 ? "(" + columnAnnotation.length() + ")" : "";
					}
					break; // Pk Found
				}
			}

			String query = dialect.getCreateTableStatment(joinTableName)
				+ " ("
				+ myColumnNameInRelation
				+ " "
				+ getDataType(primaryKeyColumnNames.get(0).field.getType())
				+ myPkLength + ", "
				+ relationColumnName
				+ " "
				+ dialect.getDataType(relationPkClass) 
				+ relationPkLength 
				+ ")";
			db.executeUpdate(false, query);
		}
		catch (Exception e) {
			throw new EzquError(e, e.getMessage());
		}
	}
}