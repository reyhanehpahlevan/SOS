@echo off

if "%1" == "/?" GOTo :PrintUsage
echo "USAGE: start.bat map [--nomenu] [--autorun]
set tmp=
if "%1" == "" set tmp=G:\berlin3\rescue.soslog
::call :MakeClasspath
call :StartKernel %tmp%%*

goto :eof

:StartKernel
	:MakeClasspath
		setlocal EnableDelayedExpansion
		set CP=..\supplement;
		for /f "tokens=*" %%i in ('dir ..\lib /b'); do set CP=!CP!..\lib\%%i;
		
		for /f "tokens=*" %%i in ('dir ..\jars /b'); do set CP=!CP!..\jars\%%i;
	::goto :eof

echo [Runnig SOS Log Viewer]...  logviewer -c config %*
	::echo !CP!
java -Xmx1024m -cp !CP! rescuecore2.log.LogViewer -c config %*
goto :eof


:PrintUsage
echo USAGE: start.bat map [--nomenu] [--autorun]
echo For Example: start.bat test --autorun --nomenu