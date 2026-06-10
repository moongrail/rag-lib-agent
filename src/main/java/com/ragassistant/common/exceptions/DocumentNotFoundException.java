package com.ragassistant.common.exceptions;

public class DocumentNotFoundException extends RagException {

    public DocumentNotFoundException(String id) {
        super("Document not found: " + id);
    }
}
