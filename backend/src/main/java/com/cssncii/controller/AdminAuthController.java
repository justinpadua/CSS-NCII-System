package com.cssncii.controller;

import com.cssncii.dto.AdminAccountResponse;
import com.cssncii.dto.AdminLoginRequest;
import com.cssncii.dto.AuthErrorResponse;
import com.cssncii.dto.CsrfTokenResponse;
import com.cssncii.security.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfAuthenticationStrategy csrfAuthenticationStrategy;

    public AdminAuthController(
        AuthenticationManager authenticationManager,
        SecurityContextRepository securityContextRepository,
        CsrfAuthenticationStrategy csrfAuthenticationStrategy
    ) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.csrfAuthenticationStrategy = csrfAuthenticationStrategy;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
        @Valid @RequestBody AdminLoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                    loginRequest.username().trim(), loginRequest.password()));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new AuthErrorResponse("Invalid username or password."));
        }

        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        csrfAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        AdminPrincipal admin = (AdminPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(new AdminAccountResponse(admin.getId(), admin.getUsername()));
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrfToken(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @GetMapping("/me")
    public AdminAccountResponse currentAdmin(@AuthenticationPrincipal AdminPrincipal admin) {
        return new AdminAccountResponse(admin.getId(), admin.getUsername());
    }
}
