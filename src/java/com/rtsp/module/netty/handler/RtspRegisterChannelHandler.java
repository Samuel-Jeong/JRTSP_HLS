package com.rtsp.module.netty.handler;

import com.rtsp.config.ConfigManager;
import com.rtsp.module.RtspManager;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.module.netty.module.RtspRegisterNettyChannel;
import com.rtsp.protocol.register.RegisterRtspUnitReq;
import com.rtsp.protocol.register.RegisterRtspUnitRes;
import com.rtsp.service.AppInstance;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class RtspRegisterChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private final String ip;
    private final int port;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspRegisterChannelHandler(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, DatagramPacket datagramPacket) throws Exception {
        RtspRegisterNettyChannel rtspRegisterNettyChannel = NettyChannelManager.getInstance().getRegisterChannel();
        if (rtspRegisterNettyChannel == null) {
            return;
        }

        ByteBuf buf = datagramPacket.content();
        if (buf == null) {
            return;
        }

        int readBytes = buf.readableBytes();
        if (buf.readableBytes() <= 0) {
            return;
        }

        byte[] data = new byte[readBytes];
        buf.getBytes(0, data);

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();

        RegisterRtspUnitReq registerRtspUnitReq = new RegisterRtspUnitReq(data);
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
                    RegisterRtspUnitRes.NOT_ACCEPTED
            );
            registerRtspUnitRes.setReason("NOT_AUTHORIZED");

            rtspRegisterNettyChannel.sendResponse(registerRtspUnitRes);
        } else {
            // 1) Check nonce
            // 2) If ok, open rtsp channel
            // 3) If not, reject
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(configManager.getRealm().getBytes(StandardCharsets.UTF_8));
            messageDigest.update(configManager.getHashKey().getBytes(StandardCharsets.UTF_8));
            byte[] a1 = messageDigest.digest();
            messageDigest.reset();
            messageDigest.update(a1);

            RegisterRtspUnitRes registerRtspUnitRes;
            String curNonce = new String(messageDigest.digest());
            if (curNonce.equals(nonce)) {
                // RTSP Channel OPEN (New RtspUnit)
                RtspManager.getInstance().openRtspUnit(
                        rtspUnitId,
                        configManager.getLocalListenIp(),
                        configManager.getLocalRtspListenPort()
                );

                registerRtspUnitRes = new RegisterRtspUnitRes(
                        configManager.getMagicCookie(),
                        registerRtspUnitReq.getURtspHeader().getMessageType(),
                        registerRtspUnitReq.getURtspHeader().getSeqNumber(),
                        registerRtspUnitReq.getURtspHeader().getTimeStamp(),
                        configManager.getRealm(),
                        RegisterRtspUnitRes.SUCCESS
                );
            } else {
                registerRtspUnitRes = new RegisterRtspUnitRes(
                        configManager.getMagicCookie(),
                        registerRtspUnitReq.getURtspHeader().getMessageType(),
                        registerRtspUnitReq.getURtspHeader().getSeqNumber(),
                        registerRtspUnitReq.getURtspHeader().getTimeStamp(),
                        configManager.getRealm(),
                        RegisterRtspUnitRes.NOT_ACCEPTED
                );
                registerRtspUnitRes.setReason("WRONG_NONCE");
            }

            rtspRegisterNettyChannel.sendResponse(registerRtspUnitRes);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {

    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
