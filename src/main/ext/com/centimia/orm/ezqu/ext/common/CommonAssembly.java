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
 ISSUE			DATE			AUTHOR
-------		   ------	       --------
Created		   May 6, 2012		shai

*/
package com.centimia.orm.ezqu.ext.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import com.centimia.orm.ezqu.ext.asm.EzquClassAdapter;
import com.centimia.orm.ezqu.ext.asm.SafeClassWriter;

/**
 * @author shai
 */
public class CommonAssembly {

	private CommonAssembly() {}
	
	public static BuildStats assembleFiles(File outputDir, StringBuilder successReport, StringBuilder failedReport) {
		ArrayList<File> files = new ArrayList<>();
		getAllFiles(outputDir, files);
		
		int success = 0;
		int failure = 0;
		int ignored = 0;
		for (File classFile: files) {
			try {
				if (assembleFile(classFile)) {
					successReport.append(String.format("SUCCESS -- %s%n", classFile));
					success++;
				}
				else {
					successReport.append(String.format("IGNORED -- %s%n", classFile));
					ignored++;
				}
			}
			catch (Exception e) {
				failedReport.append(String.format("FAILED -- %s --> %s%n", classFile, e.getMessage()));
				failure++;
			}
		}
		return new BuildStats(success, failure, ignored);
	}
	
	/**
	 * Assembles the EzQu augmented file, deletes the old and places the new 
	 * in its place.
	 * 
	 * @param classFile - the file that is tested for augmentation and then augmented if necessary
	 * @return boolean - true when successfully augmented a file
	 * @throws IOException
	 */
	public static boolean assembleFile(File classFile) throws IOException {
		FileInputStream fis = new FileInputStream(classFile);
        byte[] b = assembleFile(fis);       
        
        if (b != null && classFile.delete()) {
    		classFile.createNewFile();
            try (FileOutputStream fos = new FileOutputStream(classFile)) {
            	fos.write(b);
            	fos.flush();
            }
            return true;
        }

        return false;
	}

	/**
	 * Assembles the EzQu augmented file. Returning the bytes of the augmented file.
	 * no change is done to the original, this is left up to the caller.
	 * 
	 * @param is
	 * @return byte[]
	 * @throws IOException
	 */
	public static byte[] assembleFile(InputStream is) throws IOException {
		try (is) {
			byte[] fileBytes = is.readAllBytes();
			if (!isAlreadyAugmented(fileBytes))	{	
				ClassReader cr = new ClassReader(fileBytes);
				ClassWriter cw = new SafeClassWriter(cr, null, ClassWriter.COMPUTE_FRAMES);
				
				EzquClassAdapter ezquClassAdapter = new EzquClassAdapter(Opcodes.ASM9, cw);
				cr.accept(ezquClassAdapter, 0);
				
				if (ezquClassAdapter.isEzquAnnotated()) {
					return cw.toByteArray();
				}
			}
			return null;
		}
	}
	
	private static boolean isAlreadyAugmented(byte[] is) {
	    // 1. Create a reader for the bytecode
	    ClassReader reader = new ClassReader(is);
	    
	    // Use a 1-element array to store the result (so we can write to it from the anonymous class)
	    final boolean[] isPresent = {false};

	    // 2. Accept a visitor. 
	    // IMPORTANT: Pass flags to SKIP everything we don't need for maximum speed.
	    // We skip Method Code, Debug info (line numbers), and Stack Frames.
	    int flags = ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;

	    reader.accept(new ClassVisitor(Opcodes.ASM9) {
	        @Override
	        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
	            // Check if the current field matches your added field
	            if ("db".equals(name)) {
	                isPresent[0] = true;
	                // We found it! We could throw an exception to stop parsing here 
	                // for micro-optimization, but usually letting it finish is fine.
	            }
	            return null; // Return null so we don't visit the field's annotations/attributes
	        }
	    }, flags);

	    return isPresent[0];
	}
	
	private static void getAllFiles(File outputDir, List<File> files){
		File[] fileList = outputDir.listFiles(f -> f.getName().endsWith(".class") || f.isDirectory());
		
		for (File file: fileList) {
			if (file.isDirectory()) {
				getAllFiles(file, files);
			}
			else
				files.add(file);
		}
	}
}