package com.bank.userservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "account_transfers",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_account_transfer_reference",
                        columnNames = "transfer_reference"
                )
        },
        indexes = {
                @Index(
                        name = "idx_account_transfer_sender",
                        columnList = "sender_user_id"
                ),
                @Index(
                        name = "idx_account_transfer_receiver",
                        columnList = "receiver_user_id"
                ),
                @Index(
                        name = "idx_account_transfer_created_at",
                        columnList = "created_at"
                )
        }
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(name = "transfer_reference", nullable = false, length = 100)
    private String transferReference;

    @Column(name = "sender_user_id", nullable = false)
    private Long senderUserId;

    @Column(name = "receiver_user_id", nullable = false)
    private Long receiverUserId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountTransferStatus status;

    @Column(name = "sender_balance_after", precision = 19, scale = 2)
    private BigDecimal senderBalanceAfter;

    @Column(name = "receiver_balance_after", precision = 19, scale = 2)
    private BigDecimal receiverBalanceAfter;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

}