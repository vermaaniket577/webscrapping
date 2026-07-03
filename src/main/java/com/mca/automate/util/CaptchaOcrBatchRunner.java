package com.mca.automate.util;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

public class CaptchaOcrBatchRunner {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Pass an input folder explicitly.");
            return;
        }
        Path inputDir = Path.of(args[0]);
        if (!Files.isDirectory(inputDir)) {
            System.err.println("Input folder not found: " + inputDir.toAbsolutePath());
            return;
        }

        List<Path> images = listImages(inputDir);
        if (images.isEmpty()) {
            System.err.println("No PNG/JPG captcha images found in: " + inputDir.toAbsolutePath());
            return;
        }

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS").format(new Date());
        Path batchDir = Path.of("ocr-debug", "batch_" + timestamp);
        Files.createDirectories(batchDir);

        Map<String, String> expectedLabels = readExpectedLabels(inputDir.resolve("labels.csv"));
        List<String> summary = new ArrayList<>();
        summary.add("image,prediction,length,expected,match,status,outputDir,resultFile,finalImage,error");

        int successCount = 0;
        for (Path image : images) {
            String baseName = stripExtension(image.getFileName().toString());
            Path outputDir = batchDir.resolve(baseName);
            String prediction = "";
            String status = "OK";
            String error = "";
            try {
                prediction = CaptchaOcrDebugRunner.solveCaptcha(image, outputDir);
                ++successCount;
            }
            catch (Exception ex) {
                status = "ERROR";
                error = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
                Files.createDirectories(outputDir);
            }

            String expected = expectedLabels.getOrDefault(image.getFileName().toString(), "");
            String match = expected.isBlank() ? "" : String.valueOf(expected.equals(prediction));
            summary.add(String.join(",",
                    csv(image.getFileName().toString()),
                    csv(prediction),
                    csv(String.valueOf(prediction.length())),
                    csv(expected),
                    csv(match),
                    csv(status),
                    csv(outputDir.toAbsolutePath().toString()),
                    csv(outputDir.resolve("result.txt").toAbsolutePath().toString()),
                    csv(outputDir.resolve("step6_final.png").toAbsolutePath().toString()),
                    csv(error)));
        }

        Path summaryFile = batchDir.resolve("summary.csv");
        Files.write(summaryFile, summary, StandardCharsets.UTF_8);
        System.out.println("Input dir: " + inputDir.toAbsolutePath());
        System.out.println("Images processed: " + images.size());
        System.out.println("Successful OCR runs: " + successCount);
        System.out.println("Batch output dir: " + batchDir.toAbsolutePath());
        System.out.println("Summary CSV: " + summaryFile.toAbsolutePath());
    }

    private static List<Path> listImages(Path inputDir) throws Exception {
        try (Stream<Path> stream = Files.list(inputDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> isPng(path) || isJpeg(path))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
        }
    }

    private static Map<String, String> readExpectedLabels(Path labelsFile) throws Exception {
        Map<String, String> labels = new LinkedHashMap<>();
        if (!Files.isRegularFile(labelsFile)) {
            return labels;
        }
        for (String line : Files.readAllLines(labelsFile, StandardCharsets.UTF_8)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.equalsIgnoreCase("image,expected")) {
                continue;
            }
            String[] parts = trimmed.split(",", 2);
            if (parts.length == 2) {
                labels.put(parts[0].trim(), parts[1].trim());
            }
        }
        return labels;
    }

    private static String csv(String value) {
        String safe = value == null ? "" : value;
        return "\"" + safe.replace("\"", "\"\"") + "\"";
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
