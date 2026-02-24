/*
 * Copyright (c) 2025-2030 Shai Bentin & Centimia Ltd..
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
package com.centimia.orm.ezqu.ext.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.centimia.orm.ezqu.ext.common.BuildStats;
import com.centimia.orm.ezqu.ext.common.CommonAssembly;

/**
 * 
 */
@Mojo(name = "ezqu-post-compile",
	defaultPhase = LifecyclePhase.PROCESS_CLASSES,
	threadSafe = true)
public class PostCompileMojo extends AbstractMojo {

	/**
	 * The directory where compiled .class files are placed (default
	 * ${project.build.outputDirectory}).
	 */
	@Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
	private File classesDirectory;

	@Override
	public void execute() throws MojoExecutionException {
		getLog().info("Post‑compile Mojo is running.");
		getLog().info("Working on classes in: " + classesDirectory.getAbsolutePath());
		
		if (!classesDirectory.exists() || null == classesDirectory.listFiles()) {
            getLog().warn("No compiled classes found.");
            return;
        }
		
		try {
			StringBuilder successReport = new StringBuilder();
			StringBuilder failedReport = new StringBuilder();
			BuildStats stats = CommonAssembly.assembleFiles(classesDirectory, successReport, failedReport);
			boolean failed = stats.getFailure() > 0;
			if (failed) {
				getLog().error("EZQU POST COMPILE FAILED for " + classesDirectory.getAbsolutePath() + " - converted " + stats.getSuccess() + " files, ignored " + stats.getIgnored() + " files, failed to convert " + stats.getFailure() + " files");
				getLog().error(failedReport.toString());
			}
			else {
				getLog().info("EZQU POST COMPILE SUCCESSFUL for " + classesDirectory.getAbsolutePath() + " - converted " + stats.getSuccess() + " files, ignored " + stats.getIgnored());
			}
			if (getLog().isDebugEnabled()) {
				getLog().debug(successReport.toString());
			}
		}
		catch (Exception e) {
			throw new MojoExecutionException("Failed to create marker file", e);
		}		
	}
}