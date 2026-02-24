/*
 * Copyright (c) 2025-2030 Shai Bentin & Centimia Ltd..
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
package com.centimia.orm.ezqu.ext.asm;

import java.util.Map;

import org.objectweb.asm.AnnotationVisitor;

/**
 * @author shai
 */
public class EzquAnnotationVisitor extends AnnotationVisitor {

	private final Map<String, String[]> abstractFields;
	private String name;
	
	public EzquAnnotationVisitor(int api, String name, AnnotationVisitor av, Map<String, String[]> abstractFields) {
		super(api, av);
		this.name = name;
		this.abstractFields = abstractFields;
	}

	@Override
	public AnnotationVisitor visitArray(String name) {
		abstractFields.computeIfAbsent(this.name, k -> new String[0]);
		this.av = av.visitArray(name);
		return this;
	}

	@Override
	public void visit(String name, Object value) {
		av.visit(name, value);
		String[] fields = abstractFields.get(this.name);
		String s = value.toString();
		String[] tmp = new String[fields.length + 1];
		System.arraycopy(fields, 0, tmp, 0, fields.length);
		tmp[fields.length] = s;
		abstractFields.put(this.name, tmp);
	}

	@Override
	public void visitEnd() {
		av.visitEnd();
	}
}
