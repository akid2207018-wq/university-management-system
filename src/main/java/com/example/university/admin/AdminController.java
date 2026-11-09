package com.example.university.admin;

import com.example.university.user.Role;
import com.example.university.user.UserAccountService;
import com.example.university.user.UserResponse;
import com.example.university.user.UserWriteRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/admins")
public class AdminController {

    private static final Role ROLE = Role.ADMIN;

    private final UserAccountService userAccountService;

    public AdminController(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userAccountService.list(ROLE));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return ResponseEntity.ok(userAccountService.get(ROLE, id));
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserWriteRequest request) {
        UserResponse created = userAccountService.create(ROLE, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserWriteRequest request
    ) {
        return ResponseEntity.ok(userAccountService.update(ROLE, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userAccountService.delete(ROLE, id);
        return ResponseEntity.noContent().build();
    }
}
