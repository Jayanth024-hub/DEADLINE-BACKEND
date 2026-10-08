package com.deadlineiq.service;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.repository.UserRepository;
import com.deadlineiq.util.PasswordUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing user authentication, registration, profile updates, and persistence in MySQL.
 * Uses BCryptPasswordEncoder as the primary hashing and verification mechanism.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, User> activeTokens = new ConcurrentHashMap<>();

    private static final String TOKEN_SECRET = "DeadlineIQ_AuthSecretKey_CSE_2026_Secure_Token";
    private final Set<String> invalidatedTokens = ConcurrentHashMap.newKeySet();

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<User> authenticate(String email, String plainPassword) {
        if (email == null || plainPassword == null) return Optional.empty();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email.trim());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String storedHash = user.getPassword();
            if (storedHash == null) return Optional.empty();

            // 1. Primary authentication: BCrypt verification
            if (isBcryptHash(storedHash)) {
                if (passwordEncoder.matches(plainPassword, storedHash)) {
                    return Optional.of(user);
                }
                return Optional.empty();
            }

            // 2. Progressive migration: verify legacy salted SHA-256 hash
            if (PasswordUtils.isLegacySha256Match(plainPassword, storedHash)) {
                // Safely upgrade verified legacy password to BCrypt in MySQL
                String newBcryptHash = passwordEncoder.encode(plainPassword);
                user.setPassword(newBcryptHash);
                userRepository.save(user);
                return Optional.of(user);
            }
        }
        return Optional.empty();
    }

    private boolean isBcryptHash(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }

    public String generateApiToken(User user) {
        long timestamp = System.currentTimeMillis();
        String payload = user.getId() + ":" + user.getEmail() + ":" + timestamp;
        String signature = hmacSha256(payload, TOKEN_SECRET);
        String token = "Bearer-" + Base64.getUrlEncoder().withoutPadding().encodeToString((payload + ":" + signature).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        
        // Bound cache size to prevent memory bloat
        if (activeTokens.size() > 500) {
            activeTokens.clear();
        }
        activeTokens.put(token, user);
        return token;
    }

    public Optional<User> getUserByToken(String token) {
        if (token == null || !token.startsWith("Bearer-") || invalidatedTokens.contains(token)) {
            return Optional.empty();
        }

        // 1. Fast in-memory cache check
        User cached = activeTokens.get(token);
        if (cached != null) {
            return Optional.of(cached);
        }

        // 2. Cryptographic signature check (survives Spring Boot restarts)
        try {
            String encoded = token.substring("Bearer-".length()).trim();
            byte[] decoded = Base64.getUrlDecoder().decode(encoded);
            String raw = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
            String[] parts = raw.split(":");
            if (parts.length == 4) {
                Long userId = Long.parseLong(parts[0]);
                String email = parts[1];
                long timestamp = Long.parseLong(parts[2]);
                String signature = parts[3];

                String payload = userId + ":" + email + ":" + timestamp;
                String expectedSig = hmacSha256(payload, TOKEN_SECRET);
                if (expectedSig.equals(signature)) {
                    // Valid cryptographic token: fetch fresh user from MySQL
                    Optional<User> userOpt = userRepository.findById(userId);
                    if (userOpt.isPresent() && userOpt.get().getEmail().equalsIgnoreCase(email)) {
                        if (activeTokens.size() > 500) {
                            activeTokens.clear();
                        }
                        activeTokens.put(token, userOpt.get());
                        return userOpt;
                    }
                }
            }
        } catch (Exception ignored) {}

        return Optional.empty();
    }

    public void invalidateToken(String token) {
        if (token != null) {
            activeTokens.remove(token);
            if (invalidatedTokens.size() > 1000) {
                invalidatedTokens.clear();
            }
            invalidatedTokens.add(token);
        }
    }

    private static String hmacSha256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hmac) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) sb.append('0');
                sb.append(hex);
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("HMAC computation failed", e);
        }
    }

    public boolean register(User user) {
        if (user.getEmail() == null || userRepository.existsByEmailIgnoreCase(user.getEmail().trim())) {
            return false;
        }
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return true;
    }

    public boolean updateUserProfile(Long id, String name, String department, String year, String section, String newPassword) {
        Optional<User> opt = userRepository.findById(id);
        if (opt.isPresent()) {
            User user = opt.get();
            if (name != null && !name.trim().isEmpty()) user.setName(name.trim());
            if (department != null && !department.trim().isEmpty()) user.setDepartment(department.trim());
            if (year != null && !year.trim().isEmpty()) user.setYear(year.trim());
            if (section != null && !section.trim().isEmpty()) user.setSection(section.trim());
            if (newPassword != null && !newPassword.trim().isEmpty()) {
                user.setPassword(passwordEncoder.encode(newPassword.trim()));
            }
            userRepository.save(user);
            return true;
        }
        return false;
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        return userRepository.findByEmailIgnoreCase(email.trim());
    }

    public Optional<User> findById(Long id) {
        if (id == null) return Optional.empty();
        return userRepository.findById(id);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public long countByRole(Role role) {
        return userRepository.countByRole(role);
    }

    public boolean deleteUser(Long id) {
        if (id != null && userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
