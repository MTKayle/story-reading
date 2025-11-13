package org.example.storyreading.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DepositRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "10000.0", message = "Minimum deposit amount is 10,000 VND")
    private BigDecimal amount;

    private String description;

    // explicit getter in case Lombok annotation processing is not available in the build environment
    public BigDecimal getAmount() {
        return this.amount;
    }

    public String getDescription() {
        return this.description;
    }

}
