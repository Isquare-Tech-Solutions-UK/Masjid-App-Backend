package com.masjidapp.dto.donation;

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
    private String donorEmail;
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

    public static DonationDto toDto(Donation donation) {
        return DonationDto.builder()
                .id(donation.getId())
                .campaignId(donation.getCampaign().getId())
                .campaignTitle(donation.getCampaign().getTitle())
                .donorName(donation.getDonorName())
                .donorEmail(donation.getDonorEmail())
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
