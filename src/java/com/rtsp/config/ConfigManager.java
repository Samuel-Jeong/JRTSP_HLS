package com.rtsp.config;

import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.service.ServiceManager;

import java.io.File;
import java.io.IOException;

/**
 * @class public class UserConfig
 * @brief UserConfig Class
 */
public class ConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(ConfigManager.class);

    private Ini ini = null;

    // Section String
    public static final String SECTION_COMMON = "COMMON"; // COMMON Section 이름
    public static final String SECTION_FFMPEG = "FFMPEG"; // FFMPEG Section 이름
    public static final String SECTION_NETWORK = "NETWORK"; // NETWORK Section 이름
    public static final String SECTION_HLS = "HLS"; // HLS Section 이름
    public static final String SECTION_REGISTER = "REGISTER"; // REGISTER Section 이름

    // Field String
    public static final String FIELD_SEND_BUF_SIZE = "SEND_BUF_SIZE";
    public static final String FIELD_RECV_BUF_SIZE = "RECV_BUF_SIZE";

    public static final String FIELD_FFMPEG_PATH = "FFMPEG_PATH";
    public static final String FIELD_FFPROBE_PATH = "FFPROBE_PATH";

    public static final String FIELD_STREAM_THREAD_POOL_SIZE = "STREAM_THREAD_POOL_SIZE";
    public static final String FIELD_LOCAL_LISTEN_IP = "LOCAL_LISTEN_IP";
    public static final String FIELD_LOCAL_RTSP_REGISTER_LISTEN_PORT = "LOCAL_RTSP_REGISTER_LISTEN_PORT";
    public static final String FIELD_LOCAL_RTSP_LISTEN_PORT = "LOCAL_RTSP_LISTEN_PORT";
    public static final String FIELD_LOCAL_RTCP_LISTEN_PORT = "LOCAL_RTCP_LISTEN_PORT";
    public static final String FIELD_TARGET_IP = "TARGET_IP";
    public static final String FIELD_TARGET_RTP_PORT_MIN = "TARGET_RTP_PORT_MIN";
    public static final String FIELD_TARGET_RTP_PORT_MAX = "TARGET_RTP_PORT_MAX";

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

    // FFMPEG
    private String ffmpegPath = null;
    private String ffprobePath = null;

    // NETWORK
    private int streamThreadPoolSize = 1;
    private String localListenIp = null;
    private int localRtspRegisterListenPort = 0;
    private int localRtspListenPort = 0;
    private int localRtcpListenPort = 0;
    private String targetIp = null;
    private int targetRtpPortMin = 0;
    private int targetRtpPortMax = 0;

    // HLS
    private int hlsListSize = 0;
    private int hlsTime = 0;
    private boolean deleteM3u8 = true;
    private boolean deleteTs = true;

    // REGISTER
    private String realm;
    private String magicCookie;
    private String hashKey;

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
            return;
        }

        this.recvBufSize = Integer.parseInt(getIniValue(SECTION_COMMON, FIELD_RECV_BUF_SIZE));
        if (this.recvBufSize <= 0) {
            return;
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
            return;
        }

        this.ffprobePath = getIniValue(SECTION_FFMPEG, FIELD_FFPROBE_PATH);
        if (this.ffprobePath == null) {
            return;
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
            return;
        }

        this.localListenIp = getIniValue(SECTION_NETWORK, FIELD_LOCAL_LISTEN_IP);
        if (this.localListenIp == null) {
            return;
        }

        this.localRtspRegisterListenPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_LOCAL_RTSP_REGISTER_LISTEN_PORT));
        if (this.localRtspRegisterListenPort <= 0) {
            return;
        }

        this.localRtspListenPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_LOCAL_RTSP_LISTEN_PORT));
        if (this.localRtspListenPort <= 0) {
            return;
        }

        this.localRtcpListenPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_LOCAL_RTCP_LISTEN_PORT));
        if (this.localRtcpListenPort <= 0) {
            return;
        }

        this.targetIp = getIniValue(SECTION_NETWORK, FIELD_TARGET_IP);
        if (this.targetIp == null) {
            return;
        }

        this.targetRtpPortMin = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_TARGET_RTP_PORT_MIN));
        if (this.targetRtpPortMin <= 0) {
            return;
        }

        this.targetRtpPortMax = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_TARGET_RTP_PORT_MAX));
        if (this.targetRtpPortMax <= 0) {
            return;
        }

        if (targetRtpPortMin > targetRtpPortMax) {
            return;
        }

        logger.debug("Load [{}] config...(OK)", SECTION_NETWORK);
    }

    /**
     * @fn private void loadHlsConfig()
     * @brief HLS Section 을 로드하는 함수
     */
    private void loadHlsConfig() {
        this.hlsListSize = Integer.parseInt(getIniValue(SECTION_HLS, FIELD_HLS_LIST_SIZE));
        if (this.hlsListSize <= 0) {
            return;
        }

        this.hlsTime = Integer.parseInt(getIniValue(SECTION_HLS, FIELD_HLS_TIME));
        if (this.hlsTime <= 0) {
            return;
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

    public String getTargetIp() {
        return targetIp;
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
}
