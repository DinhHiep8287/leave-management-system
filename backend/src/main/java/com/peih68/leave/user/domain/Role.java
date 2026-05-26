package com.peih68.leave.user.domain;

public enum Role {
    EMPLOYEE,
    MANAGER,
    HR,
    ADMIN;

    public String authority() {
        return "ROLE_" + name();
    }
}
