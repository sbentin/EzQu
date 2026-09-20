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

import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.sql.Clob;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import com.centimia.orm.ezqu.EzquError;
import com.centimia.orm.ezqu.FieldConverter;
import com.centimia.orm.ezqu.annotation.Entity;
import com.centimia.orm.ezqu.annotation.MappedSuperclass;

/**
 * Generic utility methods.
 */
public class Utils {
	private static final AtomicLong COUNTER = new AtomicLong(0);
	private static final boolean MAKE_ACCESSIBLE = true;

	private static final List<FieldConverter> FIELD_CONVERTERS = List.of(
			// 1. Assignable (no conversion needed)
	        (v, t) -> t.isAssignableFrom(v.getClass()) ? Optional.of(v) : Optional.empty(),
	        		
			// 2. String (incl. Clob)
	        (v, t) -> {
	            if (t == String.class) {
	                if (Clob.class.isAssignableFrom(v.getClass())) {
	                    try (Reader r = ((Clob) v).getCharacterStream()) {
	                        return Optional.of(IOUtils.readStringAndClose(r, -1));
	                    }
	                    catch (Exception e) {
	                        return Optional.of(new EzquError(e, "Error converting CLOB to String: %s", e.getMessage()));
	                    }
	                }
	                return Optional.of(v.toString());
	            }
	            return Optional.empty();
	        },

	        // 3. Entity / MappedSuperclass – must run **before** number conversion
	        (v, t) -> {
	            if (t.getAnnotation(Entity.class) != null ||
	                t.getAnnotation(MappedSuperclass.class) != null) {
	            	// the current value is a primary key for a related table.
	    			try {
	    				return Optional.of(t.getConstructor().newInstance());
	    			}
	    			catch (Exception e) {
	    				return Optional.of(new EzquError("Can not convert the value " + v + " from " + v.getClass() + " to " + t));
	    			}
	            }
	            return Optional.empty();
	        },
	        
	        // 4. Number
	        (v, t) -> {
	            if (Number.class.isAssignableFrom(v.getClass())) {
	                Number n = (Number) v;
	                if (t == Integer.class) return Optional.of(n.intValue());
	                if (t == Long.class)    return Optional.of(n.longValue());
	                if (t == Double.class)  return Optional.of(n.doubleValue());
	                if (t == Float.class)   return Optional.of(n.floatValue());
	            }
	            return Optional.empty();
	        }
		);
	
	/**
	 * Utility class. Prevents instantiation.
	 */
	private Utils() {}

	/**
	 * Creates a thread‑safe {@link java.util.Map} backed by a {@link java.util.HashMap}.
	 *
	 * @param &lt;A&gt; the type of keys
	 * @param &lt;B&gt; the type of values
	 * @return a synchronized map instance
	 */
	public static <A, B> Map<A, B> newSynchronizedHashMap() {
		HashMap<A, B> map = new HashMap<>();
		return Collections.synchronizedMap(map);
	}

