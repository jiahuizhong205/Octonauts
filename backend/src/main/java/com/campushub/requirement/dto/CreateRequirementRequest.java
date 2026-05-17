package com.campushub.requirement.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateRequirementRequest(
        @NotBlank(message = "标题不能为空")
        @Size(min = 5, max = 50, message = "标题长度需在 5-50 字符之间")
        String title,

        @NotBlank(message = "描述不能为空")
        @Size(max = 500, message = "描述不能超过 500 字符")
        String description,

        @NotNull(message = "预算不能为空")
        @DecimalMin(value = "0.0", message = "预算金额必须大于等于0")
        BigDecimal budget,

        @NotBlank(message = "需求分类不能为空")
        String type
) {
}
