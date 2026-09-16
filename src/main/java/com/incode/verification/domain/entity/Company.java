package com.incode.verification.domain.entity;

import java.util.Objects;

public record Company(String name, String address, boolean active) {
    public Company {
        name = Objects.requireNonNull(name, "name");
        address = Objects.requireNonNull(address, "address");
    }
}