	/**
	 * Creates a new array of the specified component type and size.
	 *
	 * @param &lt;T&gt; the component type
	 * @param componentType the {@link Class} object representing the component type
	 * @param size the length of the new array
	 * @return a new array instance
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] newArray(Class<T> componentType, int size) {
		return (T[]) Array.newInstance(componentType, size);
	}
	
	/**
	 * Instantiates a new object of the specified class. Handles primitive wrapper types,
	 * common Java types (e.g., {@link java.util.Date}, {@link java.time.LocalDate}), enums,
	 * {@link java.util.UUID}, byte arrays, and attempts to invoke a no‑arg constructor.
	 * For unsupported types a {@link com.centimia.orm.ezqu.EzquError} is thrown.
	 *
	 * @param &lt;T&gt; the type to instantiate
	 * @param clazz the {@link Class} object of the type
	 * @return a new instance of the specified type
	 * @throws com.centimia.orm.ezqu.EzquError if instantiation fails
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "removal", 
		"java:S107", "java:S2129", "java:S106", "java:S3878" })
	public static <T> T newObject(Class<T> clazz) {
		// must create new instances
		if (clazz == Integer.class) {
			return (T) Integer.valueOf((int) COUNTER.incrementAndGet());
		}
		else if (clazz == String.class) {
			return (T) ("" + COUNTER.incrementAndGet());
		}
		else if (clazz == Character.class) {
			char c = (char)COUNTER.incrementAndGet();
			return (T) Character.valueOf(c);
		}
		else if (clazz == Long.class) {
			return (T) Long.valueOf(COUNTER.incrementAndGet());
		}
		else if (clazz == Short.class) {
			return (T) Short.valueOf((short) COUNTER.incrementAndGet());
		}
		else if (clazz == Byte.class) {
			return (T) Byte.valueOf((byte) COUNTER.incrementAndGet());
		}
		else if (clazz == Float.class) {
			return (T) Float.valueOf(COUNTER.incrementAndGet());
		}
		else if (clazz == Double.class) {
			return (T) Double.valueOf(COUNTER.incrementAndGet());
		}
		else if (clazz == Boolean.class || clazz == boolean.class) {
			COUNTER.getAndIncrement();
			// although this is deprecated we must use a new object otherwise we will not be able to distinguish between boolean placeholders.
			// also since Boolean is an Object I don't think this constructor will ever go away it might be made in accessible in the future 
			// but we can overcome that if need be.
			return (T) new Boolean(false);
		}
		else if (clazz == BigDecimal.class) {
			return (T) new BigDecimal(COUNTER.incrementAndGet());
		}
		else if (clazz == BigInteger.class) {
			return (T) new BigInteger("" + COUNTER.incrementAndGet());
		}
		else if (clazz == java.sql.Date.class) {
			return (T) new java.sql.Date(COUNTER.incrementAndGet());
		}
		else if (clazz == java.sql.Time.class) {
			return (T) new java.sql.Time(COUNTER.incrementAndGet());
		}
		else if (clazz == java.sql.Timestamp.class) {
			return (T) new java.sql.Timestamp(COUNTER.incrementAndGet());
		}
		else if (clazz == java.util.Date.class) {
			return (T) new java.util.Date(COUNTER.incrementAndGet());
		}
		else if (clazz == LocalDate.class) {
			return (T) LocalDate.MIN.plus(COUNTER.incrementAndGet(), ChronoUnit.DAYS);
		}
		else if (clazz == LocalDateTime.class) {
			return (T) LocalDateTime.MIN.plus(COUNTER.incrementAndGet(), ChronoUnit.MICROS);
		}
		else if (clazz == ZonedDateTime.class) {
			return (T) ZonedDateTime.of(LocalDateTime.MIN, ZoneId.systemDefault()).plus(COUNTER.incrementAndGet(), ChronoUnit.MICROS);
		}
		else if (clazz == LocalTime.class) {
			return (T) LocalTime.MIN.plus(COUNTER.incrementAndGet(), ChronoUnit.MICROS);
		}
		else if (clazz == List.class) {
			COUNTER.getAndIncrement();
			return (T) new ArrayList();
		}
		else if (clazz == Set.class) {
			COUNTER.getAndIncrement();
			return (T) new HashSet();
		}
		else if (clazz.isEnum()) {
			COUNTER.getAndIncrement();
			return clazz.getEnumConstants()[0];
		}
		else if (clazz == java.util.UUID.class) {
			COUNTER.getAndIncrement();
			return(T) UUID.randomUUID();
		}
		else if (clazz == byte[].class) {
            COUNTER.getAndIncrement();
            return (T) new byte[0];
		}
		try {
			return clazz.getConstructor().newInstance();
		}
		catch (Exception e) {
			if (MAKE_ACCESSIBLE) {
				Constructor[] constructors = clazz.getDeclaredConstructors();
				// try 0 length constructors
				for (Constructor c : constructors) {
					if (c.getParameterTypes().length == 0) {
						c.setAccessible(true);
						try {
							return clazz.getConstructor().newInstance();
						}
						catch (Exception e2) {
							// system out because logger is not available here							
							System.out.println(e2.getMessage());
						}
						finally {
							c.setAccessible(false);
						}
					}
				}
				// try 1 length constructors
				for (Constructor c : constructors) {
					if (c.getParameterTypes().length == 1) {
						c.setAccessible(true);
						try {
							return (T) c.newInstance(new Object[1]);
						}
						catch (Exception e2) {
							// ignore
						}
						finally {
							c.setAccessible(false);
						}
					}
				}
			}
			throw new EzquError(e, "Exception trying to create %s: %s", clazz.getName(), e.getMessage());
		}
	}

	/**
	 * Returns the enum constant at the specified ordinal for the given enum type.
	 *
	 * @param &lt;E&gt; the enum type
	 * @param clazz the {@link Class} object of the enum
	 * @param ordinal the ordinal of the desired constant
	 * @return the enum constant
	 * @throws com.centimia.orm.ezqu.EzquError if the class is not an enum
	 */
	public static <E> E newEnum(Class<E> clazz, int ordinal) {
		if (clazz.isEnum()) {
			return clazz.getEnumConstants()[ordinal];			
		}
		throw new EzquError(clazz.getName() + ": is not an enum type"); 
	}
	
