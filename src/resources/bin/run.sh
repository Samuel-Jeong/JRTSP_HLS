#!/bin/sh

SERVICE_HOME=/home/jamesj/jrtsp
SERVICE_NAME=jrtsp
PATH_TO_JAR=$SERVICE_HOME/lib/jrtsp_server.jar
JAVA_CONF=$SERVICE_HOME/config/user_conf.ini
JAVA_OPT="-Dlogback.configurationFile=$SERVICE_HOME/config/logback.xml"
JAVA_OPT="$JAVA_OPT -XX:+UseG1GC -XX:G1RSetUpdatingPauseTimePercent=5 -XX:MaxGCPauseMillis=500 -XX:+PrintGCDetails -verbosegc -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime -XX:+PrintPromotionFailure -XX:PrintFLSStatistics=1 -Xloggc:$SERVICE_HOME/logs/gc.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=10M"

function exec_start() {
	ulimit -n 65535
	ulimit -s 65535
	ulimit -u 10240
	ulimit -Hn 65535
	ulimit -Hs 65535
	ulimit -Hu 10240

	java -jar $JAVA_OPT $PATH_TO_JAR RtspServerMain $JAVA_CONF > /dev/null 2>&1 &
	echo "$SERVICE_NAME started ..."
}

function exec_stop() {
	PID=`ps -ef | grep java | grep RtspServerMain | awk '{print $2}'`
	if [ -z $PID ]
	then
		echo "JRTSP is not running"
	else
		echo "stopping JRTSP"
		kill $PID
		sleep 1
		PID=`ps -ef | grep java | grep RtspServerMain | awk '{print $2}'`
		if [ ! -z $PID ]
		then
			echo "kill -9"
			kill -9 $PID
		fi
		echo "JRTSP stopped"
	fi
}

case $1 in
    restart)
		exec_stop
		exec_start
		;;
    start)
		exec_start
    ;;
    stop)
		exec_stop
    ;;
esac
