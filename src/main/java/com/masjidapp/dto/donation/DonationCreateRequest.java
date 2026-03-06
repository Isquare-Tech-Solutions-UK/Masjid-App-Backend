package com.masjidapp.dto.donation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DonationCreateRequest {
    @NotBlank
    private String donorName;
    @NotBlank
    private String donorEmail;
    @NotNull
    @Positive
    private BigDecimal amount;
    private Boolean isAnonymous = Boolean.FALSE;
    private boolean coverFee = false;
    @NotBlank
    private String successUrl;
    @NotBlank
    private String cancelUrl;

}
