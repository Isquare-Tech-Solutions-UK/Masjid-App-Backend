package com.masjidapp.settings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.masjidapp.settings.entity.MasjidSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberMasjidInfoResponse {

    private String name;
    private String about;
    private String logoUrl;
    private MemberAddress address;
    private MasjidSettings.Contact contact;
    private MasjidSettings.Location location;
    private MemberCapacity capacity;
    private List<String> services;
    private List<String> facilities;
    private boolean donationsEnabled;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberAddress {
        private String line1;
        private String line2;
        private String city;
        private String postcode;
        private String country;
        private String formatted;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberCapacity {
        private Integer mens;
        private Integer womens;
        private Integer total;
    }

    public static MemberMasjidInfoResponse fromEntity(MasjidSettings settings) {
        return MemberMasjidInfoResponse.builder()
                .name(settings.getName())
                .about(settings.getAbout())
                .logoUrl(settings.getLogoUrl())
                .address(buildAddress(settings.getAddress()))
                .contact(settings.getContact())
                .location(settings.getLocation())
                .capacity(buildCapacity(settings.getCapacity()))
                .services(buildEnabledServices(settings.getServices()))
                .facilities(buildEnabledFacilities(settings.getFacilities()))
                .donationsEnabled(hasPaymentConfigured(settings.getPayment()))
                .build();
    }

    private static MemberAddress buildAddress(MasjidSettings.Address addr) {
        if (addr == null) return null;

        String formatted = Stream.of(addr.getLine1(), addr.getLine2(), addr.getCity(), addr.getPostcode())
                .filter(s -> s != null && !s.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);

        return MemberAddress.builder()
                .line1(addr.getLine1())
                .line2(addr.getLine2())
                .city(addr.getCity())
                .postcode(addr.getPostcode())
                .country(addr.getCountry())
                .formatted(formatted)
                .build();
    }

    private static MemberCapacity buildCapacity(MasjidSettings.Capacity cap) {
        if (cap == null) return null;

        int mens = cap.getMens() != null ? cap.getMens() : 0;
        int womens = cap.getWomens() != null ? cap.getWomens() : 0;

        return MemberCapacity.builder()
                .mens(cap.getMens())
                .womens(cap.getWomens())
                .total(mens + womens)
                .build();
    }

    private static List<String> buildEnabledServices(MasjidSettings.Services svc) {
        if (svc == null) return List.of();

        List<String> enabled = new ArrayList<>();
        if (Boolean.TRUE.equals(svc.getMarriageService())) enabled.add("Marriage Service");
        if (Boolean.TRUE.equals(svc.getHallRental())) enabled.add("Hall Rental");
        if (Boolean.TRUE.equals(svc.getIftarProgram())) enabled.add("Iftar Program");
        if (Boolean.TRUE.equals(svc.getCounseling())) enabled.add("Counseling");
        if (Boolean.TRUE.equals(svc.getNewMuslimSupport())) enabled.add("New Muslim Support");
        if (Boolean.TRUE.equals(svc.getFuneralService())) enabled.add("Funeral Service");
        return enabled;
    }

    private static List<String> buildEnabledFacilities(MasjidSettings.Facilities fac) {
        if (fac == null) return List.of();

        List<String> enabled = new ArrayList<>();
        if (Boolean.TRUE.equals(fac.getParking())) enabled.add("Parking");
        if (Boolean.TRUE.equals(fac.getWomensArea())) enabled.add("Women's Area");
        if (Boolean.TRUE.equals(fac.getShoeRacks())) enabled.add("Shoe Racks");
        if (Boolean.TRUE.equals(fac.getWuduFacilities())) enabled.add("Wudu Facilities");
        if (Boolean.TRUE.equals(fac.getWashroom())) enabled.add("Washroom");
        return enabled;
    }

    private static boolean hasPaymentConfigured(MasjidSettings.Payment payment) {
        if (payment == null) return false;
        return (payment.getBankAccountName() != null && !payment.getBankAccountName().isBlank())
                || (payment.getBankAccountNumber() != null && !payment.getBankAccountNumber().isBlank());
    }
}
