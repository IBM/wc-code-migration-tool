# wc-code-migration-tool

# Overview
This project is used to migrate APIs from one version to another. This is accomplished through patterns written in XML pattern files. This project includes a pattern file from WebSphere Commerce Version 8 to WebSphere Commerce Version 9.

# Users Manual

> Note: The Code Migration Tool is subject to the terms of the accompanying Apache 2.0 License agreement, which is included in the package. By downloading and by using this package, you agree to those license terms.

1. Download the [CMTv1.2.zip](CMTv1.2.zip) package. 
2. Extract the package to a temporary directory.
3. Copy the plug-in JAR files to the following directory. If a subdirectory does not exist, create it. 
   ```
   SDP\dropins\cmt\eclipse\plugins
   ```
4. Copy the cmt.bat file to the following directory. 
   ```
   WCDE_installdir\bin
   ```
5. Copy the api-v8.zip file to the following directory.
   ```
   WCDE_installdir\logs
   ```
6. Copy the v8-v9-wc-patterns.xml file to the following directory. 
   ```
   WCDE_installdir\logs
   ```
7. Open a command prompt window and change to the following directory. 
   ```
   C:\Program Files\IBM\SDP
   ```
8. To ensure that Eclipse recognizes the plug-ins, run the following command. 
   ```
   eclipsec -clean 
   ```
   > Optional: Increase the maximum heap size of Rational Application Developer before you run the tool. The tool loads previous and current APIs in memory when it performs the analysis. 
   1. Open the C:\IBM\SDP\eclipse.ini file for editing.
   2. Update the -Xmx option to -Xmx2560m. You can increase this value if needed.
   3. Save and close the file. 
      > If you are prompted for a workspace location, Eclipse successfully recognized the plug-ins. You can exit the command prompt. 
9. Run the Code Migration Tool by using either of the following methods. 
   1. Command prompt:
      1. Open a command prompt, and change to your WCDE_installdir\bin directory.
      2. Run the following command.
         ```
         cmt.bat [-planfile plan filename] [-logfile log filename]
             -patternfile pattern filename [-patternfile pattern filename […]] 
         ```
         Where: 
         ```
         planfile
         ```
         - Optional. The name of the output file that is created when the command runs. The file contains the following information.
           - Issue number
           - Pattern that detected the issue
           - Location of the issue (file, line numbers)
           - Last modified time of the file that contains the issue
           - A short description of the problem
           - The code that was detected, and the replacement text (if any)
         The default value is for the plan file cmtplan.xml.
         ```
         patternfile
         ```
         - Required. The name of a file that contains which patterns to use. At least one -patternfile must be given, and more than one might be given.
         ```
         logfile
         ```
         - Optional. The name of the log file. The default value is cmt.log.
   2. Graphical user interface:
      1. Open WebSphere Commerce Developer. 
      2. Click on the  Code Migration Tool (CMT) icon from the menu. Clicking this icon adds a nature for the plug-in to each of your .project files in each workspace project.
      3. Perform a build. This triggers a rebuild of all your projects.
      > Note When you close Rational Application Developer, the CMT user interface is disabled and must be started again if you want to run the CMT utility.
10. (Optional) After reviewing the changes, you can run the tool again to perform the available replacement steps for you.  This must be done on the command line.  To do so, add the parameters "-mode migrate" to the command line in step 9.1.  For example:
    ```
    cmt.bat -patternfile ..\logs\v8-v9-wc-patterns.xml -mode migrate
    ```
	When running CMT in this way, the &lt;log&gt; messages in the plan will be written to the log file, and the &lt;replaceinfile&gt; steps will be performed.
	  
CMT runs on all .java file in your workspace. After the build is completed, any issues are marked in the Marker view in WebSphere Commerce Developer. Look for the CMT markers in the Java Problem heading.

Regardless of which method you chose to run the Code Migration Tool, either by command prompt or interface during a build, you must review and correct any of the issues that are found by the tool. 

If you ran the utility through command prompt, review the cmtplan.xml, and take all necessary actions to resolve the issues that are flagged. 

If you ran the utility by starting a build in WebSphere Commerce Developer, switch to your Markers view, and click through the markers that are flagged CMT. Clicking these flags opens the corresponding java file and locates the related code. Some markers offer a Quick Fix option. If you hover or select the applicable code, a potential fix is provided. You can then click the Quick Fix icon to perform the substitution.

You can customize the v8-v9-wc-patterns.xml file by changing some of the &lt;log&gt; steps with &lt;replaceinfile&gt; steps, or vice versa.  You can also create new patterns, remove patterns, or modify patterns to suit your needs.  Use the existing patterns as examples.