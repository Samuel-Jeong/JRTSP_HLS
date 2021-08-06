package com.rtsp.module.netty;

import com.rtsp.module.netty.module.NettyChannel;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @class public class NettyChannelManager
 * @brief Netty channel manager 클래스
 * RTP Netty Channel 을 관리한다.
 */
public class NettyChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(NettyChannelManager.class);

    private static NettyChannelManager manager = null;

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn private NettyChannelManager ()
     * @brief NettyChannelManager 생성자 함수
     */
    private NettyChannelManager() {
        // Nothing
    }

    /**
     * @return 최초 호출 시 새로운 NettyChannelManager 전역 변수, 이후 모든 호출에서 항상 이전에 생성된 변수 반환
     * @fn public static NettyChannelManager getInstance ()
     * @brief NettyChannelManager 싱글턴 변수를 반환하는 함수
     */
    public static NettyChannelManager getInstance () {
        if (manager == null) {
            manager = new NettyChannelManager();

        }
        return manager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public NettyChannel openChannel(String ip, int port, int channelType) {
        NettyChannel nettyChannel;

        nettyChannel = new NettyChannel(port);
        nettyChannel.run(ip + ":" + port, channelType);

        Channel channel = nettyChannel.openChannel(
                ip,
                port
        );

        if (channel == null) {
            logger.warn("| Fail to open channel.");
        } else {
            logger.debug("| Success to open channel.");
        }

        return nettyChannel;
    }

    public void closeChannel(NettyChannel nettyChannel) {
        if (nettyChannel != null) {
            nettyChannel.closeChannel();
            nettyChannel.stop();

            logger.debug("| Success to close the channel.");
        }
    }

}
