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

import java.util.List;
import java.util.Map;

/**
 * This class represents a query with a condition.
 *
 * @param &lt;T&gt; the return type
 */
public class QueryWhere<T> {

	Query<T> query;

	QueryWhere(Query<T> query) {
		this.query = query;
	}

	/**
	 * Perform an 'AND' operator in the query
	 * @param &lt;A&gt; - represents any field type that exists on Object <T>
	 * @param x - The field (gives field name) that should be attached with 'AND' to the query
	 * @return QueryCondition
	 */
	public <A> QueryCondition<T, A> and(A x) {
		query.addConditionToken(ConditionAndOr.AND);
		return new QueryCondition<>(query, x);
	}

	/**
	 * Perform an 'AND' operator in the query</br>
	 * Adds a '(' after the 'and' operator. Close with {@link #endWrap()}
	 * 
	 * @param &lt;A&gt; - represents any field type that exists on Object <T>
	 * @param x - The field (gives field name) that should be attached with 'AND' to the query
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <A> QueryCondition<T, A> andWrap(A x) {
		query.addConditionToken(ConditionAndOr.AND);
		query.addConditionToken((s, q) -> s.appendSQL(" ("));
		return new QueryCondition<>(query, x);
	}
	
	/**
	 * wraps the entity object and changes the condition to support the primary key.
	 * Useful in cases when the object holds an entity relation but you do not want to create the relation in the
	 * fluent query.
	 *
	 * @see Db#asPrimaryKey(Object, Class)
	 * @param &lt;K&gt;
	 * @param &lt;A&gt;
	 * @param mask
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <K, A> QueryCondition<T, A> and(GenericMask<K, A> mask) {
		query.addConditionToken(ConditionAndOr.AND);
		return new QueryCondition<>(query, mask, mask.mask());
	}

	/**
	 * wraps the entity object and changes the condition to support the primary key.
	 * Useful in cases when the object holds an entity relation but you do not want to create the relation in the
	 * fluent query.<br>
	 * Adds a '(' after the 'and' operator. Close with {@link #endWrap()}
	 *
	 * @see Db#asPrimaryKey(Object, Class)
	 * @param &lt;K&gt;
	 * @param &lt;A&gt;
	 * @param mask
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <K, A> QueryCondition<T, A> andWrap(GenericMask<K, A> mask) {
		query.addConditionToken(ConditionAndOr.AND);
		query.addConditionToken((s, q) -> s.appendSQL(" ("));
		return new QueryCondition<>(query, mask, mask.mask());
	}
	
	/**
	 * Perform an 'OR' operator in the query
	 * &lt;A&gt; - represents any field type that exists on Object &lt;T&gt;
	 * 
	 * @param x - The field (gives field name) that should be attached with 'OR' to the query
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <A> QueryCondition<T, A> or(A x) {
		query.addConditionToken(ConditionAndOr.OR);
		return new QueryCondition<>(query, x);
	}

	/**
	 * Perform an 'OR' operator in the query
	 * &lt;A&gt; - represents any field type that exists on Object &lt;T&gt;<br>
	 * Adds a '(' after the 'and' operator. Close with {@link #endWrap()}
	 * 
	 * @param x - The field (gives field name) that should be attached with 'OR' to the query
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <A> QueryCondition<T, A> orWrap(A x) {
		query.addConditionToken(ConditionAndOr.OR);
		query.addConditionToken((s, q) -> s.appendSQL(" ("));
		return new QueryCondition<>(query, x);
	}
	
	/**
	 * wraps the entity object and changes the condition to support the primary key.
	 * Useful in cases when the object holds an entity relation but you do not want to create the relation in the
	 * fluent query.
	 *
	 * @see Db#asPrimaryKey(Object, Class)
	 * @param mask
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <K, A> QueryCondition<T, A> or(GenericMask<K, A> mask) {
		query.addConditionToken(ConditionAndOr.OR);
		return new QueryCondition<>(query, mask, mask.mask());
	}

	/**
	 * wraps the entity object and changes the condition to support the primary key.
	 * Useful in cases when the object holds an entity relation but you do not want to create the relation in the
	 * fluent query.<br>
	 * Adds a '(' after the 'and' operator. Close with {@link #endWrap()}
	 * 
	 * @see Db#asPrimaryKey(Object, Class)
	 * @param mask
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <K, A> QueryCondition<T, A> orWrap(GenericMask<K, A> mask) {
		query.addConditionToken(ConditionAndOr.OR);
		query.addConditionToken((s, q) -> s.appendSQL(" ("));
		return new QueryCondition<>(query, mask, mask.mask());
	}
	
	/**
	 * a "where exists" clause. Adds "WHERE EXISTS (subQuery)" to the query
	 * @param subQuery
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> whereExists(QueryWhere<?> subQuery) {
		query.addConditionToken(new ExistsToken(subQuery));
		return this;
	}
	
	/**
	 * a "where not exists" clause. Adds "WHERE NOT EXISTS (subQuery)" to the query
	 * @param subQuery
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> whereNotExists(QueryWhere<?> subQuery) {
		query.addConditionToken((s, q) -> s.appendSQL("NOT"));
		query.addConditionToken(new ExistsToken(subQuery));
		return this;
	}
	
	/**
	 * a "and exists" clause. Adds "AND EXISTS (subQuery)" to the query
	 * @param subQuery
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> andExists(QueryWhere<?> subQuery) {
		query.addConditionToken(ConditionAndOr.AND);
		query.addConditionToken(new ExistsToken(subQuery));
		return this;
	}
	
	/**
	 * a "or exists" clause. Adds "OR EXISTS (subQuery)" to the query
	 * @param subQuery
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> orExists(QueryWhere<?> subQuery) {
		query.addConditionToken(ConditionAndOr.OR);
		query.addConditionToken(new ExistsToken(subQuery));
		return this;
	}
	
	/**
	 * a "and not exists" clause. Adds "AND NOT EXISTS (subQuery)" to the query
	 * @param subQuery
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> andNotExists(QueryWhere<?> subQuery) {
		query.addConditionToken((s, q) -> s.appendSQL("AND NOT"));
		query.addConditionToken(new ExistsToken(subQuery));
		return this;
	}
	
	/**
	 * a "or not exists" clause. Adds "OR NOT EXISTS (subQuery)" to the query
	 * @param subQuery
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> orNotExists(QueryWhere<?> subQuery) {
		query.addConditionToken((s, q) -> s.appendSQL("OR NOT"));
		query.addConditionToken(new ExistsToken(subQuery));
		return this;
	}
	
	/**
	 * Create a having clause based on the column given.
	 * <b>You can only use a single having in a select clause</b>
	 *
	 * @param x
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <A> QueryCondition<T, A> having(final A x) {
		return query.having(x);
	}

	/**
	 * Create a having clause based on the column given.
	 * <br>
	 * Adds a '(' after the 'and' operator. Close with {@link #endWrap()}
	 * 
	 * @param x
	 * @return QueryCondition&lt;T, A&gt;
	 */
	public <A> QueryCondition<T, A> havingWrap(final A x) {
		return query.havingWrap(x);
	}
	
