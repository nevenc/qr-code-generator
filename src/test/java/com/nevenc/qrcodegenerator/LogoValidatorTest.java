package com.nevenc.qrcodegenerator;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class LogoValidatorTest {

    @Test
    void acceptsValidPng() throws IOException {
        byte[] png = pngOfSize(64, 64);
        MockMultipartFile file = new MockMultipartFile(
                "logo", "logo.png", "image/png", png);

        BufferedImage img = LogoValidator.validate(file);

        assertNotNull(img);
        assertEquals(64, img.getWidth());
        assertEquals(64, img.getHeight());
    }

    @Test
    void rejectsNullFile() {
        InvalidLogoException ex = assertThrows(
                InvalidLogoException.class,
                () -> LogoValidator.validate(null));
        assertEquals("Logo file is required", ex.getMessage());
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile empty = new MockMultipartFile(
                "logo", "logo.png", "image/png", new byte[0]);
        InvalidLogoException ex = assertThrows(
                InvalidLogoException.class,
                () -> LogoValidator.validate(empty));
        assertEquals("Logo file is required", ex.getMessage());
    }

    @Test
    void rejectsWrongContentType() throws IOException {
        byte[] png = pngOfSize(64, 64);
        MockMultipartFile file = new MockMultipartFile(
                "logo", "logo.jpg", "image/jpeg", png);

        InvalidLogoException ex = assertThrows(
                InvalidLogoException.class,
                () -> LogoValidator.validate(file));
        assertEquals("Logo must be a valid PNG image", ex.getMessage());
    }

    @Test
    void rejectsOversizedFile() {
        byte[] big = new byte[(int) (1024L * 1024L + 1)];
        MockMultipartFile file = new MockMultipartFile(
                "logo", "logo.png", "image/png", big);

        InvalidLogoException ex = assertThrows(
                InvalidLogoException.class,
                () -> LogoValidator.validate(file));
        assertEquals("Logo file must be 1 MB or smaller", ex.getMessage());
    }

    @Test
    void rejectsCorruptPng() {
        byte[] junk = new byte[] { 1, 2, 3, 4, 5 };
        MockMultipartFile file = new MockMultipartFile(
                "logo", "logo.png", "image/png", junk);

        InvalidLogoException ex = assertThrows(
                InvalidLogoException.class,
                () -> LogoValidator.validate(file));
        assertEquals("Logo must be a valid PNG image", ex.getMessage());
    }

    @Test
    void rejectsOversizedDimensions() throws IOException {
        byte[] png = pngOfSize(1025, 1025);
        MockMultipartFile file = new MockMultipartFile(
                "logo", "logo.png", "image/png", png);

        InvalidLogoException ex = assertThrows(
                InvalidLogoException.class,
                () -> LogoValidator.validate(file));
        assertEquals(
                "Logo must be 1024\u00d71024 pixels or smaller",
                ex.getMessage());
    }

    @Test
    void rejectsUnreadableStream() {
        MockMultipartFile broken = new MockMultipartFile(
                "logo", "logo.png", "image/png", new byte[] { 1, 2, 3, 4 }) {
            @Override
            public java.io.InputStream getInputStream() throws IOException {
                throw new IOException("simulated stream failure");
            }
        };

        InvalidLogoException ex = assertThrows(
                InvalidLogoException.class,
                () -> LogoValidator.validate(broken));
        assertEquals("Logo must be a valid PNG image", ex.getMessage());
    }

    private static byte[] pngOfSize(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(
                width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
