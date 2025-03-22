package com.nevenc.qrcodegenerator;

import io.nayuki.qrcodegen.QrCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class QrEncoder {

    private static final Logger logger = LoggerFactory.getLogger(QrEncoder.class);

    private static final int DEFAULT_SCALE = 10;
    private static final int DEFAULT_BORDER = 5;
    private static final int DEFAULT_LIGHT_COLOUR = 0xFFFFFF;
    private static final int DEFAULT_DARK_COLOUR = 0x000000;
    private static final QrCode.Ecc DEFAULT_ECC = QrCode.Ecc.MEDIUM;

    public static BufferedImage generateQrCodeBufferedImage(String text) throws Exception {
        logger.debug("Generating QR code BufferedImage with text={}", text);
        return generateQrCodeBufferedImage(text, DEFAULT_SCALE, DEFAULT_BORDER);
    }

    public static BufferedImage generateQrCodeBufferedImage(String text, int scale, int border) throws Exception {
        logger.debug("Generating QR code BufferedImage with scale={}, border={}, text={}", scale, border, text);
        return generateQrCodeBufferedImage(text, scale, border, DEFAULT_LIGHT_COLOUR, DEFAULT_DARK_COLOUR, DEFAULT_ECC);
    }

    public static BufferedImage generateQrCodeBufferedImage(String text, int scale, int border, int lightColour, int darkColour, QrCode.Ecc ecc) throws Exception {

        logger.debug("Generating QR code BufferedImage with scale={}, border={}, lightcolour={}, darkcolour={}, ecc={}, text={}",
                scale, border, Integer.toHexString(lightColour), Integer.toHexString(darkColour), ecc.toString(), text);

        // text can't be null
        Objects.requireNonNull(text);

        // scale and border need to be positive integers
        if (scale <= 0 || border < 0) {
            logger.error("Scale or Border should be greater than 0.");
            throw new IllegalArgumentException("Value out of range");
        }

        // Create QR Code
        QrCode qr = QrCode.encodeText(text, ecc);

        // set upper bound on scale and border
        if (border > Integer.MAX_VALUE / 2 || qr.size + border * 2L > Integer.MAX_VALUE / scale) {
            logger.error("Border or scale is too large.");
            throw new IllegalArgumentException("Scale or border too large");
        }

        // create image with border and scale
        BufferedImage result = new BufferedImage(
                (qr.size + border * 2) * scale,
                (qr.size + border * 2) * scale,
                BufferedImage.TYPE_INT_RGB
        );

        // set the RGB
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                boolean color = qr.getModule(x / scale - border, y / scale - border);
                result.setRGB(x, y, color ? darkColour : lightColour);
            }
        }

        return result;
    }

    public static byte[] generateQrCodeBytes(String text) throws Exception {
        logger.debug("Generating QR code bytes with text={}", text);
        return generateQrCodeBytes(text, DEFAULT_SCALE, DEFAULT_BORDER);
    }

    public static byte[] generateQrCodeBytes(String text, int scale, int border) throws Exception {
        logger.debug("Generating QR code bytes with scale={}, border={}, text={}", scale, border, text);
        return generateQrCodeBytes(text, scale, border, DEFAULT_LIGHT_COLOUR, DEFAULT_DARK_COLOUR, DEFAULT_ECC);
    }

    public static byte[] generateQrCodeBytes(String text, int scale, int border, int lightColour, int darkColour, QrCode.Ecc ecc) throws Exception {
        logger.debug("Generating QR code bytes with scale={}, border={}, lightcolour={}, darkcolour={}, ecc={}, text={}",
                scale, border, Integer.toHexString(lightColour), Integer.toHexString(darkColour), ecc, text);
        BufferedImage qrImage = generateQrCodeBufferedImage(text, scale, border, lightColour, darkColour, ecc);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "png", baos);
        byte[] imageBytes = baos.toByteArray();
        return imageBytes;
    }

}
