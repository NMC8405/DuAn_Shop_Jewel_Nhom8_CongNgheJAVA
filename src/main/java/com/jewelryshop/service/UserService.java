package com.jewelryshop.service;

import com.jewelryshop.dto.RegisterDto;
import com.jewelryshop.entity.User;
import com.jewelryshop.enums.Role;
import com.jewelryshop.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    public User register(RegisterDto dto) {
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại!");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Email đã được sử dụng!");
        }

        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .fullName(dto.getFullName())
                .phone(dto.getPhone())
                .role(Role.USER)
                .active(true)
                .build();
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng #" + id));
    }

    public User updateProfile(Long id, String fullName, String phone, String address) {
        User user = findById(id);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setAddress(address);
        return userRepository.save(user);
    }

    public void changePassword(Long id, String oldPass, String newPass) {
        User user = findById(id);
        if (!passwordEncoder.matches(oldPass, user.getPassword())) {
            throw new RuntimeException("Mật khẩu cũ không đúng!");
        }
        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
    }

    public void toggleLock(Long id) {
        User user = findById(id);
        user.setActive(!user.isActive());
        userRepository.save(user);
    }

    public User save(User user) {
        if (user.getId() == null) {
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại!");
            }
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng!");
            }
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            return userRepository.save(user);
        } else {
            User existing = findById(user.getId());
            if (!existing.getUsername().equals(user.getUsername()) && userRepository.existsByUsername(user.getUsername())) {
                throw new RuntimeException("Tên đăng nhập đã tồn tại!");
            }
            if (!existing.getEmail().equals(user.getEmail()) && userRepository.existsByEmail(user.getEmail())) {
                throw new RuntimeException("Email đã được sử dụng!");
            }
            if (user.getPassword() != null && !user.getPassword().isBlank()) {
                existing.setPassword(passwordEncoder.encode(user.getPassword()));
            }
            existing.setUsername(user.getUsername());
            existing.setEmail(user.getEmail());
            existing.setFullName(user.getFullName());
            existing.setPhone(user.getPhone());
            existing.setAddress(user.getAddress());
            existing.setRole(user.getRole());
            existing.setActive(user.isActive());
            if (existing.getCreatedAt() == null) {
                existing.setCreatedAt(java.time.LocalDateTime.now());
            }
            return userRepository.save(existing);
        }
    }

    public void delete(Long id) {
        User user = findById(id);
        if (user.getRole() == Role.ADMIN) {
            throw new RuntimeException("Không thể xóa tài khoản Admin!");
        }
        userRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }
}
