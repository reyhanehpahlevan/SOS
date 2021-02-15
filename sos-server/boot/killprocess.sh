#! /bin/bash
a=`ps -eo pid,cmd|cut -c1-60`

for f in `echo "$a"|grep "$1"|cut -c1-6`
do
	kill "$f"
done
#echo "$a">~/Desktop/A.TXT
#ps -eo pid,cmd|cut -c1-60|grep "$1"| cut -c1-6
