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

/*
 * Update Log
 * 
 *  Date			User				Comment
 * ------			-------				--------
 * 01/02/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu.ext.asm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.centimia.orm.ezqu.annotation.Entity;

/**
 * asm adapter which adds post compile data to the classes annotated with {@link Entity}
 * 
 * @author Shai Bentin
 *
 */
public class EzquClassAdapter extends ClassVisitor implements Opcodes {

	private static final String LJAVA_LANG_CLASS = "[Ljava/lang/Class;";
	private static final String JAVA_LANG_RUNTIME_EXCEPTION = "java/lang/RuntimeException";
	private static final String JAVA_LANG_REFLECT_METHOD = "java/lang/reflect/Method";
	private static final String JAVA_LANG_CLASS = "java/lang/Class";
	private static final String JAVA_LANG_OBJECT = "java/lang/Object";
	private static final String COM_CENTIMIA_ORM_EZQU_DB = "com/centimia/orm/ezqu/Db";
	private static final String JAVA_LANG_EXCEPTION = "java/lang/Exception";
	private static final String LCOM_CENTIMIA_ORM_EZQU_DB = "Lcom/centimia/orm/ezqu/Db;";
	private static final String $ORIG = "$orig_";
	
	private String className;
	private Set<String> relationFields = new HashSet<>();
	private Set<String> lazyLoadFields = new HashSet<>();
	private Map<String, String[]> abstractFields = new HashMap<>();
	private boolean isEntityAnnotationPresent = false;
	private boolean isMappedSupperClass = false;
	private boolean isInherited = false;
	
