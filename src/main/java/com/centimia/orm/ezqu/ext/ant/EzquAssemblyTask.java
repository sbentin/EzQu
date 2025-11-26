/*
 * Copyright (c) 2007-2010 Centimia Ltd.
 * All rights reserved.  Unpublished -- rights reserved
 *
 * Use of a copyright notice is precautionary only, and does
 * not imply publication or disclosure.
 * 
 * Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 2.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group, Centimia Inc.
 */

/*
 * Update Log
 * 
 *  Date			User				Comment
 * ------			-------				--------
 * 10/02/2010		Shai Bentin			 create
 */
package com.centimia.orm.ezqu.ext.ant;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.centimia.orm.ezqu.ext.common.BuildStats;
import com.centimia.orm.ezqu.ext.common.CommonAssembly;

/**
 * This task is used in an Ant build script to support the post compilation of Ezqu Entities.
 * <p>
 * Example Use:
 * <p>
 * <pre>
 * &lt;taskdef classpathref="projectPath" name="EzquAssembly" classname="com.centimia.orm.ezqu.ext.ant.EzquAssemblyTask"/&gt;
 * 
 * &lt;target name="EzqiAssembly" depends="main"&gt;
 * 	&lt;!-- the outputDir is the directory where the original .class files before post compile reside --&gt;
 * 	&lt;EzquAssembly classOutputDirectory="${outputDir}"/&gt;
 * &lt;/target&gt;
 * </pre>
 * <br>
 * <b>Note: </b> Easier solution exists with the Ezqu Plugin for eclipse
 * @author Shai Bentin
 *
 */
public class EzquAssemblyTask extends Task {

	private String classOutputDirectory;
	
	@Override
	public void execute() throws BuildException {
		File outputDir = new File(classOutputDirectory);
		if (!outputDir.exists())
			throw new BuildException(String.format("Output dir %s does not exist!!!", classOutputDirectory));
		
		StringBuilder report = new StringBuilder();
		BuildStats stats = CommonAssembly.assembleFiles(outputDir, report, report);
		if (stats.getFailure() > 0) {
			report.insert(0, "BUILD FAILED - converted " + stats.getSuccess() + " files, failed to convert " + stats.getFailure() + " files\n");
			throw new BuildException(report.toString());
		}
		getOwningTarget().getProject().log("BUILD SUCCESS - converted " + stats.getSuccess() + " files");
	}

	@Override
	public String getTaskName() {
		return "Ezqu Assembly Task";
	}
	
	public void setClassOutputDirectory(String outputDir) {
		this.classOutputDirectory = outputDir;
	}
}
