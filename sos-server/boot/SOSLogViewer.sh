#! /bin/bash

. functions.sh

makeClasspath $BASEDIR/jars $BASEDIR/lib
xterm -e "java -Xmx1024m -cp $CP sosNamayangar.NewSOSViewer -c config --kernel.logname=$1"
