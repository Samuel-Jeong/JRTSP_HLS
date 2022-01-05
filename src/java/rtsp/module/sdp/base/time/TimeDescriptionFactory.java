package rtsp.module.sdp.base.time;

import rtsp.module.sdp.base.SdpFactory;
import rtsp.module.sdp.base.field.TimeField;

/**
 * @class public class TimeDescriptionFactory
 * @brief TimeDescriptionFactory class
 */
public class TimeDescriptionFactory extends SdpFactory {

    // Mandatory
    private TimeField timeField;

    ////////////////////////////////////////////////////////////////////////////////

    public TimeDescriptionFactory(
            char timeType,
            String startTime, String endTime) {
        this.timeField = new TimeField(
                timeType,
                startTime,
                endTime
        );
    }

    ////////////////////////////////////////////////////////////////////////////////

    public TimeField getTimeField() {
        return timeField;
    }

    public void setTimeField(TimeField timeField) {
        this.timeField = timeField;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getData () {
        return timeField.getTimeType() + "=" +
                timeField.getStartTime() + " " +
                timeField.getEndTime() +
                CRLF;
    }

}
