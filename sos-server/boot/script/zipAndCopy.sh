#! /bin/bash

CLIENT=$1
PATH=$2
NAME=$3".7z"

echo "Zip And Copy $CLIENT $PATH $NAME";
echo "compressing $NAME logs";

#/usr/bin/ssh -X $CLIENT "cd ~/Desktop/sos-binary-code;tar -czf logs-$NAME soslogs";
/usr/bin/ssh -X $CLIENT "cd ~/Desktop/sos-binary-code;7za a -mx4 logs-$NAME soslogs >/dev/null";
echo "copying $NAME logs";
/usr/bin/scp $CLIENT:~/Desktop/sos-binary-code/logs-$NAME $PATH/client-$NAME;
echo "finished copying $NAME logs";
