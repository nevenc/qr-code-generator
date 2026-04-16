package com.nevenc.qrcodegenerator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class QrControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void getQrReturnsPng() throws Exception {
        mockMvc.perform(get("/qr").param("text", "hello"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void postQrWithoutLogoReturnsPng() throws Exception {
        mockMvc.perform(multipart("/qr")
                        .param("text", "hello")
                        .param("includeLogo", "false"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void postQrWithDefaultLogoReturnsPng() throws Exception {
        mockMvc.perform(multipart("/qr")
                        .param("text", "hello")
                        .param("includeLogo", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void postQrWithUploadedLogoReturnsPng() throws Exception {
        MockMultipartFile logo = new MockMultipartFile(
                "logo", "logo.png", "image/png", pngOfSize(64, 64));

        mockMvc.perform(multipart("/qr")
                        .file(logo)
                        .param("text", "hello")
                        .param("includeLogo", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"));
    }

    @Test
    void postQrRejectsNonPngLogo() throws Exception {
        MockMultipartFile logo = new MockMultipartFile(
                "logo", "logo.jpg", "image/jpeg", pngOfSize(64, 64));

        mockMvc.perform(multipart("/qr")
                        .file(logo)
                        .param("text", "hello")
                        .param("includeLogo", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Logo must be a valid PNG or SVG image"));
    }

    @Test
    void postQrRejectsOversizedLogo() throws Exception {
        byte[] big = new byte[(int) (1024L * 1024L + 1)];
        MockMultipartFile logo = new MockMultipartFile(
                "logo", "logo.png", "image/png", big);

        mockMvc.perform(multipart("/qr")
                        .file(logo)
                        .param("text", "hello")
                        .param("includeLogo", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Logo file must be 1 MB or smaller"));
    }

    @Test
    void postQrRejectsCorruptPng() throws Exception {
        MockMultipartFile logo = new MockMultipartFile(
                "logo", "logo.png", "image/png", new byte[] { 1, 2, 3, 4 });

        mockMvc.perform(multipart("/qr")
                        .file(logo)
                        .param("text", "hello")
                        .param("includeLogo", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Logo must be a valid PNG or SVG image"));
    }

    @Test
    void postQrRejectsOversizedDimensions() throws Exception {
        MockMultipartFile logo = new MockMultipartFile(
                "logo", "logo.png", "image/png", pngOfSize(1025, 1025));

        mockMvc.perform(multipart("/qr")
                        .file(logo)
                        .param("text", "hello")
                        .param("includeLogo", "true"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "Logo must be 1024\u00d71024 pixels or smaller"));
    }

    private static byte[] pngOfSize(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(
                w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, w, h);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
