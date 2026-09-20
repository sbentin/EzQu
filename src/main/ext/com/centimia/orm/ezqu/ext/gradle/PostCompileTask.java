/*
 * Copyright (c) 2025-2030 Shai Bentin & Centimia Inc..
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 *
 * THIS SOFTWARE CONTAINS CONFIDENTIAL INFORMATION AND TRADE
 * SECRETS OF Shai Bentin USE, DISCLOSURE, OR
 * REPRODUCTION IS PROHIBITED WITHOUT THE PRIOR EXPRESS
 * WRITTEN PERMISSION OF Shai Bentin & CENTIMIA, INC.
 */
package com.centimia.orm.ezqu.ext.gradle;

import java.io.File;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

import com.centimia.orm.ezqu.ext.common.BuildStats;
import com.centimia.orm.ezqu.ext.common.CommonAssembly;

/**
 *  abstract class allows Gradle to inject the implementation for properties
 */
public abstract class PostCompileTask extends DefaultTask {

    // Input classes from compilation
	@InputFiles
    @Classpath 
    public abstract ConfigurableFileCollection getInputClasses();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getManualOutputDir();

    @OutputDirectory
    @Optional
    public abstract DirectoryProperty getOutputDir();
    
    // Inject Gradle file operations for clean directory copying
    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();
    
    @Inject
    @SuppressWarnings("java:S5993")
    public PostCompileTask() {
        setDescription("Runs ezqu post-compile assembly.");
    }

    @TaskAction
    public void execute() {
    	File targetDir = getManualOutputDir().isPresent() 
                ? getManualOutputDir().get().getAsFile() 
                : getOutputDir().get().getAsFile();

    	// Copy raw compiled classes into the target directory first
        getFileSystemOperations().copy(spec -> {
            spec.from(getInputClasses());
            spec.into(targetDir);
        });

        StringBuilder successReport = new StringBuilder();
        StringBuilder failedReport = new StringBuilder();
        
        // Process input files and write transformed classes to targetDir
        BuildStats stats = CommonAssembly.assembleFiles(targetDir, successReport, failedReport);
        
        boolean failed = stats.getFailure() > 0;
        if (failed) {
            getLogger().lifecycle("POST COMPILE FAILED for " + getInputClasses().getAsPath() + " - converted " + stats.getSuccess() + " files, ignored " + stats.getIgnored() + " files, failed to convert " + stats.getFailure() + " files");
        }
        else {
            getLogger().lifecycle("POST COMPILE SUCCESSFUL for " + getInputClasses().getAsPath() + " - converted " + stats.getSuccess() + " files, ignored " + stats.getIgnored());
        }
        if (getLogger().isDebugEnabled()) {
			getLogger().debug(successReport.toString());
		}
    }
}