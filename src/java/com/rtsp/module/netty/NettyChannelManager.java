package com.rtsp.module.netty;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.config.ConfigManager;
import com.rtsp.module.Streamer;
import com.rtsp.module.netty.module.RtcpNettyChannel;
import com.rtsp.module.netty.module.RtspNettyChannel;
import com.rtsp.module.netty.module.RtspRegisterNettyChannel;
import com.rtsp.service.AppInstance;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @class public class NettyChannelManager
 * @brief Netty channel manager 클래스
 * RTP Netty Channel 을 관리한다.
 */
public class NettyChannelManager {

    private static final Logger logger = LoggerFactory.getLogger(NettyChannelManager.class);

    private static NettyChannelManager manager = null;

    private RtspRegisterNettyChannel rtspRegisterNettyChannel = null;

    private final HashMap<String, RtspNettyChannel> rtspChannelMap = new HashMap<>();
    private final ReentrantLock rtspChannelMapLock = new ReentrantLock();

    private final HashMap<String, RtcpNettyChannel> rtcpChannelMap = new HashMap<>();
    private final ReentrantLock rtcpChannelMapLock = new ReentrantLock();

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

    public void stop() {
        deleteAllRtspChannels();
        deleteAllRtcpChannels();
    }

    ////////////////////////////////////////////////////////////////////////////////

