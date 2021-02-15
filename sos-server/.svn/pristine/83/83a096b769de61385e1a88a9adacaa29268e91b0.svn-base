#! /bin/bash

. constant.sh
LOGFOLDER="$HOME/Desktop/Rescue"
#OS=`sh os-detector.sh`
#if [ "$OS" == "mac" ]; then
#   LOGFOLDER="/Users/$USER/Desktop/Rescue"
#fi
echo $LOGFOLDER
#./copy-built.sh

./commandToClients.sh "cd ~/Desktop/sos-binary-code;if [ -e kill.sh ]; then bash kill.sh; fi"

./copy-files.sh $CLIENT1 $CLIENT2 $CLIENT3
set -m
MAPS=$*
if [ -z "$1" ]; then
    MAPS="pnu-open/Berlin2 pnu-open/Paris1 pnu-open/Kobe1 pnu-open/Istanbul1 pnu-open/Kobe2 pnu-open/Kobe4 pnu-open/Kobe3 pnu-open/Mexico1 pnu-open/Berlin3 pnu-open/Paris3 pnu-open/Berlin1 pnu-open/Eindhoven2 pnu-open/VC2 pnu-open/VC3 pnu-open/Eindhoven1 pnu-open/Paris2 pnu-open/VC1 IranOpen2013/Day2/Berlin2 IranOpen2013/Day2/Istanbul2 IranOpen2013/Day2/Kobe2 IranOpen2013/Day2/Kobe3 IranOpen2013/Day2/Istanbul3 IranOpen2013/Day2/Challenge IranOpen2013/Day2/Mexico2 IranOpen2013/Day2/Challenge2 IranOpen2013/Day2/VC3 IranOpen2013/Day2/Eindhoven1 IranOpen2013/Day2/Paris2 2011/Berlin2 2011/Paris1 2011/Kobe1 2011/Paris5 2011/Istanbul2 2011/Istanbul1 2011/Kobe2 2011/Kobe4 2011/Kobe3 2011/Berlin5 2011/Istanbul3 2011/Paris4 2011/VC5 2011/test1 2011/VC4 2011/Istanbul5 2011/kobetest 2011/test0 2011/Berlin3 2011/AMBTEST 2011/Paris3 2011/test 2011/Istanbul4 2011/Berlin1 2011/VC2 2011/Paris2 2011/kobetestfire 2011/Berlin4 2011/VC1 iranopen2012/Berlin2 iranopen2012/Istanbul2 iranopen2012/Kobe4 iranopen2012/Kobe3 iranopen2012/Berlin3 iranopen2012/VC2 iranopen2012/Tehran1 iranopen2012/Paris2 2012/Paris1 2012/Kobe1 2012/Istanbul2 2012/Kobe4 2012/Eindhoven5 2012/Kobe3 2012/Berlin5 2012/Istanbul3 2012/Paris4 2012/Mexico1 2012/Eindhoven3 2012/VC4-low 2012/Mexico3 2012/Eindhoven4 2012/Berlin3 2012/Paris3 2012/test 2012/Mexico2 2012/Berlin1 2012/VC3 2012/Eindhoven1 2012/Berlin4 2012/VC1"
	#MAPS="Kobe2"
fi

if [ ! -d $LOGFOLDER ];then mkdir -p $LOGFOLDER/; fi

echo "new running===============on `date +%Y/%m/%d-%H:%M:%S`" >>$LOGFOLDER/result
for MAP in $MAPS; do

	echo -n "Running on $MAP "
	xterm -T "Running on $MAP " -e "./run.sh  $MAP  $CLIENT1 $CLIENT2 $CLIENT3 $LOGFOLDER 2>&1 |tee run-out.log"
	score=`grep "====>score=" run-out.log |cut -d: -f2`
	echo -n $score
	echo ""
	echo "$MAP $score" >>$LOGFOLDER/result
done


