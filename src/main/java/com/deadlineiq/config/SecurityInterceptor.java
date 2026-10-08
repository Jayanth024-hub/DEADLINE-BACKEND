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
 * for both Web Sessions (Browser) and REST APIs.
 */
@Component
public class SecurityInterceptor implements HandlerInterceptor {

    private final UserService userService;

    public SecurityInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 0. Always permit CORS OPTIONS preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }

        String uri = request.getRequestURI();

        // 1. Static assets and logout are always allowed
        if (uri.startsWith("/css/") || uri.startsWith("/js/")
                || uri.startsWith("/images/") || uri.equals("/favicon.ico") || uri.equals("/logout")
                || uri.equals("/api/auth/logout")) {
            return true;
        }

        // 2. Public auth endpoints, health checks, and public landing page are always open
        if (uri.startsWith("/api/auth/") || uri.startsWith("/auth/")
                || uri.equals("/api/auth/login") || uri.equals("/auth/login")
                || uri.equals("/api/auth/register") || uri.equals("/auth/register")
                || uri.equals("/api/auth/me") || uri.equals("/auth/me")
                || uri.equals("/api/rag/status")) {
            return true;
        }

        // 3. Resolve Current User via Session or Headers
        User currentUser = resolveUser(request);

        // 4. Public browser pages
        if (uri.equals("/") || uri.equals("/login") || uri.equals("/register")) {
            return true;
        }

        // 5. If not authenticated, block all protected APIs and pages
        if (currentUser == null) {
            if (uri.startsWith("/api/")) {
                sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized: Please log in to access this resource.");
                return false;
            } else {
                response.sendRedirect("/login");
                return false;
            }
        }

        // 6. Role-Based Access Control
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
        if (uri.startsWith("/admin") && role != Role.ADMIN) {
            return rejectAccess(request, response, uri);
        }

        // API role checks
        if (uri.startsWith("/api/admin") && role != Role.ADMIN) {
            return rejectAccess(request, response, uri);
        }

        return true;
    }

    private User resolveUser(HttpServletRequest request) {
        // 1. Check session first
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute("currentUser") != null) {
            return (User) session.getAttribute("currentUser");
        }

        // 2. Check Authorization Bearer token header (cryptographically verified)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer-")) {
            Optional<User> userOpt = userService.getUserByToken(authHeader.trim());
            if (userOpt.isPresent()) {
                request.getSession(true).setAttribute("currentUser", userOpt.get());
                return userOpt.get();
            }
        }

        return null;
    }

    private boolean rejectAccess(HttpServletRequest request, HttpServletResponse response, String uri) throws IOException {
        if (uri.startsWith("/api/")) {
            sendJsonError(response, HttpServletResponse.SC_FORBIDDEN, "Forbidden: Your account role does not have permission to access this resource.");
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
}
