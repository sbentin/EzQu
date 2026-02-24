/*
 * Copyright (c) 2025-2030 Shai Bentin & Centimia Inc..
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

/**
 * A result holder for counting rows in a table per column result
 * 
 * @author shai
 */
public class ColumnCount<C> implements Serializable, Comparable<ColumnCount<C>> {
	private static final long serialVersionUID = -7567693402872109588L;

	private final C columnValue;
	private final long count;

	ColumnCount(C c, long count) {
		this.columnValue = c;
		this.count = count;
	}

	/**
	 * @return the columnValue
	 */
	public C getColumnValue() {
		return columnValue;
	}

	/**
	 * @return the count
	 */
	public long getCount() {
		return count;
	}

	@Override
	public int compareTo(ColumnCount<C> obj) {
		return (count < obj.count) ? -1 : ((count == obj.count) ? compareColumnValue(obj.columnValue) : 1);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private int compareColumnValue(C anotherValue) {
		if (columnValue == null && anotherValue == null)
			return 0;
		
		if (columnValue == null)
			return 1;

		if (anotherValue == null)
			return -1;
		
		if (Number.class.isAssignableFrom(columnValue.getClass())) {
			long n1 = ((Number) columnValue).longValue();
			long n2 = ((Number) anotherValue).longValue();
			return (n1 < n2) ? -1 : ((n1 == n2) ? 0 : 1);
		}
		if (columnValue instanceof Comparable c && columnValue.getClass().isInstance(anotherValue)) {
			return c.compareTo((Comparable) anotherValue);
		}
		return String.CASE_INSENSITIVE_ORDER.compare(columnValue.toString(), anotherValue.toString());
	}
}
