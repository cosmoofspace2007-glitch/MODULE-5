package com.example.transactionprocessingengine;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.antlr.v4.runtime.misc.NotNull;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestDto {

    @JsonAlias({"fromAccount", "senderAccountId"})
    @NotBlank(message = "Source account ID is required")
    private String fromAccountId;

    @JsonAlias({"toAccount", "receiverAccountId"})
    @NotBlank(message = "Destination account ID is required")
    private String toAccountId;

    @NotNull
    @Positive(message = "Transfer amount must be strictly greater than 0")
    private BigDecimal amount;

    @Builder.Default
    private String currency = "VND";

    private String description;

    // Helper getters tương thích với cả 2 cách đặt tên: fromAccount và fromAccountId
    public String getFromAccount() {
        return this.fromAccountId;
    }

    public void setFromAccount(String fromAccount) {
        this.fromAccountId = fromAccount;
    }

    public String getToAccount() {
        return this.toAccountId;
    }

    public void setToAccount(String toAccount) {
        this.toAccountId = toAccount;
    }
}
