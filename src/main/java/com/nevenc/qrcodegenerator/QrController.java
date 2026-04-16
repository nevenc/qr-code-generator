package com.nevenc.qrcodegenerator;

import io.nayuki.qrcodegen.QrCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@RestController
class QrController {

    private static final Logger logger = LoggerFactory.getLogger(QrController.class);
    private static final String DEFAULT_TEXT = "https://spring.io";
    private static final int LIGHT_COLOUR = 0xFFFFFF;
    private static final int DARK_COLOUR = 0x000000;
    private static final String DEFAULT_LOGO_PATH =
            "static/images/spring-boot-logo.png";
    private static final Set<String> VALID_ICONS = Set.of(
            "spring", "globe", "phone", "sms", "email", "wifi", "vcard", "text");
    private static final String ICONS_PATH = "static/images/logos/";
    private static final int SVG_RASTER_SIZE = 256;

    private final BufferedImage defaultLogo;

    QrController() {
        this.defaultLogo = loadDefaultLogo();
    }

    @GetMapping("/qr")
    ResponseEntity<byte[]> generateQRCode(
            @RequestParam(defaultValue = DEFAULT_TEXT) String text,
            @RequestParam(defaultValue = "8") int scale,
            @RequestParam(defaultValue = "1") int border) {

        try {
            logger.debug("GET /qr scale={} border={} text={}", scale, border, text);
            byte[] imageBytes = QrEncoder.generateQrCodeBytes(text, scale, border);
            return pngResponse(imageBytes);
        } catch (Exception e) {
            logger.error("Error generating QR code", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping(value = "/qr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<byte[]> generateQRCodeWithLogo(
            @RequestParam(defaultValue = DEFAULT_TEXT) String text,
            @RequestParam(defaultValue = "8") int scale,
            @RequestParam(defaultValue = "1") int border,
            @RequestParam(defaultValue = "false") boolean includeLogo,
            @RequestParam(required = false) MultipartFile logo,
            @RequestParam(required = false) String defaultIcon) {

        try {
            logger.debug("POST /qr scale={} border={} includeLogo={} hasFile={} text={}",
                    scale, border, includeLogo,
                    logo != null && !logo.isEmpty(), text);

            byte[] imageBytes;
            if (!includeLogo) {
                imageBytes = QrEncoder.generateQrCodeBytes(text, scale, border);
            } else {
                BufferedImage logoImage;
                if (logo != null && !logo.isEmpty()) {
                    logoImage = LogoValidator.validate(logo);
                } else if (defaultIcon != null && !defaultIcon.isBlank()) {
                    logoImage = loadBuiltInIcon(defaultIcon);
                } else {
                    logoImage = defaultLogo;
                }
                imageBytes = QrEncoder.generateQrCodeBytes(
                        text, scale, border,
                        LIGHT_COLOUR, DARK_COLOUR,
                        QrCode.Ecc.HIGH, logoImage);
            }
            return pngResponse(imageBytes);
        } catch (InvalidLogoException e) {
            throw e; // handled by @ExceptionHandler below
        } catch (Exception e) {
            logger.error("Error generating QR code with logo", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler(InvalidLogoException.class)
    ResponseEntity<String> handleInvalidLogo(InvalidLogoException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.parseMediaType("text/plain;charset=UTF-8"))
                .body(e.getMessage());
    }

    private ResponseEntity<byte[]> pngResponse(byte[] imageBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setContentLength(imageBytes.length);
        headers.setContentDisposition(
                ContentDisposition.inline().filename("qrcode.png").build());
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);
    }

    private BufferedImage loadBuiltInIcon(String iconName) {
        if (!VALID_ICONS.contains(iconName)) {
            throw new InvalidLogoException("Unknown icon: " + iconName);
        }
        String path = ICONS_PATH + iconName + ".svg";
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream in = resource.getInputStream()) {
            return SvgRasterizer.rasterize(in, SVG_RASTER_SIZE);
        } catch (IOException e) {
            logger.error("Failed to load built-in icon: {}", path, e);
            throw new InvalidLogoException("Failed to load icon: " + iconName);
        }
    }

    private BufferedImage loadDefaultLogo() {
        // Try spring.svg first
        ClassPathResource svgResource = new ClassPathResource(ICONS_PATH + "spring.svg");
        if (svgResource.exists()) {
            try (InputStream in = svgResource.getInputStream()) {
                BufferedImage img = SvgRasterizer.rasterize(in, SVG_RASTER_SIZE);
                logger.info("Loaded default logo from: {}", ICONS_PATH + "spring.svg");
                return img;
            } catch (Exception e) {
                logger.warn("Failed to load spring.svg: {}", e.getMessage());
            }
        }
        // Fall back to PNG
        ClassPathResource pngResource = new ClassPathResource(DEFAULT_LOGO_PATH);
        if (pngResource.exists()) {
            try (InputStream in = pngResource.getInputStream()) {
                BufferedImage img = ImageIO.read(in);
                if (img != null) {
                    logger.info("Loaded default logo from classpath: {}",
                            DEFAULT_LOGO_PATH);
                    return img;
                }
                logger.warn("Default logo at {} is unreadable; using placeholder",
                        DEFAULT_LOGO_PATH);
            } catch (IOException e) {
                logger.warn("Failed to load default logo at {}: {}",
                        DEFAULT_LOGO_PATH, e.getMessage());
            }
        } else {
            logger.info("No default logo at {}; using programmatic placeholder",
                    DEFAULT_LOGO_PATH);
        }
        // Fall back to programmatic placeholder
        return generatePlaceholderLogo();
    }

    private BufferedImage generatePlaceholderLogo() {
        final int size = 128;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(new Color(0x6d, 0xb3, 0x3f)); // Spring green
            g.fillRect(0, 0, size, size);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 84));
            FontMetrics fm = g.getFontMetrics();
            String text = "S";
            int x = (size - fm.stringWidth(text)) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(text, x, y);
        } finally {
            g.dispose();
        }
        return img;
    }
}
