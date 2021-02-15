DIR=`pwd`
BASEDIR="`cd .. && pwd`"
PIDS=
MAPPATH="maps"

LOGDIR="logs"
MAP=""
TEAM="SOS"
TIMESTAMP_LOGS=""
OTHER_OPTIONS=""

# Wait for a regular expression to appear in a file.
# $1 is the log to check
# $2 is the regex to wait for
# $3 is the optional output frequency. Messages will be output every n sleeps. Default 1.
# $4 is the optional sleep time. Defaults to 1 second.
realpath() {
[[ $1 = /* ]] && echo "$1" || echo "$PWD/${1#./}"
}


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
    echo "Usage: $0 <map> [options]"
    echo "Options"
    echo "======="
    echo "<map> Set the map name"
    echo "-m	--map			  Set the map name"
    echo "-l    --log       <logdir>      Set the log directory. Default is \"logs\""
    echo "-s    --timestamp               Append a timestamp, the team name and map name to the log directory name"
    echo "-t    --team      <teamname>    Set the team name. Default is \"\""
}



# Process arguments
function processArgs {
    while [[ ! -z "$1" ]]; do

        case "$1" in
            -m | --map)
		    if [ -z $2 ] ; then
			echo "MAP is require"
		        printUsage
		        exit 1
		    fi

		MAPNAME="$2"
		MAP=$MAPPATH/$MAPNAME/map
	    	CONFIG=$MAPPATH/$MAPNAME/config/kernel.cfg
                shift 2
                ;;
            -l | --log)
    		    if [ -z $2 ] ; then
			echo "log is require"
		        printUsage
		        exit 1
		    fi
                LOGDIR="$2"
                shift 2
                ;;
            -t | --team)
    		    if [ -z $2 ] ; then
			echo "team is require"
		        printUsage
		        exit 1
		    fi
                TEAM="$2"
                shift 2
                ;;
            -s | --timestamp)
                TIMESTAMP_LOGS="yes";
                shift
                ;;
            -h | --help)
                printUsage
                exit 1;
                ;;
            
            *)
#		echo "$OTHER_OPTIONS '$1'"
                #$OTHER_OPTIONS=: $1
                shift
                ;;

        esac
    done

    if [ -z $MAP ] ; then
	echo "MAP is require"
        printUsage
        exit 1
    fi
    if [ ! -d $MAP ] ; then
        echo "$MAP is not a directory"
        printUsage
        exit 1
    fi

    if [ ! -z "$TIMESTAMP_LOGS" ] ; then
        TIME="`date +%m%d-%H%M%S`"
        MAPNAME="`basename $MAP`"
        if [ -z "$TEAM" ]; then
            LOGDIR="$LOGDIR/$TIME-$MAPNAME"
        else
            LOGDIR="$LOGDIR/$TIME-$TEAM-$MAPNAME"
        fi
    fi

    mkdir -p $LOGDIR
    LOGDIR=`realpath $LOGDIR`

	echo $LOGDIR
}

# Process arguments
function processArgs1 {

    LOGDIR="logs"
    MAPPATH="maps"
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
    KERNEL_OPTIONS="-c $CONFIG --gis.map.dir=$MAP --kernel.logname=$LOGDIR/rescue.soslog $*"
    SIMULATORS="--kernel.simulators.auto=collapse.CollapseSimulator,clear.ClearSimulator,misc.MiscSimulator,firesimulator.FireSimulatorWrapper,traffic3.simulator.TrafficSimulator,ignition.IgnitionSimulator"	
#    VIEWER="--kernel.viewers.auto=sample.SampleViewer --viewer.team-name="$TEAM" --viewer.maximise=true"
    VIEWER="--kernel.viewers.auto=sosNamayangar.SOSViewer --viewer.team-name="$TEAM" --viewer.maximise=true"
    CIVILIAN="--kernel.agents.auto=sample.SampleCivilian*n"
    makeClasspath $BASEDIR/jars $BASEDIR/lib
#    xterm -T kernel -e "java -cp $CP kernel.StartKernel $KERNEL_OPTIONS $SIMULATORS $VIEWER $CIVILIAN $OTHER_OPTIONS 2>&1 | tee $LOGDIR/kernel-out.log" &

java -Xmx3G -cp $CP kernel.StartKernel $KERNEL_OPTIONS $SIMULATORS $VIEWER $CIVILIAN $OTHER_OPTIONS 2>&1 | tee $LOGDIR/kernel-out.log &

echo "pid=$!";
# echo "java -cp $CP kernel.StartKernel $KERNEL_OPTIONS $SIMULATORS $VIEWER $CIVILIAN $OTHER_OPTIONS 2>&1 | tee $LOGDIR/kernel-out.log"
    echo $KERNEL_OPTIONS
}

