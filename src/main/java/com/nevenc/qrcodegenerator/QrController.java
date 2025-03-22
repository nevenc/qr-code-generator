package com.nevenc.qrcodegenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class QrController {

    private static final Logger logger = LoggerFactory.getLogger(QrController.class);
    private static final String DEFAULT_TEXT = "https://spring.io";

    @GetMapping("/qr")
    ResponseEntity<byte[]> generateQRCode(
            @RequestParam(defaultValue = DEFAULT_TEXT) String text,
            @RequestParam(defaultValue = "8") int scale,
            @RequestParam(defaultValue = "1") int border) {

        try {

            logger.debug("Generating QR code with scale=%d, border=%d and text=%s", scale, border, text);

            // Generate QR code as byte[]
            byte[] imageBytes = QrEncoder.generateQrCodeBytes(text, scale, border);

            // Set HTTP headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.IMAGE_PNG);
            headers.setContentLength(imageBytes.length);
            headers.setContentDisposition(
                    ContentDisposition.inline().filename("qrcode.png").build());

            return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

        } catch (Exception e) {
            logger.error("Error while generating QR code", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
