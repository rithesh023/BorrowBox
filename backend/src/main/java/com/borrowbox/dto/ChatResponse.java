package com.borrowbox.dto;

import java.util.List;

public record ChatResponse(String message, List<ItemResponse> items, boolean aiEnhanced, String aiStatus) {}
