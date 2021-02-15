#! /bin/bash

. functions.sh

processArgs -m $*

# Delete old logs
rm -f $LOGDIR/*.log

#startGIS
startKernel $3 

#startSims --nogui --viewer.team-name="$TEAM" --viewer.maximise=true

waitFor $LOGDIR/kernel-out.log
echo "Start your agents"

#read -n1 -r -p "Press any key to continue..." key
