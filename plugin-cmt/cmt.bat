@echo off

REM -----------------------------------------------------------------
REM  Licensed Materials - Property of IBM
REM
REM  WebSphere Commerce
REM
REM  (C) Copyright IBM Corp. 2007, 2008 All Rights Reserved.
REM
REM  US Government Users Restricted Rights - Use, duplication or
REM  disclosure restricted by GSA ADP Schedule Contract with
REM  IBM Corp.
REM -----------------------------------------------------------------

setlocal
pushd %~d0%~p0

call setenv
cd %WCLOGDIR%
set PATH=%~p0;%PATH%

set RESTVAR=
:loop2
if "%1"=="" goto after_loop2
set RESTVAR=%RESTVAR% %1
shift
goto loop2

:after_loop2

%RAD_HOME%\eclipsec.exe -application com.ibm.commerce.cmt.CMT -showlocation -data %WORKSPACE_DIR% %RESTVAR%

popd
endlocal
