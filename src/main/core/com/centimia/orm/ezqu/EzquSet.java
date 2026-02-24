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
 * 02/02/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A set implementation of the Ezqu Collection
 *
 * see AbstractEzquCollection
 * @author Shai Bentin
 */
class EzquSet<E> extends AbstractEzquCollection<E> implements Set<E> {
	private static final long	serialVersionUID	= -1456310904754891892L;
	
	public EzquSet(Set<E> origSet, Db db, FieldDefinition definition, Object parentPk) {
		super(origSet, db, definition, parentPk);
	}

	@Override
	void merge() {
		super.merge();
		originalList = originalList.stream().map(e -> db.get().checkSession(e)).collect(Collectors.toSet());
	}
}