	/**
	 * having clause with a supported aggregate function
	 * @param function
	 * @param x
	 * @return QueryCondition&lt;T, Long&gt;
	 */
	public <A> QueryCondition<T, Long> having(HavingFunctions function, final A x) {
		return query.having(function, x);
	}
	
	/**
	 * having clause with a supported aggregate function<br>
	 * Adds a '(' after the 'and' operator. Close with {@link #endWrap()}
	 * 
	 * @param function
	 * @param x
	 * @return QueryCondition&lt;T, Long&gt;
	 */
	public <A> QueryCondition<T, Long> havingWrap(HavingFunctions function, final A x) {
		return query.havingWrap(function, x);
	}

	/**
	 * wraps everything following with "("<br>
	 * <b>Must follow with matching "endWrap</b>
	 *
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> wrap() {
    	query.addConditionToken((s, q) -> s.appendSQL("("));
    	return this;
    }

	/**
	 * ends a previous wrap with ")"
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> endWrap() {
    	query.addConditionToken((s, q) -> s.appendSQL(")"));
    	return this;
    }

	/**
	 * Performs the select of the query. Returns the results or an empty list. Does not return null.
	 * This select returns a List of a given object that mapping is given to from the result set to the field in that object
	 *
	 * @param &lt;X&gt;
	 * @param &lt;Z&gt;
	 * @param x
	 * @return List&lt;Z&gt;
	 */
	public <Z> List<Z> select(Z x) {
		return query.select(x);
	}