	public EzquClassAdapter(int api, ClassVisitor classVisitor) {
		super(api, classVisitor);
	}
	
	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		this.className = name;
		cv.visit(version, access, name, signature, superName, interfaces);
	}
	
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		if (desc != null && desc.indexOf("com/centimia/orm/ezqu/annotation/Entity") != -1) {
			this.isEntityAnnotationPresent = true;
		}
		if (desc != null && desc.indexOf("com/centimia/orm/ezqu/annotation/MappedSuperclass") != -1){
			this.isMappedSupperClass = true;
		}
		if (desc != null && desc.indexOf("com/centimia/orm/ezqu/annotation/Inherited") != -1){
			this.isInherited  = true;
		}
		return super.visitAnnotation(desc, visible);
	}
	
	@Override
	public MethodVisitor visitMethod(int access, String methodName, String desc, String signature, String[] exceptions) {
		// 1. if name is in the list of o2m and return type is collection instrument 
		// add call to db.getRelationFromDb or db.getRelationArrayFromDb, only if value of field is null
		 if ((isEntityAnnotationPresent || isMappedSupperClass) && methodName.startsWith("get")) {
			final String checkName = methodName.substring(3).toLowerCase();
			 // this is a getter check if it is a relation getter
			if (relationFields.contains(checkName)) {
				// this is a relationship.
				String newMethodName = $ORIG + methodName;
				String fieldName = camelCase(methodName);
				
				generateNewMethodBody(access, desc, signature, exceptions, methodName, newMethodName, fieldName);
				
				return super.visitMethod(access, newMethodName, desc, signature, exceptions);
			}
			else if (lazyLoadFields.contains(checkName)) {
				// this is a O2O relationship which should be lazy loaded
				String newMethodName = $ORIG + methodName;
				String fieldName = camelCase(methodName);
				
				generateLazyRelation(access, desc, exceptions, methodName, newMethodName, fieldName);
				return super.visitMethod(access, newMethodName, desc, signature, exceptions);
			}
			else
				return super.visitMethod(access, methodName, desc, signature, exceptions);
		}
		else if (isEntityAnnotationPresent || isMappedSupperClass)
			return super.visitMethod(access, methodName, desc, signature, exceptions);
		
		else
			return null;
	}
	
	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		if ((isEntityAnnotationPresent || isMappedSupperClass)
				&& (desc.indexOf("java/util/List") != -1 || desc.indexOf("java/util/Set") != -1 || desc.indexOf("java/util/Collection") != -1)) {
			// collect the fields that are relation by rule. (Collection type fields....)
			relationFields.add(name.toLowerCase());
		}
		return new EzquFieldVisitor(Opcodes.ASM9, super.visitField(access, name, desc, signature, value), name.toLowerCase(), relationFields, lazyLoadFields, abstractFields);
	}
	
	@Override
	public void visitEnd() {
		if (!isInherited && (isEntityAnnotationPresent || isMappedSupperClass)) {
			FieldVisitor fv = cv.visitField(ACC_PUBLIC+ACC_TRANSIENT, "db", LCOM_CENTIMIA_ORM_EZQU_DB, null, null);
			fv.visitEnd();
			
			fv = cv.visitField(ACC_PUBLIC, "isLazy", "Z", null, null);
			// add the ezquIgnore annotation to the lazy field because we need to carry this field around the network but not persist it
			AnnotationVisitor av = fv.visitAnnotation("Lcom/centimia/orm/ezqu/annotation/Transient;", true);
			av.visitEnd();
			fv.visitEnd();
		}

		super.visitEnd();
	}
	
	public final boolean isEntityAnnotationPresent() {
		return this.isEntityAnnotationPresent;
	}
	
	public final boolean isMappedSuperClass() {
		return this.isMappedSupperClass;
	}

	public final boolean isInherited() {
		return this.isInherited;
	}
	
	/**
	 * Generates the new method body, copy the old to a new method and connect them.
	 * 
	 * the structure of the new method is:<br>
	 * <br><b><div style="background:lightgray">
	 * <pre>
	 * public [CollectionType] [getterName]() {
	 * 	if ([fieldName] == null){
	 * 		try {
	 * 			if (null == db || db.isClosed())
	 * 				return $orig_[getterName]();
	 * 			Method method = db.getClass().getDeclaredMethod("getRelationFromDb", String.class, Object.class, Class.class);
	 * 			method.setAccessible(true);
	 * 			children = (Collection<TestTable>)method.invoke(db, [fieldName], this, TestTable.class);
	 * 			method.setAccessible(false);
	 * 		}
	 * 		catch (Exception e) {
	 * 			if (e instanceof RuntimeException)
	 * 				throw (RuntimeException)e;
	 * 			throw new RuntimeException(e.getMessage(), e);
	 * 		}
	 * 	}
	 * return $orig_[getterName]();
	 * }
	 * </pre>
	 * </div>
	 * 
	 * @param access
	 * @param desc
	 * @param signature
	 * @param exceptions
	 * @param name - currentMethodName
	 * @param newName - the new Method name (orig_[currentMethodName]);
	 * @param fieldName
	 */
	private void generateNewMethodBody(int access, String desc, String signature, String[] exceptions, String name, String newName, String fieldName) {
		MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
		String fieldSignature = signature.substring(signature.indexOf(')') + 1, signature.lastIndexOf('<')) + ";";
		String type = signature.substring(signature.indexOf('<') + 1, signature.indexOf('>'));
		String cast = desc.substring(desc.indexOf("java/"), desc.indexOf(';'));
		
		mv.visitCode();
		Label l0 = new Label();
		Label l1 = new Label();
		Label l2 = new Label();
		mv.visitTryCatchBlock(l0, l1, l2, JAVA_LANG_EXCEPTION);
		Label l3 = new Label();
		Label l4 = new Label();
		mv.visitTryCatchBlock(l3, l4, l2, JAVA_LANG_EXCEPTION);		
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, fieldName, fieldSignature);
		Label l5 = new Label();
		mv.visitJumpInsn(IFNONNULL, l5);
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "db", LCOM_CENTIMIA_ORM_EZQU_DB);
		Label l6 = new Label();
		mv.visitJumpInsn(IFNULL, l6);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "db", LCOM_CENTIMIA_ORM_EZQU_DB);
		mv.visitMethodInsn(INVOKEVIRTUAL, COM_CENTIMIA_ORM_EZQU_DB, "isClosed", "()Z", false);
		mv.visitJumpInsn(IFEQ, l3);
		mv.visitLabel(l6);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, newName, desc, false);
		mv.visitLabel(l1);
		mv.visitInsn(ARETURN);
		mv.visitLabel(l3);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "db", LCOM_CENTIMIA_ORM_EZQU_DB);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_OBJECT, "getClass", "()Ljava/lang/Class;", false);
		mv.visitLdcInsn("getRelationFromDb");
		mv.visitInsn(ICONST_3);
		mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_CLASS);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_1);
		mv.visitLdcInsn(Type.getType("Ljava/lang/Object;"));
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_2);
		mv.visitLdcInsn(Type.getType("Ljava/lang/Class;"));
		mv.visitInsn(AASTORE);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_CLASS, "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
		mv.visitVarInsn(ASTORE, 1);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitInsn(ICONST_1);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_REFLECT_METHOD, "setAccessible", "(Z)V", false);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "db", LCOM_CENTIMIA_ORM_EZQU_DB);
		mv.visitInsn(ICONST_3);
		mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_OBJECT);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_0);
		mv.visitLdcInsn(fieldName);
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_1);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitInsn(AASTORE);
		mv.visitInsn(DUP);
		mv.visitInsn(ICONST_2);
		mv.visitLdcInsn(Type.getType(type));
		mv.visitInsn(AASTORE);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_REFLECT_METHOD, "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, cast);
		mv.visitFieldInsn(PUTFIELD, className, fieldName, fieldSignature);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitInsn(ICONST_0);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_REFLECT_METHOD, "setAccessible", "(Z)V", false);
		mv.visitLabel(l4);
		mv.visitJumpInsn(GOTO, l5);
		mv.visitLabel(l2);
		mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {JAVA_LANG_EXCEPTION});
		mv.visitVarInsn(ASTORE, 1);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(INSTANCEOF, JAVA_LANG_RUNTIME_EXCEPTION);
		Label l7 = new Label();
		mv.visitJumpInsn(IFEQ, l7);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, JAVA_LANG_RUNTIME_EXCEPTION);
		mv.visitInsn(ATHROW);
		mv.visitLabel(l7);
		mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {JAVA_LANG_EXCEPTION}, 0, null);
		mv.visitTypeInsn(NEW, JAVA_LANG_RUNTIME_EXCEPTION);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_EXCEPTION, "getMessage", "()Ljava/lang/String;", false);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_RUNTIME_EXCEPTION, "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
		mv.visitInsn(ATHROW);
		mv.visitLabel(l5);
		mv.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, newName, desc, false);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(7, 2);
		mv.visitEnd();
	}
		
	/**
	 * Generates the new method body, copy the old to a new method and connect them.
	 * the structure of the new method is:<br>
	 * <br><b><div style="background:lightgray;color:black">
	 * <pre>
	 * public [entityType] [getterName]() {
	 *	if ([field] != null && [field].isLazy) {
	 *		try {
	 *			if (null == db)
	 *				return null;
	 *			
	 *			[parentType] parent = this.getClass().newInstance();
	 *			[entityType] desc = TestB.class.newInstance();
	 *			
	 *			// get the primary key
	 *			Object pk = db.getPrimaryKey(this);
	 *			
	 *			// get the object
	 *			[field] = db.from(desc).innerJoin(parent).on(parent.[entityValue]).is(desc).where(db.getPrimaryKey(parent)).is(pk).selectFirst();
	 *		}
	 *		catch (Exception e) {
	 *			if (e instanceof RuntimeException)
	 *				throw (RuntimeException)e;
	 *			throw new RuntimeException(e.getMessage(), e);
	 *		}
	 *	}
	 *	return $orig_[getterName]();
	 *  }
	 * </pre>
	 * </div>
	 * 
	 * @param access
	 * @param desc
	 * @param exceptions
	 * @param methodName - current method name
	 * @param newMethodName - new method name (the $orig_[current method name])
	 * @param fieldName
	 */
	public void generateLazyRelation(int access, String desc, String[] exceptions, String methodName, String newMethodName, String fieldName) {
		MethodVisitor mv = cv.visitMethod(access, methodName, desc, null, exceptions);
		String fieldSignature = desc.substring(desc.indexOf(')') + 1);
		String fieldClassName = desc.substring(desc.indexOf(')') + 2, desc.length() - 1);
		
		mv.visitCode();
		Label l0 = new Label();
		Label l1 = new Label();
		Label l2 = new Label();
		mv.visitTryCatchBlock(l0, l1, l2, JAVA_LANG_EXCEPTION);
		Label l3 = new Label();
		Label l4 = new Label();
		mv.visitTryCatchBlock(l3, l4, l2, JAVA_LANG_EXCEPTION);		
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, fieldName, fieldSignature);
		Label l5 = new Label();
		mv.visitJumpInsn(IFNULL, l5);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, fieldName, fieldSignature);
		mv.visitFieldInsn(GETFIELD, fieldClassName, "isLazy", "Z");
		mv.visitJumpInsn(IFEQ, l5);
		mv.visitLabel(l0);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "db", LCOM_CENTIMIA_ORM_EZQU_DB);
		mv.visitJumpInsn(IFNULL, l1);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "db", LCOM_CENTIMIA_ORM_EZQU_DB);
		mv.visitMethodInsn(INVOKEVIRTUAL, COM_CENTIMIA_ORM_EZQU_DB, "isClosed", "()Z", false);
		mv.visitJumpInsn(IFEQ, l3);
		mv.visitLabel(l1);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitInsn(ACONST_NULL);
		mv.visitInsn(ARETURN);
		mv.visitLabel(l3);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_OBJECT, "getClass", "()Ljava/lang/Class;", false);
		mv.visitInsn(ICONST_0);
		mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_CLASS);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_CLASS, "getConstructor", "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;", false);
		mv.visitInsn(ICONST_0);
		mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_OBJECT);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Constructor", "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, className);
		mv.visitVarInsn(ASTORE, 1);		
		// takes care of the class array that is in the annotation
		String[] relationClasses = abstractFields.get(fieldName.toLowerCase());
		if (null == relationClasses || 0 == relationClasses.length) {
			relationClasses = new String[] {fieldSignature};
		}
		if (relationClasses.length < 5)
			mv.visitInsn(relationClasses.length + 3);
		else
			mv.visitIntInsn(BIPUSH, relationClasses.length);
		mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_CLASS);		
		for (int i = 0; i < relationClasses.length; i++) {
			String classSig = relationClasses[i];
			mv.visitInsn(DUP);
			if (i <= 5)
				mv.visitInsn(i + 3);
			else
				mv.visitIntInsn(BIPUSH, i);
			mv.visitLdcInsn(Type.getType(classSig));
			mv.visitInsn(AASTORE);
		}
		mv.visitVarInsn(ASTORE, 2);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "db", LCOM_CENTIMIA_ORM_EZQU_DB);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, COM_CENTIMIA_ORM_EZQU_DB, "getPrimaryKey", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitVarInsn(ASTORE, 3);
		mv.visitInsn(ACONST_NULL);
		mv.visitVarInsn(ASTORE, 4);
		mv.visitVarInsn(ALOAD, 2);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ASTORE, 8);
		mv.visitInsn(ARRAYLENGTH);
		mv.visitVarInsn(ISTORE, 7);
		mv.visitInsn(ICONST_0);
		mv.visitVarInsn(ISTORE, 6);
		Label l6 = new Label();
		mv.visitJumpInsn(GOTO, l6);
		Label l7 = new Label();
		mv.visitLabel(l7);
		mv.visitFrame(Opcodes.F_FULL, 9, new Object[] {className, className, LJAVA_LANG_CLASS, JAVA_LANG_OBJECT, fieldClassName, Opcodes.TOP, Opcodes.INTEGER, Opcodes.INTEGER, LJAVA_LANG_CLASS}, 0, new Object[] {});
		mv.visitVarInsn(ALOAD, 8);
		mv.visitVarInsn(ILOAD, 6);
		mv.visitInsn(AALOAD);
		mv.visitVarInsn(ASTORE, 5);
		mv.visitVarInsn(ALOAD, 5);
		mv.visitInsn(ICONST_0);
		mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_CLASS);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_CLASS, "getConstructor", "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;", false);
		mv.visitInsn(ICONST_0);
		mv.visitTypeInsn(ANEWARRAY, JAVA_LANG_OBJECT);
		mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Constructor", "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, fieldClassName);
		mv.visitVarInsn(ASTORE, 9);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "db", LCOM_CENTIMIA_ORM_EZQU_DB);
		mv.visitVarInsn(ALOAD, 9);
		mv.visitMethodInsn(INVOKEVIRTUAL, COM_CENTIMIA_ORM_EZQU_DB, "from", "(Ljava/lang/Object;)Lcom/centimia/orm/ezqu/Query;", false);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/centimia/orm/ezqu/Query", "innerJoin", "(Ljava/lang/Object;)Lcom/centimia/orm/ezqu/QueryJoin;", false);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitFieldInsn(GETFIELD, className, fieldName, fieldSignature);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/centimia/orm/ezqu/QueryJoin", "on", "(Ljava/lang/Object;)Lcom/centimia/orm/ezqu/QueryJoinCondition;", false);
		mv.visitVarInsn(ALOAD, 9);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/centimia/orm/ezqu/QueryJoinCondition", "is", "(Ljava/lang/Object;)Lcom/centimia/orm/ezqu/QueryJoinWhere;", false);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitFieldInsn(GETFIELD, className, "db", LCOM_CENTIMIA_ORM_EZQU_DB);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, COM_CENTIMIA_ORM_EZQU_DB, "getPrimaryKey", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/centimia/orm/ezqu/QueryJoinWhere", "where", "(Ljava/lang/Object;)Lcom/centimia/orm/ezqu/QueryCondition;", false);
		mv.visitVarInsn(ALOAD, 3);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/centimia/orm/ezqu/QueryCondition", "is", "(Ljava/lang/Object;)Lcom/centimia/orm/ezqu/QueryWhere;", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, "com/centimia/orm/ezqu/QueryWhere", "selectFirst", "()Ljava/lang/Object;", false);
		mv.visitTypeInsn(CHECKCAST, fieldClassName);
		mv.visitVarInsn(ASTORE, 4);
		mv.visitVarInsn(ALOAD, 4);
		Label l8 = new Label();
		mv.visitJumpInsn(IFNULL, l8);
		Label l9 = new Label();
		mv.visitJumpInsn(GOTO, l9);
		mv.visitLabel(l8);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitIincInsn(6, 1);
		mv.visitLabel(l6);
		mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
		mv.visitVarInsn(ILOAD, 6);
		mv.visitVarInsn(ILOAD, 7);
		mv.visitJumpInsn(IF_ICMPLT, l7);
		mv.visitLabel(l9);
		mv.visitFrame(Opcodes.F_FULL, 5, new Object[] {className, className, LJAVA_LANG_CLASS, JAVA_LANG_OBJECT, fieldClassName}, 0, new Object[] {});
		mv.visitVarInsn(ALOAD, 0);
		mv.visitVarInsn(ALOAD, 4);
		mv.visitFieldInsn(PUTFIELD, className, fieldName, fieldSignature);
		mv.visitLabel(l4);
		mv.visitJumpInsn(GOTO, l5);
		mv.visitLabel(l2);
		mv.visitFrame(Opcodes.F_FULL, 1, new Object[] {className}, 1, new Object[] {JAVA_LANG_EXCEPTION});
		mv.visitVarInsn(ASTORE, 1);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(INSTANCEOF, JAVA_LANG_RUNTIME_EXCEPTION);
		Label l10 = new Label();
		mv.visitJumpInsn(IFEQ, l10);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitTypeInsn(CHECKCAST, JAVA_LANG_RUNTIME_EXCEPTION);
		mv.visitInsn(ATHROW);
		mv.visitLabel(l10);
		mv.visitFrame(Opcodes.F_APPEND,1, new Object[] {JAVA_LANG_EXCEPTION}, 0, null);
		mv.visitTypeInsn(NEW, JAVA_LANG_RUNTIME_EXCEPTION);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_EXCEPTION, "getMessage", "()Ljava/lang/String;", false);
		mv.visitVarInsn(ALOAD, 1);
		mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_RUNTIME_EXCEPTION, "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V", false);
		mv.visitInsn(ATHROW);
		mv.visitLabel(l5);
		mv.visitFrame(Opcodes.F_CHOP,1, null, 0, null);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKEVIRTUAL, className, newMethodName, desc, false);
		mv.visitInsn(ARETURN);
		mv.visitMaxs(4, 10);
		mv.visitEnd();
	}
	
	/**
	 * Returns true when the adapter has dealt with a EzQu annotated class and altered it.
	 * @return boolean
	 */
	public boolean isEzquAnnotated() {
		return isEntityAnnotationPresent || isMappedSupperClass;
	}
	
	/**
	 * @param name
	 * @return String
	 */
	private String camelCase(String name) {
		char[] realName = name.substring(3).toCharArray();
		realName[0] = Character.toLowerCase(realName[0]);
		return new String(realName);
	}
}
