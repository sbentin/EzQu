package com.centimia.asm.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

import com.centimia.orm.ezqu.ext.common.CommonAssembly;

class CommonAssemblyTest {

	@Test
	void testAssembleFile() {
		File classFile = new File("/home/shai/git/jaqu-orm/EzQu/bin/test/com/centimia/asm/util/CommonAssemblyTestModel.class");
		try {
			if (!CommonAssembly.assembleFile(classFile))
				fail("Assembly failed");
		} 
		catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Assembly failed with exception");
		}
	}

}
