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
package com.centimia.orm.ezqu.util;

import java.lang.reflect.Field;
import java.util.HashMap;

import com.centimia.orm.ezqu.annotation.Inherited;

/**
 * This utility class contains functions related to class loading. There is a mechanism to restrict class loading.
 */
public class ClassUtils {

	private static HashMap<Class<?>, Class<?>> map = new HashMap<>();
	static {
	    map.put(boolean.class, Boolean.class);
	    map.put(byte.class, Byte.class);
	    map.put(short.class, Short.class);
	    map.put(char.class, Character.class);
	    map.put(int.class, Integer.class);
	    map.put(long.class, Long.class);
	    map.put(float.class, Float.class);
	    map.put(double.class, Double.class);
	}
	
	private ClassUtils() {
	
	}

	@SuppressWarnings("unchecked")
	public static <X> Class<X> getClass(X x) {
		return (Class<X>) x.getClass();
	}

	public static Class<?> loadClass(String className) {
		try {
			return Class.forName(className);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * returns the Object Type class of a primitive class type. i.e. int.class returns Integer.class
	 * @param clazz
	 * @return Class<?>
	 */
	public static Class<?> getWrapperClass(Class<?> clazz) {
		if (clazz.isPrimitive())
			return map.get(clazz);
		return clazz;
	}
	
	/**
	 * find a field in a class
	 * @param <A>
	 * @param clazz
	 * @param fieldName
	 * @return Field
	 * @throws NoSuchFieldException 
	 */
	public static <A> Field findField(Class<A> clazz, final String fieldName) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(fieldName);
		}
		catch (NoSuchFieldException nsfe) {
			Inherited inherited = clazz.getAnnotation(Inherited.class);
			if (null != inherited) {
				return findField(clazz.getSuperclass(), fieldName);
			}
		}
		throw new NoSuchFieldException();
	}
}
