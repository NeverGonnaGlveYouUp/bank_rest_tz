package com.example.bankcards.controller;

import com.example.bankcards.service.UserCardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/protected/user")
@RequiredArgsConstructor
public class UserCardController {

    private final UserCardService userCardService;

    @GetMapping("/findAllByRsql")
    public ResponseEntity<?> findAllByRsql(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size
    ) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("findAllByRsql: user: {} search:{}, sort:{}, page:{}, size:{}",
                adminUsername, search, sort, page, size);
        search = UriUtils.decode(search, StandardCharsets.UTF_8);
        return ResponseEntity.ok(userCardService.findAllByRsql(search, sort, page, size));
    }

}