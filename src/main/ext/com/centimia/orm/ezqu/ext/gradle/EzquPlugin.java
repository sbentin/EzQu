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
Created		   May 6, 2012		 shai

*/
package com.centimia.orm.ezqu.ext.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;

/**
 * 
 * @author shai
 */
public class EzquPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		// 'apply' is idempotent; it won't crash if already applied.
		project.getPlugins().apply("java-library");

		project.getTasks().register("ezquPostCompile", PostCompileTask.class, task -> {
			// Wire the Java classes (Fallback)
			JavaPluginExtension javaExt = project.getExtensions().getByType(JavaPluginExtension.class);
			task.getDefaultClasses().from(javaExt.getSourceSets().getByName("main").getOutput().getClassesDirs());
			
			task.dependsOn(project.getTasks().named("compileJava"));
			
		    // If we are using the default classes, we must run AFTER java compilation.
		    // The 'from' above usually handles the dependency automatically, 
		    // but explicit 'mustRunAfter' can be safer if you encounter race conditions.
		    task.mustRunAfter(project.getTasks().named("compileJava"));
		});
	}
}
