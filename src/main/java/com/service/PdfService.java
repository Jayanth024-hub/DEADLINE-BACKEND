package com.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;

@Service
public class PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfService.class);
    private static final int MAX_PAGES_TO_EXTRACT = 30;
    private static final int MAX_TEXT_LENGTH_CHARS = 150_000;

    public String extractText(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        // Stream directly using RandomAccessReadBuffer without keeping duplicate byte[] in heap
        try (InputStream inputStream = file.getInputStream();
             RandomAccessReadBuffer readBuffer = new RandomAccessReadBuffer(inputStream);
             PDDocument document = Loader.loadPDF(readBuffer)) {

            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            pdfTextStripper.setStartPage(1);
            int totalPages = document.getNumberOfPages();
            int endPage = Math.min(totalPages, MAX_PAGES_TO_EXTRACT);
            pdfTextStripper.setEndPage(endPage);

            String text = pdfTextStripper.getText(document);
            if (text != null && text.length() > MAX_TEXT_LENGTH_CHARS) {
                logger.warn("Extracted PDF text exceeds {} chars, safely truncating to bound RAM usage.", MAX_TEXT_LENGTH_CHARS);
                text = text.substring(0, MAX_TEXT_LENGTH_CHARS);
            }
            return text != null ? text : "";
        }
    }
}

