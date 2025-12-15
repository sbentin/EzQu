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

import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;

/**
 * A condition contains one or two operands and a compare operation.
 *
 * @param <A> the operand type
 */
class Condition<A> implements Token {
    CompareType compareType;
    A y;
    Object key;

	Condition(Object x, A y, CompareType compareType) {
        this.compareType = compareType;
        if (x instanceof GenericMask mask) {
        	this.key = mask.orig();
        }
        else {
        	this.key = x;
        }
        this.y = y;
    }

    @Override
    public void appendSQL(SQLStatement stat, Query<?> query) {
        // 1. Append left side
        appendProperValue(stat, query, key, false);

        // 2. Separator & comparison operator
        stat.appendSQL(" ").appendSQL(compareType.getString());

        // 3. Append right side if it exists
        if (compareType.hasRightExpression()) {
            stat.appendSQL(" ");
            appendProperValue(stat, query, y, true);
        }
    }
    
    private void appendProperValue(SQLStatement stat, Query<?> q, Object obj, boolean checkEnum) {
        if (obj == null) {
            q.appendSQL(stat, obj, false, null);
            return;
        }

        Class<?> cl = obj.getClass();
        boolean isEntity = cl.getAnnotation(Entity.class) != null ||
                           cl.getAnnotation(MappedSuperclass.class) != null;

        if (isEntity) {
            Object pk = q.getDb().factory.getPrimaryKey(obj);
            q.appendSQL(stat, pk != null ? pk : obj, false, null);
            return;
        }

        if (checkEnum && (cl.isEnum() || cl.getSuperclass().isEnum())) {
            // We rely on the field that owns the enum to decide how to render it
            FieldDefinition fd = q.getSelectColumn(key).getFieldDefinition();
            switch (fd.type) {
                case ENUM -> q.appendSQL(stat, obj.toString(), false, null);
                case ENUM_INT -> q.appendSQL(stat, ((Enum<?>)obj).ordinal(), false, null);
                case UUID -> q.appendSQL(stat, obj.toString(), false, null);
                default -> q.appendSQL(stat, obj, false, null);
            }
            return;
        }

        // Default fallback
        q.appendSQL(stat, obj, false, null);
    }
}
