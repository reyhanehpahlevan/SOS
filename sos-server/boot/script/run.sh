#! /bin/bash
# $1 is the map name
# $2 is the Amb&center Client name or ip
# $3 is the Fire Client name or ip
# $4 is the Police Client name or ip
# $5 is the optional log directory
SERVER_ARGS=--autorun1

if [ -z "$1" ]; then
    echo "Usage: $0 Map user@Amb&centerIP user@FireIP user@PoliceIP [logdir]"
    exit
fi

. functions.sh
PIDS=
START_SLEEP=10
SIMULATION_SLEEP=900


SERVER_IP=`ifconfig eth| grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'|sed -n '1p'`
if [ "" == "$SERVER_IP" ]; then
	SERVER_IP=`ifconfig wlan| grep 'inet addr:' | cut -d: -f2 | awk '{ print $1}'|sed -n '1p'`
fi
if [ "" == "$SERVER_IP" ]; then
    SERVER_IP=`ifconfig en0| grep 'inet' | cut -d: -f2 | awk '{ print $2}'| sed -n '2p'`
fi
if [ "" == "$SERVER_IP" ]; then
    SERVER_IP=`ifconfig en1| grep 'inet' | cut -d: -f2 | awk '{ print $2}'| sed -n '2p'`
fi

echo $SERVER_IP
#exit 0
CLIENT1=$2
CLIENT2=$3
CLIENT3=$4

ssh -X $CLIENT1 'killall xclock;xclock -digital -strftime "`cat /etc/hostname` %r" -update 1'&
ssh -X $CLIENT2 'killall xclock;xclock -digital -strftime "`cat /etc/hostname` %r" -update 1'&
ssh -X $CLIENT3 'killall xclock;xclock -digital -strftime "`cat /etc/hostname` %r" -update 1'&



MAP=$1

LOGDIR="logs"
if [ ! -z "$5" ]; then
    LOGDIR=$5
fi

# First start the simulator

echo "Starting server"

sh -c 'if [ -z run-out.log ];then rm run-out.log;fi'
sh -c 'cd ~/Desktop/sos-server/boot;if [ -d logs ];then rm -r logs; fi;mkdir -p logs;'

xterm -T server -e "cd ~/Desktop/sos-server/boot/; ./scriptedstartkernel.sh -m $MAP $SERVER_ARGS  2>&1 | tee $HOME/Desktop/sos-server/boot/logs/startkernel-out.log" &
PIDS="${PIDS} ${!}"
# Wait for it to st art
echo "Sleeping for $START_SLEEP seconds"
sleep $START_SLEEP


# Now start agents
echo "starting agents..."

ssh -X $CLIENT1 'cd ~/Desktop/sos-binary-code;if [ -d soslogs ];then rm -r soslogs; fi;mkdir -p soslogs;'
ssh -X $CLIENT2 'cd ~/Desktop/sos-binary-code;if [ -d soslogs ];then rm -r soslogs; fi;mkdir -p soslogs;'
ssh -X $CLIENT3 'cd ~/Desktop/sos-binary-code;if [ -d soslogs ];then rm -r soslogs; fi;mkdir -p soslogs;'

xterm -geometry 150x50 -T clientAmbulance_Center -e "ssh -X $CLIENT1 'export DISPLAY=:0;cd ~/Desktop/sos-binary-code; bash start.sh -h $SERVER_IP -ambulance -center ' 2>&1 | tee '$HOME/Desktop/sos-server/boot/script/logs/agentAmbulance_Center.log'" &

PIDS="${PIDS} ${!}"
xterm -geometry 150x50 -T clientFire -e "ssh -X $CLIENT2 'export DISPLAY=:0; cd ~/Desktop/sos-binary-code; bash start.sh -h $SERVER_IP -fire' 2>&1 | tee '$HOME/Desktop/sos-server/boot/script/logs/agentFire.log'" &

PIDS="${PIDS} ${!}"
xterm -geometry 150x50 -T clientPolice -e "ssh -X $CLIENT3 'export DISPLAY=:0;cd ~/Desktop/sos-binary-code; bash start.sh -h $SERVER_IP -police' 2>&1 | tee '$HOME/Desktop/sos-server/boot/script/logs/agentPolice.log'" &

PIDS="${PIDS} ${!}"
waitFor ~/Desktop/sos-server/boot/logs/startkernel-out.log "simulation has been finished" 30
grep "====>score=" ~/Desktop/sos-server/boot/logs/startkernel-out.log |cut -d: -f2 | awk '{ print $1}'

# Kill all processes
echo "Killing processes"
ssh $CLIENT1 "cd ~/Desktop/sos-binary-code;if [ -e kill.sh ]; then bash kill.sh; fi"&
ssh $CLIENT2 "cd ~/Desktop/sos-binary-code;if [ -e kill.sh ]; then bash kill.sh; fi"&
ssh $CLIENT3 "cd ~/Desktop/sos-binary-code;if [ -e kill.sh ]; then bash kill.sh; fi"&



#ssh $CLIENT1 "killall xterm"
#ssh $CLIENT2 "killall xterm"
#ssh $CLIENT3 "killall xterm"

#bash killprocess.sh "xterm -T client"

#bash killallserverprocess.sh
kill -9 $PIDS

TIME="`date +%m-%d%%%H:%M`"
path=${LOGDIR}/${MAP}/$TIME
echo $path
if [ ! -e $path ]; then
    mkdir -p $path
fi

./zipAndCopy.sh $CLIENT1 $path ambcenter &
./zipAndCopy.sh $CLIENT2 $path fire &
./zipAndCopy.sh $CLIENT3 $path police &

echo "compressing and moving server logs"
sh -c "cd ~/Desktop/sos-server/boot;7za a -mx4 logs.7z logs"
if [ "$?" == "0" ];then
    mv ~/Desktop/sos-server/boot/logs.7z $path/server.7z
else
    sh -c "cd ~/Desktop/sos-server/boot;tar -czf logs.tar logs"
    mv ~/Desktop/sos-server/boot/logs.tar $path/server.tar
fi
#sh -c "cd ~/Desktop/sos-server/boot;tar -czf logs.tgz logs"
#mv ~/Desktop/sos-server/boot/logs.tgz $path/server.tgz
ssh -X $CLIENT1 'killall xclock;'
ssh -X $CLIENT2 'killall xclock;'
ssh -X $CLIENT3 'killall xclock;'
wait

echo "compressing and copying finished..."
sleep 5;








