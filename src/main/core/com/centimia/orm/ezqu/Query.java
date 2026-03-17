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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.centimia.orm.ezqu.ISelectTable.JOIN_TYPE;
import com.centimia.orm.ezqu.constant.Constants;
import com.centimia.orm.ezqu.util.Utils;

/**
 * This class represents a query.
 *
 * @param <T> the return type (entity)
 * @author shai
 */
public class Query<T> {
	private static final String ILLEGAL_STATE_0 = "IllegalState 0 Entity based on %s must be part of a join query!!";
	
	private Db db;
    private SelectTable<T> from;
    private ArrayList<Token> conditions = new ArrayList<>();
    private ArrayList<Token> setTokens = new ArrayList<>();
    private ArrayList<SelectTable< ? >> joins = new ArrayList<>();
    private final IdentityHashMap<Object, SelectColumn<T>> aliasMap = new IdentityHashMap<>();
    private ArrayList<OrderExpression<T>> orderByList = new ArrayList<>();
    private LimitToken limit = null;
    private OffsetToken offset = null;
    private Object[] groupByExpressions;

    Query(Db db) {
        this.db = db;
    }

    /**
     * Creates a new query with the given alias as the from clause. 
     * The alias must be an unique instance of a class
     * and must not be an anonymous class.
     * 
     * @param &lt;T&gt;
     * @param db
     * @param alias
     * @return Query&lt;T&gt;
     */
    @SuppressWarnings("unchecked")
    static <T> Query<T> from(Db db, T alias) {
        Query<T> query = new Query<>(db);
        TableDefinition<T> def = (TableDefinition<T>) db.define(alias.getClass());
        query.from = new SelectTable<>(query, alias, JOIN_TYPE.NONE);
        def.initSelectObject(query.from, alias, query.aliasMap);
        return query;
    }

    /**
	 * Do an SQL "select count(*)" on the table
	 *
	 * @return long, the count
	 */
    public long selectCount() {
        SQLStatement selectList = new SQLStatement(db);
        selectList.setSQL("COUNT(*)");
        return prepare(selectList, false).executeQuery(rs -> {
        	rs.next();
            return rs.getLong(1);
        });
    }

    /**
	 * Do an SQL "select [ColumnA], count(*)" on the table.
	 * 
	 * For more complicated counts you can do:<br>
	 * <code><pre>
	 * 	select.from([yourDescriptor]).where()....select(new YourClass() {
	 * 		{
	 * 			fiedlName = descriptor.value (matching field);
	 * 			fiedlName1 = descriptor.value (matching field1);
	 * 			fiedlName2 = Function.count(); // or Function.count(descriptor.anyField, Db)
	 * 		}
	 * });
	 * </pre></code>
	 *
	 * @param &lt;Z&gt; the column you want to select
	 * @return List&lt;ColumnCount&lt;Z&gt;&gt; the results
	 */
    public <Z> List<ColumnCount<Z>> selectCount(Z x) {
    	return selectCount(x, 0);
    }
    
    /**
	 * @see #selectCount(Object) returns with counts in descending order
	 * @param &lt;Z&gt; the column you want to select
	 * @return List&lt;ColumnCount&lt;Z&gt;&gt; the results
	 */
	public <Z> List<ColumnCount<Z>> selectCountAsc(Z x) {
    	return selectCount(x, 1);
    }
    
	/**
	 * @see #selectCount(Object) returns with counts in descending order
	 * @param &lt;Z&gt; the column you want to select
	 * @return List&lt;ColumnCount&lt;Z&gt;&gt; the results
	 */
    public <Z> List<ColumnCount<Z>> selectCountDesc(Z x) {
    	return selectCount(x, 2);
    }
    
    /**
	 * Returns a List of the main "from" type based on a Union between the two queries.<br>
	 * this query is runs a union query of the two queries.<br>
	 * <b>Note:</b> All union query rules apply here. The queries must return the same amount of columns and have the same column types and names.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U> List<T> union(Query<U> unionQuery) {
		return union(unionQuery, false);
    }

	/**
	 * same as {@link #union(Query)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U> List<T> unionDistinct(Query<U> unionQuery) {
		return union(unionQuery, true);
    }

	/**
     * Returns a list of the given type (x). The type must be a new type, not one of the table's fields.
     * this query is runs a union query of the two queries.<br>
	 * <b>Note:</b> All union query rules apply here. The queries must return the same amount of columns and have the same column types and names.
	 *
	 * @param unionQuery
	 * @param x - the type to return
	 * @return List&lt;X&gt;
	 */
	public <U, X> List<X> union(Query<U> unionQuery, X x) {
		return union(unionQuery, x, false);
    }

	/**
	 * same as {@link #union(Query, Object)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U, X> List<X> unionDistinct(Query<U> unionQuery, X x) {
		return union(unionQuery, x, true);
    }

	/**
	 * Returns a List of the main "from" type based on a Union between the two queries.<br>
	 * this query is runs a union query of the two queries.<br>
	 * <b>Note:</b> All union query rules apply here. The queries must return the same amount of columns and have the same column types and names.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U> List<T> union(QueryWhere<U> unionQuery) {
		return union(unionQuery.query, false);
	}

	/**
	 * same as {@link #union(QueryWhere)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U> List<T> unionDistinct(QueryWhere<U> unionQuery) {
		return union(unionQuery.query, true);
	}

	
	/**
     * Returns a list of the given type (x). The type must be a new type, not one of the table's fields.
     * this query is runs a union query of the two queries.<br>
	 * <b>Note:</b> All union query rules apply here. The queries must return the same amount of columns and have the same column types and names.
	 *
	 * @param unionQuery
	 * @param x - the type to return
	 * @return List&lt;X&gt;
	 */
	@SuppressWarnings("unchecked")
	public <U, X, Z> List<X> union(QueryWhere<U> unionQuery, Z x) {
		return union(unionQuery.query, (X)x, false);
    }

