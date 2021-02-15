::Author Ali Modaresi
@echo off

if "%1" == "/?" GOTo :PrintUsage
echo "USAGE: start.bat map [--nomenu] [--autorun]

::-> default map dir
set mapDir=maps

set mapName=%1
set team=%2

set defaultParameters=--nomenu --autostart
set defaultSimulators=--kernel.simulators.auto=collapse.CollapseSimulator,clear.ClearSimulator,misc.MiscSimulator,firesimulator.FireSimulatorWrapper,traffic3.simulator.TrafficSimulator,ignition.IgnitionSimulator
set defaultAgents=--kernel.agents.auto=sample.SampleCivilian*n 
set defaultViewer=""

if "%1" == "" set mapName=kobe
::if "%2" == "" set team="sample"


set mapPath=map
set configPath=config

:: Timestamp Logging
set logDir=logs\%mapName%-%team%%date:~10,4%_%date:~4,2%_%date:~7,2%_%time:~0,2%_%time:~3,2%_%time:~6,2%
::set logDir=logs\
md %logDir%


set tmp=-c %mapDir%/%mapName%/%configPath%/kernel.cfg --gis.map.dir=%mapDir%/%mapName%/%mapPath% --viewer.team-name=%TEAM% --kernel.logname=%logDir%/rescue.log
echo %logDir%
::call :MakeClasspath
echo [Starting Kernel]...  StartKernel team=%team% map=%mapName% %defaultParameters%
call :StartKernel %tmp%%* %defaultParameters% %defaultViewer% %defaultAgents% %defaultSimulators%

goto :eof

:StartKernel
	:MakeClasspath
		setlocal EnableDelayedExpansion
		set CP=..\supplement;
		for /f "tokens=*" %%i in ('dir ..\lib /b'); do set CP=!CP!..\lib\%%i;
		
		for /f "tokens=*" %%i in ('dir ..\jars /b'); do set CP=!CP!..\jars\%%i;


	::echo !CP!
java -Xmx1024m -cp !CP! kernel.StartKernel %*
start cmd /c  sosstart.bat
goto :eof


:PrintUsage
echo USAGE: start.bat map team [--nomenu] [--autorun]
echo For Example: start.bat test sample --autorun --nomenu