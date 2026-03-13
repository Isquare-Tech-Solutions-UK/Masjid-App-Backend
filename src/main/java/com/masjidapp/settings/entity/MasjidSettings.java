package com.masjidapp.settings.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "masjid_settings")
public class MasjidSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String about;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "logo_key")
    private String logoKey;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Address address = new Address();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Contact contact = new Contact();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Location location = new Location();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Capacity capacity = new Capacity();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Services services = new Services();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Facilities facilities = new Facilities();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Payment payment = new Payment();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Address {
        private String line1;
        private String line2;
        private String city;
        private String postcode;
        private String country;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Contact {
        private String phone;
        private String email;
        private String website;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Location {
        private Double latitude;
        private Double longitude;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Capacity {
        private Integer mens;
        private Integer womens;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Services {
        @Builder.Default private Boolean marriageService = false;
        @Builder.Default private Boolean hallRental = false;
        @Builder.Default private Boolean iftarProgram = false;
        @Builder.Default private Boolean counseling = false;
        @Builder.Default private Boolean newMuslimSupport = false;
        @Builder.Default private Boolean funeralService = false;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Facilities {
        @Builder.Default private Boolean parking = false;
        @Builder.Default private Boolean womensArea = false;
        @Builder.Default private Boolean shoeRacks = false;
        @Builder.Default private Boolean wuduFacilities = false;
        @Builder.Default private Boolean washroom = false;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Payment {
        private String bankAccountName;
        private String bankName;
        private String bankAccountNumber;
        private String bankSortCode;
    }
}
