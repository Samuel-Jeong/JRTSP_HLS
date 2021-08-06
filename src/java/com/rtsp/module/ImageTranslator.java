package com.rtsp.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

/**
 * @class class ImageTranslator
 * @brief ImageTranslator class
 */
class ImageTranslator {

    private static final Logger logger = LoggerFactory.getLogger(ImageTranslator.class);

    private float compressionQuality;
    private ByteArrayOutputStream baos;
    private ImageWriter writer;
    private ImageWriteParam param;

    public ImageTranslator(float cq) {
        compressionQuality = cq;

        try {
            baos =  new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            writer = writers.next();
            writer.setOutput(ios);

            param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(compressionQuality);

        } catch (Exception e) {
            logger.warn("ImageTranslator.Exception", e);
            System.exit(0);
        }
    }

    public byte[] compress(byte[] imageBytes) {
        try {
            baos.reset();

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            writer.write(null, new IIOImage(image, null, null), param);
        } catch (Exception e) {
            logger.warn("ImageTranslator.Exception", e);
            System.exit(0);
        }

        return baos.toByteArray();
    }

    public void setCompressionQuality(float cq) {
        compressionQuality = cq;
        param.setCompressionQuality(compressionQuality);
    }

}
