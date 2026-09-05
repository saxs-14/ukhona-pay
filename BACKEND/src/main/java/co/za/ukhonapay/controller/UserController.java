package co.za.ukhonapay.controller;

import co.za.ukhonapay.dto.UserResponse;
import co.za.ukhonapay.dto.UserSelfUpdateRequest;
import co.za.ukhonapay.security.CurrentUser;
import co.za.ukhonapay.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(userService.getById(CurrentUser.id()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(@Valid @RequestBody UserSelfUpdateRequest req) {
        return ResponseEntity.ok(userService.updateSelf(CurrentUser.id(), req));
    }
}
