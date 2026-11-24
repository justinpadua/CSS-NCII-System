package com.cssncii.controller;

import com.cssncii.dto.ApiErrorResponse;
import com.cssncii.dto.CsrfTokenResponse;
import com.cssncii.dto.StudentAccountResponse;
import com.cssncii.dto.StudentLoginRequest;
import com.cssncii.dto.StudentLoginResponse;
import com.cssncii.dto.StudentRegistrationRequest;
import com.cssncii.dto.StudentRegistrationResponse;
import com.cssncii.entity.Student;
import com.cssncii.security.StudentPrincipal;
import com.cssncii.service.StudentAuthenticationService;
import com.cssncii.service.StudentService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/auth")
public class StudentAuthController {
    private final StudentAuthenticationService studentAuthenticationService;
    private final StudentService studentService;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfAuthenticationStrategy csrfAuthenticationStrategy;
    private final SessionRegistry sessionRegistry;

    public StudentAuthController(
        StudentAuthenticationService studentAuthenticationService,
        StudentService studentService,
        SecurityContextRepository securityContextRepository,
        CsrfAuthenticationStrategy csrfAuthenticationStrategy,
        SessionRegistry sessionRegistry
    ) {
        this.studentAuthenticationService = studentAuthenticationService;
        this.studentService = studentService;
        this.securityContextRepository = securityContextRepository;
        this.csrfAuthenticationStrategy = csrfAuthenticationStrategy;
        this.sessionRegistry = sessionRegistry;
    }

    @PostMapping("/register")
    public ResponseEntity<StudentRegistrationResponse> register(
        @Valid @RequestBody StudentRegistrationRequest registration
    ) {
        StudentService.StudentRegistrationResult result = studentService.registerStudent(
            registration.fullName(), registration.program(), registration.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(new StudentRegistrationResponse(
            result.accountId(), result.status(), result.possibleDuplicate(),
            "Registration received. An Admin must approve the account before login."));
    }

    @PostMapping("/login")
    public ResponseEntity<StudentLoginResponse> login(
        @Valid @RequestBody StudentLoginRequest loginRequest,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        Student student = studentAuthenticationService.authenticate(
            loginRequest.accountId(), loginRequest.password());
        StudentAccountResponse account = new StudentAccountResponse(
            student.getId(), student.getAccountId(), student.getFullName(), student.getProgram());
        StudentPrincipal principal = new StudentPrincipal(
            account.id(), account.accountId(), account.fullName(), account.program());
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT")));

        HttpSession existingSession = request.getSession(false);
        if (existingSession != null) {
            sessionRegistry.removeSessionInformation(existingSession.getId());
            request.changeSessionId();
        }
        csrfAuthenticationStrategy.onAuthentication(authentication, request, response);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        HttpSession authenticatedSession = request.getSession(false);
        if (authenticatedSession != null) {
            sessionRegistry.registerNewSession(authenticatedSession.getId(), principal);
        }

        return ResponseEntity.ok(StudentLoginResponse.authenticated(account));
    }

    @GetMapping("/csrf")
    public CsrfTokenResponse csrfToken(CsrfToken csrfToken) {
        return new CsrfTokenResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    @GetMapping("/me")
    public StudentAccountResponse currentStudent(@AuthenticationPrincipal StudentPrincipal principal) {
        return new StudentAccountResponse(
            principal.id(), principal.accountId(), principal.fullName(), principal.program());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            sessionRegistry.removeSessionInformation(session.getId());
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> invalidRequest(MethodArgumentNotValidException exception) {
        return error(HttpStatus.BAD_REQUEST, "Required registration or login fields are missing or invalid.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> malformedRequest(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Request body contains invalid or malformed values.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> invalidInput(IllegalArgumentException exception) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> invalidCredentials(EntityNotFoundException exception) {
        return error(HttpStatus.UNAUTHORIZED, "Invalid Account ID or password, or the account is not active.");
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(message));
    }
}
