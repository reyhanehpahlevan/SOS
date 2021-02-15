RemoteHost=$1
shift
SERVER_IP=$1
shift
echo $RemoteHost
echo $SERVER_IP
xterm -T "client$*" -e "ssh -X $RemoteHost 'cd ~/Desktop/sos-binary-code; bash start.sh -h $SERVER_IP $* '" &