    // Register 버튼 클릭 시 호출
    public boolean addRegisterChannel() {
        if (rtspRegisterNettyChannel != null) {
            return false;
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        rtspRegisterNettyChannel = new RtspRegisterNettyChannel(
                configManager.getLocalListenIp(),
                configManager.getLocalRtspRegisterListenPort()
        );
        rtspRegisterNettyChannel.run();
        rtspRegisterNettyChannel.start();

        return true;
    }

    // 프로그램 종료 시 호출
    public void removeRegisterChannel() {
        if (rtspRegisterNettyChannel == null) {
            return;
        }

        rtspRegisterNettyChannel.stop();
        rtspRegisterNettyChannel = null;
    }

    public RtspRegisterNettyChannel getRegisterChannel() {
        return rtspRegisterNettyChannel;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public RtspNettyChannel openRtspChannel(String rtspUnitId, String ip, int port) {
        try {
            rtspChannelMapLock.lock();

            if (rtspChannelMap.get(rtspUnitId) != null) {
                logger.trace("| ({}) Fail to add the channel. Key is duplicated.", rtspUnitId);
                return null;
            }

            /*int port = ResourceManager.getInstance().takePort();
            if (port == -1) {
                logger.warn("| Fail to add the channel. Port is full. (key={})", key);
                return false;
            }*/

            RtspNettyChannel rtspNettyChannel = new RtspNettyChannel(rtspUnitId, ip, port);
            rtspNettyChannel.run(ip, port);

            // 메시지 수신용 채널 open
            Channel channel = rtspNettyChannel.openChannel(
                    ip,
                    port
            );

            if (channel == null) {
                rtspNettyChannel.closeChannel();
                rtspNettyChannel.stop();
                logger.warn("| ({}) Fail to add the channel.", rtspUnitId);
                return null;
            }

            rtspChannelMap.putIfAbsent(rtspUnitId, rtspNettyChannel);
            logger.debug("| ({}) Success to add channel.", rtspUnitId);
            return rtspNettyChannel;
        } catch (Exception e) {
            logger.warn("| ({}) Fail to add channel (ip={}, port={}).", rtspUnitId, ip, port, e);
            return null;
        } finally {
            rtspChannelMapLock.unlock();
        }
    }

    public void deleteRtspChannel(String rtspUnitId) {
        try {
            rtspChannelMapLock.lock();

            if (!rtspChannelMap.isEmpty()) {
                RtspNettyChannel rtspNettyChannel = rtspChannelMap.get(rtspUnitId);
                if (rtspNettyChannel == null) {
                    return;
                }

                rtspNettyChannel.closeChannel();
                rtspNettyChannel.stop();
                rtspChannelMap.remove(rtspUnitId);

                logger.debug("| ({}) Success to close the channel.", rtspUnitId);
            }
        } catch (Exception e) {
            logger.warn("| ({}) Fail to close the channel.", rtspUnitId, e);
        } finally {
            rtspChannelMapLock.unlock();
        }
    }

    public void deleteAllRtspChannels () {
        try {
            rtspChannelMapLock.lock();

            if (!rtspChannelMap.isEmpty()) {
                for (Map.Entry<String, RtspNettyChannel> entry : rtspChannelMap.entrySet()) {
                    RtspNettyChannel rtspNettyChannel = entry.getValue();
                    if (rtspNettyChannel == null) {
                        continue;
                    }

                    rtspNettyChannel.closeChannel();
                    rtspNettyChannel.stop();
                    rtspChannelMap.remove(entry.getKey());
                }

                logger.debug("| Success to close all channel(s).");
            }
        } catch (Exception e) {
            logger.warn("| Fail to close all channel(s).", e);
        } finally {
            rtspChannelMapLock.unlock();
        }
    }

    public RtspNettyChannel getRtspChannel(String rtspUnitId) {
        try {
            rtspChannelMapLock.lock();

            return rtspChannelMap.get(rtspUnitId);
        } catch (Exception e) {
            return null;
        } finally {
            rtspChannelMapLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public RtcpNettyChannel openRtcpChannel(String rtspUnitId, String ip, int port) {
        try {
            rtcpChannelMapLock.lock();

            if (rtcpChannelMap.get(rtspUnitId) != null) {
                logger.trace("| ({}) Fail to add the rtcp channel. Key is duplicated.", rtspUnitId);
                return null;
            }

            /*int port = ResourceManager.getInstance().takePort();
            if (port == -1) {
                logger.warn("| Fail to add the channel. Port is full. (key={})", key);
                return false;
            }*/

            RtcpNettyChannel rtcpNettyChannel = new RtcpNettyChannel(rtspUnitId, ip, port);
            rtcpNettyChannel.run(ip, port);

            // 메시지 수신용 채널 open
            Channel channel = rtcpNettyChannel.openChannel(
                    ip,
                    port
            );

            if (channel == null) {
                rtcpNettyChannel.closeChannel();
                rtcpNettyChannel.stop();
                logger.warn("| ({}) Fail to add the rtcp channel.", rtspUnitId);
                return null;
            }

            rtcpChannelMap.putIfAbsent(rtspUnitId, rtcpNettyChannel);
            logger.debug("| ({}) Success to add rtcp channel.", rtspUnitId);
            return rtcpNettyChannel;
        } catch (Exception e) {
            logger.warn("| ({}) Fail to add rtcp channel (ip={}, port={}).", rtspUnitId, ip, port, e);
            return null;
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    public void deleteRtcpChannel(String rtspUnitId) {
        try {
            rtcpChannelMapLock.lock();

            if (!rtcpChannelMap.isEmpty()) {
                RtcpNettyChannel rtcpNettyChannel = rtcpChannelMap.get(rtspUnitId);
                if (rtcpNettyChannel == null) {
                    return;
                }

                rtcpNettyChannel.closeChannel();
                rtcpNettyChannel.stop();
                rtcpChannelMap.remove(rtspUnitId);

                logger.debug("| ({}) Success to close the rtcp channel.", rtspUnitId);
            }
        } catch (Exception e) {
            logger.warn("| ({}) Fail to close the rtcp channel.", rtspUnitId, e);
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    public void deleteAllRtcpChannels () {
        try {
            rtcpChannelMapLock.lock();
            rtcpChannelMap.entrySet().removeIf(Objects::nonNull);

            logger.debug("| Success to close all rtcp channel(s).");
        } catch (Exception e) {
            logger.warn("| Fail to close all rtcp channel(s).", e);
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    public RtcpNettyChannel getRtcpChannel(String rtspUnitId) {
        try {
            rtcpChannelMapLock.lock();

            return rtcpChannelMap.get(rtspUnitId);
        } catch (Exception e) {
            return null;
        } finally {
            rtcpChannelMapLock.unlock();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public Streamer addStreamer(String rtspUnitId, String sessionId, String listenIp, int listenPort) {
        RtspNettyChannel rtspNettyChannel = getRtspChannel(rtspUnitId);
        if (rtspNettyChannel == null) {
            logger.warn("({}) ({}) Fail to add the message sender. Not found the netty channel. (listenIp={}, listenPort={})", rtspUnitId, sessionId, listenIp, listenPort);
            return null;
        }

        return rtspNettyChannel.addStreamer(sessionId);
    }

    public Streamer getStreamer(String rtspUnitId, String sessionId, String listenIp, int listenPort) {
        RtspNettyChannel rtspNettyChannel = getRtspChannel(rtspUnitId);
        if (rtspNettyChannel == null) {
            logger.warn("({}) ({}) Fail to get the message sender. Not found the netty channel. (listenIp={}, listenPort={})", rtspUnitId, sessionId, listenIp, listenPort);
            return null;
        }

        return rtspNettyChannel.getStreamer(sessionId);
    }

    public void deleteStreamer(String rtspUnitId, String sessionId, String listenIp, int listenPort) {
        RtspNettyChannel rtspNettyChannel = getRtspChannel(rtspUnitId);
        if (rtspNettyChannel == null) {
            logger.warn("({}) ({}) Fail to delete the message sender. Not found the netty channel. (listenIp={}, listenPort={})", rtspUnitId, sessionId, listenIp, listenPort);
            return;
        }

        rtspNettyChannel.deleteStreamer(sessionId);
    }

    public void startStreaming(String rtspUnitId, String sessionId, String listenIp, int listenPort) {
        RtspNettyChannel rtspNettyChannel = getRtspChannel(rtspUnitId);
        if (rtspNettyChannel == null) {
            logger.warn("({}) Fail to start to stream media. Not found the netty channel. (listenIp={}, listenPort={})", rtspUnitId, listenIp, listenPort);
            return;
        }

        rtspNettyChannel.startStreaming(sessionId);
    }

    public void pauseStreaming(String rtspUnitId, String sessionId, String listenIp, int listenPort) {
        RtspNettyChannel rtspNettyChannel = getRtspChannel(rtspUnitId);
        if (rtspNettyChannel == null) {
            logger.warn("({}) Fail to pause to stream media. Not found the netty channel. (listenIp={}, listenPort={})", rtspUnitId, listenIp, listenPort);
            return;
        }

        rtspNettyChannel.pauseStreaming(sessionId);
    }

    public void stopStreaming(String rtspUnitId, String sessionId, String listenIp, int listenPort) {
        RtspNettyChannel rtspNettyChannel = getRtspChannel(rtspUnitId);
        if (rtspNettyChannel == null) {
            logger.warn("({}) Fail to stop to stream media. Not found the netty channel. (listenIp={}, listenPort={})", rtspUnitId, listenIp, listenPort);
            return;
        }

        rtspNettyChannel.stopStreaming(sessionId);
    }

}
