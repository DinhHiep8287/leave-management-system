package com.peih68.leave.leavetype.domain;

import com.peih68.leave.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "leave_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeaveTypeEntity extends BaseEntity {

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "default_quota_days", nullable = false, precision = 5, scale = 1)
    private BigDecimal defaultQuotaDays;

    @Column(name = "requires_balance", nullable = false)
    private Boolean requiresBalance;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}
