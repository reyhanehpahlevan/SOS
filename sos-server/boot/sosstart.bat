::Author Ali Modaresi

@echo off

::if "%1" == "/?" GOTo :PrintUsage
echo "USAGE: start.bat map [--nomenu] [--autorun]

::-> default map dir
set mapDir=maps

set mapName=%1
set team=%2
set exitafterrun=0
set defaultParameters=
set defaultSimulators=--kernel.simulators.auto=collapse.CollapseSimulator,clear.ClearSimulator,misc.MiscSimulator,firesimulator.FireSimulatorWrapper,traffic3.simulator.TrafficSimulator,ignition.IgnitionSimulator
set defaultAgents=--kernel.agents.auto=sample.SampleCivilian*n 
set defaultViewer=--kernel.viewers.auto=sosNamayangar.SOSViewer --viewer.maximise=true
::set defaultViewer=--kernel.viewers.auto=

::if "%1" == "" set mapName=maps2011\kobe3
::if "%1" == "" set mapName=maps2011\Istanbul1
if "%1" == "" set mapName=maps2011\AMBTEST
::if "%2" == "" set team="sample"
if "%1" == "--fullmappath" (
	
	set mapName=%team:maps\=%
	set mapDir=maps
	set team=SOS
	set exitafterrun=1
)


set mapPath=map
set configPath=config

:: Timestamp Logging
::set logDir=logs\%mapName%-%team%%date:~10,4%_%date:~4,2%_%date:~7,2%_%time:~0,2%_%time:~3,2%_%time:~6,2%
set logDir=rescuelogs\
md %logDir%
set logName=%logDir%\%mapName:\=_%.soslog 
::set logName=%logDir%\%mapName:\=.%-%team%.soslog 

set tmp=-c %mapDir%/%mapName%/%configPath%/kernel.cfg --gis.map.dir=%mapDir%/%mapName%/%mapPath% --viewer.team-name=%TEAM% --kernel.logname=%logName%
echo %tmp%
echo %logDir%
::call :MakeClasspath
echo [Starting Kernel]...  StartKernel team=%team% map=%mapName% %defaultParameters%
call :StartKernel %tmp%%* %defaultParameters% %defaultViewer% %defaultAgents% %defaultSimulators% %other%

goto :eof

:StartKernel
	:MakeClasspath
		setlocal EnableDelayedExpansion
		set CP=..\supplement;
		for /f "tokens=*" %%i in ('dir ..\lib /b'); do set CP=!CP!..\lib\%%i;
		
		for /f "tokens=*" %%i in ('dir ..\jars /b'); do set CP=!CP!..\jars\%%i;


	::echo !CP!
	::echo %*
java -Xmx1024m -cp !CP! kernel.StartKernel %*
::start java -Xmx1024m -cp !CP! sosNamayangar.NewSOSViewer %*
::pause

if %exitafterrun% == 1 (
	::start sosstart.bat %mapName% 
	exit

)
goto :eof


:PrintUsage
echo USAGE: start.bat map team [--nomenu] [--autorun]
echo For Example: start.bat test sample --autorun --nomenu
