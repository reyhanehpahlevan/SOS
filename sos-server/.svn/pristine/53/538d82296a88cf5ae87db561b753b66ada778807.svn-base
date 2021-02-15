LOGNAME='soslogs/agent'$*'.log'
PID=
java -Xmx7G -Xms7G -Dfile.encoding=UTF-8 -classpath .:lib/jsi-1.0b2p1.jar:lib/log4j-1.2.15.jar:lib/trove-0.1.8.jar:lib/uncommons-maths-1.2.jar:lib/javaml/ajt-1.20.jar:lib/javaml/commons-math-1.1.jar:lib/javaml/javaml-0.1.4.jar:lib/javaml/weka.jar:lib/genetic/commons-math3-3.1.1/commons-math3-3.1.1.jar:lib/mysql-connector-java-5.1.18-bin.jar sos.LaunchAgents $* 2>&1 | tee "$LOGNAME"&
PIDS="${PIDS} ${!}"
echo $PID > PID
wait



