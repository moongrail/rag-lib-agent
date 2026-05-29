package com.ragassistant.api.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentSummary(UUID id, String title, String fileName, String status, int chunkCount, Instant createdAt) {
}
