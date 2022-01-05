package rtsp.module.rtsp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;

public class TsReader {

    private static final Logger logger = LoggerFactory.getLogger(TsReader.class);

    // PID : Packet 구분자, 스트림의 고유 ID
    private final HashMap<Integer, Integer> pidTable = new HashMap<>();

    public TsReader() {
        // nothing
    }

    public void read(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }

        try {

            File tsFile = new File(fileName);
            if (!tsFile.exists() || tsFile.isDirectory()) {
                return;
            }

            DataInputStream dataInputStream = new DataInputStream(new FileInputStream(tsFile));

            int pid;
            byte[] tsPacket = new byte[188];
            while (dataInputStream.read(tsPacket) != -1) {
                // sync_byte 는 0x47 로 고정 > 패킷 구분
                if (tsPacket[0] == 0x47) {
                    // PID Searching + Masking
                    pid = (tsPacket[1] & 31) << 8;
                    pid = (tsPacket[2] & 0xff) | pid;

                    Integer count;
                    if ((count = pidTable.get(pid)) == null) {
                        pidTable.put(pid, 1);
                    } else {
                        pidTable.put(pid, count + 1);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("TsReader.read.Exception", e);
        }
    }

    public void printPidTable() {
        for (int pid : pidTable.keySet()) {
            logger.debug("0x" + Integer.toHexString(pid) + " : " + pidTable.get(pid));
        }
    }

}
