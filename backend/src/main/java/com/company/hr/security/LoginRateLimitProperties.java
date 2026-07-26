package com.company.hr.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.login-rate-limit")
public class LoginRateLimitProperties {

    private int capacity;
    private int refillPerMinute;

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getRefillPerMinute() {
        return refillPerMinute;
    }

    public void setRefillPerMinute(int refillPerMinute) {
        this.refillPerMinute = refillPerMinute;
    }
}
