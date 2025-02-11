package com.exceptions;

public class TenantNotFoundException extends RuntimeException {
    public TenantNotFoundException(String domain) {
        super("Tenant not found: " + domain);
    }
}
