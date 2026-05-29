package com.ragassistant.api.dto;

import java.util.UUID;

public record UploadResponse(UUID id, String fileName, String status) {
}
