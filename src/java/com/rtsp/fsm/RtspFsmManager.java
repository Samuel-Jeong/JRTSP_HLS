package com.rtsp.fsm;

import com.fsm.StateManager;
import com.fsm.module.StateHandler;
import com.rtsp.module.base.RtspUnit;

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

        //
        rtspStateHandler.addState(
                RtspEvent.SETUP,
                RtspState.INIT, RtspState.READY,
                null,
                null,
                RtspEvent.SETUP_FAIL, 2000, 0
        );

        rtspStateHandler.addState(
                RtspEvent.SETUP_FAIL,
                RtspState.READY, RtspState.INIT,
                null,
                null,
                null, 0, 0
        );

        rtspStateHandler.addState(
                RtspEvent.PLAY,
                RtspState.READY, RtspState.PLAYING,
                null,
                null,
                null, 0, 0
        );

        /*rtspStateHandler.addState(
                RtspEvent.PLAY_FAIL,
                RtspState.PLAYING, RtspState.READY,
                null,
                null,
                null, 0, 0
        );*/

        rtspStateHandler.addState(
                RtspEvent.TEARDOWN,
                RtspState.PLAYING, RtspState.INIT,
                null,
                null,
                null, 0, 0
        );

        /*rtspStateHandler.addState(
                RtspEvent.TEARDOWN_FAIL,
                RtspState.INIT, RtspState.PLAYING,
                null,
                null,
                null, 0, 0
        );*/
        //
    }

}
