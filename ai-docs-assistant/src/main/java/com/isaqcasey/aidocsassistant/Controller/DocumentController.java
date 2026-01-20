package com.isaqcasey.aidocsassistant.Controller;

import com.isaqcasey.aidocsassistant.Impl.DocumentAnalyzerImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    @Autowired
    private DocumentAnalyzerImpl documentAnalyzer;

    // Analyze uploaded document (main endpoint - matches frontend)
    @PostMapping("/analyze")
    public String analyze(@RequestParam("file") MultipartFile file) {
        System.out.println("DocumentController: Received file: " + file.getOriginalFilename());
        return documentAnalyzer.analyze(file);
    }

    // Analyze uploaded document (alternative endpoint)
    @PostMapping("/analyze-file")
    public String analyzeFile(@RequestParam("file") MultipartFile file) {
        System.out.println("DocumentController: Received file: " + file.getOriginalFilename());
        return documentAnalyzer.analyze(file);
    }

    // Optional: Analyze plain text
    @PostMapping("/analyze-text")
    public String analyzeText(@RequestParam String text) {
        return documentAnalyzer.analyzeTextFile(text);
    }
}
