package com.company.hr.model.dto.response;

/** Shape shared by POST /auth/login and POST /auth/refresh (SPEC.md "API Contract"). */
public record AuthTokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
