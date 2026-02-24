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
Created		   May 6, 2012		 shai

*/
package com.centimia.orm.ezqu.ext.gradle;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;

import com.centimia.orm.ezqu.ext.common.BuildStats;
import com.centimia.orm.ezqu.ext.common.CommonAssembly;

import groovy.lang.Closure;

/**
 * 
 * @author shai
 */
public class PostCompileClosure extends Closure<String> {
	private static final long serialVersionUID = -467576318569482194L;
	
	private final FileCollection defaultOutputDirs;
	
	public PostCompileClosure(Object owner, FileCollection fc) {
		super(owner);
		defaultOutputDirs = fc;
	}
	
	@Override
	public String call() {
		Task postCompileTask = (Task)getOwner();
		LocationExtension location = (LocationExtension) postCompileTask.getExtensions().getByName("location");
		
		Set<File> outputDirs = null;
		if (null == location.outputDir) {
            // Use the injected file collection
            outputDirs = defaultOutputDirs.getFiles();
        }
		else {
            outputDirs = new HashSet<>();
            outputDirs.add(new File(location.outputDir));
        }
		for (File outputDir: outputDirs) {
			if (!outputDir.exists()) {
				postCompileTask.getLogger().error("Post Compile for Output dir {} failed directory does not exist!!!", outputDir.getAbsolutePath());
				continue;
			}
			StringBuilder successReport = new StringBuilder();
			StringBuilder failedReport = new StringBuilder();
			BuildStats stats = CommonAssembly.assembleFiles(outputDir, successReport, failedReport);
			boolean failed = stats.getFailure() > 0;
			if (failed) {
				postCompileTask.getLogger().lifecycle("POST COMPILE FAILED for " + outputDir.getAbsolutePath() + " - converted " + stats.getSuccess() + " files, ignored " + stats.getIgnored() + " files, failed to convert " + stats.getFailure() + " files");
				postCompileTask.getLogger().lifecycle(failedReport.toString());
			}
			else {
				postCompileTask.getLogger().lifecycle("POST COMPILE SUCCESSFUL for " + outputDir.getAbsolutePath() + " - converted " + stats.getSuccess() + " files, ignored " + stats.getIgnored());
			}
			if (postCompileTask.getLogger().isDebugEnabled()) {
				postCompileTask.getLogger().debug(successReport.toString());
			}
		}
		return "success";
	}
}