	/**
	 * same as {@link #union(QueryWhere, Object)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U, X> List<X> unionDistinct(QueryWhere<U> unionQuery, X x) {
		return union(unionQuery.query, x, true);
    }

	/**
	 * Returns a List of the main "from" type based on a Union between the two queries.<br>
	 * this query is runs a union query of the two queries.<br>
	 * <b>Note:</b> All union query rules apply here. The queries must return the same amount of columns and have the same column types and names.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U> List<T> union(QueryJoinWhere<U> unionQuery) {
		return union(unionQuery.query, false);
	}

	/**
	 * same as {@link #union(QueryJoinWhere)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U> List<T> unionDistinct(QueryJoinWhere<U> unionQuery) {
		return union(unionQuery.query, true);
	}

	/**
     * Returns a list of the given type (x). The type must be a new type, not one of the table's fields.
     * this query is runs a union query of the two queries.<br>
	 * <b>Note:</b> All union query rules apply here. The queries must return the same amount of columns and have the same column types and names.
	 *
	 * @param unionQuery
	 * @param x - the type to return
	 * @return List&lt;X&gt;
	 */
	public <U, X> List<X> union(QueryJoinWhere<U> unionQuery, X x) {
		return union(unionQuery.query, x, false);
    }

	/**
	 * same as {@link #union(QueryJoinWhere, Object)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U, X> List<X> unionDistinct(QueryJoinWhere<U> unionQuery, X x) {
		return union(unionQuery.query, x, true);
    }

	/**
	 * Returns a map of all the query's results where key is one field in the select and value is another.<br>
	 * It is also available in join queries. You can have a key from any table within the join and a value from any table as well.<br>
	 * <b>Note:</b> If keys are not unique they will override.<br>
	 * example:
	 * <pre>
	 * Db db = [new session];
	 * Table t = [tableDescriptor]
	 * Map<Long, String> results = db.from(t).where(t.[getSomeField()]).is)[someValue]....selectAsMap(t.getA(), t.getB());
	 * </pre>
	 *
	 * @param key
	 * @param value
	 * @return Map&lt;K, V&gt;
	 */
	public <K, V> Map<K, V> selectAsMap(K key, V value) {
		return selectSimpleAsMap(key, value, false);
	}

	/**
	 * same as {@link #selectAsMap(Object, Object)} but returns only distinct results.
	 *
	 * @see #selectAsMap(Object, Object)
	 * @param key
	 * @param value
	 * @return Map&lt;K, V&gt;
	 */
	public <K, V> Map<K, V> selectDistinctAsMap(K key, V value) {
		return selectSimpleAsMap(key, value, true);
	}

	/**
	 * Perform the query select
	 * @return List&lt;T&gt; results
	 */
    public List<T> select() {
        return select(false);
    }

    /**
	 * Returns the first result of the select performed.
	 * @return T
	 */
    public T selectFirst() {
    	List<T> list = select(false);
        return list.isEmpty() ? list.get(0) : null;
    }

    /**
	 * Select only distinct results in the table
	 *
	 * @return List&lt;T&gt;
	 */
    public List<T> selectDistinct() {
        return select(true);
    }

    /**
	 * Returns the first result of a select for type Z.
	 * Type Z can be any defined type with mappings from the result.
	 *
	 * @apiNote &lt;Z&gt; Reason for using X and Z generic parameters as opposed to just Z is because externally when using a special Object mapping the instance created is actually a new
	 * 			anonymous class which is identical to Z but is actually not Z by signature. So using the Casting to X we allow generic strong typing for the user.
	 * @param x
	 * @return Z
	 */
    @SuppressWarnings("unchecked")
    public <X, Z> X selectFirst(Z x) {
        List<X> list = (List<X>) select(x);
        return list.isEmpty() ? null : list.get(0);
    }

    /**
	 * Returns a String representing the select assembled from the object query.
	 *
	 * @return String
	 */
    public String getSQL() {
    	return this.getSQL(false, false);
    }

    /**
	 * returns the query of an sql
	 * 
	 * @return String
	 */
	public String getDistinctSQL() {
    	return this.getSQL(true, false);
    }

	/**
	 * returns the query of an sql build according to the given object
	 * @param z
	 * @return String
	 */
	public <Z> String getSQL(Z z) {
    	return getSQL(z, false, false);
    }

	/**
	 * returns the query of a distinct select based on the given object
	 * @param z
	 * @return String
	 */
	public <Z> String getDistinctSQL(Z z) {
    	return getSQL(z, false, false);
    }

	/**
	 * Returns a String representing the select assembled from the object query + parameters for logging.
	 *
	 * @return String
	 */
    public String logSQL() {
    	return this.getSQL(false, true);
    }

    /**
	 * returns the query of an sql + parameters for logging
	 * 
	 * @return String
	 */
	public String logDistinctSQL() {
    	return this.getSQL(true, true);
    }
	
