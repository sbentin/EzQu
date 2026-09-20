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

import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;

/**
 * @author shai
 * @param &lt;X&gt;
 */
@SuppressWarnings("java:S2326")
public final class F<X> implements Token {

	private static final String THEN = " then ";
	private static final String END = " end ";
	private static final String WHEN = " when ";
	
	private Token token;
	
	private F() {
		/* This utility class should not be instantiated */
	}
	
	@Override
	public void appendSQL(SQLStatement stat, Query<?> query) {
		token.appendSQL(stat, query);
	}

	/**
	 * returns a when clause renderer that compares the value in column x to y.<br>
	 * i.e when x = y, when x > y, when <> y ... IN, NOTIN are not supported use {@link #when(Object, Object[], CompareType)}
	 * 
	 * @param <A>
	 * @param x
	 * @param y
	 * @param operation
	 * @return F&lt;A&gt;
	 */
	public static <A> F<A> when(A x, A y, CompareType operation) {
		F<A> f = new F<>();
		f.token = new Condition<>(x, y, operation) {

			@Override
			public void appendSQL(SQLStatement stat, Query<?> query) {
				stat.appendSQL(WHEN);
				super.appendSQL(stat, query);
			}
		};
		return f;
	}

	/**
	 * returns a when clause renderer that compares the value in column x to y.<br>
	 * i.e when x in y[], when x not in y[],only supports IN, NOT IN other operations use {@link #when(Object, Object, CompareType)}
	 * 
	 * @param <A>
	 * @param x
	 * @param y
	 * @param operation
	 * @return F&lt;A&gt;
	 */
	public static <A> F<A> when(A x, A[] y, CompareType operation) {
		F<A> f = new F<>();
		f.token = new InCondition<>(x, y, operation) {

			@Override
			public void appendSQL(SQLStatement stat, Query<?> query) {
				stat.appendSQL(WHEN);
				super.appendSQL(stat, query);
			}
		};
		return f;
	}
	
	/**
	 * This is used for building structures like this:
	 * <pre>
	 * case someColumn
	 * 	when value then result
	 * 	when value then result
	 * 	else result
	 * end
	 * </pre>
	 * <b>Note</b>
	 * <ul>
	 * <li>Only one can be used in caseWhen function</li>
	 * <li>Only enum names are supported (no enum ordinal ints)</li>
	 * <li>Only primitive types can be used.</li>
	 * </ul>
	 * 
	 * @param <A>
	 * @param x
	 * @param whenOptions
	 * @param thenOptions
	 * @param elseOption
	 * @return F&lt;A&gt;
	 */
	public static <A> F<A> whenThen(A x, A[] whenOptions, Object[] thenOptions) {
		if (whenOptions.length != thenOptions.length)
			throw new EzquError("When and then option must be paired!!!");
		F<A> f = new F<>();
		f.token = (stat, query) -> {
			stat.appendSQL(" ");
			appendProperValue(stat, query, x, false);
			boolean isString = (whenOptions[0] instanceof String) || whenOptions[0].getClass().isEnum();
			boolean isThenString = (thenOptions[0] instanceof String) || thenOptions[0].getClass().isEnum();
			for (int i = 0; i < whenOptions.length; i++) {
				String whenVal = whenOptions[i].toString();
				stat.appendSQL(WHEN + (isString ? "'" + whenVal + "'": whenVal));
				String val = thenOptions[i].toString();
				stat.appendSQL(THEN + (isThenString ? "'" + val + "'": val));
			}
		};
		return f;
	}

	/**
	 * Allows nesting an entire case inside a then clause. Use "F.when", "F.then" for nesting, or F.whenThen
	 * @param <A>
	 * @param nestedWhen
	 * @return F&lt;A&gt;
	 */
	public static <A> F<A> then(final F<?> ... nestedWhenThen) {
		if (nestedWhenThen.length == 0)
    		throw new EzquError("At least one when option with when/then values or paired 'when' and 'then' options must supplied!!!");
		F<A> f;
    	if (nestedWhenThen.length == 1) {
    		f = new F<>();
    		f.token = (stat, query) -> {
    			stat.appendSQL(" then case");
    			nestedWhenThen[0].appendSQL(stat, query);
    			stat.appendSQL(END);
    		};
    	}
    	else {
    		if ((nestedWhenThen.length % 2) != 0) {
    			throw new EzquError("none whenThen case functions must come in when, then pairs");
    		}
    		f = new F<>();
    		f.token = (stat, query) -> {
    			stat.appendSQL(" then case");
    			for (int i = 0; i < nestedWhenThen.length; i+=2) {
        			nestedWhenThen[i].appendSQL(stat, query);
        			nestedWhenThen[i + 1].appendSQL(stat, query);
        		}
    			stat.appendSQL(END);
    		};    		
    	}
		return f;
	}