	/**
	 * Returns the SQL String to be performed. Use for Debug.
	 * @return String
	 */
	public String getSQL() {
		return query.getSQL();
	}

	/**
	 * @see Query#getDistinctSQL()
	 * @return String
	 */
	public String getDistinctSQL() {
		return query.getDistinctSQL();
	}

	/**
	 * @see Query#getSQL(Object)
	 * @param z
	 * @return String
	 */
	public <Z> String getSQL(Z z) {
		return query.getSQL(z);
	}

	/**
	 * @see Query#getDistinctSQL(Object)
	 * @param z
	 * @return String
	 */
	public <Z> String getDistinctSQL(Z z) {
		return query.getDistinctSQL(z);
	}

	/**
	 * Returns the SQL String to be performed. Use for Debug.
	 * @return String
	 */
	public String logSQL() {
		return query.logSQL();
	}

	/**
	 * @see Query#logDistinctSQL()
	 * @return String
	 */
	public String logDistinctSQL() {
		return query.logDistinctSQL();
	}

	/**
	 * @see Query#logSQL(Object)
	 * @param z
	 * @return String
	 */
	public <Z> String logSQL(Z z) {
		return query.logSQL(z);
	}

	/**
	 * @see Query#logDistinctSQL(Object)
	 * @param z
	 * @return String
	 */
	public <Z> String logDistinctSQL(Z z) {
		return query.logDistinctSQL(z);
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
		return query.union(unionQuery);
	}

	/**
	 * Returns a Map made up of a single union query
	 * 
	 * @param &lt;U&gt;
	 * @param &lt;K&gt;
	 * @param &lt;V&gt;
	 * @param unionQuery
	 * @param key - the field in the main query that functions as key
	 * @param value - the field in the main query that functions as value
	 * @param unionKey - the field in the union query that functions as key
	 * @param unionValue - the field in the union query that functions as value
	 * @return Map&lt;K, V&gt;
	 */
	public <U, K, V> Map<K, V> unionAsMap(Query<U> unionQuery, K key, V value, K unionKey, V unionValue) {
		return query.unionAsMap(unionQuery, key, value, unionKey, unionValue);
	}
	
	/**
	 * Returns a Map made up of a single union query applying distinct to each query
	 * 
	 * @param &lt;U&gt;
	 * @param &lt;K&gt;
	 * @param &lt;V&gt;
	 * @param unionQuery
	 * @param key - the field in the main query that functions as key
	 * @param value - the field in the main query that functions as value
	 * @param unionKey - the field in the union query that functions as key
	 * @param unionValue - the field in the union query that functions as value
	 * @return Map&lt;K, V&gt;
	 */
	public <U, K, V> Map<K, V> unionAsMapDistinct(Query<U> unionQuery, K key, V value, K unionKey, V unionValue) {
		return query.unionAsMap(unionQuery, key, value, unionKey, unionValue);
	}
	
	/**
	 * Returns a Map made up of a single union query
	 * 
	 * @param &lt;U&gt;
	 * @param &lt;K&gt;
	 * @param &lt;V&gt;
	 * @param unionQuery
	 * @param key - the field in the main query that functions as key
	 * @param value - the field in the main query that functions as value
	 * @param unionKey - the field in the union query that functions as key
	 * @param unionValue - the field in the union query that functions as value
	 * @return Map&lt;K, V&gt;
	 */
	public <U, K, V> Map<K, V> unionAsMap(QueryWhere<U> unionQuery, K key, V value, K unionKey, V unionValue) {
		return query.unionAsMap(unionQuery, key, value, unionKey, unionValue);
	}
	
	/**
	 * Returns a Map made up of a single union query applying distinct to each query
	 * 
	 * @param &lt;U&gt;
	 * @param &lt;K&gt;
	 * @param &lt;V&gt;
	 * @param unionQuery
	 * @param key - the field in the main query that functions as key
	 * @param value - the field in the main query that functions as value
	 * @param unionKey - the field in the union query that functions as key
	 * @param unionValue - the field in the union query that functions as value
	 * @return Map&lt;K, V&gt;
	 */
	public <U, K, V> Map<K, V> unionAsMapDistinct(QueryWhere<U> unionQuery, K key, V value, K unionKey, V unionValue) {
		return query.unionAsMap(unionQuery, key, value, unionKey, unionValue);
	}
	
