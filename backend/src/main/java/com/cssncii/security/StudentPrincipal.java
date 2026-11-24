package com.cssncii.security;

import java.io.Serializable;

/** Session identity for a Student account; deliberately separate from AdminPrincipal. */
public record StudentPrincipal(Long id, String accountId, String fullName, String program)
    implements Serializable {
    private static final long serialVersionUID = 1L;
}
