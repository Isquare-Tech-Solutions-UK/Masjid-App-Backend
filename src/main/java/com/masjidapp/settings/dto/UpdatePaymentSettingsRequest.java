package com.masjidapp.settings.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePaymentSettingsRequest {

    private String bankAccountName;
    private String bankName;
    private String bankAccountNumber;
    private String bankSortCode;
}
