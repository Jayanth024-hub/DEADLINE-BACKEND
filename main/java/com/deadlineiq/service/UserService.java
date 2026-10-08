package com.deadlineiq.service;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.repository.UserRepository;
import com.deadlineiq.util.PasswordUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing user authentication, registration, profile updates, and persistence in MySQL.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final Map<String, User> activeTokens = new ConcurrentHashMap<>();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> authenticate(String email, String plainPassword) {
        if (email == null || plainPassword == null) return Optional.empty();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(email.trim());
        if (userOpt.isPresent() && PasswordUtils.verifyPassword(plainPassword, userOpt.get().getPassword())) {
            return userOpt;
        }
        return Optional.empty();
    }

    public String generateApiToken(User user) {
        String token = "Bearer-" + UUID.randomUUID().toString();
        activeTokens.put(token, user);
        return token;
    }

    public Optional<User> getUserByToken(String token) {
        if (token == null) return Optional.empty();
        return Optional.ofNullable(activeTokens.get(token));
    }

    public void invalidateToken(String token) {
        if (token != null) activeTokens.remove(token);
    }

    public boolean register(User user) {
        if (user.getEmail() == null || userRepository.existsByEmailIgnoreCase(user.getEmail().trim())) {
            return false;
        }
        user.setEmail(user.getEmail().trim().toLowerCase());
        user.setPassword(PasswordUtils.hashPassword(user.getPassword()));
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
                user.setPassword(PasswordUtils.hashPassword(newPassword.trim()));
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
