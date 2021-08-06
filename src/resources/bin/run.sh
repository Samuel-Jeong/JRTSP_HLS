#!/bin/sh
HOME=/home/rtsp_server
PATH_TO_JAR=rtsp_server-jar-with-dependencies.jar
SERVICE_NAME=rtsp_server

JAVA_CONF=$HOME/rtsp_server/config/
JAVA_OPT="-Dlogback.configurationFile=$HOME/rtsp_server/config/logback.xml"
JAVA_OPT="$JAVA_OPT -Dio.netty.leakDetectionLevel=simple -Djdk.nio.maxCachedBufferSize=262144 -Dio.netty.allocator.type=unpooled"
JAVA_OPT="$JAVA_OPT -Dio.netty.noUnsafe=true -Dio.netty.noPreferDirect=true"
JAVA_OPT="$JAVA_OPT -XX:+UseG1GC -XX:G1RSetUpdatingPauseTimePercent=5 -XX:MaxGCPauseMillis=500 -XX:+UseLargePagesInMetaspace -XX:+UseLargePages -XX:+PrintGCDetails -verbosegc -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -XX:+PrintTenuringDistribution -XX:+PrintGCApplicationStoppedTime -XX:+PrintPromotionFailure -XX:PrintFLSStatistics=1 -Xloggc:$HOME/rtsp_server/logs/gc.log -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=10 -XX:GCLogFileSize=10M -Xms4G -Xmx4G"

if [ "$USER" != "rtsp_server" ] ; then
	echo "Need to be application account(rtsp_server)"
	exit 1
fi

checkfile()
{
	if [ ! -e $1 ]; then
		echo "$1" file does not exist.
		exit 2
	fi
}
checkdir()
{
	if [ ! -d $1 ]; then
		echo "$1" directory does not exist.
		exit 3
	fi
}

case $1 in
    start)
      checkdir $HOME/rtsp_server/rtsp_server/
      checkdir $HOME/rtsp_server/rtsp_server/bin
      checkdir $HOME/rtsp_server/rtsp_server/config
      checkdir $HOME/rtsp_server/rtsp_server/lib
      checkdir $HOME/rtsp_server/rtsp_server/logs

      checkfile $HOME/rtsp_server/rtsp_server/config/user_conf.ini
      checkfile $HOME/rtsp_server/rtsp_server/config/server_conf.ini
      checkfile $HOME/rtsp_server/rtsp_server/config/logback.xml

	if [ -f "$HOME/rtsp_server/lib/$PATH_TO_JAR" ]; then
	  /usr/bin/java $JAVA_OPT $DEBUG -classpath $HOME/rtsp_server/lib/$PATH_TO_JAR VoipPhoneMain $JAVA_CONF > /dev/null 2>&1 &
	  echo "$SERVICE_NAME started ..."
	  /usr/bin/logger -p info -t "$0" "rtsp_server started"
	else
	  echo "(ERROR) start fail : $?"
	  exit 4
	fi
    ;;
    stop)
	PID=`ps -ef | grep java | grep VoipPhoneMain | awk '{print $2}'`
	if [ -z $PID ]
	then
		echo "rtsp_server is not running"
	else
		echo "stopping rtsp_server"
		kill $PID
		sleep 1
		PID=`ps -ef | grep java | grep VoipPhoneMain | awk '{print $2}'`
		if [ ! -z $PID ]
		then
			echo "kill -9"
			kill -9 $PID
		fi
		echo "rtsp_server stopped"
		/usr/bin/logger -p info -t "$0" "AMF stopped"
	fi
    ;;
esac
