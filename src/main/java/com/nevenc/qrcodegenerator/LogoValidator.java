package com.nevenc.qrcodegenerator;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

public final class LogoValidator {

    private static final long MAX_BYTES = 1024L * 1024L;
    private static final int MAX_DIMENSION = 1024;
    private static final int SVG_RASTER_SIZE = 256;
    private static final String ERR_REQUIRED = "Logo file is required";
    private static final String ERR_INVALID_FORMAT = "Logo must be a valid PNG or SVG image";
    private static final String ERR_TOO_LARGE =
            "Logo file must be 1 MB or smaller";
    private static final String ERR_TOO_BIG =
            "Logo must be 1024\u00d71024 pixels or smaller";

    private LogoValidator() {
    }

    private static boolean isSvg(String contentType) {
        return "image/svg+xml".equalsIgnoreCase(contentType)
            || "application/svg+xml".equalsIgnoreCase(contentType);
    }

    public static BufferedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidLogoException(ERR_REQUIRED);
        }
        String contentType = file.getContentType();
        boolean isPng = contentType != null && contentType.equalsIgnoreCase("image/png");
        boolean isSvg = contentType != null && isSvg(contentType);
        if (!isPng && !isSvg) {
            throw new InvalidLogoException(ERR_INVALID_FORMAT);
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidLogoException(ERR_TOO_LARGE);
        }
        if (isPng) {
            BufferedImage image;
            try (var in = file.getInputStream()) {
                image = ImageIO.read(in);
            } catch (IOException e) {
                throw new InvalidLogoException(ERR_INVALID_FORMAT);
            }
            if (image == null) {
                throw new InvalidLogoException(ERR_INVALID_FORMAT);
            }
            if (image.getWidth() > MAX_DIMENSION || image.getHeight() > MAX_DIMENSION) {
                throw new InvalidLogoException(ERR_TOO_BIG);
            }
            return image;
        } else {
            try (var in = file.getInputStream()) {
                return SvgRasterizer.rasterize(in, SVG_RASTER_SIZE);
            } catch (IOException e) {
                throw new InvalidLogoException(ERR_INVALID_FORMAT);
            }
        }
    }
}
