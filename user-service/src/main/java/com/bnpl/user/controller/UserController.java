package com.bnpl.user.controller;

import com.bnpl.user.dto.CreditAccountResponse;
import com.bnpl.user.dto.CreditDeductRequest;
import com.bnpl.user.dto.UserOnboardRequest;
import com.bnpl.user.entity.User;
import com.bnpl.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/onboard")
    public ResponseEntity<User> onboardUser(@Valid @RequestBody UserOnboardRequest request) {
        User user = userService.onboardUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/{userId}/credit")
    public ResponseEntity<CreditAccountResponse> getCreditAccount(@PathVariable UUID userId) {
        return ResponseEntity.ok(userService.getCreditAccountByUserId(userId));
    }

    @PostMapping("/{userId}/credit/deduct")
    public ResponseEntity<CreditAccountResponse> deductCredit(
            @PathVariable UUID userId,
            @Valid @RequestBody CreditDeductRequest request) {
        return ResponseEntity.ok(userService.deductCredit(userId, request.getAmount()));
    }

    @PostMapping("/{userId}/credit/restore")
    public ResponseEntity<CreditAccountResponse> restoreCredit(
            @PathVariable UUID userId,
            @Valid @RequestBody CreditDeductRequest request) {
        return ResponseEntity.ok(userService.restoreCredit(userId, request.getAmount()));
    }

    @PostMapping("/{userId}/credit/upgrade")
    public ResponseEntity<CreditAccountResponse> upgradeCredit(
            @PathVariable UUID userId,
            @Valid @RequestBody com.bnpl.user.dto.CreditUpgradeRequest request) {
        return ResponseEntity.ok(userService.upgradeCreditLimit(userId, request.getNewCreditLimit(), request.getReason()));
    }
}
