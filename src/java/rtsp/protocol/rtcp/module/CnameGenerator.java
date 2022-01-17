package rtsp.protocol.rtcp.module;

import javax.xml.bind.DatatypeConverter;
import java.nio.ByteBuffer;
import java.util.UUID;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class CnameGenerator {

    // Canonical Name
    // - A canonical name is the properly denoted host name of a computer or network server.
    // - A CNAME specifies an alias or nickname for a canonical host name record in a domain name system (DNS) database.
    // - In programming, the term "canonical" means "according to the rules."
    // - The DNS is the standard method of defining the locations of sites on the Internet, particularly Web-sites.
    // - CNAME 은 무작위로 할당된 SSRC 식별자와 충돌할 수 있으므로 SSRC 와 다른 값으로 생성해야 한다.
    //      > CNAME 도 SSRC 처럼 RTP 세션 내에서 유일해야 한다.

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public static String generateCname() {
        // generate unique identifier
        UUID uuid = UUID.randomUUID();
        // get the 64 least significant bits
        long leastSignificantBits = uuid.getLeastSignificantBits();
        // get the 64 most significant bits
        long mostSignificantBits = uuid.getMostSignificantBits();

        // convert the 128 bits to byte array
        // note: 128 / 8 = 16 bytes
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(leastSignificantBits).putLong(mostSignificantBits);
        buffer.flip();

        // get the 96 least significant bits
        // note: 96 / 8 = 12
        byte[] data = new byte[12];
        buffer.get(data, 0, 12);

        // convert the least 96 bits to ASCII Base64
        return DatatypeConverter.printBase64Binary(data);
    }
    ////////////////////////////////////////////////////////////

}
