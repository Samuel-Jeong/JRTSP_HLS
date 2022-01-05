package rtsp.module.netty.handler;

import com.fsm.module.StateHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.config.ConfigManager;
import rtsp.fsm.RtspEvent;
import rtsp.fsm.RtspState;
import rtsp.module.RtspManager;
import rtsp.module.base.RtspUnit;
import rtsp.module.netty.NettyChannelManager;
import rtsp.module.netty.module.RtspRegisterNettyChannel;
import rtsp.protocol.register.RegisterRtspUnitReq;
import rtsp.protocol.register.RegisterRtspUnitRes;
import rtsp.protocol.register.UnRegisterRtspUnitReq;
import rtsp.protocol.register.UnRegisterRtspUnitRes;
import rtsp.protocol.register.base.URtspHeader;
import rtsp.protocol.register.base.URtspMessageType;
import rtsp.service.AppInstance;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class RtspRegisterChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(RtspRegisterChannelHandler.class);

    private final String ip;
    private final int port;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspRegisterChannelHandler(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) {
        try {
            RtspRegisterNettyChannel rtspRegisterNettyChannel = NettyChannelManager.getInstance().getRegisterChannel();
            if (rtspRegisterNettyChannel == null) {
                logger.warn("RtspRegister Channel is not defined.");
                return;
            }

            ByteBuf buf = datagramPacket.content();
            if (buf == null) {
                logger.warn("DatagramPacket's content is null.");
                return;
            }

            int readBytes = buf.readableBytes();
            if (buf.readableBytes() <= 0) {
                logger.warn("Message is null.");
                return;
            }

            byte[] data = new byte[readBytes];
            buf.getBytes(0, data);

            ConfigManager configManager = AppInstance.getInstance().getConfigManager();

            URtspHeader uRtspHeader = new URtspHeader(data);
            if (uRtspHeader.getMessageType() == URtspMessageType.REGISTER) {
                RegisterRtspUnitReq registerRtspUnitReq = new RegisterRtspUnitReq(data);
                logger.debug("[>] {} ({})", registerRtspUnitReq, readBytes);

                String rtspUnitId = registerRtspUnitReq.getId();
                String nonce = registerRtspUnitReq.getNonce();

                RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit(rtspUnitId);
                if (rtspUnit == null) { // NOT AUTHORIZED
                    RegisterRtspUnitRes registerRtspUnitRes = new RegisterRtspUnitRes(
                            configManager.getMagicCookie(),
                            registerRtspUnitReq.getURtspHeader().getMessageType(),
                            registerRtspUnitReq.getURtspHeader().getSeqNumber(),
                            registerRtspUnitReq.getURtspHeader().getTimeStamp(),
                            configManager.getRealm(),
                            RegisterRtspUnitRes.NOT_AUTHORIZED
                    );
                    registerRtspUnitRes.setReason("NOT_AUTHORIZED");

                    // RTSP ID 등록
                    RtspManager.getInstance().openRtspUnit(
                            rtspUnitId,
                            configManager.getLocalListenIp(),
                            configManager.getLocalRtspListenPort()
                    );

                    rtspRegisterNettyChannel.sendResponse(datagramPacket.sender().getAddress().getHostAddress(), registerRtspUnitReq.getListenPort(), registerRtspUnitRes);
                } else {
                    RegisterRtspUnitRes registerRtspUnitRes;
                    StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                    String curState = rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId()).getCurState();
                    if (!curState.equals(RtspState.IDLE)) {
                        registerRtspUnitRes = new RegisterRtspUnitRes(
                                configManager.getMagicCookie(),
                                registerRtspUnitReq.getURtspHeader().getMessageType(),
                                registerRtspUnitReq.getURtspHeader().getSeqNumber(),
                                registerRtspUnitReq.getURtspHeader().getTimeStamp(),
                                configManager.getRealm(),
                                RegisterRtspUnitRes.STATE_ERROR
                        );
                    } else {

                        if (!rtspUnit.isRegistered()) {
                            // 1) Check nonce
                            // 2) If ok, open rtsp channel
                            // 3) If not, reject
                            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
                            messageDigest.update(configManager.getRealm().getBytes(StandardCharsets.UTF_8));
                            messageDigest.update(configManager.getHashKey().getBytes(StandardCharsets.UTF_8));
                            byte[] a1 = messageDigest.digest();
                            messageDigest.reset();
                            messageDigest.update(a1);

                            String curNonce = new String(messageDigest.digest());
                            if (curNonce.equals(nonce)) {
                                // RTSP Channel OPEN (New RtspUnit)
                                registerRtspUnitRes = new RegisterRtspUnitRes(
                                        configManager.getMagicCookie(),
                                        registerRtspUnitReq.getURtspHeader().getMessageType(),
                                        registerRtspUnitReq.getURtspHeader().getSeqNumber(),
                                        registerRtspUnitReq.getURtspHeader().getTimeStamp(),
                                        configManager.getRealm(),
                                        RegisterRtspUnitRes.SUCCESS
                                );
                                rtspUnit.setRegistered(true);

                                rtspStateHandler.fire(
                                        RtspEvent.REGISTER,
                                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                );
                            } else {
                                registerRtspUnitRes = new RegisterRtspUnitRes(
                                        configManager.getMagicCookie(),
                                        registerRtspUnitReq.getURtspHeader().getMessageType(),
                                        registerRtspUnitReq.getURtspHeader().getSeqNumber(),
                                        registerRtspUnitReq.getURtspHeader().getTimeStamp(),
                                        configManager.getRealm(),
                                        RegisterRtspUnitRes.NOT_AUTHORIZED
                                );
                                registerRtspUnitRes.setReason("WRONG_NONCE");

                                // RTSP ID 등록 해제
                                RtspManager.getInstance().closeRtspUnit(rtspUnitId);
                                rtspUnit.setRegistered(false);
                            }
                        } else {
                            registerRtspUnitRes = new RegisterRtspUnitRes(
                                    configManager.getMagicCookie(),
                                    registerRtspUnitReq.getURtspHeader().getMessageType(),
                                    registerRtspUnitReq.getURtspHeader().getSeqNumber(),
                                    registerRtspUnitReq.getURtspHeader().getTimeStamp(),
                                    configManager.getRealm(),
                                    RegisterRtspUnitRes.SUCCESS
                            );

                            rtspStateHandler.fire(
                                    RtspEvent.REGISTER,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                    }

                    rtspRegisterNettyChannel.sendResponse(
                            datagramPacket.sender().getAddress().getHostAddress(),
                            registerRtspUnitReq.getListenPort(),
                            registerRtspUnitRes
                    );
                }
            } else if (uRtspHeader.getMessageType() == URtspMessageType.UNREGISTER) {
                UnRegisterRtspUnitReq unRegisterRtspUnitReq = new UnRegisterRtspUnitReq(data);
                logger.debug("[>] {} ({})", unRegisterRtspUnitReq, readBytes);

                String rtspUnitId = unRegisterRtspUnitReq.getId();
                RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit(rtspUnitId);
                UnRegisterRtspUnitRes unRegisterRtspUnitRes;
                if (rtspUnit == null) {
                    unRegisterRtspUnitRes = new UnRegisterRtspUnitRes(
                            configManager.getMagicCookie(),
                            unRegisterRtspUnitReq.getURtspHeader().getMessageType(),
                            unRegisterRtspUnitReq.getURtspHeader().getSeqNumber(),
                            unRegisterRtspUnitReq.getURtspHeader().getTimeStamp(),
                            UnRegisterRtspUnitRes.NOT_ACCEPTED
                    );
                } else {
                    String curState = rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId()).getCurState();
                    if (!curState.equals(RtspState.REGISTER)) {
                        unRegisterRtspUnitRes = new UnRegisterRtspUnitRes(
                                configManager.getMagicCookie(),
                                unRegisterRtspUnitReq.getURtspHeader().getMessageType(),
                                unRegisterRtspUnitReq.getURtspHeader().getSeqNumber(),
                                unRegisterRtspUnitReq.getURtspHeader().getTimeStamp(),
                                UnRegisterRtspUnitRes.STATE_ERROR
                        );
                    } else {
                        unRegisterRtspUnitRes = new UnRegisterRtspUnitRes(
                                configManager.getMagicCookie(),
                                unRegisterRtspUnitReq.getURtspHeader().getMessageType(),
                                unRegisterRtspUnitReq.getURtspHeader().getSeqNumber(),
                                unRegisterRtspUnitReq.getURtspHeader().getTimeStamp(),
                                UnRegisterRtspUnitRes.SUCCESS
                        );

                        // RTSP ID 등록 해제
                        RtspManager.getInstance().closeRtspUnit(rtspUnitId);
                        rtspUnit.setRegistered(false);

                        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                        rtspStateHandler.fire(
                                RtspEvent.IDLE,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );
                    }
                }

                rtspRegisterNettyChannel.sendResponse(
                        datagramPacket.sender().getAddress().getHostAddress(),
                        unRegisterRtspUnitReq.getListenPort(),
                        unRegisterRtspUnitRes
                );
            }
        } catch (Exception e) {
            logger.warn("RtspRegisterChannelHandler.channelRead0.Exception", e);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.warn("RtspRegisterChannelHandler is inactive.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("RtspRegisterChannelHandler.Exception", cause);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
