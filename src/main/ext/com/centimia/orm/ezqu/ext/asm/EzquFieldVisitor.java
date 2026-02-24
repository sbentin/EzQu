/*
 * Copyright (c) 2025-2030 Centimia Ltd.
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *  
 * Licensed under Eclipse Public License, Version 2.0,
 * 
 * 
 * Initial Developer: Shai Bentin, Centimia Ltd.
 */
package com.centimia.orm.ezqu.ext.asm;

import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;

/**
 * @author shai
 *
 */
public class EzquFieldVisitor extends FieldVisitor {

	private final Set<String> relationFields;
	private final Set<String> lazyLoadFields;
	private final Map<String, String[]> abstractFields;
	private final String name;
	
	public EzquFieldVisitor(int api, FieldVisitor fv, String name, Set<String> relationFields, Set<String> lazyLoadFields, Map<String, String[]> abstractFields) {
		super(api, fv);
		this.name = name;
		this.relationFields = relationFields;
		this.lazyLoadFields = lazyLoadFields;
		this.abstractFields = abstractFields;
	}

	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc != null) {
			if (desc.indexOf("com/centimia/orm/ezqu/annotation/Converter") != -1)
				this.relationFields.remove(name);
			
			if (desc.indexOf("com/centimia/orm/ezqu/annotation/Transient") != -1) {
				this.relationFields.remove(name);
			}
			else if (desc.indexOf("com/centimia/orm/ezqu/annotation/Lazy") != -1) {
				this.lazyLoadFields.add(name);
			}
			
			if (desc.indexOf("com/centimia/orm/ezqu/annotation/RelationTypes") != -1)
				return new EzquAnnotationVisitor(api, name, super.visitAnnotation(desc, visible), abstractFields);
		}
		return super.visitAnnotation(desc, visible);
	}
}
