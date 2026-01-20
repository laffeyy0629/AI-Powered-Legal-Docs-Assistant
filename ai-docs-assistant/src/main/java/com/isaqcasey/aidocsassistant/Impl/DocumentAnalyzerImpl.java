package com.isaqcasey.aidocsassistant.Impl;

import com.isaqcasey.aidocsassistant.Service.AIService;
import com.isaqcasey.aidocsassistant.Service.DocsAnalyzer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DocumentAnalyzerImpl implements DocsAnalyzer {

    private final AIService aiService;

    public DocumentAnalyzerImpl(AIService aiService) {
        this.aiService = aiService;
    }

    public String analyzeTextFile(String text) {
        return aiService.summarize(text); // or analyzeText(text)
    }

    @Override
    public String extractText(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return "Error: No file provided";
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null) {
                return "Error: Invalid filename";
            }
            
            filename = filename.toLowerCase();

            if (filename.endsWith(".pdf")) {
                return extractPdf(file);
            } else if (filename.endsWith(".docx")) {
                return extractDocx(file);
            } else if (filename.endsWith(".txt")) {
                return new String(file.getBytes(), "UTF-8");
            } else {
                return "Error: Unsupported file type. Please upload PDF, DOCX, or TXT files.";
            }
        } catch (Exception e) {
            System.err.println("Error extracting text: " + e.getMessage());
            e.printStackTrace();
            return "Error extracting text: " + e.getMessage();
        }
    }

    @Override
    public String analyze(MultipartFile file) {
        try {
            // Extract text
            String text = extractText(file);
            
            // Check for extraction errors
            if (text.startsWith("Error:")) {
                return text;
            }
            
            // Validate extracted text
            if (text.trim().isEmpty()) {
                return "Error: No text content found in document";
            }
            
            System.out.println("Extracted text length: " + text.length() + " characters");
            
            // Summarize
            long startTime = System.currentTimeMillis();
            String result = aiService.summarize(text);
            long duration = System.currentTimeMillis() - startTime;
            
            System.out.println("Summary generated in " + duration + "ms");
            return result;
            
        } catch (Exception e) {
            System.err.println("Error analyzing document: " + e.getMessage());
            e.printStackTrace();
            return "Error analyzing document: " + e.getMessage();
        }
    }



    // --- Private helper methods ---

    private String extractPdf(MultipartFile file) {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            if (document.getNumberOfPages() == 0) {
                return "Error: PDF has no pages";
            }
            
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            
            // Limit to first 50 pages for performance
            int maxPages = Math.min(document.getNumberOfPages(), 50);
            stripper.setEndPage(maxPages);
            
            String text = stripper.getText(document);
            
            if (text.trim().isEmpty()) {
                return "Error: PDF appears to be image-based or empty. OCR not supported.";
            }
            
            return text;
        } catch (Exception e) {
            System.err.println("Error reading PDF: " + e.getMessage());
            e.printStackTrace();
            return "Error reading PDF: " + e.getMessage();
        }
    }

    private String extractDocx(MultipartFile file) {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error reading DOCX: " + e.getMessage();
        }
    }
}
