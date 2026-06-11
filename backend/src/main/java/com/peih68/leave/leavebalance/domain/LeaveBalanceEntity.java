package com.peih68.leave.leavebalance.domain;

import com.peih68.leave.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_balances",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_balance_user_type_year",
                columnNames = {"user_id", "leave_type_id", "year"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveBalanceEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "leave_type_id", nullable = false)
    private Long leaveTypeId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "total_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal totalDays;

    @Column(name = "used_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal usedDays;

    @Column(name = "adjusted_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal adjustedDays;

    @Column(name = "carried_over_days", nullable = false, precision = 5, scale = 1)
    @Builder.Default
    private BigDecimal carriedOverDays = BigDecimal.ZERO;

    /** remaining = total + adjusted + carried_over - used (computed, not persisted). */
    public BigDecimal remaining() {
        return totalDays.add(adjustedDays).add(carriedOverDays).subtract(usedDays);
    }
}
