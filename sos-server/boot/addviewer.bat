@echo off

if "%1" == "/?" GOTo :PrintUsage
echo "USAGE: start.bat map [--nomenu] [--autorun]
set tmp=
if "%1" == "" set tmp=-h 10.42.43.69
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
java -Xmx256m -cp !CP! rescuecore2.LaunchComponents sosNamayangar.SOSViewer -c config %*
::java -Xmx256m -cp !CP! rescuecore2.LaunchComponents sample.SampleViewer -c config %*
goto :eof


:PrintUsage
echo USAGE: start.bat map [--nomenu] [--autorun]
echo For Example: start.bat test --autorun --nomenu