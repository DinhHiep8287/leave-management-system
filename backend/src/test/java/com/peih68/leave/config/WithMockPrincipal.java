package com.peih68.leave.config;

import com.peih68.leave.user.domain.Role;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Populates the SecurityContext with a real {@link com.peih68.leave.auth.domain.UserPrincipal}
 * carrying a concrete id, so @PreAuthorize SpEL like {@code #id == principal.id} can be tested
 * in @WebMvcTest slices (addFilters = false) where request-post-processor auth would not apply.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockPrincipalSecurityContextFactory.class)
public @interface WithMockPrincipal {
    long id();

    Role role() default Role.EMPLOYEE;

    String email() default "mock@demo.local";
}
