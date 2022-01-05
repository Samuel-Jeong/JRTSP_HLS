package rtsp.fsm;

/**
 * @class public class RtspEvent
 * @brief RtspEvent class
 */
public class RtspEvent {

    public static final String IDLE = "idle";
    public static final String REGISTER = "register";
    public static final String OPTIONS = "options";
    public static final String OPTIONS_FAIL = "options_fail";
    public static final String DESCRIBE = "describe";
    public static final String DESCRIBE_FAIL = "describe_fail";
    public static final String DESCRIBE_OK = "describe_ok";
    public static final String SETUP = "setup";
    public static final String SETUP_FAIL = "setup_fail";
    public static final String PLAY = "play";
    public static final String PLAY_FAIL = "play_fail";
    public static final String PAUSE = "pause";
    public static final String PAUSE_FAIL = "pause_fail";
    public static final String TEARDOWN = "teardown";
    public static final String TEARDOWN_FAIL = "teardown_fail";
    public static final String TEARDOWN_OK = "teardown_ok";

}
