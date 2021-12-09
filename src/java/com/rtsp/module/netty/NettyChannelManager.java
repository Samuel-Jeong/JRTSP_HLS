package com.rtsp.module.netty;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.module.Streamer;
import rtsp.module.netty.module.NettyChannel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @class public class NettyChannelManager
 * @brief Netty channel manager 클래스
 * RTP Netty Channel 을 관리한다.
 */
public class NettyChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(NettyChannelManager.class);

    private static NettyChannelManager manager = null;

    private final HashMap<String, NettyChannel> channelMap = new HashMap<>();
    private final ReentrantLock channelMapLock = new ReentrantLock();

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
        try {
            channelMapLock.lock();

            String key = ip + ":" + port;

            if (channelMap.get(key) != null) {
                logger.trace("| Fail to add the channel. Key is duplicated. (key={})", key);
                return null;
            }

            /*int port = ResourceManager.getInstance().takePort();
            if (port == -1) {
                logger.warn("| Fail to add the channel. Port is full. (key={})", key);
                return false;
            }*/

            NettyChannel nettyChannel = new NettyChannel(ip, port);
            nettyChannel.run(ip, port, channelType);

            // 메시지 수신용 채널 open
            Channel channel = nettyChannel.openChannel(
                    ip,
                    port
            );

            if (channel == null) {
                nettyChannel.closeChannel();
                nettyChannel.stop();
                logger.warn("| Fail to add the channel. (key={})", key);
                return null;
            }

            channelMap.putIfAbsent(key, nettyChannel);
            logger.debug("| Success to add channel (key={}).", key);
            return nettyChannel;
        } catch (Exception e) {
            logger.warn("| Fail to add channel (ip={}, port={}).", ip, port, e);
            return null;
        } finally {
            channelMapLock.unlock();
        }
    }

    public void deleteChannel(String key) {
        try {
            channelMapLock.lock();

            if (!channelMap.isEmpty()) {
                NettyChannel nettyChannel = channelMap.get(key);
                if (nettyChannel == null) {
                    return;
                }

                nettyChannel.closeChannel();
                nettyChannel.stop();
                channelMap.remove(key);

                logger.debug("| Success to close the channel. (key={})", key);
            }
        } catch (Exception e) {
            logger.warn("| Fail to close the channel. (key={})", key, e);
        } finally {
            channelMapLock.unlock();
        }
    }

    public void deleteAllChannels () {
        try {
            channelMapLock.lock();

            if (!channelMap.isEmpty()) {
                for (Map.Entry<String, NettyChannel> entry : channelMap.entrySet()) {
                    NettyChannel nettyChannel = entry.getValue();
                    if (nettyChannel == null) {
                        continue;
                    }

                    nettyChannel.closeChannel();
                    nettyChannel.stop();
                    channelMap.remove(entry.getKey());
                }

                logger.debug("| Success to close all channel(s).");
            }
        } catch (Exception e) {
            logger.warn("| Fail to close all channel(s).", e);
        } finally {
            channelMapLock.unlock();
        }
    }

    public NettyChannel getChannel(String key) {
        try {
            channelMapLock.lock();

            return channelMap.get(key);
        } catch (Exception e) {
            return null;
        } finally {
            channelMapLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public Streamer addStreamer(String key, String listenIp, int listenPort) {
        NettyChannel nettyChannel = getChannel(listenIp + ":" + listenPort);
        if (nettyChannel == null) {
            logger.warn("Fail to add the message sender. Not found the netty channel. (listenIp={}, listenPort={})", listenIp, listenPort);
            return null;
        }

        return nettyChannel.addStreamer(key);
    }

    public Streamer getStreamer(String key, String listenIp, int listenPort) {
        NettyChannel nettyChannel = getChannel(listenIp + ":" + listenPort);
        if (nettyChannel == null) {
            logger.warn("Fail to get the message sender. Not found the netty channel. (listenIp={}, listenPort={})", listenIp, listenPort);
            return null;
        }

        return nettyChannel.getStreamer(key);
    }

    public void deleteStreamer(String key, String listenIp, int listenPort) {
        NettyChannel nettyChannel = getChannel(listenIp + ":" + listenPort);
        if (nettyChannel == null) {
            logger.warn("Fail to delete the message sender. Not found the netty channel. (listenIp={}, listenPort={})", listenIp, listenPort);
            return;
        }

        nettyChannel.deleteStreamer(key);
    }

    public void startStreaming(String key, String listenIp, int listenPort) {
        NettyChannel nettyChannel = getChannel(listenIp + ":" + listenPort);
        if (nettyChannel == null) {
            logger.warn("Fail to start to stream media. Not found the netty channel. (listenIp={}, listenPort={})", listenIp, listenPort);
            return;
        }

        nettyChannel.startStreaming(key);
    }

    public void stopStreaming(String key, String listenIp, int listenPort) {
        NettyChannel nettyChannel = getChannel(listenIp + ":" + listenPort);
        if (nettyChannel == null) {
            logger.warn("Fail to stop to stream media. Not found the netty channel. (listenIp={}, listenPort={})", listenIp, listenPort);
            return;
        }

        nettyChannel.stopStreaming(key);
    }

}
