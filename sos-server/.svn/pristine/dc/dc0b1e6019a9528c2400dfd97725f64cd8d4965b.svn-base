DIR=`pwd`
BASEDIR="`cd .. && pwd`"
PIDS=

# Wait for a regular expression to appear in a file.
# $1 is the log to check
# $2 is the regex to wait for
# $3 is the optional output frequency. Messages will be output every n sleeps. Default 1.
# $4 is the optional sleep time. Defaults to 1 second.
function waitFor {
    SLEEP_TIME=3
    FREQUENCY=1
    if [ ! -z "$3" ]; then
        FREQUENCY=$3
    fi
    if [ ! -z "$4" ]; then
        SLEEP_TIME=$4
    fi
    F=$FREQUENCY
    echo "Waiting for '$1' to exist..."
    while [[ ! -e $1 ]]; do
        if (( --F == 0 )); then
            echo "Still waiting for '$1' to exist..."
            F=$FREQUENCY
        fi
        sleep $SLEEP_TIME
    done
#    echo "Waiting for '$2'..."
    while [ ! -z "$2" ] && [ -z "`grep \"$2\" \"$1\"`" ]; do
        if (( --F == 0 )); then
            echo "Waiting for '$2'..."
            F=$FREQUENCY
        fi
        sleep $SLEEP_TIME
    done
}

# Make a classpath argument by looking in a directory of jar files.
# Positional parameters are the directories to look in
function makeClasspath {
    RESULT="../supplement"
    while [[ ! -z "$1" ]]; do
        for NEXT in $1/*.jar; do
            RESULT="$RESULT:$NEXT"
        done
        shift
    done
    CP=${RESULT#:}
}

# Print the usage statement
function printUsage {
    echo "Usage: $0 [options]"
    echo "Options"
    echo "======="
    echo "-m    --map       <mapdir>      Set the map directory. require!"
    echo "-t    --team      <teamname>    Set the team name. require!"
#    echo "-l    --log       <logdir>      Set the log directory. Default is \"logs\""
#    echo "-c    --config    <configdir>   Set the config directory. Default is \"config\""
#    echo "-s    --timestamp               Don't Append a timestamp, the team name and map name to the log directory name"

}

# Process arguments
function processArgs {

    LOGDIR="logs"
    MAPPATH="maps"
    MAPNAME="Kobe1"
    TEAM=""
    TIMESTAMP_LOGS="yes"
    CONFIGDIR="config"
    MAPDIR="map"
    MAPNAME="$1"
    TEAM="$2"

    if [ -z $MAPNAME ] ; then
        printUsage
        exit 1
    fi
    if [ -z "$TEAM" ]; then
		printUsage
        exit 1
    fi
    	MAPPATH=$MAPPATH/$MAPNAME
	MAP=$MAPPATH/map
    	CONFIG=$MAPPATH/config/kernel.cfg


    if [ ! -d $MAP ] ; then
        echo "$MAP is not a directory"
        printUsage
        exit 1
    fi

        
        LOGDIR="$LOGDIR/$MAPNAME/$TEAM"

#    LOGDIR=`readlink -f $LOGDIR`
	echo $LOGDIR
    mkdir -p $LOGDIR
}

# Start the kernel
function startKernel {
    KERNEL_OPTIONS="-c $CONFIG --gis.map.dir=$MAP --kernel.logname=$LOGDIR/rescue.log $*"
    SIMULATORS="--kernel.simulators.auto=collapse.CollapseSimulator,clear.ClearSimulator,misc.MiscSimulator,firesimulator.FireSimulatorWrapper,traffic3.simulator.TrafficSimulator,ignition.IgnitionSimulator"	
#    VIEWER="--kernel.viewers.auto=sample.SampleViewer --viewer.team-name="$TEAM" --viewer.maximise=true"
    VIEWER="--kernel.viewers.auto=sosNamayangar.SOSViewer --viewer.team-name="$TEAM" --viewer.maximise=true"
    CIVILIAN="--kernel.agents.auto=sample.SampleCivilian*n"
    makeClasspath $BASEDIR/jars $BASEDIR/lib
    xterm -T kernel -e "java -cp $CP kernel.StartKernel $KERNEL_OPTIONS $SIMULATORS $VIEWER $CIVILIAN 2>&1 | tee $LOGDIR/kernel-out.log" &
	echo "pid=$!";
}

