package com.deadlineiq.config;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Optional;

/**
 * Security Interceptor providing Authentication and Role-Based Authorization
 * for both Web Sessions (Browser) and REST API Tokens (Postman / Axios).
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public SecurityInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 1. Static assets and logout are always allowed
        if (uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/images/") || uri.equals("/logout")) {
            return true;
        }

        // 2. Resolve User via Session (Browser/Axios) OR Token Header (Postman)
        User currentUser = resolveUser(request);

        // 3. If user is authenticated and attempts to visit the startpage ("/") or auth pages ("/login", "/register")
        if (currentUser != null && (uri.equals("/") || uri.equals("/login") || uri.equals("/register"))) {
            response.sendRedirect(getDashboardUrl(currentUser.getRole()));
            return false;
        }

        // 4. If not logged in, allow startpage ("/") and public auth/chat/rag endpoints
        if (uri.equals("/") || uri.equals("/login") || uri.equals("/register") || uri.equals("/ai-chat")
                || uri.startsWith("/api/chat") || uri.startsWith("/api/rag")
                || uri.startsWith("/api/auth/login") || uri.startsWith("/api/auth/register")) {
            return true;
        }

        // 5. Handle unauthenticated requests to protected endpoints
        if (currentUser == null) {
            if (uri.startsWith("/api/")) {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Please sign in or pass 'Authorization: Bearer-<token>' header.");
                return false;
            } else {
                String target = uri;
                if (request.getQueryString() != null) {
                    target += "?" + request.getQueryString();
                }
                response.sendRedirect("/login?redirect=" + java.net.URLEncoder.encode(target, java.nio.charset.StandardCharsets.UTF_8));
                return false;
            }
        }

        // 4. Role-based access control
        Role role = currentUser.getRole();
        if (uri.startsWith("/student") && role != Role.STUDENT && role != Role.ADMIN) {
            return rejectAccess(request, response, uri);
        }
        if (uri.startsWith("/faculty") && role != Role.FACULTY && role != Role.ADMIN) {
            return rejectAccess(request, response, uri);
        }
        if (uri.startsWith("/coordinator") && role != Role.COORDINATOR && role != Role.ADMIN) {
            return rejectAccess(request, response, uri);
        }
        if (uri.startsWith("/dean") && role != Role.DEAN) {
            return rejectAccess(request, response, uri);
        }
        if (uri.startsWith("/admin") && role != Role.ADMIN) {
            return rejectAccess(request, response, uri);
        }

        return true;
    }

    private User resolveUser(HttpServletRequest request) {
        // Check session first
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            return (User) session.getAttribute("currentUser");
        }

        // Check Authorization header for Postman: 'Authorization: Bearer-...'
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer-")) {
            Optional<User> userOpt = userService.getUserByToken(authHeader.trim());
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
        }

        // Check custom header 'X-Auth-Token'
        String tokenHeader = request.getHeader("X-Auth-Token");
        if (tokenHeader != null) {
            Optional<User> userOpt = userService.getUserByToken(tokenHeader.trim());
            if (userOpt.isPresent()) {
                return userOpt.get();
            }
        }

        return null;
    }

    private boolean rejectAccess(HttpServletRequest request, HttpServletResponse response, String uri) throws IOException {
        if (uri.startsWith("/api/")) {
            sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden: Your role does not have permission to access this resource.");
        } else {
            response.sendRedirect("/login?unauthorized=true");
        }
        return false;
    }

    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"success\": false, \"status\": " + status + ", \"error\": \"" + message + "\"}");
    }

    private String getDashboardUrl(Role role) {
        if (role == null) return "/student/dashboard";
        switch (role) {
            case STUDENT: return "/student/dashboard";
            case FACULTY: return "/faculty/dashboard";
            case COORDINATOR: return "/coordinator/dashboard";
            case DEAN: return "/dean/dashboard";
            case ADMIN: return "/admin/dashboard";
            default: return "/student/dashboard";
        }
    }
}
