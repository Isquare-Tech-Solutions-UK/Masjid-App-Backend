package com.masjidapp.dto.donation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.masjidapp.entity.Donation;
import com.masjidapp.entity.DonationStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class DonationDto {
    private UUID id;
    private UUID campaignId;
    private String campaignTitle;
    private String donorName;
    @JsonProperty("isAnonymous")
    private boolean isAnonymous;
    private boolean coverFee;
    private BigDecimal amount;
    private BigDecimal processingFee;
    private BigDecimal totalCharged;
    private String currency;
    private String paymentMethod;
    private DonationStatus status;
    private boolean receiptSent;
    private Instant receiptSentAt;
    private Instant createdAt;
    private Instant completedAt;

    /**
     * Public-facing view for member/donor lists: anonymous donations have the donor's
     * name replaced with "Anonymous" and their email hidden. Use for any list shown to
     * other users; use {@link #toDto} for admin/owner views that need the real donor.
     */
    public static DonationDto toPublicDto(Donation donation) {
        DonationDto dto = toDto(donation);
        if (donation.isAnonymous()) {
            dto.setDonorName("Anonymous");
        }
        return dto;
    }

    public static DonationDto toDto(Donation donation) {
        return DonationDto.builder()
                .id(donation.getId())
                .campaignId(donation.getCampaign().getId())
                .campaignTitle(donation.getCampaign().getTitle())
                .donorName(donation.getDonorName())
                .isAnonymous(donation.isAnonymous())
                .amount(donation.getAmount())
                .processingFee(donation.getProcessingFee())
                .coverFee(donation.isCoverFee())
                .totalCharged(donation.getTotalCharged())
                .currency(donation.getCurrency())
                .paymentMethod(donation.getPaymentMethod())
                .status(donation.getStatus())
                .receiptSent(donation.isReceiptSent())
                .receiptSentAt(donation.getReceiptSentAt())
                .createdAt(donation.getCreatedAt())
                .completedAt(donation.getCompletedAt())
                .build();
    }
}
