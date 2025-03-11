package org.oyyj.adminservice.exception;

import org.springframework.security.core.AuthenticationException;


public class SourceNotLegitimateException extends AuthenticationException {
    public SourceNotLegitimateException(String msg) {
        super(msg);
    }
}
