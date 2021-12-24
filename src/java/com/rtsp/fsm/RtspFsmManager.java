package com.rtsp.fsm;

import com.fsm.StateManager;
import com.fsm.module.StateHandler;
import com.rtsp.module.base.RtspUnit;

import java.util.Arrays;
import java.util.HashSet;

/**
 * @class public class RtspFsmManager
 * @brief RtspFsmManager class
 */
public class RtspFsmManager {

    private final StateManager stateManager = new StateManager(10);

    ////////////////////////////////////////////////////////////////////////////////

    public RtspFsmManager() {
        // Nothing
    }

    ////////////////////////////////////////////////////////////////////////////////

    public StateManager getStateManager() {
        return stateManager;
    }

    public void init(RtspUnit rtspUnit) {
        if (rtspUnit == null) {
            return;
        }

        //
        stateManager.addStateHandler(RtspState.NAME);
        StateHandler rtspStateHandler = stateManager.getStateHandler(RtspState.NAME);
        //

        // REGISTER
        rtspStateHandler.addState(
                RtspEvent.REGISTER,
                RtspState.IDLE, RtspState.REGISTER,
                null,
                null,
                null, 0, 0
        );

        // OPTIONS
        rtspStateHandler.addState(
                RtspEvent.OPTIONS,
                RtspState.REGISTER, RtspState.OPTIONS,
                null,
                null,
                null, 0, 0
        );
        //

        // OPTIONS_FAIL
        rtspStateHandler.addState(
                RtspEvent.OPTIONS_FAIL,
                RtspState.OPTIONS, RtspState.REGISTER,
                null,
                null,
                null, 0, 0
        );
        //

        // DESCRIBE
        rtspStateHandler.addState(
                RtspEvent.DESCRIBE,
                RtspState.OPTIONS, RtspState.DESCRIBE,
                null,
                null,
                null, 0, 0
        );
        //

        // DESCRIBE_FAIL
        HashSet<String> describeFailPrevStateSet = new HashSet<>(
                Arrays.asList(
                        RtspState.DESCRIBE, RtspState.SDP_READY
                )
        );
        rtspStateHandler.addState(
                RtspEvent.DESCRIBE_FAIL,
                describeFailPrevStateSet, RtspState.REGISTER,
                null,
                null,
                null, 0, 0
        );
        //

        // DESCRIBE_OK
        rtspStateHandler.addState(
                RtspEvent.DESCRIBE_OK,
                RtspState.DESCRIBE, RtspState.SDP_READY,
                null,
                null,
                null, 0, 0
        );
        //

        // SETUP
        rtspStateHandler.addState(
                RtspEvent.SETUP,
                RtspState.SDP_READY, RtspState.SETUP,
                null,
                null,
                null, 0, 0
        );
        //

        // SETUP_FAIL
        rtspStateHandler.addState(
                RtspEvent.SETUP_FAIL,
                RtspState.SETUP, RtspState.REGISTER,
                null,
                null,
                null, 0, 0
        );
        //

        // PLAY
        HashSet<String> playPrevStateSet = new HashSet<>(
                Arrays.asList(
                        RtspState.SETUP, RtspState.PAUSE
                )
        );
        rtspStateHandler.addState(
                RtspEvent.PLAY,
                playPrevStateSet, RtspState.PLAY,
                null,
                null,
                null, 0, 0
        );
        //

        // PLAY_FAIL
        rtspStateHandler.addState(
                RtspEvent.PLAY_FAIL,
                RtspState.PLAY, RtspState.REGISTER,
                null,
                null,
                null, 0, 0
        );
        //

        // PAUSE
        rtspStateHandler.addState(
                RtspEvent.PAUSE,
                RtspState.PLAY, RtspState.PAUSE,
                null,
                null,
                null, 0, 0
        );
        //

        // PAUSE_FAIL
        rtspStateHandler.addState(
                RtspEvent.PAUSE_FAIL,
                RtspState.PAUSE, RtspState.PLAY,
                null,
                null,
                null, 0, 0
        );
        //

        // TEARDOWN
        HashSet<String> stopPrevStateSet = new HashSet<>(
                Arrays.asList(
                        RtspState.PLAY, RtspState.PAUSE
                )
        );
        rtspStateHandler.addState(
                RtspEvent.TEARDOWN,
                stopPrevStateSet, RtspState.STOP,
                null,
                null,
                null, 0, 0
        );

        // TEARDOWN_FAIL
        rtspStateHandler.addState(
                RtspEvent.TEARDOWN_FAIL,
                RtspState.STOP, RtspState.PLAY,
                null,
                null,
                null, 0, 0
        );
        //

        // TEARDOWN_OK
        rtspStateHandler.addState(
                RtspEvent.TEARDOWN_OK,
                RtspState.STOP, RtspState.REGISTER,
                null,
                null,
                null, 0, 0
        );
        //

        // IDLE
        HashSet<String> idlePrevStateSet = new HashSet<>(
                Arrays.asList(
                        RtspState.REGISTER, RtspState.OPTIONS, RtspState.DESCRIBE, RtspState.SDP_READY,
                        RtspState.SETUP, RtspState.PLAY, RtspState.PAUSE, RtspState.STOP
                )
        );
        rtspStateHandler.addState(
                RtspEvent.IDLE,
                idlePrevStateSet, RtspState.IDLE,
                null,
                null,
                null, 0, 0
        );
        //
    }

}
