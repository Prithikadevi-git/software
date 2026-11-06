package com.datamint.energy.controller;

import com.datamint.energy.model.*;
import com.datamint.energy.repository.*;
import com.datamint.energy.security.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** Module 11 - login / logout / users / audit log. */
@RestController @RequestMapping("/api")
public class AuthController {
    private final AppUserRepository users; private final TokenStore tokens; private final AuditLogRepository audit;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(AppUserRepository users, TokenStore tokens, AuditLogRepository audit) { this.users = users; this.tokens = tokens; this.audit = audit; }

    @PostMapping("/auth/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        AppUser u = users.findByUsername(body.getOrDefault("username", "")).orElse(null);
        if (u == null || !u.active || !encoder.matches(body.getOrDefault("password", ""), u.passwordHash))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        u.lastLogin = LocalDateTime.now(); users.save(u);
        AuditLog a = new AuditLog(); a.username = u.username; a.action = "LOGIN"; a.path = "/api/auth/login"; audit.save(a);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("token", tokens.issue(u)); m.put("username", u.username); m.put("fullName", u.fullName); m.put("role", u.role);
        return m;
    }

    @PostMapping("/auth/logout") public void logout(HttpServletRequest req) { tokens.remove(AuthInterceptor.bearer(req)); }

    @GetMapping("/auth/me")
    public AppUser me(HttpServletRequest req) {
        AppUser u = tokens.get(AuthInterceptor.bearer(req));
        if (u == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        return u;
    }

    @GetMapping("/users") public List<AppUser> list() { return users.findAll(); }

    @PostMapping("/users") public AppUser create(@RequestBody AppUser u) {
        u.id = null; u.passwordHash = encoder.encode(u.password == null ? "changeme123" : u.password); return users.save(u);
    }

    @PutMapping("/users/{id}") public AppUser update(@PathVariable Long id, @RequestBody AppUser in) {
        AppUser u = users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));
        if (in.username != null) u.username = in.username;
        if (in.fullName != null) u.fullName = in.fullName;
        if (in.role != null) u.role = in.role;
        u.active = in.active; u.allowedBuildings = in.allowedBuildings;
        if (in.password != null && !in.password.isBlank()) u.passwordHash = encoder.encode(in.password);
        return users.save(u);
    }

    @DeleteMapping("/users/{id}") public void deactivate(@PathVariable Long id) {
        users.findById(id).ifPresent(u -> { u.active = false; users.save(u); });
    }

    @GetMapping("/audit") public List<AuditLog> audit() {
        List<AuditLog> all = audit.findAll(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        return all.size() > 200 ? all.subList(0, 200) : all;
    }
}
