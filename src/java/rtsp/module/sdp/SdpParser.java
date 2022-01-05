package rtsp.module.sdp;

import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.parser.SDPAnnounceParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.module.sdp.base.Sdp;
import rtsp.module.sdp.base.attribute.base.AttributeFactory;
import rtsp.module.sdp.base.attribute.base.FmtpAttributeFactory;
import rtsp.module.sdp.base.attribute.base.RtpMapAttributeFactory;
import rtsp.module.sdp.base.media.MediaDescriptionFactory;
import rtsp.module.sdp.base.media.MediaFactory;
import rtsp.module.sdp.base.session.SessionDescriptionFactory;
import rtsp.module.sdp.base.time.TimeDescriptionFactory;

import javax.sdp.*;
import java.util.Vector;

/**
 * SDP parser
 * @author jamesj
 */
public class SdpParser {

    private static final Logger logger = LoggerFactory.getLogger(SdpParser.class);

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public Sdp parseSdp (String callId, String fromNo, String toNo, String sdpStr) throws Exception
     * @param sdpStr sdp message
     * @return Sdp
     */
    public Sdp parseSdp (String callId, String fromNo, String toNo, String sdpStr) throws Exception {
        if (sdpStr == null || sdpStr.length() == 0) {
            return null;
        }

        Sdp sdp = new Sdp(callId);

        SDPAnnounceParser parser = new SDPAnnounceParser(sdpStr);
        SessionDescriptionImpl sdi = parser.parse();
        if (sdi.getVersion().getVersion() != 0) {
            logger.warn("({}) ({}) ({}) sdp version is not 0. sdp={}", callId, fromNo, toNo, sdpStr);
            return null;
        }

        // 1) Session Description
        SessionDescriptionFactory sessionDescriptionFactory = new SessionDescriptionFactory(
                sdi.getVersion().getTypeChar(),
                sdi.getVersion().getVersion(),
                sdi.getOrigin().getTypeChar(),
                sdi.getOrigin().getUsername(),
                sdi.getOrigin().getAddress(),
                sdi.getOrigin().getAddressType(),
                sdi.getOrigin().getNetworkType(),
                sdi.getOrigin().getSessionId(),
                sdi.getOrigin().getSessionVersion(),
                sdi.getSessionName().getTypeChar(),
                sdi.getSessionName().getValue()
        );

        Connection connection = sdi.getConnection();
        if (connection != null) {
            sessionDescriptionFactory.setConnectionField(
                    connection.getTypeChar(),
                    connection.getAddress(),
                    connection.getAddressType(),
                    connection.getNetworkType()
            );
        }

        sdp.setSessionDescriptionFactory(sessionDescriptionFactory);
        //logger.debug("({}) ({}) ({}) Session Description=\n{}", callId, fromNo, toNo, sdp.getSessionDescriptionFactory().getData());

        // 2) Time Description
        Vector tdVector = sdi.getTimeDescriptions(false);
        if (tdVector == null || tdVector.isEmpty()) {
            logger.warn("({}) ({}) ({}) sdp hasn't time description. sdp={}", callId, fromNo, toNo, sdpStr);
            return null;
        }

        for (Object o : tdVector) {
            TimeDescription timeDescription = (TimeDescription) o;
            if (timeDescription != null) {
                String timeData = timeDescription.toString();

                int equalPos = timeData.indexOf("=");
                int spacePos = timeData.indexOf(" ");
                int crlfPos = timeData.indexOf("\r\n");

                String startTimeStr = timeData.substring(equalPos + 1, spacePos);
                String endTimeStr = timeData.substring(spacePos + 1, crlfPos);

                TimeDescriptionFactory timeDescriptionFactory = new TimeDescriptionFactory(
                        timeDescription.getTime().getTypeChar(),
                        startTimeStr,
                        endTimeStr
                );
                sdp.setTimeDescriptionFactory(timeDescriptionFactory);
                break;
            }
        }
        //logger.debug("({}) ({}) ({}) Time Description=\n{}", callId, fromNo, toNo, sdp.getTimeDescriptionFactory().getData());

        // 3) Media Description
        Vector mdVector = sdi.getMediaDescriptions(false);
        if (mdVector == null || mdVector.isEmpty()) {
            logger.warn("({}) ({}) ({}) sdp hasn't media description. sdp={}", callId, fromNo, toNo, sdpStr);
            return null;
        }

        MediaDescriptionFactory mediaDescriptionFactory = new MediaDescriptionFactory();

        for (Object o1 : mdVector) {
            MediaDescription md = (MediaDescription) o1;
            if (md != null) {
                Media media = md.getMedia();
                if (media != null && (media.getMediaType().equals(Sdp.AUDIO) || media.getMediaType().equals(Sdp.VIDEO))) {
                    // Media
                    MediaFactory mediaFactory = new MediaFactory(
                            media.getTypeChar(),
                            media.getMediaType(),
                            media.getMediaFormats(false),
                            media.getMediaPort(),
                            media.getProtocol(),
                            media.getPortCount()
                    );

                    // Bandwidth
                    Vector bwVector = md.getBandwidths(false);
                    if (bwVector != null && !bwVector.isEmpty()) {
                        for (Object o : bwVector) {
                            BandWidth bandWidth = (BandWidth) o;
                            if (bandWidth == null) {
                                continue;
                            }

                            mediaFactory.addBandwidthField(
                                    bandWidth.getTypeChar(),
                                    bandWidth.getType(),
                                    bandWidth.getValue()
                            );
                        }
                    }

                    // Connection
                    /*Connection mediaConnection = md.getConnection();
                    if (mediaConnection != null) {
                        mediaFactory.setConnectionField(
                                mediaConnection.getTypeChar(),
                                mediaConnection.getAddress(),
                                mediaConnection.getAddressType(),
                                mediaConnection.getNetworkType()
                        );
                    }*/

                    // Attributes
                    Vector adVector = md.getAttributes(false);
                    if (adVector == null || adVector.isEmpty()) {
                        logger.warn("({}) ({}) ({}) sdp hasn't attribute description. sdp={}", callId, fromNo, toNo, sdpStr);
                        return null;
                    }

                    //for (int i = adVector.size() - 1; i >= 0; i--) {
                    for (Object o : adVector) {
                        Attribute attribute = (Attribute) o;
                        if (attribute == null) {
                            continue;
                        }

                        String attributeName = attribute.getName();
                        if (attributeName == null) {
                            continue;
                        }

                        if (attributeName.equals(MediaFactory.RTPMAP)) {
                            RtpMapAttributeFactory rtpMapAttributeFactory = new RtpMapAttributeFactory(
                                    attribute.getTypeChar(),
                                    attribute.getName(),
                                    attribute.getValue(),
                                    mediaFactory.getMediaField().getMediaFormats()
                            );
                            mediaFactory.addRtpAttributeFactory(rtpMapAttributeFactory);
                            //logger.debug("({}) ({}) ({}) [{}] RtpMapAttributeFactory=\n{}", callId, fromNo, toNo, rtpMapAttributeFactory.getPayloadId(), rtpMapAttributeFactory.getData());
                        } else if (attributeName.equals(MediaFactory.FMTP)) {
                            FmtpAttributeFactory fmtpAttributeFactory = new FmtpAttributeFactory(
                                    attribute.getTypeChar(),
                                    attribute.getName(),
                                    attribute.getValue(),
                                    mediaFactory.getMediaField().getMediaFormats()
                            );
                            mediaFactory.addFmtpAttributeFactory(fmtpAttributeFactory);
                            //logger.debug("({}) ({}) ({}) [{}] FmtpAttributeFactory=\n{}", callId, fromNo, toNo, fmtpAttributeFactory.getPayloadId(), fmtpAttributeFactory.getData());
                        } else {
                            AttributeFactory attributeFactory = new AttributeFactory(
                                    attribute.getTypeChar(),
                                    attribute.getName(),
                                    attribute.getValue(),
                                    mediaFactory.getMediaField().getMediaFormats()
                            );
                            mediaFactory.addAttributeFactory(attributeFactory);
                            //logger.debug("({}) ({}) ({}) [{}] AttributeFactory=\n{}", callId, fromNo, toNo, attributeFactory.getPayloadId(), attributeFactory.getData());
                        }
                    }

                    mediaDescriptionFactory.addMediaFactory(mediaFactory);
                    //logger.debug("({}) ({}) ({}) Media=\n{}", callId, fromNo, toNo, mediaFactory.getData());
                    break;
                }
            }
        }

        sdp.setMediaDescriptionFactory(mediaDescriptionFactory);
        //logger.debug("({}) ({}) ({}) Media Description=\n{}", callId, fromNo, toNo, sdp.getMediaDescriptionFactory().getData());

        return sdp;
    }

}