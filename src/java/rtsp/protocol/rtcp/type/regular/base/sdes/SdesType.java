package rtsp.protocol.rtcp.type.regular.base.sdes;

public enum SdesType {

    /**
     *    abbrev.    name                              value
     *    END        end of SDES list                      0
     *    CNAME      canonical name                        1
     *    NAME       user name                             2
     *    EMAIL      user's electronic mail address        3
     *    PHONE      user's phone number                   4
     *    LOC        geographic user location              5
     *    TOOL       name of application or tool           6
     *    NOTE       notice about the source               7
     *    PRIV       private extensions                    8
     */

    END, // End of item

    // Canonical end-point identifier
    // The CNAME item is mandatory in every SDES packet,
    // which in turn is mandatory part of every compound RTCP packet.
    CNAME,

    NAME, // User name
    EMAIL, // Electronic mail address
    PHONE, // Phone number
    LOC, // Geographic user location
    TOOL, // Application or tool name
    NOTE, // Notice/status
    PRIV, // Private extenstions
    UNKNOWN // NOT DEFINED

}
