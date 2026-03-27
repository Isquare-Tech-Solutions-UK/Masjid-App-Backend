package com.masjidapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcType;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "donations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "donor_name", length = 100)
    private String donorName;

    @Column(name = "donor_email", length = 255)
    private String donorEmail;

    @Column(name = "is_anonymous")
    private boolean anonymous = false;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "processing_fee")
    private BigDecimal processingFee = BigDecimal.ZERO;

    @Column(name = "cover_fee")
    private boolean coverFee = false;

    @Column(name = "total_charged")
    private BigDecimal totalCharged;

    @Column(name = "currency", length = 3)
    private String currency = "GBP";

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;

    @Column(name = "stripe_checkout_session_id", length = 255)
    private String stripeCheckoutSessionId;

    @Column(name = "payment_method", length = 20)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(columnDefinition = "donation_status")
    @Builder.Default
    private DonationStatus status = DonationStatus.pending;

    @Column(name = "receipt_sent")
    private boolean receiptSent = false;

    @Column(name = "receipt_sent_at")
    private Instant receiptSentAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;

}