	/**
	 * returns the query of an sql build according to the given object with parameters for logging
	 * 
	 * @param z
	 * @return String
	 */
	public <Z> String logSQL(Z z) {
    	return getSQL(z, false, true);
    }
	
	/**
	 * returns the query of a distinct select based on the given object
	 * @param z
	 * @return String
	 */
	public <Z> String logDistinctSQL(Z z) {
    	return getSQL(z, false, true);
    }
	
	/**
     * A convenience method to get the object representing the right hand side of the join relationship only (without the need to specify the mapping between fields)
     * Returns a list of results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param tableClass - the object descriptor of the type needed on return
     * @throws EzquError - when not in join query
     * @return List&lt;U&gt;
     */
	public <U> List<U> selectRightHandJoin(U tableClass) {
    	return selectRightHandJoin(tableClass, false);
    }

	/**
     * A convenience method get a field result from an object representing the right hand side of the join relationship only. This is for a single field only
     * Returns a list of results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param tableClass - the object descriptor of the type needed on return
     * @throws EzquError - when not in join query
     * @return List&lt;Z&gt;
     */
    public <U, Z> List<Z> selectRightHandJoin(U tableClass, Z x) {
    	return selectRightHandJoin(tableClass, false, x);
    }

    /**
     * A convenience method to get the object representing the right hand side of the join relationship only (without the need to specify the mapping between fields)
     * Returns a list of distinct results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param tableClass - the object descriptor of the type needed on return
     * @throws EzquError - when not in join query
     * @return List&lt;U&gt;
     */
	public <U> List<U> selectDistinctRightHandJoin(U tableClass) {
    	return selectRightHandJoin(tableClass, true);
    }

	/**
     * A convenience method to a field of the object representing the right hand side of the join relationship only. Based on a single field only
     * Returns a list of distinct results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param tableClass - the object descriptor of the type needed on return
     * @throws EzquError - when not in join query
     * @return List&lt;U&gt;
     */
	public <U, Z> List<Z> selectDistinctRightHandJoin(U tableClass, Z x) {
    	return selectRightHandJoin(tableClass, true, x);
    }

	/**
     * A convenience method to get the object representing the right hand side of the join relationship only (without the need to specify the mapping between fields)
     * Returns the first result of a list of results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param tableClass - the object descriptor of the type needed on return
     * @throws EzquError - when not in join query
     */
	public <U> U selectFirstRightHandJoin(U tableClass) {
    	List<U> list = selectRightHandJoin(tableClass, false);
    	return list.isEmpty() ? null : list.get(0);
    }

	/**
     * A convenience method to get a field result from an object representing the right hand side of the join relationship only. This is for a single field only
     * Returns the first result of a list of results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param tableClass - the object descriptor of the type needed on return
     * @throws EzquError - when not in join query
     */
    public <U, Z> Z selectFirstRightHandJoin(U tableClass, Z x) {
    	List<Z> list = selectRightHandJoin(tableClass, false, x);
    	return list.isEmpty() ? null : list.get(0);
    }

    /**
	 * Returns distinct results of type X using a query on object Z
	 *
	 * @apiNote - Reason for using X and Z generic parameters as opposed to just Z is because externally when using a special Object mapping the instance created is actually a new
	 * 	 		  anonymous class which is identical to Z but is actually not Z by signature. So using the Casting to X we allow generic strong typing for the user.
	 * @param &lt;X&gt; - The type of object returned after the specific object is built from results.
	 * @param &lt;Z&gt; - The type of the Object used for describing the query
	 * @param x - A descriptor instance of type Z
	 * @return List&lt;X&gt;
	 */
    @SuppressWarnings("unchecked")
	public <X, Z> List<X> selectDistinct(Z x) {
        return select((X)x, true);
    }

    /**
	 * Returns the result of a select for object of type Z from Table T.
	 *
	 * @param &lt;Z&gt; Reason for using X and Z generic parameters as opposed to just Z is because externally when using a special Object mapping the instance created is actually a new
	 * 	 				anonymous class which is identical to Z but is actually not Z by signature. So using the Casting to X we allow generic strong typing for the user.
	 * @param x
	 * @return List&lt;Z&gt; results
	 */
    @SuppressWarnings("unchecked")
	public <X, Z> List<X> select(Z x) {
        return select((X)x, false);
    }

    /**
	 * Performs a delete query.
	 * <b>Note</b> Since delete executes without objects the multi reEntrent cache is cleared
	 * and objects taken from the db before will no longer be the same instance if fetched again</b>
	 *
	 * @return int - number of rows deleted
	 */
    public int delete() {
    	try {
			TableDefinition<T> def = from.getAliasDefinition();
			SQLStatement stat = new SQLStatement(db);
			if (def.isAggregateParent) {
				// before we delete we must take care of relationships
				stat.appendSQL("SELECT * FROM ");
				from.appendSQL(stat);
				appendWhere(stat);
				if (db.factory.isShowSQL())
					StatementLogger.select(stat.logSQL());

				stat.executeQuery(rs -> {
					while (rs.next()) {
						T item = from.getAliasDefinition().readRow(rs, db);
						for (FieldDefinition fdef : def.getFields()) {
							if (fdef.fieldType.isCollectionRelation()) {
								// this is a relation
								db.deleteParentRelation(fdef, item); // item has relations so it must be a Entity type
							}
						}
						db.multiCallCache.removeReEntrent(item);
					}
					return null;
				});
			}
			stat = new SQLStatement(db);
			// Nasty hack for MYSQL
			if (def.dialect == Dialect.MYSQL)
				stat.appendSQL("DELETE " + from.getAs() + " FROM ");
			else
				stat.appendSQL("DELETE FROM ");
			from.appendSQL(stat);
			appendWhere(stat);
			if (db.factory.isShowSQL())
				StatementLogger.delete(stat.logSQL());
			return stat.executeUpdate();
		}
		finally {
			db.multiCallCache.clearReEntrent();
		}
    }

