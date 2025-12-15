package com.centimia.orm.ezqu;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.Lazy;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;
import com.centimia.orm.ezqu.annotation.RelationTypes;
import com.centimia.orm.ezqu.constant.Constants;
import com.centimia.orm.ezqu.util.ClassUtils;
import com.centimia.orm.ezqu.util.FieldComperator;
import com.centimia.orm.ezqu.util.Utils;

/**
 * The meta data of a field.
 */
class FieldDefinition implements Comparable<FieldDefinition> {
	private static final String OBJECT_VALUE = "[Object: %s], [value: %s]\t";
	
	String columnName;
	Field field;
	String dataType;
	int maxLength = 0;
	boolean isPrimaryKey;
	FieldType fieldType = FieldType.NORMAL;
	RelationDefinition relationDefinition;
	boolean isSilent = false;
	boolean noUpdateField = false;
	Types type;
	Method getter;
	boolean unique;
	boolean notNull;
	boolean isVersion = false;
	boolean isExtension;
	String sequenceQuery = null;
	GeneratorType genType = GeneratorType.NONE;
	
	@SuppressWarnings("rawtypes")
	Object getValue(Object obj) {
		try {
			field.setAccessible(true);
			Object actualValue = field.get(obj);
			if (null != actualValue) {
				switch (type) {
					case ENUM: {
						if (null == actualValue.toString())
							return actualValue;
						return actualValue.toString();
					}
					case ENUM_INT: {
						if (null == actualValue.toString())
							return actualValue;
						return ((Enum)actualValue).ordinal();
					}
					case UUID: return actualValue.toString();
					default: break;
				}
			}
			return actualValue;
		}
		catch (Exception e) {
			throw new EzquError(e, "In fieldDefinition.getValue -> %s", e.getMessage());
		}
	}