	/**
	 * Creates a new instance of the specified enum type using {@code sun.misc.Unsafe}.
	 * The instance is not initialized by any constructor and may contain default values.
	 *
	 * @param &lt;E&gt; the enum type
	 * @param clazz the {@link Class} object of the enum
	 * @return a new enum instance
	 * @throws com.centimia.orm.ezqu.EzquError if the class is not an enum or instantiation fails
	 */
	@SuppressWarnings({ "unchecked", "java:S1191", "java:S4507" })
	public static <E> E newEnum(Class<E> clazz) {
		if (clazz.isEnum()) {
			try {				
				Constructor<?> constructor = sun.misc.Unsafe.class.getDeclaredConstructors()[0];
			    constructor.setAccessible(true);
			    sun.misc.Unsafe unsafe = (sun.misc.Unsafe) constructor.newInstance();
			    Object enumValue = unsafe.allocateInstance(clazz);
			    return (E)enumValue;
			}
			catch (Exception e) {
				// nothing else we can do. no logger available here.
				e.printStackTrace();
			}
		}
		throw new EzquError("%s: is not an enum type", clazz.getName()); 
	}
	
	/**
	 * returns true when the object represents a DB simple type and could be held in a column.
	 * This method is called for select able fields, however because of the way blobs work it will not work for
	 * Objects that are stored as "blobs", "clobs"
	 * 
	 * @param clazz
	 * @return boolean
	 */
	public static <T> boolean isSimpleType(Class<T> clazz) {
		return (Number.class.isAssignableFrom(clazz) || 
			String.class.isAssignableFrom(clazz) || 
			Date.class.isAssignableFrom(clazz) ||
			Boolean.class.isAssignableFrom(clazz) ||
			Character.class.isAssignableFrom(clazz) ||
			Temporal.class.isAssignableFrom(clazz));
	}
	
	/**
	 * Constructs a {@link java.util.UUID} from a 16‑byte array.
	 *
	 * @param bytes a 16‑byte array representing the UUID
	 * @return the corresponding {@link java.util.UUID}
	 * @throws IllegalArgumentException if the array is null or not 16 bytes long
	 */
	public static UUID newUUID(byte[] bytes) {
		if (bytes == null || bytes.length != 16) {
            throw new IllegalArgumentException("Expected 16 bytes, got " + (bytes == null ? "null" : bytes.length));
        }
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long most = bb.getLong();
        long least = bb.getLong();
        return new UUID(most, least);
	}
	
