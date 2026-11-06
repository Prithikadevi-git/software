package com.datamint.energy.security;

import com.datamint.energy.model.*;
import com.datamint.energy.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Module 11 - role based access.
 * VIEWER: read only | ENGINEER: may write readings, alerts, equipment | MANAGER: write everything except users/settings | ADMIN: everything.
 * Every successful write is stored in audit_logs.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    @Value("${app.auth.enabled:true}") private boolean enabled;
    private final TokenStore tokens; private final AuditLogRepository audit;

    public AuthInterceptor(TokenStore tokens, AuditLogRepository audit) { this.tokens = tokens; this.audit = audit; }

    public static String bearer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        return h != null && h.startsWith("Bearer ") ? h.substring(7) : null;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) throws Exception {
        if (!enabled || "OPTIONS".equals(req.getMethod())) return true;
        String p = req.getRequestURI();
        if (p.equals("/api/auth/login")) return true;
        AppUser u = tokens.get(bearer(req));
        if (u == null) return deny(res, 401, "Login required");
        boolean write = !"GET".equals(req.getMethod());
        boolean adminArea = p.startsWith("/api/users") || (p.startsWith("/api/settings") && write);
        boolean engineerArea = p.startsWith("/api/readings") || p.startsWith("/api/alerts") || p.startsWith("/api/equipment");
        if (p.startsWith("/api/users") && u.role != Role.ADMIN) return deny(res, 403, "Admin only");
        if (adminArea && u.role != Role.ADMIN) return deny(res, 403, "Admin only");
        if (write && !p.equals("/api/auth/logout")) {
            if (u.role == Role.VIEWER) return deny(res, 403, "Read-only role");
            if (u.role == Role.ENGINEER && !engineerArea) return deny(res, 403, "Engineers cannot change this");
        }
        req.setAttribute("user", u);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        Object u = req.getAttribute("user");
        if (u instanceof AppUser && !"GET".equals(req.getMethod()) && res.getStatus() < 400) {
            AuditLog a = new AuditLog(); a.username = ((AppUser) u).username; a.action = req.getMethod(); a.path = req.getRequestURI();
            audit.save(a);
        }
    }

    private boolean deny(HttpServletResponse res, int status, String msg) throws Exception {
        res.setStatus(status); res.setContentType("application/json");
        res.getWriter().write("{\"error\":\"" + msg + "\"}");
        return false;
    }
}
