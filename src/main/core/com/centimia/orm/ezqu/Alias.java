/*
 * Copyright (c) 2020-2024 Shai Bentin & Centimia Inc..
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * THIS SOFTWARE CONTAINS CONFIDENTIAL INFORMATION AND TRADE
 * SECRETS OF Shai Bentin USE, DISCLOSURE, OR
 * REPRODUCTION IS PROHIBITED WITHOUT THE PRIOR EXPRESS
 * WRITTEN PERMISSION OF Shai Bentin & CENTIMIA, INC.
 */
package com.centimia.orm.ezqu;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents the alias name for the table described by the entity object 
 * within the query;
 * 
 * @author shai
 */
public final class Alias implements Serializable {
	private static final long serialVersionUID = 6148657754364675980L;

	/** Holds the entity descriptor for the table being aliased */
	public Object aliasEntity;

	/** Holds the alias table name in the query */
	public String theAlias;

	public Alias(Object aliasEntity, String as) {
		this.aliasEntity = aliasEntity;
		this.theAlias = as;
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(aliasEntity);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Alias a) || null == aliasEntity)
			return false;
		return Objects.equals(aliasEntity, a.aliasEntity);
	}

	@Override
	public String toString() {
		return this.theAlias;
	}
}
