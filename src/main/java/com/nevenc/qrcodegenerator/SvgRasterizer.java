package com.nevenc.qrcodegenerator;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import com.github.weisj.jsvg.view.ViewBox;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

public final class SvgRasterizer {

    private SvgRasterizer() {
    }

    public static BufferedImage rasterize(InputStream svgStream, int maxDimension) {
        SVGLoader loader = new SVGLoader();
        SVGDocument document;
        try {
            document = loader.load(svgStream, null, LoaderContext.createDefault());
        } catch (Exception e) {
            throw new InvalidLogoException("Logo must be a valid PNG or SVG image");
        }

        if (document == null) {
            throw new InvalidLogoException("Logo must be a valid PNG or SVG image");
        }

        FloatSize size = document.size();
        float svgWidth = size.width;
        float svgHeight = size.height;

        int targetWidth;
        int targetHeight;

        if (svgWidth <= 0 || svgHeight <= 0) {
            targetWidth = maxDimension;
            targetHeight = maxDimension;
        } else {
            float scale = Math.min(maxDimension / svgWidth, maxDimension / svgHeight);
            targetWidth = Math.round(svgWidth * scale);
            targetHeight = Math.round(svgHeight * scale);
        }

        BufferedImage image = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            document.render(null, g2d, new ViewBox(0, 0, targetWidth, targetHeight));
        } finally {
            g2d.dispose();
        }

        return image;
    }
}
