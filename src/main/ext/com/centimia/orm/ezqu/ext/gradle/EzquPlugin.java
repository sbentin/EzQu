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
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.JavaCompile;

/**
 * 
 * @author shai
 */
public class EzquPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		// 'apply' is idempotent; it won't crash if already applied.
		project.getPlugins().apply("java-library");

		JavaPluginExtension javaExt = project.getExtensions().getByType(JavaPluginExtension.class);
		SourceSet mainSourceSet = javaExt.getSourceSets().getByName("main");
		TaskProvider<JavaCompile> compileJava = project.getTasks().named("compileJava", JavaCompile.class);
		
		TaskProvider<PostCompileTask> ezquPostCompile =project.getTasks().register("ezquPostCompile", PostCompileTask.class, task -> {
			// Wire the Java classes (Fallback)
			task.getInputClasses().from(compileJava.flatMap(JavaCompile::getDestinationDirectory));
			
			// Set the target output directory
            task.getOutputDir().convention(
                project.getLayout().getBuildDirectory().dir("transformed-classes")
            );
		});
		
		ConfigurableFileCollection classesDirs = (ConfigurableFileCollection) mainSourceSet.getOutput().getClassesDirs();
        classesDirs.setFrom(ezquPostCompile.flatMap(PostCompileTask::getOutputDir));
	}
}
