package com.datamint.energy.security;

import com.datamint.energy.model.AppUser;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** In-memory login sessions (tokens are lost when the server restarts - fine for this project). */
@Component
public class TokenStore {
    private final Map<String, AppUser> tokens = new ConcurrentHashMap<>();
    public String issue(AppUser u) { String t = UUID.randomUUID().toString(); tokens.put(t, u); return t; }
    public AppUser get(String t) { return t == null ? null : tokens.get(t); }
    public void remove(String t) { if (t != null) tokens.remove(t); }
}
