package com.example.bajaj.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PdfHelper {
    private static final Logger log = LoggerFactory.getLogger(PdfHelper.class);

    // direct download links (uc?export=download) based on provided drive ids
    private static final String QUESTION_ODD = "https://drive.google.com/uc?export=download&id=1IeSI6l6KoSQAFfRihIT9tEDICtoz-G";
    private static final String QUESTION_EVEN = "https://drive.google.com/uc?export=download&id=143MR5cLFrlNEuHzzWJ5RHnEWuijuM9X";

    public static void downloadQuestionPdfForRegNo(String regNo, String downloadDir) {
        try {
            String lastTwo = lastTwoDigits(regNo);
            boolean isEven = false;
            try { int n = Integer.parseInt(lastTwo); isEven = n % 2 == 0; } catch (Exception e) { log.warn("Could not parse last two digits: {}", lastTwo); }

            String url = isEven ? QUESTION_EVEN : QUESTION_ODD;
            log.info("Downloading question PDF from: {}", url);

            Path dir = Paths.get(downloadDir);
            if (!Files.exists(dir)) Files.createDirectories(dir);

            String fileName = dir.resolve(isEven ? "question_even.pdf" : "question_odd.pdf").toString();

            downloadFile(url, fileName);

            // try extract text
            extractTextFromPdf(fileName);

        } catch (Exception e) {
            log.error("Failed to download or extract PDF: {}", e.getMessage());
        }
    }

    private static void downloadFile(String urlString, String destPath) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (InputStream in = new BufferedInputStream(conn.getInputStream()); FileOutputStream out = new FileOutputStream(destPath)) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        }
        log.info("Downloaded file to {}", destPath);
    }

    private static void extractTextFromPdf(String path) {
        try (PDDocument doc = PDDocument.load(new File(path))) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(doc);
            log.info("Extracted text (first 1000 chars):\n{}", text.length() > 1000 ? text.substring(0,1000) : text);
            Path txtPath = Paths.get(path + ".txt");
            Files.writeString(txtPath, text);
            log.info("Saved extracted text to {}", txtPath.toString());
        } catch (Exception e) {
            log.warn("Could not extract PDF text: {}", e.getMessage());
        }
    }

    private static String lastTwoDigits(String regNo) {
        if (!StringUtils.hasText(regNo)) return "";
        String digits = regNo.replaceAll("\\D+", "");
        if (digits.length() >= 2) return digits.substring(digits.length()-2);
        return digits;
    }
}