    /**
	 * Perform the update requested by the specific where clause
	 * <b>Note</b> Since update executes without objects the multi reEntrent cache is cleared
	 * and objects taken from the db before will no longer be the same instance if fetched again</b>
	 *
	 * @return int - number of lines updated.
	 */
    public int update() {
        try {
			SQLStatement stat = new SQLStatement(db);
			stat.appendSQL("UPDATE ");
			from.appendSQL(stat);
			appendUpdate(stat);
			appendWhere(stat);
			if (stat.getSQL().indexOf("SET") == -1)
				throw new EzquError("IllegalState - To perform update use the set directive after from...!!!");
			if (db.factory.isShowSQL())
				StatementLogger.update(stat.logSQL());
			return stat.executeUpdate();
		}
		finally {
			db.multiCallCache.clearReEntrent();
		}
    }

    /**
	 * wraps everything following with "("<br>
	 * <b>Must follow with matching "endWrap</b>
	 *
	 * @return Query&lt;T&gt;
	 */
    public Query<T> wrap() {
    	this.addConditionToken((stat, query) -> stat.appendSQL("("));
    	return this;
    }

    /**
	 * Allows a simple string where clause
	 * 
	 * @param &lt;A&gt;
	 * @param whereCondition
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> where(final StringFilter whereCondition) {
    	Token conditionCode = (stat, query) -> stat.appendSQL(whereCondition.getConditionString(query.from)); 
		conditions.add(conditionCode);
		return new QueryWhere<>(this);
    }

	/**
	 * wraps the entity object and changes the condition to support the primary key.
	 * Useful in cases when the object holds an entity relation but you do not want to create the relation in the
	 * fluent query.
	 *
	 * @param &lt;A&gt;
	 * @param mask
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <K, A> QueryCondition<T, A> where(final GenericMask<K, A> mask) {
        return new QueryCondition<>(this, mask, mask.mask());
    }

	/**
	 * Start a where clause on the SQL query.
	 *
	 * @param &lt;A&gt;
	 * @param x
	 * @return QueryCondition<T, A>
	 */
    public <A> QueryCondition<T, A> where(A x) {
        return new QueryCondition<>(this, x);
    }
	
    /**
	 * Used on update queries to set the parameters of the update according to the
	 * given object.
	 *
	 * @param x - field from descriptor object
	 * @param v - value of the same type.
	 *
	 * @return QuerySet&lt;T, A&gt;
	 */
    public <A> QuerySet<T, A> set(A x, A v) {
    	if (Collection.class.isAssignableFrom(x.getClass())) {
    		// this is a relation updating is not supported like this
    		throw new EzquError("IllegalState - To update relations use db.update(Object)");
    	}
    	return new QuerySet<>(this, x, v);
    }

    /**
	 * Returns a primary key condition. Usually used internally, when the primary key is a definite identifier which is unknown....
	 * Does not support complex primary keys. Use this query only when the primary key to compare with is definite, any other condition
	 * is not supported
	 *
	 * @return QueryCondition&lt;T, Object&gt;
	 */
    public QueryCondition<T, Object> primaryKey() {
    	TableDefinition<?> def = from.getAliasDefinition();

    	// def will not be null here
    	List<FieldDefinition> primaryKeys = def.getPrimaryKeyFields();
    	if (primaryKeys == null || primaryKeys.isEmpty()) {
            throw new EzquError("IllegalState - No primary key columns defined for table %s - no update possible", def.tableName);
        }
    	if (primaryKeys.size() > 1)
    		throw new EzquError("UnsupportedOperation - Entity relationship is not supported for complex primary keys. Found in %s", def.tableName);
   		return new QueryCondition<>(this, primaryKeys.get(0).getValue(from.getAlias()));
	}

    /**
	 * Adds a where true condition to the query
	 * 
	 * @param condition
	 * @return QueryWhere&lt;T&gt;
	 */
    public QueryWhere<T> whereTrue(Boolean condition) {
        Token token = new Function("", condition);
        addConditionToken(token);
        return new QueryWhere<>(this);
    }

    /**
	 * a "where exists" clause. Adds "WHERE EXISTS (subQuery)" to the query
	 * @param subQuery
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> whereExists(QueryWhere<?> subQuery) {
		this.addConditionToken(new ExistsToken(subQuery));
		return new QueryWhere<>(this);
	}
	
	/**
	 * a "where not exists" clause. Adds "WHERE NOT EXISTS (subQuery)" to the query
	 * @param subQuery
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> whereNotExists(QueryWhere<?> subQuery) {
		this.addConditionToken((s, q) -> s.appendSQL("NOT"));
		this.addConditionToken(new ExistsToken(subQuery));
		return new QueryWhere<>(this);
	}
	
    /**
	 * Order by a number of columns.
	 *
	 * @param expressions the columns
	 * @return the query
	 */
	public Query<T> orderBy(Object ... expressions) {
		for (Object expr : expressions) {
			OrderExpression<T> e = new OrderExpression<>(this, expr, false, false, false);
			this.addOrderBy(e);
		}
		return this;
	}