	/**
	 * Converts a {@link java.util.UUID} to a 16‑byte array in big‑endian order.
	 *
	 * @param uuid the UUID to convert
	 * @return a 16‑byte array representation of the UUID
	 */
	public static byte[] toUUIDBytes(UUID uuid) {
		// UUID stores the value as two 64‑bit longs
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array(); // BIG_ENDIAN (the default) → 128‑bit big‑endian
	}
	
	/**
	 * Creates an {@link java.net.InetAddress} from a byte array. Supports IPv4 (4 bytes)
	 * and IPv6 (16 bytes). For IPv4, the array is padded to 16 bytes.
	 *
	 * @param bytes the byte array containing the IP address
	 * @return the corresponding {@link java.net.InetAddress}
	 * @throws com.centimia.orm.ezqu.EzquError if the address cannot be resolved
	 */
	public static InetAddress newInetAddress(byte[] bytes) {
		try {
			return InetAddress.getByAddress(bytes.length == 4 ? bytes : java.util.Arrays.copyOfRange(bytes, 12, 16));
		}
		catch (UnknownHostException e) {
			throw new EzquError("Can not convert the value " + bytes + " from byte[] to INetAddress");
		}
	}
	
	/**
	 * Converts an {@link java.net.InetAddress} to a 16‑byte array. IPv4 addresses are
	 * padded with leading zeros to fit the 16‑byte column.
	 *
	 * @param inetAddr the address to convert
	 * @return a 16‑byte array representation of the address
	 */
	public static byte[] toINetBytes(InetAddress inetAddr) {
		byte[] ipBytes = inetAddr.getAddress(); // IPv4 = 4 bytes, IPv6 = 16 bytes

		// Store in a 16‑byte column (pad IPv4 with leading zeros)
		byte[] raw16 = new byte[16];
		if (ipBytes.length == 4) {
		    System.arraycopy(ipBytes, 0, raw16, 12, 4);  // store in lower 4 bytes
		}
		else {
		    System.arraycopy(ipBytes, 0, raw16, 0, 16);
		}
		return raw16;
	}
	
	/**
	 * Attempts to convert the given object to the specified target type. Supports conversion
	 * to {@link String} (including {@link Clob} to {@link String}), numeric types
	 * (Integer, Long, Double, Float), and returns an empty {@link Optional} if conversion
	 * is not possible.
	 *
	 * @param o the object to convert
	 * @param targetType the desired target type
	 * @return Optional&lt;Object&gt; an {@link Optional} containing the converted value, or {@code Optional.empty()}
	 *         if conversion could not be performed
	 */
	public static final FieldConverter primitiveConverter = (v, t) -> {
		if (null == v)
			return Optional.empty();

		return IntStream.of(0, 1, 3)
			.mapToObj(i -> FIELD_CONVERTERS.get(i).tryConvert(v, t))
			.flatMap(Optional::stream)
			.findFirst();
	};

	/**
	 * Attempts to convert the given object to the specified target type. Supports conversion
	 * to {@link String} (including {@link Clob} to {@link String}), numeric types
	 * (Integer, Long, Double, Float), or to a an Entity type in case of Foreign Key.
	 * Returns an empty {@link Optional} if conversion is not possible.
	 *
	 * @param o the object to convert
	 * @param targetType the desired target type
	 * @return Optional&lt;Object&gt; an {@link Optional} containing the converted value, or {@code Optional.empty()}
	 *         if conversion could not be performed
	 */
	public static final FieldConverter fullConverter = (v, t) -> {
		if (null == v)
			return Optional.empty();

		return IntStream.range(0, FIELD_CONVERTERS.size())
			.mapToObj(i -> FIELD_CONVERTERS.get(i).tryConvert(v, t))
			.flatMap(Optional::stream)
			.findFirst();
	};
}
