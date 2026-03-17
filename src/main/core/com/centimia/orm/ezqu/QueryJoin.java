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
 * This class represents a query with a join.
 * 
 * @param <T> - the entity to join to
 * @author shai
 */
public class QueryJoin<T> {

    private Query<T> query;
    private SelectTable<T> join;

    QueryJoin(Query<T> query, SelectTable<T> join) {
        this.query = query;
        this.join = join;
    }

    /**
     * Use this method when the object questioned holds an Object and you want to save a join and use the other sides
     * primaryKey instead.
     * 
     * @param <A> - the type of primary key
     * @param x - the mask used to retrieve the PrimaryKey from the object
     * @return QueryJoinCondition&lt;T, A&gt;
     */
    public <A> QueryJoinCondition<T, A> on(GenericMask<?, A> x) {
        return new QueryJoinCondition<>(query, join, x);
    }

    /**
     * Use this method when connecting on an entity object
     * 
     * @param <A> - the type of column
     * @param x - the target object entity to join on
     * @return QueryJoinCondition&lt;T, A&gt;
     */
    public <A> QueryJoinCondition<T, A> on(A x) {
        return new QueryJoinCondition<>(query, join, x);
    }
}