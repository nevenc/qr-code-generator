package com.nevenc.qrcodegenerator;

import org.junit.jupiter.api.Test;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SvgRasterizerTest {

    @Test
    void rasterizesSimpleSvg() {
        byte[] svg = minimalSvg(100, 100, "green");
        BufferedImage img = SvgRasterizer.rasterize(
                new ByteArrayInputStream(svg), 256);
        assertNotNull(img);
        assertEquals(256, img.getWidth());
        assertEquals(256, img.getHeight());
        // Center pixel should be green (from the rect fill)
        int rgb = img.getRGB(128, 128) & 0xFFFFFF;
        assertEquals(0x008000, rgb, "center should be green");
    }

    @Test
    void preservesAspectRatio() {
        byte[] svg = minimalSvg(200, 100, "red");
        BufferedImage img = SvgRasterizer.rasterize(
                new ByteArrayInputStream(svg), 256);
        assertNotNull(img);
        assertEquals(256, img.getWidth());
        assertEquals(128, img.getHeight());
    }

    @Test
    void throwsOnInvalidSvg() {
        byte[] junk = new byte[] { 1, 2, 3, 4 };
        assertThrows(InvalidLogoException.class,
                () -> SvgRasterizer.rasterize(
                        new ByteArrayInputStream(junk), 256));
    }

    @Test
    void throwsOnEmptyStream() {
        assertThrows(InvalidLogoException.class,
                () -> SvgRasterizer.rasterize(
                        new ByteArrayInputStream(new byte[0]), 256));
    }

    private static byte[] minimalSvg(int width, int height, String fill) {
        String svg = String.format(
                "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 %d %d\">"
                + "<rect width=\"%d\" height=\"%d\" fill=\"%s\"/>"
                + "</svg>",
                width, height, width, height, fill);
        return svg.getBytes(StandardCharsets.UTF_8);
    }
}
