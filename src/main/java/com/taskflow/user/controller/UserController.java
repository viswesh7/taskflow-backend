package com.taskflow.user.controller;

import com.taskflow.user.dto.MeResponse;
import com.taskflow.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) { this.userService = userService; }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return userService.getMe(UUID.fromString(authentication.getName()));
    }
}
