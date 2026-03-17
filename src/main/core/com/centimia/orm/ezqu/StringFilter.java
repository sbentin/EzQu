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
 * Update Log
 *
 *  Date			User				Comment
 * ------			-------				--------
 * 03/02/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu;

import java.io.Serializable;

/**
 * Allows String based SQL clause inside a condition. It can be a stand alone where or just a condition to
 * be concatenated with other conditions.<br>
 * <b>Note:</b> Currently is not supported by join queries.
 * <p><pre>
 * use:
 * select.from(persistable).where(new StringFilter(){...}).select();
 * select.from(persistable).where(new StringFilter(){...}).and(persistable.getSomeField()).is(someValue).select()
 * </pre></p>
 *
 * @author shai
 */
@FunctionalInterface
public interface StringFilter extends Serializable {

	/**
	 * Returns the condition string that should be injected into the SQL.
	 *
	 * @param selectTable
	 * @return String
	 */
	String getConditionString(ISelectTable selectTable);

}
