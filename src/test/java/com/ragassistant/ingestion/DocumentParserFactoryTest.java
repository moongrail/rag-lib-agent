package com.ragassistant.ingestion;

import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentParserFactoryTest {

    private final DocumentParserFactory factory = new DocumentParserFactory();

    @Test
    void pdfByContentType_usesPdfBox() {
        assertThat(factory.parserFor("application/pdf", "x.txt"))
                .isInstanceOf(ApachePdfBoxDocumentParser.class);
    }

    @Test
    void pdfByExtension_usesPdfBox() {
        assertThat(factory.parserFor("text/plain", "report.pdf"))
                .isInstanceOf(ApachePdfBoxDocumentParser.class);
    }

    @Test
    void nonPdf_usesTika() {
        assertThat(factory.parserFor("text/plain", "doc.docx"))
                .isInstanceOf(ApacheTikaDocumentParser.class);
        assertThat(factory.parserFor("application/xml", "data.xml"))
                .isInstanceOf(ApacheTikaDocumentParser.class);
    }
}
