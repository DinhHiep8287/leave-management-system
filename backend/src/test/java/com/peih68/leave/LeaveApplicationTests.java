package com.peih68.leave;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class LeaveApplicationTests {

    @Test
    void contextLoads() {
        // Spring context boots and Flyway migrations apply against the test Postgres database.
    }
}
