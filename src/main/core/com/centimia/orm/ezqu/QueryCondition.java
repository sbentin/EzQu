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
 * This class represents a query with an incomplete condition.
 *
 * @param <T> the return type of the query
 * @param <A> the incomplete condition data type
 */
public class QueryCondition<T, A> {

    protected Query<T> query;
    protected Object x;

	QueryCondition(Query<T> query, GenericMask<?, A> x, Class<A> maskedType) {
        this.query = query;
        this.x = x;
    }

    QueryCondition(Query<T> query, A x) {
        this.query = query;
        this.x = x;
    }

    public QueryWhere<T> is(A y) {
    	if (null == y)
    		return isNull();
   		query.addConditionToken(new Condition<>(x, y, CompareType.EQUAL));
        return new QueryWhere<>(query);
    }

    public QueryWhere<T> isNot(A y) {
    	if (null == y)
    		throw new EzquError("Cannot compare column to null using `isNot()`. Use `isNotNull()` instead. If null was unexpected fix your code!!!");
    	query.addConditionToken(new Condition<>(x, y, CompareType.NOT_EQUAL));
        return new QueryWhere<>(query);
    }

    public QueryWhere<T> in(A[] y) {
    	if (null == y || 0 == y.length)
    		throw new EzquError("Cannot call in() with null or empty list");
    	query.addConditionToken(new InCondition<>(x, y, CompareType.IN));
    	return new QueryWhere<>(query);
    }

    public QueryWhere<T> notIn(A[] y) {
    	if (null == y || 0 == y.length)
    		throw new EzquError("Cannot call notIn() with null or empty list");
    	query.addConditionToken(new InCondition<>(x, y, CompareType.NOT_IN));
    	return new QueryWhere<>(query);
    }

    public QueryWhere<T> bigger(A y) {
    	if (null == y)
    		throw new EzquError("Cannot compare column to null using `bigger()`. Null was unexpected fix your code!!!");
        query.addConditionToken(new Condition<>(x, y, CompareType.BIGGER));
        return new QueryWhere<>(query);
    }

    public QueryWhere<T> biggerEqual(A y) {
    	if (null == y)
    		throw new EzquError("Cannot compare column to null using `biggerEqual()`. Null was unexpected fix your code!!!");
        query.addConditionToken(new Condition<>(x, y, CompareType.BIGGER_EQUAL));
        return new QueryWhere<>(query);
    }

    public QueryWhere<T> smaller(A y) {
    	if (null == y)
    		throw new EzquError("Cannot compare column to null using `smaller()`. Null was unexpected fix your code!!!");
        query.addConditionToken(new Condition<>(x, y, CompareType.SMALLER));
        return new QueryWhere<>(query);
    }

    public QueryWhere<T> smallerEqual(A y) {
    	if (null == y)
    		throw new EzquError("Cannot compare column to null using `smallerEqual()`. Null was unexpected fix your code!!!");
        query.addConditionToken(new Condition<>(x, y, CompareType.SMALLER_EQUAL));
        return new QueryWhere<>(query);
    }

    /**
     * Like allows the 'LIKE' query. depending on the query string given. If '%' is used in the 'pattern' it will effect the result.
     *
     * @param pattern the pattern to check against.
     * @return QueryWhere&lt;T&gt;
     */
    public QueryWhere<T> like(A pattern) {
    	if (null == pattern)
    		throw new EzquError("Cannot compare column to null using `like()`. Null was unexpected fix your code!!!");
        query.addConditionToken(new Condition<>(x, pattern, CompareType.LIKE));
        return new QueryWhere<>(query);
    }

    /**
     * Although @{link {@link QueryCondition#like(Object)} allows the use of '%' within the
     * pattern sometime the user will want to specify it without changing the original pattern or might
     * have no control over the pattern. This method does like '%[pattern]'.
     *
     * @param pattern
     * @return QueryWhere&lt;T&gt;
     */
    public QueryWhere<T> startingWith(A pattern) {
    	if (null == pattern)
    		throw new EzquError("Cannot compare column to null using `like()`. Null was unexpected fix your code!!!");
        query.addConditionToken(new LikeCondition<>(x, pattern, LikeMode.START));
        return new QueryWhere<>(query);
    }

    /**
     * Although @{link {@link QueryCondition#like(Object)} allows the use of '%' within the
     * pattern sometime the user will want to specify it without changing the original pattern or might
     * have no control over the pattern. This method does like '[pattern]%'.
     *
     * @param pattern
     * @return QueryWhere&lt;T&gt;
     */
    public QueryWhere<T> endingWith(A pattern) {
    	if (null == pattern)
    		throw new EzquError("Cannot compare column to null using `like()`. Null was unexpected fix your code!!!");
        query.addConditionToken(new LikeCondition<>(x, pattern, LikeMode.END));
        return new QueryWhere<>(query);
    }
    
    /**
     * Although @{link {@link QueryCondition#like(Object)} allows the use of '%' within the
     * pattern sometime the user will want to specify it without changing the original pattern or might
     * have no control over the pattern. This method does like '%[pattern]%'.
     *
     * @param pattern
     * @return QueryWhere&lt;T&gt;
     */
    public QueryWhere<T> within(A pattern) {
    	if (null == pattern)
    		throw new EzquError("Cannot compare column to null using `like()`. Null was unexpected fix your code!!!");
        query.addConditionToken(new LikeCondition<>(x, pattern, LikeMode.ANYWHERE));
        return new QueryWhere<>(query);
    }
    
    /**
     * A condition representing a check for is not null
     * 
     * @return QueryWhere&lt;T&gt;
     */
    public QueryWhere<T> isNotNull() {
        query.addConditionToken(new Condition<A>(x, null, CompareType.IS_NOT_NULL));
        return new QueryWhere<>(query);
    }

    /**
     * A condition representing a check for is null
     * 
     * @return QueryWhere&lt;T&gt;
     */
    public QueryWhere<T> isNull() {
        query.addConditionToken(new Condition<A>(x, null, CompareType.IS_NULL));
        return new QueryWhere<>(query);
    }

    /**
     * 
     * @param y
     * @return QueryCondition&lt;T, A&gt;
     */
    @SuppressWarnings("unchecked")
	public QueryBetween<T, A> between(A y){
    	// here we don't add a condition we will do it after we have all the data for both left and right of the between
    	if (x instanceof GenericMask)
    		return new QueryBetween<>(query, (GenericMask<T, A>)x, y);
    	return new QueryBetween<>(query, (A)x, y);
    }
    
    /**
     * Opens a '(' parenthesis
     * 
     * @return QueryCondition&lt;T, A&gt;
     */
    @SuppressWarnings({ "unchecked", "hiding" })
	public <A> QueryCondition<T, A> wrap() {
    	query.addConditionToken((s, q) -> s.appendSQL("("));
    	return new QueryCondition<>(this.query, (A)this.x);
    }
}