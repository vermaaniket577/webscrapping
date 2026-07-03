package com.mca.automate.util;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

@Component
public class Preprocess {
    private static final int DARK_THRESHOLD = 90;
    private static final int CROP_PADDING = 8;
    private static final int SCALE_FACTOR = 4;

    public BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), 10);
        Graphics2D g = gray.createGraphics();
        g.drawImage((Image)src, 0, 0, null);
        g.dispose();
        return gray;
    }

    public BufferedImage threshold(BufferedImage gray, int t) {
        BufferedImage out = new BufferedImage(gray.getWidth(), gray.getHeight(), 12);
        for (int y = 0; y < gray.getHeight(); ++y) {
            for (int x = 0; x < gray.getWidth(); ++x) {
                int rgb = gray.getRGB(x, y) & 0xFF;
                int v = rgb < t ? 0 : 0xFFFFFF;
                out.setRGB(x, y, 0xFF000000 | v);
            }
        }
        return out;
    }

    public BufferedImage preProcessCaptchaImg(byte[] captchaImgArr) throws IOException {
        return this.preProcessCaptchaVariants(captchaImgArr).get(0);
    }

    public List<BufferedImage> preProcessCaptchaVariants(byte[] captchaImgArr) throws IOException {
        BufferedImage capImg = ImageIO.read(new ByteArrayInputStream(captchaImgArr));
        if (capImg == null) {
            throw new IOException("Unable to decode captcha image");
        }
        BufferedImage gray = this.toGrayscale(capImg);
        BufferedImage mask = this.extractDarkPixels(gray, DARK_THRESHOLD);
        BufferedImage cropped = this.cropToContent(mask, CROP_PADDING);
        BufferedImage scaled = this.resize(cropped, SCALE_FACTOR);
        BufferedImage finalImage = this.invertBinary(scaled);
        List<BufferedImage> variants = new ArrayList<>();
        variants.add(finalImage);
        return variants;
    }

    private BufferedImage extractDarkPixels(BufferedImage gray, int threshold) {
        BufferedImage out = new BufferedImage(gray.getWidth(), gray.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < gray.getHeight(); ++y) {
            for (int x = 0; x < gray.getWidth(); ++x) {
                int value = gray.getRGB(x, y) & 0xFF;
                out.setRGB(x, y, value <= threshold ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        return out;
    }

    private BufferedImage cropToContent(BufferedImage src, int padding) {
        int minX = src.getWidth();
        int minY = src.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < src.getHeight(); ++y) {
            for (int x = 0; x < src.getWidth(); ++x) {
                if (!this.isWhite(src, x, y)) continue;
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }
        if (maxX < minX || maxY < minY) {
            return src;
        }
        int left = Math.max(minX - padding, 0);
        int top = Math.max(minY - padding, 0);
        int right = Math.min(maxX + padding, src.getWidth() - 1);
        int bottom = Math.min(maxY + padding, src.getHeight() - 1);
        return src.getSubimage(left, top, right - left + 1, bottom - top + 1);
    }

    private BufferedImage resize(BufferedImage img, int scale) {
        int w = img.getWidth() * scale;
        int h = img.getHeight() * scale;
        BufferedImage out = new BufferedImage(w, h, 10);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(img, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private BufferedImage invertBinary(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < src.getHeight(); ++y) {
            for (int x = 0; x < src.getWidth(); ++x) {
                out.setRGB(x, y, this.isWhite(src, x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return out;
    }

    private boolean isWhite(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF;
    }
}
