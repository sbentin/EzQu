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
package com.centimia.orm.ezqu.converter;

import com.centimia.orm.ezqu.util.EzquConverter;

/**
 * Converts between "Y"/"N" and yes/no. Use with @Converter annotation.
 * resulting value for true is "Y" and for false is "N". The fromDb method also accepts "YES" and "NO" as input.
 */
public class YesNoConverter implements EzquConverter<String, Boolean> {

	@Override
	public Boolean fromDb(String value) {
		if (null == value) {
			return null;
		}
		value = value.toUpperCase();
				
		switch (value) {
			case "Y", "YES": return true;
			case "N", "NO": return false;
			default: return null;
		}
	}

	@Override
	public String toDb(Boolean value) {
		if (null == value) {
			return null;
		}
		return value ? "Y": "N";
	}

}
