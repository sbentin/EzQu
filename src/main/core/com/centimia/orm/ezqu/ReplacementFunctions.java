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
 * Prototype for functions that do replacement in the query
 * @author shai
 */
public abstract class ReplacementFunctions implements Token {

	protected Object[] x;
    protected String name;
    protected boolean isField;

    protected ReplacementFunctions(final boolean isField, final String name, Object ... x){
        this.name = name;
        this.x = x;
        this.isField = isField;
    }
}