	/**
	 * Returns a Map made up of a single union query
	 * 
	 * @param &lt;U&gt;
	 * @param &lt;K&gt;
	 * @param &lt;V&gt;
	 * @param unionQuery
	 * @param key - the field in the main query that functions as key
	 * @param value - the field in the main query that functions as value
	 * @param unionKey - the field in the union query that functions as key
	 * @param unionValue - the field in the union query that functions as value
	 * @return Map&lt;K, V&gt;
	 */
	public <U, K, V> Map<K, V> unionAsMap(QueryJoinWhere<U> unionQuery, K key, V value, K unionKey, V unionValue) {
		return query.unionAsMap(unionQuery, key, value, unionKey, unionValue);
	}
	
	/**
	 * Returns a Map made up of a single union query applying distinct to each query
	 * 
	 * @param &lt;U&gt;
	 * @param &lt;K&gt;
	 * @param &lt;V&gt;
	 * @param unionQuery
	 * @param key - the field in the main query that functions as key
	 * @param value - the field in the main query that functions as value
	 * @param unionKey - the field in the union query that functions as key
	 * @param unionValue - the field in the union query that functions as value
	 * @return Map&lt;K, V&gt;
	 */
	public <U, K, V> Map<K, V> unionAsMapDistinct(QueryJoinWhere<U> unionQuery, K key, V value, K unionKey, V unionValue) {
		return query.unionAsMap(unionQuery, key, value, unionKey, unionValue);
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
		return query.union(unionQuery.query);
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
		return query.union(unionQuery);
	}

	/**
	 * same as {@link #union(QueryJoinWhere)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U> List<T> unionDistinct(QueryJoinWhere<U> unionQuery) {
		return query.unionDistinct(unionQuery);
	}
	
	/**
	 * same as {@link #union(QueryWhere)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U> List<T> unionDistinct(QueryWhere<U> unionQuery) {
		return query.unionDistinct(unionQuery);
	}

	/**
	 * same as {@link #union(Query)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;T&gt;
	 */
	public <U> List<T> unionDistinct(Query<U> unionQuery) {
		return query.unionDistinct(unionQuery);
	}

	/**
	 * same as {@link #union(QueryJoinWhere, Object)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;X&gt;
	 */
	public <U, X> List<X> unionDistinct(QueryJoinWhere<U> unionQuery, X x) {
		return query.unionDistinct(unionQuery, x);
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
		return query.union(unionQuery, x);
	}

	/**
	 * same as {@link #union(QueryWhere, Object)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @param x
	 * @return List&lt;X&gt;
	 */
	public <U, X> List<X> unionDistinct(QueryWhere<U> unionQuery, X x) {
		return query.unionDistinct(unionQuery, x);
	}

	/**
     * Returns a list of the given type (x). The type must be a new type, not one of the table's fields.
     * this query is runs a union query of the two queries.<br>
	 * <b>Note:</b> All union query rules apply here. The queries must return the same amount of columns and have the same column types and names.
	 *
	 * @param unionQuery
	 * @param x - the type to return
	 * @return List&lt;Z&gt;
	 */
	public <U, Z> List<Z> union(QueryWhere<U> unionQuery, Z x) {
		return query.union(unionQuery, x);
	}

	/**
	 * same as {@link #union(Query, Object)} but returns distinct results of both queries.
	 *
	 * @param unionQuery
	 * @return List&lt;X&gt;
	 */
	public <U, X> List<X> unionDistinct(Query<U> unionQuery, X x) {
		return query.unionDistinct(unionQuery, x);
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
		return query.union(unionQuery, x);
	}

	/**
	 * Performs A select similar to {@link #select(Object)} but with the 'DISTINCT' directive.
	 * Returns results or empty List. Never 'null'
	 *
	 * @param x
	 * @return List&lt;Z&gt;
	 */
	public <Z> List<Z> selectDistinct(Z x) {
		return query.selectDistinct(x);
	}

