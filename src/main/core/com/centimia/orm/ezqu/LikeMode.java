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
 ISSUE			DATE			AUTHOR
-------		   ------	       --------
Created		   Nov 1, 2012			shai

*/
package com.centimia.orm.ezqu;

/**
 * The mode of like
 * <ol>
 * <li>START - put a '%' at the beginning of the pattern</li>
 * <li>END - put a '%' at the end of the pattern</li>
 * <li>ANYWHERE - put '%' both at the beginning and end of pattern</li>
 * </ol>
 * @author shai
 */
enum LikeMode {
	START, END, ANYWHERE
}
