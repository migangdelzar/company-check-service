package com.incode.verification.architecture;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.incode.verification.domain.aggregate.Verification;
import org.junit.jupiter.api.Test;

class HexagonalDependencyTest {
    @Test
    void domainTypesHaveNoSpringAnnotations() {
        assertFalse(Verification.class.isAnnotationPresent(org.springframework.stereotype.Component.class));
    }
}
