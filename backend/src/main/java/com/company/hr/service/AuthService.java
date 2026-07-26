package com.company.hr.service;

import com.company.hr.model.dto.response.AuthTokenResponse;

/** Login/refresh/logout — see SPEC.md "API Contract" > Auth and docs/adr/0003-jwt-access-refresh-auth.md. */
public interface AuthService {

    AuthTokenResponse login(String email, String rawPassword);

    AuthTokenResponse refresh(String rawRefreshToken);

    void logout(String rawRefreshToken);
}
