package com.company.hr.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Injects the authenticated Employee directly into a controller method parameter, e.g.:
 * {@code public ResponseEntity<?> me(@CurrentUser Employee employee)}.
 * Resolved by CurrentUserArgumentResolver, registered in config.WebMvcConfig.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
