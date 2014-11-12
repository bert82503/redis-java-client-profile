
##
# 简单的Redis初始化脚本模板
#
# 根据"进程ID文件"是否存在来进行相应的逻辑处理
##

case "$1" in
    start)
        if [ -f $PIDFILE ]
        then
            echo "$PIDFILE exists, process is already running or crashed"
        else
            echo "Starting Redis server..."
            # 加载配置文件
            $EXEC $CONF
        fi
        ;;
    stop)
        if [ ! -f $PIDFILE ]
        then
            echo "$PIDFILE does not exist, process is not running"
        else
            PID=$(cat $PIDFILE)
            echo "Stopping ..."
            $CLIEXEC -p $REDISPORT shutdown
            while [ -x /proc/${PID} ]
            do
                echo "Waiting for Redis to shutdown ..."
                sleep 1
            done
            echo "Redis stopped"
        fi
        ;;
    status)
        if [ ! -f $PIDFILE ]
        then
            echo 'Redis is not running'
        else
            echo "Redis is running ($(<$PIDFILE))"
        fi
        ;;
    restart)
        $0 stop
        $0 start
        ;;
    *)
        echo "Please use start, stop, restart or status as first argument"
        ;;
esac
