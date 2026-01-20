package com.isaqcasey.aidocsassistant.Service;

import org.springframework.web.multipart.MultipartFile;

public interface DocsAnalyzer {
    public String analyze(MultipartFile file);
    public String extractText(MultipartFile file);
}