	/**
	 * Order by one or more columns in descending order
	 * 
	 * @param expr
	 * @return QueryWhere&lt;T&gt; - the query
	 */
	public Query<T> orderByNullsFirst(Object ... expr) {
		return orderBy(false, true, false, expr);
	}

	/**
	 * Order by one or more columns in ascending order
	 * @param expr
	 * @return QueryWhere&lt;T&gt;
	 */
	public Query<T> orderByNullsLast(Object ... expr) {
		return orderBy(false, false, true, expr);
	}

	/**
	 * Descending order by a single given column
	 *
	 * @param expr
	 * @return Query
	 */
	public Query<T> orderByDesc(Object ... expr) {
		return orderBy(true, false, false, expr);
	}

	/**
	 * Order by in descending order nulls will be first
	 * 
	 * @param expr
	 * @return Query&lt;T&gt;
	 */
	public Query<T> orderByDescNullsFirst(Object ... expr) {
		return orderBy(true, true, false, expr);
	}

	/**
	 * Order by one or more columns in ascending order
	 * @param expr
	 * @return QueryWhere&lt;T&gt;
	 */
	public Query<T> orderByDescNullsLast(Object ... expr) {
		return orderBy(true, false, true, expr);
	}

	/**
	 * Create a having clause based on the column given.
	 * <b>You can only use a single having in a select clause</b>
	 *
	 * @param x
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <A> QueryCondition<T, A> having(final A x) {
		HavingToken conditionCode = new HavingToken();
		conditions.add(conditionCode);
		return new QueryCondition<>(this, x);
	}

	/**
	 * having clause with a supported aggregate function
	 * 
	 * @param function
	 * @param x
	 * @return QueryCondition&lt;T, Long&gt;
	 */
	public <A> QueryCondition<T, Long> having(HavingFunctions function, final A x) {
		HavingToken conditionCode = new HavingToken();
		conditions.add(conditionCode);
		conditions.add(new Function(function.name(), x));
		return new QueryCondition<>(this, Function.ignore());
	}

	/**
	 * Group By ordered objects
	 * 
	 * @param groupBy
	 * @return Query&lt;T&gt;
	 */
    public Query<T> groupBy(Object ... groupBy) {
        this.groupByExpressions = groupBy;
        return this;
    }

	/**
	 * adds a limit to the query<br>
	 * <b>Note:</b> You can not use 'limit' and 'offset' methods at the same time use {@link#offet(int, int)} instead!
	 * 
	 * @param limitNum
	 * @return Query&lt;T&gt;
	 */
	public Query<T> limit(int limitNum) {
		this.limit = new LimitToken(limitNum);
		return this;
	}

	/**
	 * adds an offset with <b>no limit</b> to the query
	 * 
	 * @param limitNum
	 * @param offsetNum
	 * @return Query&lt;T&gt;
	 */
	public Query<T> offset(int offsetNum) {
		this.offset = new OffsetToken(-1, offsetNum);
		return this;
	}
	
	/**
	 * adds an offset with limit to the query<br>
	 * If you want a limit with no offset you can issue {@link #limit(int)} instead, or use 0 as offsetNum
	 * 
	 * @param limitNum
	 * @param offsetNum
	 * @return Query&lt;T&gt;
	 */
	public Query<T> offset(int limitNum, int offsetNum) {
		this.offset = new OffsetToken(limitNum, offsetNum);
		return this;
	}
	
    /**
	 * inner Join another table. (returns only rows that match)
	 * The alias must be an unique instance of a class representing a table
     * and must not be an anonymous class.
     * 
	 * @param alias an alias for the table to join
	 * @return the joined query
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
    public <U> QueryJoin<T> innerJoin(U alias) {
        TableDefinition<T> def = (TableDefinition<T>) db.define(alias.getClass());
        SelectTable<T> join = new SelectTable(this, alias, JOIN_TYPE.INNER_JOIN);
        def.initSelectObject(join, alias, aliasMap);
        joins.add(join);
        return new QueryJoin(this, join);
    }
    
	/**
	 * Left Outer Join another table. (Return all rows from left table, and matching from rightHandSide)
	 * The alias must be an unique instance of a class representing a table
     * and must not be an anonymous class.
     * 
	 * @param alias an alias for the table to join
	 * @return the joined query
	 */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <U> QueryJoin<T> leftOuterJoin(U alias) {
        TableDefinition<T> def = (TableDefinition<T>) db.define(alias.getClass());
        SelectTable<T> join = new SelectTable(this, alias, JOIN_TYPE.LEFT_OUTER_JOIN);
        def.initSelectObject(join, alias, aliasMap);
        joins.add(join);
        return new QueryJoin(this, join);
    }

