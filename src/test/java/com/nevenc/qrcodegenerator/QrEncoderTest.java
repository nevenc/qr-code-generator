package com.nevenc.qrcodegenerator;

import io.nayuki.qrcodegen.QrCode;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class QrEncoderTest {

    @Test
    void existingNoLogoOverloadStillProducesBytes() throws Exception {
        byte[] bytes = QrEncoder.generateQrCodeBytes("hello");
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void logoOverloadPlacesLogoColorAtCenter() throws Exception {
        BufferedImage logo = solidSquare(64, new Color(0x6d, 0xb3, 0x3f));

        BufferedImage result = QrEncoder.generateQrCodeBufferedImage(
                "hello", 10, 4, 0xFFFFFF, 0x000000, QrCode.Ecc.HIGH, logo);

        int cx = result.getWidth() / 2;
        int cy = result.getHeight() / 2;
        int rgb = result.getRGB(cx, cy) & 0xFFFFFF;
        assertEquals(0x6db33f, rgb,
                "center pixel should show the logo color");
    }

    @Test
    void logoOverloadKeepsFinderPatternBlackPixels() throws Exception {
        BufferedImage logo = solidSquare(64, Color.RED);

        BufferedImage result = QrEncoder.generateQrCodeBufferedImage(
                "hello", 10, 4, 0xFFFFFF, 0x000000, QrCode.Ecc.HIGH, logo);

        // Scan the top-left finder region (inside the border).
        int border = 4 * 10;
        boolean foundBlack = false;
        for (int y = border; y < border + 60 && !foundBlack; y++) {
            for (int x = border; x < border + 60 && !foundBlack; x++) {
                if ((result.getRGB(x, y) & 0xFFFFFF) == 0x000000) {
                    foundBlack = true;
                }
            }
        }
        assertTrue(foundBlack,
                "finder pattern area should still contain black pixels");
    }

    @Test
    void logoOverloadIsDeterministic() throws Exception {
        BufferedImage logo = solidSquare(64, Color.RED);

        BufferedImage a = QrEncoder.generateQrCodeBufferedImage(
                "hello", 10, 4, 0xFFFFFF, 0x000000, QrCode.Ecc.HIGH, logo);
        BufferedImage b = QrEncoder.generateQrCodeBufferedImage(
                "hello", 10, 4, 0xFFFFFF, 0x000000, QrCode.Ecc.HIGH, logo);

        assertEquals(a.getWidth(), b.getWidth());
        assertEquals(a.getHeight(), b.getHeight());
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                assertEquals(a.getRGB(x, y), b.getRGB(x, y),
                        "pixel mismatch at (" + x + "," + y + ")");
            }
        }
    }

    @Test
    void logoOverloadBytesMethodReturnsPngBytes() throws Exception {
        BufferedImage logo = solidSquare(64, Color.BLUE);

        byte[] bytes = QrEncoder.generateQrCodeBytes(
                "hello", 10, 4, 0xFFFFFF, 0x000000, QrCode.Ecc.HIGH, logo);

        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
        // PNG magic bytes
        assertEquals((byte) 0x89, bytes[0]);
        assertEquals((byte) 'P', bytes[1]);
        assertEquals((byte) 'N', bytes[2]);
        assertEquals((byte) 'G', bytes[3]);
    }

    private BufferedImage solidSquare(int size, Color color) {
        BufferedImage img = new BufferedImage(
                size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(color);
            g.fillRect(0, 0, size, size);
        } finally {
            g.dispose();
        }
        return img;
    }
}
