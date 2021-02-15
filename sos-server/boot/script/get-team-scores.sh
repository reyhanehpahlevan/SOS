#!/bin/sh
TEAMS="Aladdinrescue RoboAkut Persia Test Hinomiyagura YowAI NAITO-Rescue TsinghuaAeolus Seu MRL Impossibles SOS"

if [ -z "$1" ]; then
	echo "Usage: $0 <filename>"
	exit
fi

FILE="$1"

for TEAM in $TEAMS; do
	SCORE=`grep $TEAM $FILE | cut -f'3'`
	RANK=`grep -n $TEAM $FILE | cut -d':' -f1`
	echo "$TEAM $SCORE $RANK"
done
