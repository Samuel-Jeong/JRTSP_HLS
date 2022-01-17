package rtsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.config.ConfigManager;
import rtsp.service.AppInstance;
import rtsp.service.ServiceManager;

/**
 * @class public class RtspServerMain
 * @brief RtspServerMain class
 */
public class RtspServerMain {

    private static final Logger logger = LoggerFactory.getLogger(RtspServerMain.class);

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Argument Error. (&0: VoipPhoneMain, &1: config_path)");
            return;
        }

        String configPath = args[1].trim();
        logger.debug("| Config path: {}", configPath);
        ConfigManager configManager = new ConfigManager(configPath);

        AppInstance appInstance = AppInstance.getInstance();
        appInstance.setConfigManager(configManager);

        ServiceManager serviceManager = ServiceManager.getInstance();
        serviceManager.loop();
    }


}
