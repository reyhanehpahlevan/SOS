#! /bin/bash

. functions.sh

processArgs -m $*

# Delete old logs
rm -f $LOGDIR/*.log

#startGIS
startKernel --nomenu $3

#startSims --nogui --viewer.team-name="$TEAM" --viewer.maximise=true

waitFor $LOGDIR/kernel-out.log
echo "Start your agents"
waitFor $LOGDIR/kernel-out.log "Kernel is shutting down" 300
echo "simulation has been finished"
grep "====>score=" $LOGDIR/kernel-out.log |cut -d: -f2 | awk '{ print $5}'

#read -n1 -r -p "Press any key to continue..." key
