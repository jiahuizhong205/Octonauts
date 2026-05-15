package com.campushub.requirement.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RequirementListItem(
        Long reqId,
        String title,
        BigDecimal budget,
        String type,
        String status,
        String publisherName,
        LocalDateTime createdAt
) {
}