    void appendSQL(SQLStatement stat, Object x, boolean isEnum, Class<?> enumClass) {
    	if (x == Function.count()) {
            stat.appendSQL("COUNT(*)");
            return;
        }
        if (x == Function.ignore()) {
            return;
        }
        SelectColumn<T> col = null;
        Token token = db.getToken(x);
        if (null == token && isEnum) {
           	// try to get the token according to an enum value
        	Object enumValue = handleAsEnum(enumClass, x);
           	token = db.getToken(enumValue);
         	col = aliasMap.get(enumValue);
        }

        if (token != null) {
            token.appendSQL(stat, this);
            return;
        }

        if (null == col)
        	col = aliasMap.get(x);

        if (col != null) {
            col.appendSQL(stat, from.getAs());
            return;
        }
        stat.appendSQL("?");
        stat.addParameter(x);
    }

    void addConditionToken(Token condition) {
        conditions.add(condition);
    }

    void addUpdateToken(Token setToken) {
    	setTokens.add(setToken);
    }

    void appendWhere(SQLStatement stat) {
    	if (!conditions.isEmpty()) {
            if (!(conditions.get(0) instanceof HavingToken))
            	// if the first token is not a "having" sql clause then we print WHERE
            	stat.appendSQL(" WHERE ");
            for (Token token : conditions) {
                token.appendSQL(stat, this);
                stat.appendSQL(" ");
            }

            // add the discriminator if 'T' is a part of an inheritance tree.
            if (InheritedType.DISCRIMINATOR == from.getAliasDefinition().inheritedType) {
            	stat.appendSQL(" AND " + from.getAs() + "." + from.getAliasDefinition().discriminatorColumn + "='" + from.getAliasDefinition().discriminatorValue + "' ");
            }
        }
    	else {
    		// add the discriminator if 'T' is a part of an inheritance tree.
            if (InheritedType.DISCRIMINATOR == from.getAliasDefinition().inheritedType) {
            	stat.appendSQL(" WHERE " + from.getAs() + "." + from.getAliasDefinition().discriminatorColumn + "='" + from.getAliasDefinition().discriminatorValue + "' ");
            }
    	}
    }

