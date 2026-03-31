package com.moviebooking.service;

import com.moviebooking.dao.UserDao;
import com.moviebooking.dto.LoginRequest;
import com.moviebooking.dto.RegisterRequest;
import com.moviebooking.model.User;
import com.moviebooking.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserDao userDao, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> register(RegisterRequest request) {
        // Check for duplicate email BEFORE inserting.
        // WHY not rely on the DB UNIQUE constraint alone?
        // The DB constraint would also reject duplicates, but it throws a raw
        // DataIntegrityViolationException which gives a poor error message.
        // This check lets us return a clean, user-friendly error message.
        if (userDao.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        // WHY hash the password before saving?
        // Never store plain-text passwords. If the DB is compromised, BCrypt hashes
        // cannot be reversed to recover the original password. BCrypt also adds a
        // unique salt per password, so identical passwords produce different hashes.
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Long userId = userDao.save(user);

        // Return the JWT immediately after registration so the user doesn't
        // need to make a separate login request — better UX.
        Map<String, Object> response = new HashMap<>();
        response.put("userId", userId);
        response.put("name", request.getName());
        response.put("email", request.getEmail());
        response.put("token", jwtUtil.generateToken(userId, request.getEmail()));
        return response;
    }

    public Map<String, Object> login(LoginRequest request) {
        // Fetch user by email first, then verify password separately.
        User user = userDao.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        // WHY passwordEncoder.matches() instead of direct string comparison?
        // The stored password is a BCrypt hash. BCrypt is not reversible —
        // we can't decrypt the hash. Instead, matches() hashes the incoming
        // plain-text password with the same salt and compares the results.
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            // WHY same error message for both "user not found" and "wrong password"?
            // Giving different messages (e.g., "user not found" vs "wrong password")
            // would let attackers enumerate which emails are registered. A single
            // generic message leaks no information about what failed.
            throw new IllegalArgumentException("Invalid email or password");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("userId", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("token", jwtUtil.generateToken(user.getId(), user.getEmail()));
        return response;
    }
}
