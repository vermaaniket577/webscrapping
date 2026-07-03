package com.mca.automate.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;

public class CaptchaOcrDebugRunner {
    private static final int DARK_THRESHOLD = 90;
    private static final int PADDING = 8;
    private static final int SCALE_FACTOR = 4;

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Pass an image path explicitly.");
            return;
        }
        Path imagePath = Path.of(args[0]);
        if (imagePath == null || !Files.exists(imagePath)) {
            System.err.println("Captcha image not found: " + imagePath.toAbsolutePath());
            return;
        }

        Path outputDir = createOutputDir(imagePath);
        String result = solveCaptcha(imagePath, outputDir);
        System.out.println("Input: " + imagePath.toAbsolutePath());
        System.out.println("Output dir: " + outputDir.toAbsolutePath());
        System.out.println("OCR result: " + result);
    }

    public static String solveCaptcha(Path imagePath, Path outputDir) throws Exception {
        Files.createDirectories(outputDir);
        BufferedImage original = ImageIO.read(imagePath.toFile());
        if (original == null) {
            throw new IOException("Unable to decode image: " + imagePath);
        }

        saveStep(original, outputDir, "step1_original.png");

        BufferedImage gray = toGrayscale(original);
        saveStep(gray, outputDir, "step2_grayscale.png");

        BufferedImage mask = extractDarkPixels(gray, DARK_THRESHOLD);
        saveStep(mask, outputDir, "step3_dark_mask.png");

        BufferedImage cropped = cropToContent(mask, PADDING);
        saveStep(cropped, outputDir, "step4_cropped.png");

        BufferedImage scaled = resize(cropped, SCALE_FACTOR);
        saveStep(scaled, outputDir, "step5_scaled.png");

        BufferedImage inverted = invertBinary(scaled);
        saveStep(inverted, outputDir, "step6_final.png");

        String rawText = runTesseract(inverted);
        String cleanedText = clean(rawText);

        List<String> lines = new ArrayList<>();
        lines.add("input=" + imagePath.toAbsolutePath());
        lines.add("outputDir=" + outputDir.toAbsolutePath());
        lines.add("finalImage=" + outputDir.resolve("step6_final.png").toAbsolutePath());
        lines.add("raw=" + printable(rawText));
        lines.add("cleaned=" + cleanedText);
        lines.add("prediction=" + cleanedText);
        Files.write(outputDir.resolve("result.txt"), lines, StandardCharsets.UTF_8);
        return cleanedText;
    }

    private static Path createOutputDir(Path imagePath) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        String baseName = stripExtension(imagePath.getFileName().toString());
        Path dir = Path.of("ocr-debug", baseName + "_" + timestamp);
        Files.createDirectories(dir);
        return dir;
    }

    private static BufferedImage toGrayscale(BufferedImage src) {
        BufferedImage gray = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return gray;
    }

    private static BufferedImage extractDarkPixels(BufferedImage gray, int threshold) {
        BufferedImage out = new BufferedImage(gray.getWidth(), gray.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < gray.getHeight(); ++y) {
            for (int x = 0; x < gray.getWidth(); ++x) {
                int value = gray.getRGB(x, y) & 0xFF;
                out.setRGB(x, y, value <= threshold ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        return out;
    }

    private static BufferedImage erode(BufferedImage src, int kernelSize) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        int radius = kernelSize / 2;
        for (int y = 0; y < src.getHeight(); ++y) {
            for (int x = 0; x < src.getWidth(); ++x) {
                boolean keep = true;
                for (int ky = -radius; ky <= radius && keep; ++ky) {
                    for (int kx = -radius; kx <= radius; ++kx) {
                        int px = x + kx;
                        int py = y + ky;
                        if (px < 0 || py < 0 || px >= src.getWidth() || py >= src.getHeight() || !isWhite(src, px, py)) {
                            keep = false;
                            break;
                        }
                    }
                }
                out.setRGB(x, y, keep ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        return out;
    }

    private static BufferedImage dilate(BufferedImage src, int kernelSize) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        int radius = kernelSize / 2;
        for (int y = 0; y < src.getHeight(); ++y) {
            for (int x = 0; x < src.getWidth(); ++x) {
                boolean on = false;
                for (int ky = -radius; ky <= radius && !on; ++ky) {
                    for (int kx = -radius; kx <= radius; ++kx) {
                        int px = x + kx;
                        int py = y + ky;
                        if (px >= 0 && py >= 0 && px < src.getWidth() && py < src.getHeight() && isWhite(src, px, py)) {
                            on = true;
                            break;
                        }
                    }
                }
                out.setRGB(x, y, on ? 0xFFFFFFFF : 0xFF000000);
            }
        }
        return out;
    }

    private static BufferedImage cropToContent(BufferedImage src, int padding) {
        int minX = src.getWidth();
        int minY = src.getHeight();
        int maxX = -1;
        int maxY = -1;
        for (int y = 0; y < src.getHeight(); ++y) {
            for (int x = 0; x < src.getWidth(); ++x) {
                if (isWhite(src, x, y)) {
                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                }
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

    private static BufferedImage resize(BufferedImage src, int scaleFactor) {
        int width = src.getWidth() * scaleFactor;
        int height = src.getHeight() * scaleFactor;
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.drawImage(src, 0, 0, width, height, null);
        g.dispose();
        return out;
    }

    private static BufferedImage invertBinary(BufferedImage src) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_BINARY);
        for (int y = 0; y < src.getHeight(); ++y) {
            for (int x = 0; x < src.getWidth(); ++x) {
                out.setRGB(x, y, isWhite(src, x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return out;
    }

    private static String runTesseract(BufferedImage image) throws Exception {
        String nativeLibraryPath = System.getenv("OCR_NATIVE_LIBRARY_PATH");
        if (nativeLibraryPath != null && !nativeLibraryPath.isBlank()) {
            System.setProperty("jna.library.path", nativeLibraryPath);
        }

        ITesseract tesseract = new Tesseract();
        String tessdataPath = System.getenv("OCR_TESSDATA_PATH");
        if (tessdataPath != null && !tessdataPath.isBlank()) {
            tesseract.setDatapath(tessdataPath);
        }
        tesseract.setLanguage(System.getenv().getOrDefault("OCR_LANGUAGE", "eng"));
        tesseract.setPageSegMode(8);
        tesseract.setOcrEngineMode(1);
        tesseract.setTessVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        String result = tesseract.doOCR(image);
        return result == null ? "" : result;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "");
    }

    private static String printable(String value) {
        return value == null ? "" : value.replace("\r", "\\r").replace("\n", "\\n");
    }

    private static boolean isWhite(BufferedImage image, int x, int y) {
        return (image.getRGB(x, y) & 0xFFFFFF) == 0xFFFFFF;
    }

    private static void saveStep(BufferedImage image, Path outputDir, String name) throws IOException {
        ImageIO.write(image, "png", outputDir.resolve(name).toFile());
    }

    private static String stripExtension(String filename) {
        int idx = filename.lastIndexOf('.');
        return idx >= 0 ? filename.substring(0, idx) : filename;
    }

    private static boolean isPng(Path path) {
        return path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png");
    }

    private static boolean isJpeg(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".jpg") || name.endsWith(".jpeg");
    }
}