	Object initWithNewObject(Object obj) {
		switch (type) {
			case ENUM, ENUM_INT: {
				// initialize with the first value in the enum (to be used as key)
				Class<?> enumClass = field.getType();
				Object newEnum = Utils.newEnum(enumClass);
				field.setAccessible(true);
				try {
					field.set(obj, newEnum);
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
					String objectString = (null == obj) ? "null" : obj.getClass().getName();
					String valueString = newEnum.toString();
					String msg = String.format(OBJECT_VALUE, objectString, valueString);
					throw new EzquError(e, msg + e.getMessage());
				}
				return newEnum;
			}
			case UUID: {
				UUID sUUID = UUID.randomUUID();
				try {					
					field.set(obj, sUUID);
					return sUUID.toString();
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
					String objectString = (null == obj) ? "null" : obj.getClass().getName();
					String valueString = sUUID.toString();
					String msg = String.format(OBJECT_VALUE, objectString, valueString);
					throw new EzquError(e, msg + e.getMessage());
				}
			}
			case FK: {
				// this is a O2O relation. Must be an entity and thus must have an empty constructor.
				Object o = Utils.newObject(field.getType());
				try {
					field.set(obj, o);
				}
				catch (IllegalArgumentException | IllegalAccessException e) {
					// No reason for this to happen, we set it as null which would cause a null pointer exception
					o = null;
				}
				return o;
			}
			default: {
				Object o = Utils.newObject(field.getType());
				setValue(obj, o, null);
				return o;
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	void setValue(final Object objToSet, Object fieldValueFromDb, final Db db) {
		try {
			Object tmp = fieldValueFromDb;
			switch (fieldType) {
				case NORMAL:
					// if 'o' equals null then setting the 'enum' will cause a nullPointerException
					if ((Types.ENUM_INT == type || Types.ENUM == type) && null != fieldValueFromDb) {
						Class enumClass = field.getType();
						if (Types.ENUM_INT == type) {
							field.set(objToSet, enumClass.getEnumConstants()[(Integer)fieldValueFromDb]);
						}
						else {
							field.set(objToSet, Enum.valueOf(enumClass, (String)fieldValueFromDb));
						}
					}
					else if (Types.UUID == type && null != fieldValueFromDb) {
						// object from DB should be a String by mapping
						field.set(objToSet, UUID.fromString((String)fieldValueFromDb));
					}
					else
						field.set(objToSet, fieldValueFromDb);
					break;
				case M2O: {
					if (null != db) {
						// here we need to fetch the parent object based on the id which is in the relationTable
						// this is the same as an FK field accept for the fact that we have no value in the DB, i.e. 'tmp' == null and 'o' == null
						// get the primary key...
						String query = "select " + relationDefinition.relationColumnName + " from " + relationDefinition.relationTableName + " where " + relationDefinition.relationFieldName + " = ?";
						final Object pk = db.factory.getPrimaryKey(objToSet);
						Object result = db.executeQuery(query, rs -> {
							if (rs.next())
								return db.factory.getDialect().getValueByType(type, rs, relationDefinition.relationColumnName);
							return null;
						}, pk);
						if (null != result) {
							// field from db would have a 'null' which in this case is in correct so we have to set it up.
							// create the object to hold the data (I could use field.getType() but this way we can find mistakes
							fieldValueFromDb = Utils.newObject(relationDefinition.dataType[0]);
							tmp = result;
							// if parent was found we continue to FK
						}
						else
							break;
					}
					else
						break;
					// we don't explicitly break as there is a possibility case where we continue to FK choice
				}
				case FK: {
					if (null == field.getAnnotation(Lazy.class)) {
						RelationTypes relationTypes = field.getAnnotation(RelationTypes.class);
						Class<?>[] types = null;
						if (null == relationTypes)
							types = new Class<?>[] {field.getType()};
						else
							types = relationTypes.value();

						boolean found = false;
						for (Class<?> innerType: types) {
							fieldValueFromDb = Utils.convert(fieldValueFromDb, innerType);
							if (null != fieldValueFromDb && !innerType.isInstance(tmp)) {
								Object reEntrant = db.reEntrantCache.checkReEntrent(fieldValueFromDb.getClass(), tmp);
								if (null != reEntrant) {
									fieldValueFromDb = reEntrant;
									found = true;
									break;
								}
								else {
									List<?> result = db.from(fieldValueFromDb).primaryKey().is(tmp.toString()).select();
									if (!result.isEmpty()) {
										fieldValueFromDb = result.get(0);
										found = true;
										break;
									}
								}
							}
							else {
								// either found it as an MTO type or with currently have no relationship i.e fieldValueFronDb is null
								found = true;
							}
						}
						if (!found)
							throw new EzquError("""
									Data Consistency error - Foreign relation does not exist!!
									Error column was {%s} with value %s in table %s
									missing in table %s
									""", field.getName(), tmp, objToSet.getClass().getName(), 
										fieldValueFromDb.getClass().getName());
						field.set(objToSet, fieldValueFromDb);
					}
					else {
						// should be marked as lazy loaded
						if (null != field.getType().getAnnotation(Entity.class) ||
								null != field.getType().getAnnotation(MappedSuperclass.class)) {
							Class<?> innerType;
							boolean isAbstract = Modifier.isAbstract(field.getType().getModifiers());
							if (isAbstract && null != field.getAnnotation(RelationTypes.class)) {
								// take the first class and use that as an instance
								innerType = field.getAnnotation(RelationTypes.class).value()[0];
							}
							else if (!isAbstract) {
								innerType = field.getType();
							}
							else
								throw new EzquError("Can not instanciate a class of type %s on class %s because it is abstract!", field.getType().getName(), objToSet.getClass().getName());
							Field lazyfield = innerType.getField(Constants.IS_LAZY);
							fieldValueFromDb = innerType.getConstructor().newInstance();
				 			lazyfield.setBoolean(fieldValueFromDb, true);
							field.set(objToSet, fieldValueFromDb);
						}
					}
					break;
				}
				case O2M: {
					if (relationDefinition.eagerLoad && db != null) {
						if (relationDefinition.relationTableName != null) {
							List resultList = new ArrayList<>();
							for (Class<?> lDataType: relationDefinition.dataType) {
								resultList.addAll(db.getRelationByRelationTable(this, db.factory.getPrimaryKey(objToSet), lDataType));
							}

							if (!resultList.isEmpty()) {
								if (relationDefinition.dataType.length > 1 && null != relationDefinition.orderByField) {
									resultList.sort(new FieldComperator(relationDefinition.dataType[0], relationDefinition.orderByField));
								}
								if (this.field.getType().isAssignableFrom(resultList.getClass()))
									fieldValueFromDb = new EzquList(resultList, db, this, db.factory.getPrimaryKey(objToSet));
								else {
									// only when the type is a Set type we will be here
									Set set = new HashSet<>();
									set.addAll(resultList);
									fieldValueFromDb = new EzquSet(set, db, this, db.factory.getPrimaryKey(objToSet));
								}
							}
							else {
								// on eager loading if no result exists we set to an empty collection
								if (this.field.getType().isAssignableFrom(resultList.getClass()))
									fieldValueFromDb = new EzquList(new ArrayList<>(), db, this, db.factory.getPrimaryKey(objToSet)) ;
								else
									fieldValueFromDb = new EzquSet(new HashSet<>(), db, this, db.factory.getPrimaryKey(objToSet));
							}
						}
						else {
							List resultList = new ArrayList<>();
							for (Class<?> lDataType: relationDefinition.dataType) {
								Object descriptor = Utils.newObject(lDataType);
								QueryWhere<?> where = db.from(descriptor).where(st -> {
									FieldDefinition fdef = ((SelectTable)st).getAliasDefinition().getDefinitionForField(relationDefinition.relationFieldName);
									Object myPrimaryKey = db.factory.getPrimaryKey(objToSet);
									String pk = (myPrimaryKey instanceof String s) ? "'" + s.replace("'", "''") + "'" : myPrimaryKey.toString();

									if (null != fdef)
										// this is the case when it is a two sided relationship. To allow that the name of the column in the DB and the name of the field are
										// different we use the columnName property.
										return st.getAs() + "." + fdef.columnName + " = " + pk;
									// This is the case of one sided relationship. In this case the name of the FK is given to us in the relationFieldName
									return st.getAs() + "." + relationDefinition.relationFieldName + " = " + pk;
								});
								String orderByField = relationDefinition.orderByField;
								if (null != orderByField) {
									Field lField = ClassUtils.findField(lDataType, relationDefinition.orderByField);
									lField.setAccessible(true);
									if ("DESC".equals(relationDefinition.direction))
										resultList.addAll(where.orderByDesc(lField.get(descriptor)).select());
									else
										resultList.addAll(where.orderBy(lField.get(descriptor)).select());
								}
								else
									resultList.addAll(where.select());
							}
							if (!resultList.isEmpty()) {
								if (relationDefinition.dataType.length > 1 && null != relationDefinition.orderByField) {
									resultList.sort(new FieldComperator(relationDefinition.dataType[0], relationDefinition.orderByField));
								}
								if (this.field.getType().isAssignableFrom(resultList.getClass()))
									fieldValueFromDb = new EzquList(resultList, db, this, db.factory.getPrimaryKey(objToSet));
								else {
									// only when the type is a Set type we will be here
									Set set = new HashSet<>();
									set.addAll(resultList);
									fieldValueFromDb = new EzquSet(set, db, this, db.factory.getPrimaryKey(objToSet));
								}
							}
							else {
								// on eager loading if no result exists we set to an empty collection
								if (this.field.getType().isAssignableFrom(resultList.getClass()))
									fieldValueFromDb = new EzquList(new ArrayList<>(), db, this, db.factory.getPrimaryKey(objToSet)) ;
								else
									fieldValueFromDb = new EzquSet(new HashSet<>(), db, this, db.factory.getPrimaryKey(objToSet));
							}
						}
					}
					else {
						// instrument this instance of the class
						Field dbField = objToSet.getClass().getField("db");
						// put the open connection on the object. As long as the connection is open calling the getter method on the
						// 'obj' will produce the relation
						dbField.set(objToSet, db);
						fieldValueFromDb = null;
					}
					field.set(objToSet, fieldValueFromDb);
					break;
				}
				case M2M: {
					// instrument this instance of the class
					Field dbField = objToSet.getClass().getField("db");
					// put the open connection on the object. As long as the connection is open calling the getter method on the 'obj'
					// will produce the relation
					dbField.set(objToSet, db);
					break;
				}
				default:
					throw new EzquError("IllegalState - Field %s was marked as relation but has no relation MetaData in define method", columnName);
			}
		}
		catch (Exception e) {
			String objectString = (null == objToSet) ? "null" : objToSet.getClass().getName();
			String valueString = (null == fieldValueFromDb) ? null : fieldValueFromDb.toString();
			String msg = String.format(OBJECT_VALUE, objectString, valueString);
			throw new EzquError(e, msg + e.getMessage());
		}
	}

	Object read(ResultSet rs, Dialect dialect) {
		try {
			return dialect.getValueByType(type, rs, this.columnName);
		}
		catch (SQLException e) {
			throw new EzquError(e, e.getMessage());
		}
	}

	@Override
	public int hashCode() {
		if (null == columnName)
			return 0;
		return columnName.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof FieldDefinition))
			return false;
		return columnName.equals(((FieldDefinition)obj).columnName);
	}

	/**
	 * for sorting. sort by fieldType ascending unless same coloumnName is used.
	 */
	@Override
	public int compareTo(FieldDefinition o) {
		if (o == null)
			return 1;
		if (null != columnName && columnName.equals(o.columnName))
			return 0;
		return fieldType.compareTo(o.fieldType);
	}
}