    void appendUpdate(SQLStatement stat) {
        if (!setTokens.isEmpty()) {
            stat.appendSQL(" SET ");
            boolean first = true;
            for (Token token : setTokens) {
            	if (!first)
            		stat.appendSQL(", ");
            	first = false;
                token.appendSQL(stat, this);
                stat.appendSQL(" ");
            }
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    SQLStatement prepare(SQLStatement selectList, boolean distinct) {
        SQLStatement stat = selectList;
        String selectSQL = stat.getSQL();
        stat.setSQL("");
        stat.appendSQL("SELECT ");
        if (distinct) {
            stat.appendSQL("DISTINCT ");
        }
        stat.appendSQL(selectSQL);
        stat.appendSQL(" FROM ");
        from.appendSQL(stat);
        for (SelectTable join : joins) {
            join.appendSQLAsJoin(stat, this);
        }
        appendWhere(stat);
        if (groupByExpressions != null) {
            int havingIdx = stat.getSQL().indexOf("having");
            String havingQuery = null;
            if (havingIdx != -1) {
            	// we need to insert the group by before the having.
            	String currentQuery = stat.getSQL().substring(0, havingIdx);
            	havingQuery = stat.getSQL().substring(havingIdx);
            	stat.setSQL(currentQuery);
            }
        	stat.appendSQL(" GROUP BY ");
            int i = 0;
            for (Object obj : groupByExpressions) {
                if (i++ > 0) {
                    stat.appendSQL(", ");
                }
                appendSQL(stat, obj, obj.getClass().isEnum(), obj.getClass());
                stat.appendSQL(" ");
            }
            if (null != havingQuery)
            	stat.appendSQL(havingQuery);
        }
        if (!orderByList.isEmpty()) {
            stat.appendSQL(" ORDER BY ");
            int i = 0;
            for (OrderExpression o : orderByList) {
                if (i++ > 0) {
                    stat.appendSQL(", ");
                }
                o.appendSQL(stat);
                stat.appendSQL(" ");
            }
        }
        if (null != limit && null == offset) {
        	limit.appendSQL(stat, this);
        }
        else if (null != offset && null == limit) {
			offset.appendSQL(stat, this);
		}
        return stat;
    }

    Db getDb() {
        return db;
    }

    boolean isJoin() {
        return !joins.isEmpty();
    }

    SelectColumn<T> getSelectColumn(Object obj) {
        return aliasMap.get(obj);
    }

    void addOrderBy(OrderExpression<T> expr) {
        orderByList.add(expr);
    }

    SelectTable<T> getSelectTable() {
    	return from;
    }

    List<SelectTable<?>> getJoins() {
    	return joins;
    }

    
    @SuppressWarnings("rawtypes")
	IdentityHashMap aliasMap() {
		return aliasMap;
	}
    
    private String getSQL(boolean distinct, boolean forLog) {
    	TableDefinition<T> def = from.getAliasDefinition();
        SQLStatement selectList = def.getSelectList(db, from.getAs());
        if (forLog)
			return prepare(selectList, distinct).logSQL().trim();
        return prepare(selectList, distinct).getSQL().trim();
    }

    @SuppressWarnings("unchecked")
	private <X> String getSQL(X x, boolean distinct, boolean forLog) {
    	Class<X> clazz = (Class<X>) x.getClass();
    	if (clazz.isAnonymousClass())
    		clazz = (Class<X>) clazz.getSuperclass();
    	TableDefinition<X> def = EzquSessionFactory.define(clazz, db, false);
        SQLStatement selectList = def.getSelectList(this, x);
        if (forLog)
        	return prepare(selectList, distinct).logSQL().trim();
        return prepare(selectList, distinct).getSQL().trim();
    }

    private List<T> select(boolean distinct) {
    	List<T> result = new ArrayList<>();
        TableDefinition<T> def = from.getAliasDefinition();
        SQLStatement selectList = def.getSelectList(db, from.getAs());
        prepare(selectList, distinct).executeQuery(rs -> {
        	 while (rs.next()) {
                 T item = def.readRow(rs, db);
                 db.addSession(item);
                 result.add(item);
             }
        	 return null;
        });

        return result;
    }

    private <U> List<T> union(Query<U> unionQuery, boolean distinct) {
    	if (null == unionQuery) {
			return this.select();
    	}
    	List<T> result = new ArrayList<>();
    	TableDefinition<T> def = from.getAliasDefinition();
    	SQLStatement selectList = def.getSelectList(db, from.getAs());

    	TableDefinition<U> unionDef = unionQuery.from.getAliasDefinition();
    	SQLStatement unionSelectList = unionDef.getSelectList(db, unionQuery.from.getAs());

    	selectList = prepare(selectList, distinct);
    	selectList.executeUnion(unionQuery.prepare(unionSelectList, distinct), rs -> {
    		while (rs.next()) {
                T item = def.readRow(rs, db);
                db.addSession(item);
                result.add(item);
            }
    		return null;
    	});
    	return result;
    }

    private <U, X> List<X> union(Query<U> unionQuery, X x, boolean distinct) {
    	if (null != x){
			Class<?> clazz = x.getClass();
			if (!Utils.isSimpleType(clazz) && !clazz.isEnum()) {
				if (null == unionQuery)
					return this.select(x);

				if (!Object.class.equals(clazz.getSuperclass()))
		        	clazz = clazz.getSuperclass();
				List<X> result = new ArrayList<>();
		    	@SuppressWarnings("unchecked")
				TableDefinition<X> def = EzquSessionFactory.define((Class<X>)clazz, db, false);
		    	SQLStatement selectList = def.getSelectList(db, from.getAs());
		    	SQLStatement unuionSelectList = def.getSelectList(db, unionQuery.from.getAs());

		    	selectList = prepare(selectList, distinct);
		    	selectList.executeUnion(unionQuery.prepare(unuionSelectList, distinct), rs -> {
		    		while (rs.next()) {
		                X item = def.readRow(rs, db);
		                result.add(item);
		            }
		    		return null;
		    	});
		    	return result;
			}
		}
		return new ArrayList<>();
    }

	@SuppressWarnings("unchecked")
	private <U> List<U> selectRightHandJoin(U tableClass, boolean distinct) {
    	if (tableClass.getClass().isAnonymousClass())
    		throw new EzquError(Constants.TO_GET_A_SUBSET);
    	if (this.joins == null || this.joins.isEmpty())
    		throw new EzquError(ILLEGAL_STATE_0, tableClass.getClass().getName());

    	TableDefinition<U> definition = null;
    	String as = null;
    	for (SelectTable<?> selectTable: joins) {
    		if (selectTable.getAlias() == tableClass) {
    			as = selectTable.getAs();
    			definition = (TableDefinition<U>) selectTable.getAliasDefinition();
    			break;
    		}
    	}
    	if (null == definition)
    		throw new EzquError(ILLEGAL_STATE_0, tableClass.getClass().getName());
    	final TableDefinition<U> def = definition;
    	SQLStatement selectList = def.getSelectList(db, as);
    	List<U> result = new ArrayList<>();
        prepare(selectList, distinct).executeQuery(rs -> {
        	while (rs.next()) {
                U item = def.readRow(rs, db);
                db.addSession(item);
                result.add(item);
            }
        	return null;
        });

        return result;
    }

	@SuppressWarnings("unchecked")
	private <U, Z> List<Z> selectRightHandJoin(U tableClass, boolean distinct, Z x) {
    	if (tableClass.getClass().isAnonymousClass())
    		throw new EzquError(Constants.TO_GET_A_SUBSET);
    	if (this.joins == null || this.joins.isEmpty())
    		throw new EzquError(ILLEGAL_STATE_0, tableClass.getClass().getName());

    	List<Z> result = new ArrayList<>();
    	if (null != x) {
    		// we want a specific list of a single value from the joined tables
    		Class< ? > clazz = x.getClass();
    		if (Utils.isSimpleType(clazz) || clazz.isEnum()) {
		    	SQLStatement selectList = new SQLStatement(db);
		    	for (SelectTable<?> selectTable: joins) {
		    		if (selectTable.getAlias() == tableClass) {
		    			selectTable.appendSqlColumnFromField(selectList, x);
		    		}
		    	}

	        	prepare(selectList, distinct).executeQuery(rs -> {
	        		while (rs.next()) {
		        		Z value = null;
	                	if (x.getClass().isEnum())
	                		value = (Z)handleAsEnum(x.getClass(), rs.getObject(1));
	                	else {
	                    	Types type = Types.valueOf(x.getClass().getSimpleName().toUpperCase());
	                		value = (Z) db.factory.getDialect().getValueByType(type, rs, 1);
	                	}
	                    result.add(value);
                	}
	        		return null;
	        	});
	        }
	        else
	        	throw new EzquError(Constants.TO_GET_A_SUBSET);
    	}
    	return result;
    }

    @SuppressWarnings("unchecked")
    private <Z> List<Z> select(Z x, boolean distinct) {
        Class< ? > clazz = x.getClass();
        if (Utils.isSimpleType(clazz) || clazz.isEnum()) {
            return getSimple(x, distinct);
        }
        if (!Object.class.equals(clazz.getSuperclass()))
        	clazz = clazz.getSuperclass();
        return select((Class<Z>) clazz,  x, distinct);
    }

    @SuppressWarnings("unchecked")
	private <K, V> Map<K, V> selectSimpleAsMap(K key, V value, boolean distinct) {
    	SQLStatement selectList = new SQLStatement(db);
        appendSQL(selectList, key, false, null);
        selectList.appendSQL(", ");
        appendSQL(selectList, value, false, null);
        Map<K, V> result = new HashMap<>();
        prepare(selectList, distinct).executeQuery(rs -> {
        	while (rs.next()) {
                try {
                	K theKey = null;
                	V theValue = null;
                	if (key.getClass().isEnum())
                		theKey = (K)handleAsEnum(key.getClass(), rs.getObject(1));
                	else {
                		Types type = Types.valueOf(value.getClass().getSimpleName().toUpperCase());
                		theKey = (K) db.factory.dialect.getValueByType(type, rs, 1);
                	}
                	if (value.getClass().isEnum())
                		theValue = (V)handleAsEnum(value.getClass(), rs.getObject(2));
                	else {
                		Types type = Types.valueOf(value.getClass().getSimpleName().toUpperCase());
                		theValue = (V) db.factory.dialect.getValueByType(type, rs, 2);
                	}
                    result.put(theKey, theValue);
                }
                catch (Exception e) {
                    throw new EzquError(e, e.getMessage());
                }
            }
        	return null;
        });
        return result;
    }

    private <X> List<X> select(Class<X> clazz, X x, boolean distinct) {
        List<X> result = new ArrayList<>();
        TableDefinition<X> def = EzquSessionFactory.define(clazz, db, false);
        SQLStatement selectList = def.getSelectList(this, x);
        prepare(selectList, distinct).executeQuery(rs -> {
        	 while (rs.next()) {
                 X row = def.readRow(rs, db);
                 result.add(row);
             }
        	 return null;
        });
        return result;
    }

    private Query<T> orderBy(boolean desc, boolean nullsFirst, boolean nullsLast, Object ... expr) {
    	int length = expr.length;
		switch (length) {
			case 0: return this;
			case 1: {
				OrderExpression<T> e = new OrderExpression<>(this, expr[length - 1], desc, nullsFirst, nullsLast);
				this.addOrderBy(e);
				return this;
			}
			default: {
				for (int i = 0; i < length - 1; i++) {
					OrderExpression<T> e = new OrderExpression<>(this, expr[i], false, false, false);
					this.addOrderBy(e);
				}
				OrderExpression<T> e = new OrderExpression<>(this, expr[length - 1], desc, nullsFirst, nullsLast);
				this.addOrderBy(e);
				return this;
			}
		}
    }
    
    private <Z> List<ColumnCount<Z>> selectCount(Z x, int sort) {
    	Class<?> clazz = x.getClass();
    	if (Utils.isSimpleType(clazz) || clazz.isEnum()) {
    		SQLStatement selectList = new SQLStatement(db);
    		appendSQL(selectList, x, false, null);
			selectList.appendSQL(", COUNT(*)");
			groupBy(x);
						
			List<ColumnCount<Z>> result = new ArrayList<>();
			prepare(selectList, false).executeQuery(rs -> {
				while (rs.next()) {
					@SuppressWarnings("unchecked")
					ColumnCount<Z> row = new ColumnCount<>((Z)rs.getObject(1), rs.getLong(2));
					result.add(row);
				}
				return null;
	        });
			
			switch (sort) {
				case 1: result.sort(null); break;
				case 2: result.sort(null); Collections.reverse(result); break;
				default: break;
			}
			return result;
		}
		else {
			throw new EzquError("UnsupportedOperation - selectCount only supports simple types and enums. "
					+ "To select complex types use groupBy and select and do\n"
					+ "[your fluent query].select(new YourClass() {{static block with function count}})");
    	}
    }
    
    @SuppressWarnings("unchecked")
    private <X> List<X> getSimple(X x, boolean distinct) {
        SQLStatement selectList = new SQLStatement(db);
        appendSQL(selectList, x, false, null);
        List<X> result = new ArrayList<>();
        prepare(selectList, distinct).executeQuery(rs -> {
        	while (rs.next()) {
                try {
                	X value = null;
                	if (x.getClass().isEnum())
                		value = (X)handleAsEnum(x.getClass(), rs.getObject(1));
                	else {
                		Types type = Types.valueOf(x.getClass().getSimpleName().toUpperCase());
                		value = (X) db.factory.getDialect().getValueByType(type, rs, 1);
                	}
                    result.add(value);
                }
                catch (Exception e) {
                    throw new EzquError(e, e.getMessage());
                }
            }
        	return null;
        });
        return result;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	private Object handleAsEnum(Class enumClass, Object object) {
    	if (null == object || object.getClass().isEnum())
    		return object;
		if (String.class.isAssignableFrom(object.getClass()))
			return Enum.valueOf(enumClass, (String) object);
		// it must be an int type
		return Utils.newEnum(enumClass, (Integer)object);
	}
}
