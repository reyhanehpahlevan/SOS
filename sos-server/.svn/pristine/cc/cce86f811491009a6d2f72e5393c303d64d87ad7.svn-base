#!/bin/sh

#  os-detector.sh
#  
#
#  Created by sos on 3/29/14.
#
lowercase(){
echo "$1" | sed "y/ABCDEFGHIJKLMNOPQRSTUVWXYZ/abcdefghijklmnopqrstuvwxyz/"
}

OS=`lowercase \`uname\``

if [ "$OS" == "windowsnt" ]; then
OS=windows
elif [ "$OS" == "darwin" ]; then
OS=mac
else
OS=linux
fi
echo $OS

