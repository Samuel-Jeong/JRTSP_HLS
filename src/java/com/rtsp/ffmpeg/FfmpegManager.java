package com.rtsp.ffmpeg;

import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import io.lindstrom.m3u8.parser.ParsingMode;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.tracks.h264.H264TrackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.config.ConfigManager;
import com.rtsp.service.AppInstance;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;

/**
 * @class public class FfmpegManager
 * @brief FfmpegManager class
 */
public class FfmpegManager {

    private static final Logger logger = LoggerFactory.getLogger(FfmpegManager.class);

    public static final String FFMPEG_TAG = "ffmpeg";

    ////////////////////////////////////////////////////////////////////////////////

    public FfmpegManager() {
        //Nothing
    }

    ////////////////////////////////////////////////////////////////////////////////

    public static MediaPlaylist createPlayList() {
        MediaPlaylist mediaPlaylist = MediaPlaylist.builder()
                .version(3)
                .targetDuration(2)
                .mediaSequence(0)
                .ongoing(false)
                .addMediaSegments(
                        MediaSegment.builder()
                                .duration(9.009)
                                .uri("http://media.example.com/first.ts")
                                .build(),
                        MediaSegment.builder()
                                .duration(9.009)
                                .uri("http://media.example.com/second.ts")
                                .build(),
                        MediaSegment.builder()
                                .duration(3.003)
                                .uri("http://media.example.com/third.ts")
                                .build())
                .build();

        MediaPlaylistParser parser = new MediaPlaylistParser();
        logger.debug("{}", parser.writePlaylistAsString(mediaPlaylist));

        return mediaPlaylist;
    }

