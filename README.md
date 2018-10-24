# wc-code-migration-tool

# Overview
This project is used to migrate APIs from one version to another. This is accomplished through patterns written in XML pattern files. This project includes a pattern file from WebSphere Commerce Version 8 to WebSphere Commerce Version 9.

This repository contains various eclipse plugins.  Gradle build script is added for building the plugins. It will 
produce one archive, which can be used as a installable source 
in Eclipse.

#* `com.ibm.commerce.toolkit.plugin-<VERSION>.zip`



# How to install the plugin

## Installing using GUI

1. Download the archive to local file system.
2. Open RAD and from the menu bar, **click** `Help -> Install New Software...`
3. On the "Install" window, **click** "Add" button beside the "Work with" dropdown field
4. On the "Add Repository" window, **click** "Archive" button and select the archive downloaded earlier
5. On the "Add Repository" window, **click** "OK"
6. On the "Install" window, select the plugins and go through installation wizard to install them

## Installing using command line

```
<RAD_HOME>\eclipsec.exe -clean -purgeHistory -application org.eclipse.equinox.
p2.director -noSplash -repository jar:file:/com.ibm.commerce.toolkit.plugin-<VERSION>.zip!/
-installIUs com.ibm.commerce.toolkit.plugin.feature.group
```

# How to use this repository

Below is the structure that should be followed for this repository.

	- master/
	 |- gradle/
	 |- build.gradle
	 |- settings.gradle
	 |- gradlew
	 |- gradlew.bat
	- eclipse-repository/
	 |- build.gradle
	- Plugin1/
	- Plugin2/
	- Plugin3/
	- Plugin4/
	- Plugin5/

## How to add a new plugin

When adding a new plugin source code to this repository, take following steps 
to ensure that it is built and included in the installable archive.

1. Add the new plugin to root of this repository as shown above
2. Edit `master/settings.gradle` file and append so that the new plugin is 
included in the build
3. Edit `eclipse-repository/build.gradle` file and append to `dependencies{}` block:
 1. Add `feature project(':<plugin_project_name>')`
4. Add a `README.md` file to the root of the new plugin directory with overview and usage of the plugin

