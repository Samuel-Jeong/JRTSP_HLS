package rtsp.config;

import org.apache.commons.net.ntp.TimeStamp;
import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.module.sdp.SdpParser;
import rtsp.module.sdp.base.Sdp;
import rtsp.service.ServiceManager;

import java.io.File;
import java.io.IOException;

/**
 * @class public class UserConfig
 * @brief UserConfig Class
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    public static final int MP2T_TYPE = 33; //RTP payload type for MJPEG video
    public static final String MP2T_TAG = "MP2T"; //RTP payload tag for MJPEG video

    private Ini ini = null;

    // Section String
    public static final String SECTION_COMMON = "COMMON"; // COMMON Section 이름
    public static final String SECTION_FFMPEG = "FFMPEG"; // FFMPEG Section 이름
    public static final String SECTION_NETWORK = "NETWORK"; // NETWORK Section 이름
    public static final String SECTION_HLS = "HLS"; // HLS Section 이름
    public static final String SECTION_REGISTER = "REGISTER"; // REGISTER Section 이름
    private static final String SECTION_SDP = "SDP"; // SDP Section 이름

    // Field String
    public static final String FIELD_SEND_BUF_SIZE = "SEND_BUF_SIZE";
    public static final String FIELD_RECV_BUF_SIZE = "RECV_BUF_SIZE";
    public static final String FIELD_EXTERNAL_CLIENT_ACCESS = "EXTERNAL_CLIENT_ACCESS";
    public static final String FIELD_LONG_SESSION_LIMIT_TIME = "LONG_SESSION_LIMIT_TIME";

    public static final String FIELD_FFMPEG_PATH = "FFMPEG_PATH";
    public static final String FIELD_FFPROBE_PATH = "FFPROBE_PATH";
    public static final String FIELD_FPS = "FPS";
    public static final String FIELD_GOP = "GOP";

    public static final String FIELD_STREAM_THREAD_POOL_SIZE = "STREAM_THREAD_POOL_SIZE";
    public static final String FIELD_LOCAL_LISTEN_IP = "LOCAL_LISTEN_IP";
    public static final String FIELD_LOCAL_RTSP_REGISTER_LISTEN_PORT = "LOCAL_RTSP_REGISTER_LISTEN_PORT";
    public static final String FIELD_LOCAL_RTSP_LISTEN_PORT = "LOCAL_RTSP_LISTEN_PORT";
    public static final String FIELD_LOCAL_RTCP_LISTEN_PORT = "LOCAL_RTCP_LISTEN_PORT";
    public static final String FIELD_TARGET_RTP_PORT_MIN = "TARGET_RTP_PORT_MIN";
    public static final String FIELD_TARGET_RTP_PORT_MAX = "TARGET_RTP_PORT_MAX";

    public static final String FIELD_DIRECT_PARSING = "DIRECT_CONVERTING";
    public static final String FIELD_HLS_LIST_SIZE = "HLS_LIST_SIZE";
    public static final String FIELD_HLS_TIME = "HLS_TIME";
    public static final String FIELD_DELETE_M3U8 = "DELETE_M3U8";
    public static final String FIELD_DELETE_TS = "DELETE_TS";

    private static final String FIELD_REALM = "REALM";
    private static final String FIELD_MAGIC_COOKIE = "MAGIC_COOKIE";
    private static final String FIELD_HASH_KEY = "HASH_KEY";

    // COMMON
    private int sendBufSize = 0;
    private int recvBufSize = 0;
    private boolean isExternalClientAccess = false;
    private long localSessionLimitTime = 0; // ms

    // FFMPEG
    private String ffmpegPath = null;
    private String ffprobePath = null;
    private int fps = 0;
    private int gop = 0;

    // NETWORK
    private int streamThreadPoolSize = 1;
    private String localListenIp = null;
    private int localRtspRegisterListenPort = 0;
    private int localRtspListenPort = 0;
    private int localRtcpListenPort = 0;
    private int targetRtpPortMin = 0;
    private int targetRtpPortMax = 0;

    // HLS
    private boolean isM3u8DirectConverting = false;
    private int hlsListSize = 0;
    private int hlsTime = 0;
    private boolean deleteM3u8 = true;
    private boolean deleteTs = true;

    // REGISTER
    private String realm;
    private String magicCookie;
    private String hashKey;

    // SDP
    private final SdpParser sdpParser = new SdpParser();
    private String version;
    private String origin;
    private String session;
    private String time;
    private String connection;
    private String media;
    String[] mp2tAttributeList;
    String[] attributeList;

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn public AuditConfig(String configPath)
     * @brief AuditConfig 생성자 함수
     * @param configPath Config 파일 경로 이름
     */
    public ConfigManager(String configPath) {
        File iniFile = new File(configPath);
        if (!iniFile.isFile() || !iniFile.exists()) {
            logger.warn("Not found the config path. (path={})", configPath);
            return;
        }

        try {
            this.ini = new Ini(iniFile);

            loadCommonConfig();
            loadFfmpegConfig();
            loadNetworkConfig();
            loadHlsConfig();
            loadRegisterConfig();
            loadSdpConfig();

            logger.info("Load config [{}]", configPath);
        } catch (IOException e) {
            logger.error("ConfigManager.IOException", e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn private void loadCommonConfig()
     * @brief COMMON Section 을 로드하는 함수
     */
    private void loadCommonConfig() {
        this.sendBufSize = Integer.parseInt(getIniValue(SECTION_COMMON, FIELD_SEND_BUF_SIZE));
        if (this.sendBufSize <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_COMMON, FIELD_SEND_BUF_SIZE, sendBufSize);
            System.exit(1);
        }

        this.recvBufSize = Integer.parseInt(getIniValue(SECTION_COMMON, FIELD_RECV_BUF_SIZE));
        if (this.recvBufSize <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_COMMON, FIELD_RECV_BUF_SIZE, recvBufSize);
            System.exit(1);
        }

        this.isExternalClientAccess = Boolean.parseBoolean(getIniValue(SECTION_COMMON, FIELD_EXTERNAL_CLIENT_ACCESS));

        this.localSessionLimitTime = Long.parseLong(getIniValue(SECTION_COMMON, FIELD_LONG_SESSION_LIMIT_TIME));
        if (this.localSessionLimitTime < 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_COMMON, FIELD_LONG_SESSION_LIMIT_TIME, localSessionLimitTime);
            System.exit(1);
        }

        logger.debug("Load [{}] config...(OK)", SECTION_COMMON);
    }

    /**
     * @fn private void loadFfmpegConfig()
     * @brief FFMPEG Section 을 로드하는 함수
     */
    private void loadFfmpegConfig() {
        this.ffmpegPath = getIniValue(SECTION_FFMPEG, FIELD_FFMPEG_PATH);
        if (this.ffmpegPath == null) {
            logger.error("Fail to load [{}-{}].", SECTION_FFMPEG, FIELD_FFMPEG_PATH);
            System.exit(1);
        }

        this.ffprobePath = getIniValue(SECTION_FFMPEG, FIELD_FFPROBE_PATH);
        if (this.ffprobePath == null) {
            logger.error("Fail to load [{}-{}].", SECTION_FFMPEG, FIELD_FFPROBE_PATH);
            System.exit(1);
        }

        String fpsString = getIniValue(SECTION_FFMPEG, FIELD_FPS);
        if (fpsString == null) {
            logger.error("Fail to load [{}-{}].", SECTION_FFMPEG, FIELD_FPS);
            System.exit(1);
        } else{
            fps = Integer.parseInt(fpsString);
            if (fps <= 0) {
                logger.error("Fail to load [{}-{}]. FPS is not positive. (fps={})", SECTION_FFMPEG, FIELD_FPS, fps);
                System.exit(1);
            }

            if (fps > 1000) {
                fps = 1000;
            }
        }

        // GOP Size is up to 30 (15 is also very common)
        String gopString = getIniValue(SECTION_FFMPEG, FIELD_GOP);
        if (gopString == null) {
            logger.error("Fail to load [{}-{}].", SECTION_FFMPEG, FIELD_GOP);
            System.exit(1);
        } else{
            gop = Integer.parseInt(gopString);
            if (gop < 0) {
                logger.error("Fail to load [{}-{}]. GOP is not positive. (fps={})", SECTION_FFMPEG, FIELD_GOP, gop);
                System.exit(1);
            }

            if (gop > 30) {
                gop = 15;
            }
        }

        logger.debug("Load [{}] config...(OK)", SECTION_FFMPEG);
    }

    /**
     * @fn private void loadNetworkConfig()
     * @brief NETWORK Section 을 로드하는 함수
     */
    private void loadNetworkConfig() {
        this.streamThreadPoolSize = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_STREAM_THREAD_POOL_SIZE));
        if (this.streamThreadPoolSize <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_NETWORK, FIELD_STREAM_THREAD_POOL_SIZE, streamThreadPoolSize);
            System.exit(1);
        }

        this.localListenIp = getIniValue(SECTION_NETWORK, FIELD_LOCAL_LISTEN_IP);
        if (this.localListenIp == null) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_NETWORK, FIELD_LOCAL_LISTEN_IP, localListenIp);
            System.exit(1);
        }

        this.localRtspRegisterListenPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_LOCAL_RTSP_REGISTER_LISTEN_PORT));
        if (this.localRtspRegisterListenPort <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_NETWORK, FIELD_LOCAL_RTSP_REGISTER_LISTEN_PORT, localRtspRegisterListenPort);
            System.exit(1);
        }

        this.localRtspListenPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_LOCAL_RTSP_LISTEN_PORT));
        if (this.localRtspListenPort <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_NETWORK, FIELD_LOCAL_RTSP_LISTEN_PORT, localRtspListenPort);
            System.exit(1);
        }

        this.localRtcpListenPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_LOCAL_RTCP_LISTEN_PORT));
        if (this.localRtcpListenPort <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_NETWORK, FIELD_LOCAL_RTCP_LISTEN_PORT, localRtcpListenPort);
            System.exit(1);
        }

        this.targetRtpPortMin = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_TARGET_RTP_PORT_MIN));
        if (this.targetRtpPortMin <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_NETWORK, FIELD_TARGET_RTP_PORT_MIN, targetRtpPortMin);
            System.exit(1);
        }

        this.targetRtpPortMax = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_TARGET_RTP_PORT_MAX));
        if (this.targetRtpPortMax <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_NETWORK, FIELD_TARGET_RTP_PORT_MAX, targetRtpPortMax);
            System.exit(1);
        }

        if (targetRtpPortMin > targetRtpPortMax) {
            logger.error("Fail to load [{}]. RtpPortRange is wrong. ({}-{})", SECTION_NETWORK, targetRtpPortMin, targetRtpPortMax);
            System.exit(1);
        }

        logger.debug("Load [{}] config...(OK)", SECTION_NETWORK);
    }

    /**
     * @fn private void loadHlsConfig()
     * @brief HLS Section 을 로드하는 함수
     */
    private void loadHlsConfig() {
        this.isM3u8DirectConverting = Boolean.parseBoolean(getIniValue(SECTION_HLS, FIELD_DIRECT_PARSING));

        this.hlsListSize = Integer.parseInt(getIniValue(SECTION_HLS, FIELD_HLS_LIST_SIZE));
        if (this.hlsListSize <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_HLS, FIELD_HLS_LIST_SIZE, hlsListSize);
            System.exit(1);
        }

        this.hlsTime = Integer.parseInt(getIniValue(SECTION_HLS, FIELD_HLS_TIME));
        if (this.hlsTime <= 0) {
            logger.error("Fail to load [{}-{}]. ({})", SECTION_HLS, FIELD_HLS_TIME, hlsTime);
            System.exit(1);
        }

        this.deleteM3u8 = Boolean.parseBoolean(getIniValue(SECTION_HLS, FIELD_DELETE_M3U8));
        this.deleteTs = Boolean.parseBoolean(getIniValue(SECTION_HLS, FIELD_DELETE_TS));

        logger.debug("Load [{}] config...(OK)", SECTION_HLS);
    }

    /**
     * @fn private void loadRegisterConfig()
     * @brief COMMON Section 을 로드하는 함수
     */
    private void loadRegisterConfig() {
        realm = getIniValue(SECTION_REGISTER, FIELD_REALM);
        magicCookie = getIniValue(SECTION_REGISTER, FIELD_MAGIC_COOKIE);
        hashKey = getIniValue(SECTION_REGISTER, FIELD_HASH_KEY);

        logger.debug("Load [{}] config...(OK)", SECTION_REGISTER);
    }

    private void loadSdpConfig() {
        version = getIniValue(SECTION_SDP, "VERSION");
        if (version == null) {
            logger.error("[SECTION_SDP] VERSION IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }
        version = "v=" + version + "\r\n";

        origin = getIniValue(SECTION_SDP, "ORIGIN");
        if (origin == null) {
            logger.error("[SECTION_SDP] ORIGIN IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        session = getIniValue(SECTION_SDP, "SESSION");
        if (session == null) {
            logger.error("[SECTION_SDP] SESSION IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }
        session = "s=" + session + "\r\n";

        time = getIniValue(SECTION_SDP, "TIME");
        if (time == null) {
            logger.error("[SECTION_SDP] TIME IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }
        time = "t=" + time + "\r\n";

        connection = getIniValue(SECTION_SDP, "CONNECTION");
        if (connection == null) {
            logger.error("[SECTION_SDP] CONNECTION IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        media = getIniValue(SECTION_SDP, "MEDIA");
        if (media == null) {
            logger.error("[SECTION_SDP] MEDIA IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        mp2tAttributeList = new String[1];
        String attributeMp2t = getIniValue(SECTION_SDP, String.format("ATTR_MP2T_%d", 0));
        if (attributeMp2t == null) {
            logger.error("[SECTION_SDP] ATTR_MP2T_{} IS NOT DEFINED IN THE LOCAL SDP.", 0);
            System.exit(1);
        }
        mp2tAttributeList[0] = attributeMp2t;

        int attrCount = Integer.parseInt(getIniValue(SECTION_SDP, "ATTR_COUNT"));
        if (attrCount < 0) {
            logger.error("[SECTION_SDP] ATTR_COUNT IS NOT DEFINED IN THE LOCAL SDP.");
            System.exit(1);
        }

        attributeList = new String[attrCount];
        for (int i = 0; i < attrCount; i++) {
            String attribute = getIniValue(SECTION_SDP, String.format("ATTR_%d", i));
            if (attribute != null) {
                attributeList[i] = attribute;
            }
        }
    }

    public Sdp loadLocalSdpConfig(String id, int remotePort) {
        try {
            StringBuilder sdpStr = new StringBuilder();

            // 1) Session
            // 1-1) Version
            sdpStr.append(version);

            // 1-2) Origin
            /*
                - Using NTP Timestamp
                [RFC 4566]
                  <sess-id> is a numeric string such that the tuple of <username>,
                  <sess-id>, <nettype>, <addrtype>, and <unicast-address> forms a
                  globally unique identifier for the session.  The method of
                  <sess-id> allocation is up to the creating tool, but it has been
                  suggested that a Network Time Protocol (NTP) format timestamp be
                  used to ensure uniqueness.
             */
            String originSessionId = String.valueOf(TimeStamp.getCurrentTime().getTime());
            String curOrigin = String.format(this.origin, originSessionId, localListenIp);
            curOrigin = "o=" + curOrigin + "\r\n";
            sdpStr.append(curOrigin);

            // 1-3) Session
            sdpStr.append(session);

            // 3) Media
            // 3-1) Connection
            String connection = String.format(this.connection, localListenIp);
            connection = "c=" + connection + "\r\n";
            sdpStr.append(connection);

            // 2) Time
            // 2-1) Time
            sdpStr.append(time);

            // 3) Media
            // 3-2) Media
            sdpStr.append("m=");
            String media = String.format(this.media, remotePort, MP2T_TYPE);
            sdpStr.append(media);
            sdpStr.append("\r\n");

            // 3-3) Attribute
            sdpStr.append("a=");
            sdpStr.append(String.format(mp2tAttributeList[0], MP2T_TYPE));
            sdpStr.append("\r\n");

            for (String attribute : attributeList) {
                sdpStr.append("a=");
                sdpStr.append(attribute);
                sdpStr.append("\r\n");
            }

            Sdp localSdp = null;
            try {
                localSdp = sdpParser.parseSdp(id, null, null, sdpStr.toString());
                logger.debug("({}) Local SDP=\n{}", id, localSdp.getData(false));
            } catch (Exception e) {
                logger.error("({}) Fail to parse the local sdp. ({})", id, sdpStr, e);
                System.exit(1);
            }
            return localSdp;
        } catch (Exception e) {
            logger.warn("Fail to load the local sdp.", e);
            return null;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @fn private String getIniValue(String section, String key)
     * @brief INI 파일에서 지정한 section 과 key 에 해당하는 value 를 가져오는 함수
     * @param section Section
     * @param key Key
     * @return 성공 시 value, 실패 시 null 반환
     */
    private String getIniValue(String section, String key) {
        String value = ini.get(section,key);
        if (value == null) {
            logger.warn("[ {} ] \" {} \" is null.", section, key);
            ServiceManager.getInstance().stop();
            System.exit(1);
            return null;
        }

        value = value.trim();
        logger.debug("\tGet Config [{}] > [{}] : [{}]", section, key, value);
        return value;
    }

    /**
     * @fn public void setIniValue(String section, String key, String value)
     * @brief INI 파일에 새로운 value 를 저장하는 함수
     * @param section Section
     * @param key Key
     * @param value Value
     */
    public void setIniValue(String section, String key, String value) {
        try {
            ini.put(section, key, value);
            ini.store();

            logger.debug("\tSet Config [{}] > [{}] : [{}]", section, key, value);
        } catch (IOException e) {
            logger.warn("Fail to set the config. (section={}, field={}, value={})", section, key, value);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public long getLocalSessionLimitTime() {
        return localSessionLimitTime;
    }

    public boolean isExternalClientAccess() {
        return isExternalClientAccess;
    }

    public int getSendBufSize() {
        return sendBufSize;
    }

    public int getRecvBufSize() {
        return recvBufSize;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }

    public String getFfprobePath() {
        return ffprobePath;
    }

    public int getStreamThreadPoolSize() {
        return streamThreadPoolSize;
    }

    public String getLocalListenIp() {
        return localListenIp;
    }

    public int getLocalRtspRegisterListenPort() {
        return localRtspRegisterListenPort;
    }

    public int getLocalRtspListenPort() {
        return localRtspListenPort;
    }

    public int getLocalRtcpListenPort() {
        return localRtcpListenPort;
    }

    public boolean isM3u8DirectConverting() {
        return isM3u8DirectConverting;
    }

    public void setM3u8DirectConverting(boolean m3u8DirectConverting) {
        isM3u8DirectConverting = m3u8DirectConverting;
    }

    public int getHlsListSize() {
        return hlsListSize;
    }

    public int getHlsTime() {
        return hlsTime;
    }

    public boolean isDeleteM3u8() {
        return deleteM3u8;
    }

    public boolean isDeleteTs() {
        return deleteTs;
    }

    public String getRealm() {
        return realm;
    }

    public String getMagicCookie() {
        return magicCookie;
    }

    public String getHashKey() {
        return hashKey;
    }

    public int getTargetRtpPortMin() {
        return targetRtpPortMin;
    }

    public int getTargetRtpPortMax() {
        return targetRtpPortMax;
    }

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public int getGop() {
        return gop;
    }

    public void setGop(int gop) {
        this.gop = gop;
    }
}
