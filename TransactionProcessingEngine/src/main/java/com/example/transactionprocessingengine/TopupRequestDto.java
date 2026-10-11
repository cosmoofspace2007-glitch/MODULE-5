package com.example.transactionprocessingengine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopupRequestDto {

    @NotBlank(message = "User wallet account ID is required")
    private String userAccountId;

    @NotNull(message = "Topup amount is required")
    @Positive(message = "Topup amount must be strictly greater than 0")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "VND";

    private String referenceCode;
}