	/**
	 * Returns the 'Z' type object from the first result
	 *
	 * @param x
	 * @return a result or null if there is no result
	 */
	public <Z> Z selectFirst(Z x) {
		List<Z> list = query.select(x);
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * Perform the built query Select.
	 *
	 * @return List&lt;T&gt; can be a list of one, many or empty, never 'null'. Can be used in a primary key select, but using
	 *         {@link #selectFirst} is advised.
	 */
	public List<T> select() {
		return query.select();
	}

	/**
	 * Select the first result and return it. Should also be used in a primary key select.
	 *
	 * @return T result or null if there is no result
	 */
	public T selectFirst() {
		List<T> list = select();
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * Select only distinct results in the table
	 *
	 * @return List&lt;T&gt;
	 */
	public List<T> selectDistinct() {
		return query.selectDistinct();
	}

	/**
	 * Returns a map of all the query's results where key is one field in the select and value is another.<br>
	 * It is also available in join queries. You can have a key from any table within the join and a value from any table as well.<br>
	 * <b>Note:</b> If keys are not unique they will override.<br>
	 * example:
	 * <pre>
	 * Db sb = [new session];
	 * Table t = [tableDescriptor]
	 * Map&lt;Long, String&gt; results = db.from(t).where(t.[getSomeField()]).is)[someValue]....selectAsMap(t.getA(), t.getB());
	 * </pre>
	 *
	 * @param key
	 * @param value
	 * @return Map&lt;K, V&gt;
	 */
	public <K, V> Map<K, V> selectAsMap(K key, V value){
		return query.selectAsMap(key, value);
	}

	/**
	 * same as {@link #selectAsMap(Object, Object)} but returns only distinct results.
	 *
	 * @see #selectAsMap(Object, Object)
	 * @param key
	 * @param value
	 * @return Map&lt;K, V&gt;
	 */
	public <K, V> Map<K, V> selectDistinctAsMap(K key, V value){
		return query.selectDistinctAsMap(key, value);
	}

	/**
	 * A convenience method to get the object representing the right hand side of the join relationship only (without the need to specify the mapping between fields)
	 * Returns a list of results, of the given type. The given type must be a part of a join query or an exception will be thrown
	 *
	 * @param table - the object descriptor of the type needed on return
	 * @throws EzquError - when not in join query
	 * @return List&lt;U&gt;
	 */
	public <U> List<U> selectRightHandJoin(U table) {
		return query.selectRightHandJoin(table);
	}

	/**
	 * A convenience method to a field of get the object representing the right hand side of the join relationship only. Based on a single field
	 * Returns a list of results, of the given type. The given type must be a part of a join query or an exception will be thrown
	 *
	 * @param table - the object descriptor of the type needed on return
	 * @param x
	 * @throws EzquError - when not in join query
	 * @return List&lt;Z&gt;
	 */
	public <U, Z> List<Z> selectRightHandJoin(U table, Z x) {
		return query.selectRightHandJoin(table, x);
	}

    /**
     * A convenience method to get the object representing the right hand side of the join relationship only (without the need to specify the mapping between fields)
     * Returns the first result of a list of results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param table - the object descriptor of the type needed on return
     * @throws EzquError - when not in join query
     * @return U
     */
	public <U> U selectFirstRightHandJoin(U table) {
		return query.selectFirstRightHandJoin(table);
	}

	/**
     * A convenience method to get a field of the object representing the right hand side of the join relationship only. Based on a single field
     * Returns the first result of a list of results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param table - the object descriptor of the type needed on return
     * @param x
     * @throws EzquError - when not in join query
     * @return Z
     */
	public <U, Z> Z selectFirstRightHandJoin(U table, Z x) {
		return query.selectFirstRightHandJoin(table, x);
	}

    /**
     * A convenience method to get the object representing the right hand side of the join relationship only (without the need to specify the mapping between fields)
     * Returns a list of distinct results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param table - the object descriptor of the type needed on return
     * @throws EzquError - when not in join query
     * @return List&lt;U&gt;
     */
	public <U> List<U> selectDistinctRightHandJoin(U table) {
		return query.selectDistinctRightHandJoin(table);
	}

	/**
     * A convenience method to get a field of the object representing the right hand side of the join relationship only. Based on a single field
     * Returns a list of distinct results, of the given type. The given type must be a part of a join query or an exception will be thrown
     *
     * @param table - the object descriptor of the type needed on return
     * @param x
     * @throws EzquError - when not in join query
     * @return List&lt;Z&gt;
     */
	public <U, Z> List<Z> selectDistinctRightHandJoin(U table, Z x) {
		return query.selectDistinctRightHandJoin(table, x);
	}

	/**
	 * Group By ordered objects
	 * 
	 * @param groupBy
	 * @return Query&lt;T&gt;
	 */
	public Query<T> groupBy(Object ... groupBy){
		return query.groupBy(groupBy);
	}

	/**
	 * adds a limit to the query
	 * 
	 * @param limitNum
	 * @return Query&lt;T&gt;
	 */
	public Query<T> limit(int limitNum) {
		return query.limit(limitNum);
	}

	/**
	 * Order by one or more columns in ascending order.
	 * <b>
	 * <b>Note:</b> You can send a {@link F} token and use a case when switch
	 *
	 * @param exprs the order by expressions
	 * @return QueryWhere&lt;T&gt; -  the query
	 */
	public QueryWhere<T> orderBy(Object... exprs) {
		if (null != exprs) {
			for (Object expr : exprs) {
				addOrderBy(expr, false, null);
			}
		}
		return this;
	}
	
	/**
	 * Order by one or more columns in descending order.
	 * <p> 
	 * <b>Important</b> Case / When order by clause will never return null, thus, using
	 * this method for such is redundant and not supported.
	 * 
	 * @param exprs
	 * @return QueryWhere&lt;T&gt; - the query
	 */
	public QueryWhere<T> orderByNullsFirst(Object ... exprs) {
		if (null != exprs) {
			for (Object expr : exprs) {
				addOrderBy(expr, false, true);
			}
		}
		return this;
	}

	/**
	 * Order by one or more columns in ascending order
	 * <p> 
	 * <b>Important</b> Case / When order by clause will never return null, thus, using
	 * this method for such is redundant and not supported.
	 * 
	 * @param exprs
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> orderByNullsLast(Object ... exprs) {
		if (null != exprs) {
			for (Object expr : exprs) {
				addOrderBy(expr, false, false);
			}
		}
		return this;
	}

	/**
	 * Order by in descending order
	 * <b>
	 * <b>Note:</b> You can send a {@link F} token and use a case when switch
	 * 
	 * @param exprs
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> orderByDesc(Object ... exprs) {
		if (null != exprs) {
			for (Object expr : exprs) {
				addOrderBy(expr, true, null);
			}
		}
		return this;
	}
	
	/**
	 * Order by in descending order nulls will be first
	 * <p> 
	 * <b>Important</b> Case / When order by clause will never return null, thus, using
	 * this method for such is redundant and not supported.

	 * 
	 * @param exprs
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> orderByDescNullsFirst(Object ... exprs) {
		if (null != exprs) {
			for (Object expr : exprs) {
				addOrderBy(expr, true, true);
			}
		}
		return this;
	}

	/**
	 * Order by in descending order nulls will be last
	 * <p> 
	 * <b>Important</b> Case / When order by clause will never return null, thus, using
	 * this method for such is redundant and not supported.
	 * 
	 * @param exprs
	 * @return QueryWhere&lt;T&gt;
	 */
	public QueryWhere<T> orderByDescNullsLast(Object ... exprs) {
		if (null != exprs) {
			for (Object expr : exprs) {
				addOrderBy(expr, true, false);
			}
		}
		return this;
	}

	/**
	 * Deletes the rows / entities that corresponds to the query.
	 * @return int - number of rows affected
	 */
	public int delete() {
		return query.delete();
	}

	/**
	 * Updates the rows / entities that corresponds to the query.
	 * @return int - number of rows affected
	 */
	public int update() {
		return query.update();
	}

	/**
	 * returns the count of rows / entities that correspond to the query.
	 * @return int - the count
	 */
	public long selectCount() {
		return query.selectCount();
	}
	
	private void addOrderBy(Object expr, boolean desc, Boolean nullsFirst) {
		if (query.getDb().getToken(expr) instanceof CaseWhenToken t) {
			// this means this was a case when
			if (desc) {
				query.addOrderBy((stat, q) -> {
					t.appendSQL(stat, q);
					stat.appendSQL(" DESC");
				});
			}
			else
				query.addOrderBy(t);			
		}
		else {
			OrderExpression e = new OrderExpression(expr, desc, nullsFirst);
			query.addOrderBy(e);
		}
	}
}
