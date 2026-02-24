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
 * 26/02/2011		shai				 create
 */
package com.centimia.orm.ezqu.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 
 * @author shai
 *
 */
public class StringUtils {
	
	private StringUtils() {}
	
    /**
     * Check if a String is null or empty (the length is 0).
     *
     * @param s the string to check
     * @return true if it is null or empty
     */
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.isBlank();
    }

	/**
     * Convert a string to a SQL literal. Null is converted to NULL. The text is
     * enclosed in single quotes. If there are any special characters, the method
     * STRINGDECODE is used.
     *
     * @param s the text to convert.
     * @return the SQL literal
     */
    public static String quoteStringSQL(String s) {
        if (s == null) {
            return "NULL";
        }
        int length = s.length();
        StringBuilder buff = new StringBuilder(length + 2);
        buff.append('\'');
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (c == '\'') {
                buff.append(c);
            } else if (c < ' ' || c > 127) {
                // need to start from the beginning because maybe there was a \
                // that was not quoted
                return "STRINGDECODE(" + quoteStringSQL(javaEncode(s)) + ")";
            }
            buff.append(c);
        }
        buff.append('\'');
        return buff.toString();
    }
    
    /**
     * Convert a string to a Java literal using the correct escape sequences.
     * The literal is not enclosed in double quotes. The result can be used in
     * properties files or in Java source code.
     *
     * @param s the text to convert
     * @return the Java representation
     */
	public static String javaEncode(String s) {
		int length = s.length();
		StringBuilder buff = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '\t':
					// HT horizontal tab
					buff.append("\\t");
					break;
				case '\n':
					// LF linefeed
					buff.append("\\n");
					break;
				case '\f':
					// FF form feed
					buff.append("\\f");
					break;
				case '\r':
					// CR carriage return
					buff.append("\\r");
					break;
				case '"':
					// double quote
					buff.append("\\\"");
					break;
				case '\\':
					// backslash
					buff.append("\\\\");
					break;
				default:
					int ch = c & 0xffff;
					if (ch >= ' ' && (ch < 0x80)) {
						buff.append(c);
					}
					else {
						buff.append("\\u");
						// make sure it's four characters
						buff.append(Integer.toHexString(0x10000 | ch).substring(1));
					}
			}
		}
		return buff.toString();
	}
	
	public static String replaceOrAppend(Pattern pattern, String src, String repl) {
	    Matcher m = pattern.matcher(src);

	    if (m.find()) {
	        // Replace every occurrence (or use m.replaceFirst(...) if you only want one)
	        return m.replaceAll(repl);
	    }
	    else {
	        // Nothing matched – append the replacement string
	        return src + repl;
	    }
	}
}