	/**
	 * returns a then simple value
	 * 
	 * @param <A>
	 * @param thenOutput
	 * @return F&lt;A&gt;
	 */
	public static <A> F<A> then(A thenOutput) {
		F<A> f = new F<>();		
		f.token = (stat, query) -> {
			stat.appendSQL(THEN);
			if (thenOutput.getClass().isEnum())
				stat.appendSQL("'" + ((Enum<?>)thenOutput).name() + "'");
			else
				query.appendSQL(stat, thenOutput, false, null);
		};
		return f;
	}

	/**
	 * Allows nesting an entire case inside a then clause. Use "F.when", "F.then" for nesting, or F.whenThen
	 * @param <A>
	 * @param nestedWhen
	 * @return F&lt;A&gt;
	 */
	public static <A> F<A> elseCase(final F<?> ... nestedWhenThen) {
		if (nestedWhenThen.length == 0)
    		throw new EzquError("At least one when option with when/then values or paired 'when' and 'then' options must supplied!!!");
		F<A> f;
    	if (nestedWhenThen.length == 1) {
    		f = new F<>();
    		f.token = (stat, query) -> {
    			stat.appendSQL(" else case ");
    			nestedWhenThen[0].appendSQL(stat, query);
    			stat.appendSQL(END);
    		};
    	}
    	else {
    		if ((nestedWhenThen.length % 2) != 0) {
    			throw new EzquError("none whenThen case functions must come in when, then pairs");
    		}
    		f = new F<>();
    		f.token = (stat, query) -> {
    			stat.appendSQL(" else case ");
    			for (int i = 0; i < nestedWhenThen.length; i+=2) {
        			nestedWhenThen[i].appendSQL(stat, query);
        			nestedWhenThen[i + 1].appendSQL(stat, query);
        		}
    			stat.appendSQL(END);
    		};    		
    	}
		return f;
	}

	/**
	 * returns a then simple value
	 * 
	 * @param <A>
	 * @param elseOutput
	 * @return F&lt;A&gt;
	 */
	public static <A> F<A> elseVal(A elseOutput) {
		F<A> f = new F<>();
		f.token = (stat, query) -> {
			stat.appendSQL(" else " );
			if (elseOutput.getClass().isEnum()) {
				stat.appendSQL("'" + ((Enum<?>)elseOutput).name() + "'");
			}
			else
				query.appendSQL(stat, elseOutput, false, null);
		};
		return f;
	}
	
	private static void appendProperValue(SQLStatement stat, Query<?> q, Object obj, boolean checkEnum) {
		if (null == obj) {
			q.appendSQL(stat, obj, false, null);
			return;
		}

		Class<?> cl = obj.getClass();
		boolean isEntity = cl.getAnnotation(Entity.class) != null || cl.getAnnotation(MappedSuperclass.class) != null;

		if (isEntity) {
			Object pk = q.getDb().factory.getPrimaryKey(obj);
			q.appendSQL(stat, pk != null ? pk : obj, false, null);
			return;
		}

		if (checkEnum && (cl.isEnum() || cl.getSuperclass().isEnum())) {
			// We rely on the field that owns the enum to decide how to render it
			FieldDefinition fd = q.getSelectColumn(obj).getFieldDefinition();
			switch (fd.type) {
				case ENUM -> q.appendSQL(stat, obj.toString(), false, null);
				case ENUM_INT -> q.appendSQL(stat, ((Enum<?>) obj).ordinal(), false, null);
				case UUID -> q.appendSQL(stat, obj.toString(), false, null);
				default -> q.appendSQL(stat, obj, false, null);
			}
			return;
		}

		// Default fallback
		q.appendSQL(stat, obj, false, null);
	}
}
