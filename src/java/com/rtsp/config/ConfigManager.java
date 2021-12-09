package com.rtsp.config;

import com.rtsp.service.ServiceManager;
import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    // Field String
    public static final String FIELD_SEND_BUF_SIZE = "SEND_BUF_SIZE";
    public static final String FIELD_RECV_BUF_SIZE = "RECV_BUF_SIZE";

    public static final String FIELD_FFMPEG_PATH = "FFMPEG_PATH";
    public static final String FIELD_FFPROBE_PATH = "FFPROBE_PATH";

    public static final String FIELD_STREAM_THREAD_POOL_SIZE = "STREAM_THREAD_POOL_SIZE";
    public static final String FIELD_LOCAL_LISTEN_IP = "LOCAL_LISTEN_IP";
    public static final String FIELD_LOCAL_RTSP_LISTEN_PORT = "LOCAL_RTSP_LISTEN_PORT";
    public static final String FIELD_LOCAL_RTCP_LISTEN_PORT = "LOCAL_RTCP_LISTEN_PORT";

    public static final String FIELD_HLS_LIST_SIZE = "HLS_LIST_SIZE";
    public static final String FIELD_HLS_TIME = "HLS_TIME";
    public static final String FIELD_DELETE_TS = "DELETE_TS";

    // COMMON
    private int sendBufSize = 0;
    private int recvBufSize = 0;

    // FFMPEG
    private String ffmpegPath = null;
    private String ffprobePath = null;

    // NETWORK
    private int streamThreadPoolSize = 1;
    private String localListenIp = null;
    private int localRtspListenPort = 0;
    private int localRtcpListenPort = 0;

    // HLS
    private int hlsListSize = 0;
    private int hlsTime = 0;
    private boolean deleteTs = true;

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

        this.localRtspListenPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_LOCAL_RTSP_LISTEN_PORT));
        if (this.localRtspListenPort <= 0) {
            return;
        }

        this.localRtcpListenPort = Integer.parseInt(getIniValue(SECTION_NETWORK, FIELD_LOCAL_RTCP_LISTEN_PORT));
        if (this.localRtcpListenPort <= 0) {
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

        this.deleteTs = Boolean.parseBoolean(getIniValue(SECTION_HLS, FIELD_DELETE_TS));

        logger.debug("Load [{}] config...(OK)", SECTION_HLS);
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

    public boolean isDeleteTs() {
        return deleteTs;
    }
}
