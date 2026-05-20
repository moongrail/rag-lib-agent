package com.ragassistant.ingestion;

import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.springframework.stereotype.Component;

@Component
public class DocumentParserFactory {

    public DocumentParser parserFor(String contentType, String fileName) {
        if (isPdf(contentType, fileName)) {
            return new ApachePdfBoxDocumentParser();
        }
        return new ApacheTikaDocumentParser();
    }

    private boolean isPdf(String contentType, String fileName) {
        if (contentType != null && contentType.contains("pdf")) {
            return true;
        }
        String name = fileName == null ? "" : fileName.toLowerCase();
        return name.endsWith(".pdf");
    }
}