    public static void updatePlayList(String m3u8Path, int mediaSequence) {
        try {
            MediaPlaylistParser parser = new MediaPlaylistParser(ParsingMode.STRICT);
            MediaPlaylist playlist = parser.readPlaylist(Paths.get(m3u8Path));
            MediaPlaylist updated = MediaPlaylist.builder()
                    .from(playlist)
                    .version(3)
                    .mediaSequence(mediaSequence)
                    .build();
            logger.debug("{}", parser.writePlaylistAsString(updated));
        } catch (Exception e) {
            logger.warn("FfmpegManager.updatePlayList.Exception", e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void convertH264ToMp4(String srcFilePath) {
        if (srcFilePath.endsWith(".h264")) {
            try {
                String newFileName = srcFilePath.substring(
                        0,
                        srcFilePath.lastIndexOf(".")
                );
                newFileName += ".mp4";

                H264TrackImpl h264Track = new H264TrackImpl(
                        new FileDataSourceImpl(srcFilePath)
                );

                Movie movie = new Movie();
                movie.addTrack(h264Track);

                Container mp4file = new DefaultMp4Builder().build(movie);
                FileChannel fc = new FileOutputStream(newFileName).getChannel();
                mp4file.writeContainer(fc);
                fc.close();

                logger.warn("Success to convert the h264 file. (srcFilePath={}, newFileName={})", srcFilePath, newFileName);
            } catch (Exception e) {
                logger.warn("Fail to convert the h264 file. (srcFileName={})", srcFilePath, e);
            }
        }
    }

    public void convertMp4ToM3u8(String srcFilePath, String destTotalFilePath) {
        String destFilePathOnly = destTotalFilePath.substring(
                0,
                destTotalFilePath.lastIndexOf("/")
        );

        File destFilePathOnlyFile = new File(destFilePathOnly);
        if (destFilePathOnlyFile.mkdirs()) {
            logger.debug("Success to make the directory. ({})", destFilePathOnly);
        }

        //
        try {
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            FFmpeg ffmpeg = new FFmpeg(configManager.getFfmpegPath());
            FFprobe ffprobe = new FFprobe(configManager.getFfprobePath());
            //FFmpegProbeResult in = ffprobe.probe(srcTotalFilePath);

            FFmpegBuilder builder = new FFmpegBuilder()
                    .overrideOutputFiles(true)
                    .setInput(srcFilePath)
                    .addOutput(destTotalFilePath)

                    .setFormat("hls")
                    //.addExtraArgs("-segment_start_number", String.valueOf(curTsIndex))
                    //.addExtraArgs("-hls_start_number_source", String.valueOf(curTsIndex))
                    //.addExtraArgs("-hls_delete_threshold", "5")
                    //.addExtraArgs("-start_number", String.valueOf(curTsIndex))
                    .addExtraArgs("-hls_list_size", "5")
                    .addExtraArgs("-hls_time", "2")
                    .addExtraArgs("-segment_list_flags", "live")
                    //.addExtraArgs("-hls_flags", "omit_endlist")

                    //.setVideoFrameRate(10, 1)
                    //.setVideoResolution(640, 480) // at 640x480 resolution
                    .done();

            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
            executor.createJob(builder).run();

            /*FFmpegJob job = executor.createJob(builder, new ProgressListener() {

                // Using the FFmpegProbeResult determine the duration of the input
                final double durationNs = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

                @Override
                public void progress(Progress progress) {
                    double percentage = progress.out_time_ns / durationNs;

                    // Print out interesting information about the progress
                    logger.debug("{}", String.format(
                            "[%.0f%%] status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx",
                            percentage * 100,
                            progress.status,
                            progress.frame,
                            FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
                            progress.fps.doubleValue(),
                            progress.speed
                    ));
                }
            });
            job.run();*/
        } catch (Exception e) {
            // Ignored
            //logger.warn("FfmpegManager.convertJpegsToM3u8.Exception", e);
        }
        //

        //
        /*List<String> cmdList = new ArrayList<>();
        String cmd =
                "/usr/local/bin/" + FFMPEG_TAG +
                " -y " + // Overwrite output files without asking.
                //"-i /Users/jamesj/Desktop/live/test/tempJpg_test/temp_jpg_%d.jpg " +
                "-i " + srcTotalFilePath + " " +
                "-framerate 10 " + // Set the frame rate for the video stream. It defaults to 25.
                //"-frames:v 40 " + // Set the number of video frames to output. This is an obsolete alias for -frames:v, which you should use instead.
                //"-g 2 " + // Group of picture (GOP) 크기 설정
                "-f hls " + // File Type : HLS
                //"-hls_init_time 0 " + // 초기 대상 세그먼트 길이를 초 단위로 설정, 첫 번째 m3u8 목록에서 이 시간이 지나면 다음 키 프레임에서 세그먼트 삭제 (default: 0)
                //"-hls_time 2 " + // 대상 세그먼트 길이를 초 단위로 설정, 이 시간이 지나면 다음 키 프레임에서 세그먼트 삭제 (default: 2)
                //"-hls_list_size 5 " + // 최대 재생 목록 항목 수 설정 (default: 5)
                //"-hls_delete_threshold 1 " + // 세그먼트를 삭제하기 전에 디스크에 보관할 참조되지 않은 세그먼트 수를 설정 (default: 1)
                "-hls_flags omit_endlist " + // 재생 목록 끝에 EXT-X-ENDLIST 태그를 추가하지 않음
                //"-hls_flags single_file" + // Muxer 는 모든 세그먼트를 단일 MPEG-TS 파일에 저장하고 재생 목록에서 바이트 범위를 사용
                //"-hls_start_number_source " +  + " " + // 지정된 소스에 따라 재생목록 시퀀스 번호(#EXT-X-MEDIA-SEQUENCE)를 시작 (default: generic)
                //"-start_number 0 " + // hls_start_number_source 값이 generic이면 지정된 번호에서 재생 목록 시퀀스 번호(#EXT-X-MEDIA-SEQUENCE)를 시작 (default), hls_flags single_file이 설정되지 않은 경우 세그먼트 및 자막 파일 이름의 시작 시퀀스 번호도 지정 (default: 0)
                "-segment_start_number " + curFrameCount + " " + // Set the sequence number of the first segment (default: 0)
                "-segment_list_flags live " + // Allow live-friendly file generation
                //"-strftime 1" + // local time 사용
                //"-strftime_mkdir 1" + // local time 으로 설정된 하위 디렉토리 생성
                //"-hls_flags second_level_segment_index" +       // strftime이 켜져있을 때 날짜, 시간 값 외에 hls_segment_filename 표현식에서 세그먼트 인덱스를 %%d로 사용하게 함
                //"-hls_segment_filename " + destFilePathOnly + "/%Y/%m/%d/" + destFileNameOnly + "-%Y%m%d-%s.ts " + // 세그먼트 파일 이름을 설정, hls_flags single_file이 설정되지 않은 경우 파일 이름은 세그먼트 번호가있는 문자열 형식으로 사용
                //"-hls_segment_type mpegts" +
                // mpegts : MPEG-2 전송 스트림 형식의 출력 세그먼트 파일. (모든 HLS 버전과 호환)
                // fmp4 : MPEG-DASH 와 유사한 조각화된 MP4 형식의 출력 세그먼트 파일
                destTotalFilePath;
        cmdList.add(cmd);

        BufferedReader stdOut = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(cmdList.toArray(new String[0]));

            String str;
            stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((str = stdOut.readLine()) != null) {
                logger.debug(str);
            }

            process.waitFor();
            int exitValue = process.exitValue();
            if (exitValue != 0) {
                throw new RuntimeException("exit code is not 0 [" + exitValue + "]");
            }

            logger.debug("Success to convert. (fileName={})", destTotalFilePath);
        } catch (Exception e) {
            logger.warn("FfmpegManager.convertJpegsToM3u8.Exception", e);
        } finally {
            if (process != null) {
                process.destroy();
            }

            if (stdOut != null) {
                try {
                    stdOut.close();
                } catch (IOException e) {
                    logger.warn("() () () Fail to close the BufferReader.", e);
                }
            }
        }*/
        //
    }

}
