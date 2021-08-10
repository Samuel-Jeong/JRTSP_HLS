package com.rtsp.media.control;

import java.awt.*;

/**
 * @interface public interface Control
 * @brief Control interface
 */
public interface Control {

    /**
     * Get the <code>Component</code> associated with this
     * <code>Control</code> object.
     * For example, this method might return
     * a slider for volume control or a panel containing radio buttons for
     * CODEC control.
     * The <code>getControlComponent</code> method can return
     * <CODE>null</CODE> if there is no GUI control for
     * this <code>Control</code>.
     */
    public Component getControlComponent();
}