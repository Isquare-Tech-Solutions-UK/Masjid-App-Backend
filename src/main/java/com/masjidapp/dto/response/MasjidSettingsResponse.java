package com.masjidapp.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.masjidapp.entity.MasjidSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MasjidSettingsResponse {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("about")
    private String about;

    @JsonProperty("logoUrl")
    private String logoUrl;

    @JsonProperty("address")
    private MasjidSettings.Address address;

    @JsonProperty("contact")
    private MasjidSettings.Contact contact;

    @JsonProperty("location")
    private MasjidSettings.Location location;

    @JsonProperty("capacity")
    private MasjidSettings.Capacity capacity;

    @JsonProperty("services")
    private MasjidSettings.Services services;

    @JsonProperty("facilities")
    private MasjidSettings.Facilities facilities;

    @JsonProperty("payment")
    private PaymentResponse payment;

    @JsonProperty("stripe")
    private StripeSettingsResponse stripe;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private String bankAccountName;
        private String bankName;
        private String bankAccountNumber;
        private String bankSortCode;
    }

    public static MasjidSettingsResponse fromEntity(MasjidSettings settings) {
        PaymentResponse paymentResponse = null;
        if (settings.getPayment() != null) {
            paymentResponse = PaymentResponse.builder()
                    .bankAccountName(settings.getPayment().getBankAccountName())
                    .bankName(settings.getPayment().getBankName())
                    .bankAccountNumber(settings.getPayment().getBankAccountNumber())
                    .bankSortCode(settings.getPayment().getBankSortCode())
                    .build();
        }

        return MasjidSettingsResponse.builder()
                .id(settings.getId())
                .name(settings.getName())
                .about(settings.getAbout())
                .logoUrl(settings.getLogoUrl())
                .address(settings.getAddress())
                .contact(settings.getContact())
                .location(settings.getLocation())
                .capacity(settings.getCapacity())
                .services(settings.getServices())
                .facilities(settings.getFacilities())
                .payment(paymentResponse)
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
