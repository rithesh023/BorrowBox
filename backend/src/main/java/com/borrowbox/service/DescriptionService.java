package com.borrowbox.service;

import com.borrowbox.dto.DescriptionResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;

@Service
public class DescriptionService {
    private final AIService ai;
    public DescriptionService(AIService ai) { this.ai = ai; }
    public DescriptionResponse generate(String name, String condition, String details) {
        String cleanName = name.trim();
        String cleanCondition = condition.trim();
        String cleanDetails = details == null ? "" : details.trim();
        if (!ai.isConfigured()) return basicDraft(cleanName, cleanCondition, cleanDetails,
                "No AI key is configured; a basic editable draft was created instead.");
        try {
            return new DescriptionResponse(ai.generateDescription(cleanName, cleanCondition, cleanDetails), true,
                    "AI draft created. Review it and correct any details before publishing.");
        } catch (ResponseStatusException exception) {
            return basicDraft(cleanName, cleanCondition, cleanDetails,
                    "The AI service was unavailable; a basic editable draft was created instead.");
        }
    }

    private DescriptionResponse basicDraft(String name, String condition, String details, String message) {
        String description = name + " is available to rent and is in " + condition.toLowerCase() + " condition.";
        if (!details.isBlank()) description += " " + details;
        description += " Please confirm the included items and availability with the owner before requesting.";
        if (description.length() > 3000) description = description.substring(0, 3000);
        return new DescriptionResponse(description, false, message);
    }
}
