::Author Ali Modaresi
@echo off
call :StartKernel 

goto :eof

:StartKernel
	:MakeClasspath
		setlocal EnableDelayedExpansion
		set CP=..\supplement;
		for /f "tokens=*" %%i in ('dir ..\lib /b'); do set CP=!CP!..\lib\%%i;
		
		for /f "tokens=*" %%i in ('dir ..\jars /b'); do set CP=!CP!..\jars\%%i;
	::goto :eof

echo [SOSMapChooser]...  SOSMapChooser
	::echo !CP!
java -Xmx1024m -cp !CP! sosNamayangar.SOSMapChooser
pause
goto :eof


:PrintUsage
echo USAGE: start.bat map [--nomenu] [--autorun]
echo For Example: start.bat test --autorun --nomenu
pause