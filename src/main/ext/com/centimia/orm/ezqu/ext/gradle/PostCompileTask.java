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
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import com.centimia.orm.ezqu.ext.common.BuildStats;
import com.centimia.orm.ezqu.ext.common.CommonAssembly;

/**
 *  abstract class allows Gradle to inject the implementation for properties
 */
public abstract class PostCompileTask extends DefaultTask {

    // Define the input for the "fallback" classes (from Java plugin)
    @Classpath 
    public abstract ConfigurableFileCollection getDefaultClasses();

    // Define the input for the "manual" directory (from LocationExtension)
    // We use Property<String> so Gradle can track this input.
    @Input
    @Optional
    public abstract Property<String> getManualOutputDir();
    
    @Inject
    public PostCompileTask() {
        setDescription("Runs ezqu post-compile assembly.");
    }

    @TaskAction
    public void execute() {
        Set<File> outputDirs;

        // Resolve the logic using the Properties, NOT the Extension/Project
        if (getManualOutputDir().isPresent() && getManualOutputDir().get() != null) {
            outputDirs = Collections.singleton(new File(getManualOutputDir().get()));
        } 
        else {
            outputDirs = getDefaultClasses().getFiles();
        }
        
        for (File outputDir : outputDirs) {
            if (!outputDir.exists()) {
            	getLogger().error("Post Compile for Output dir {} failed directory does not exist!!!", outputDir.getAbsolutePath());
                continue;
            }
            
            StringBuilder successReport = new StringBuilder();
			StringBuilder failedReport = new StringBuilder();
			BuildStats stats = CommonAssembly.assembleFiles(outputDir, successReport, failedReport);
			boolean failed = stats.getFailure() > 0;
			if (failed) {
				getLogger().lifecycle("POST COMPILE FAILED for " + outputDir.getAbsolutePath() + " - converted " + stats.getSuccess() + " files, ignored " + stats.getIgnored() + " files, failed to convert " + stats.getFailure() + " files");
				getLogger().lifecycle(failedReport.toString());
			}
			else {
				getLogger().lifecycle("POST COMPILE SUCCESSFUL for " + outputDir.getAbsolutePath() + " - converted " + stats.getSuccess() + " files, ignored " + stats.getIgnored());
			}
			if (getLogger().isDebugEnabled()) {
				getLogger().debug(successReport.toString());
			}
        }
    }
}
