package com.ragassistant.api.dto;

import java.util.List;

public record ChatResponse(String sessionId, String answer, List<Citation> citations) {
}
