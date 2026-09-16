package com.incode.verification.domain.entity;

import java.util.Objects;

public record Company(String name, String address, boolean active) {
    public Company(String name, String address, boolean active) {
        this.name = Objects.requireNonNull(name, "name");
        this.address = Objects.requireNonNull(address, "address");
        this.active = active;
    }
}
