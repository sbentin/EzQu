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
 * @author shai
 */
@FunctionalInterface
public interface SessionVoidSupplier {

	/**
	 * @param session - the session which runs this work
	 * @return T
	 */
	void execute(Db session);
}
