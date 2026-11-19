package com.taskflow.user.service;

import com.taskflow.common.exception.ApiException;
import com.taskflow.user.dto.MeResponse;
import com.taskflow.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class UserService {
    private final UserRepository users;
    public UserService(UserRepository users) { this.users = users; }

    public MeResponse getMe(UUID userId) {
        var user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found."));
        return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
