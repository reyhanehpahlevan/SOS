::@echo off
echo %1
cd D:\Rescue\SOS Server 1.0a 2011 Edition 6\boot
if %1 == "/?" GOTo :PrintUsage
echo "USAGE: start.bat map [--nomenu] [--autorun]
set tmp=
if %1 == "" set tmp=G:\berlin3\rescue.soslog
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

echo [Runnig SOS Log Viewer]...  NewSOSViewer -c config --kernel.logname=%1
	::echo !CP!
java -Xmx1024m -cp !CP! sosNamayangar.NewSOSViewer -c config --kernel.logname=%1
pause
goto :eof


:PrintUsage
echo USAGE: start.bat map [--nomenu] [--autorun]
echo For Example: start.bat test --autorun --nomenu
pause