package com.mca.automate.util;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageToTextExtractor {
    @Value(value="${ocr.tessdataPath}")
    String ocrTesseract;
    @Value(value="${ocr.language}")
    String ocrLang;
    @Value(value="${ocr.nativeLibraryPath:}")
    String nativeLibraryPath;

    public String getCaptchaText(BufferedImage preProcessedImg) throws TesseractException {
        configureNativeLibraryPath();
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(this.ocrTesseract);
        tesseract.setLanguage(this.ocrLang);
        tesseract.setPageSegMode(8);
        tesseract.setTessVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        tesseract.setTessVariable("load_system_dawg", "0");
        tesseract.setTessVariable("load_freq_dawg", "0");
        tesseract.setTessVariable("user_defined_dpi", "300");
        String text = tesseract.doOCR(preProcessedImg);
        return text;
    }

    private void configureNativeLibraryPath() {
        if (this.nativeLibraryPath == null || this.nativeLibraryPath.isBlank()) {
            return;
        }
        System.setProperty("jna.library.path", this.nativeLibraryPath);
    }

    public void main(String[] args) throws Exception {
        File imgFile = new File("preprocessed.png");
        if (!imgFile.exists()) {
            System.err.println("Image not found: " + imgFile.getAbsolutePath());
            return;
        }
        Tesseract tesseract = new Tesseract();
        tesseract.setPageSegMode(8);
        tesseract.setTessVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789");
        BufferedImage img = ImageIO.read(imgFile);
        try {
            String text = tesseract.doOCR(img);
            System.out.println("---- OCR RESULT ----");
            System.out.println(text);
        }
        catch (TesseractException e) {
            System.err.println("OCR failed: " + e.getMessage());
        }
    }
}
