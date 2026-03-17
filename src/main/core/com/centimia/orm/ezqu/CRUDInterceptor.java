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
Created		   Apr 23, 2012			shai

*/
package com.centimia.orm.ezqu;

import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.Interceptor;

/**
 * Implementations of this interface are used for intercepting CRUD operations on Entities {@link Entity}. It can also be used with POJOs.<br>
 * In order to use this interceptor you must mark your bean with the @interceptor {@link Interceptor} annotation.
 *
 * @param <K> the entity which is handled by intercepter
 * @author shai
 * @since 2.0.0
 */
public interface CRUDInterceptor<K> {

	/**
	 * executed on entities before the entity is inserted
	 * @param t
	 */
	public void onInsert(K t);

	/**
	 * executes on entities before the entity is merged
	 * @param t
	 */
	public void onMerge(K t);

	/**
	 * executes on entities before the entity is updated
	 * @param t
	 */
	public void onUpdate(K t);

	/**
	 * executes on entities before the entity is deleted
	 * @param t
	 */
	public void onDelete(K t);
}
