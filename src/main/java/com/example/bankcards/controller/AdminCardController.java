package com.example.bankcards.controller;

import com.example.bankcards.dto.CardDto;
import com.example.bankcards.dto.CreateCardDto;
import com.example.bankcards.service.AdminCardService;
import com.example.bankcards.util.ObjectToRsqlConverter;
import com.example.bankcards.validators.CardValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
@RestController
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/card")
@RequiredArgsConstructor
public class AdminCardController {

    private final CardValidator cardValidator;
    private final AdminCardService adminCardService;

    @PostMapping("/create")
    public ResponseEntity<?> create(
            CreateCardDto createCardDto
    ) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Administrator [{}] initiated the creation of a cardDto for user with ID: {}",
                adminUsername, createCardDto.getOwnerUserId());

        Errors errors = new BeanPropertyBindingResult(createCardDto, "createCardDto");
        cardValidator.validate(createCardDto, errors);
        if (errors.hasErrors()) return ResponseEntity.badRequest().body(errors.getAllErrors());

        CardDto cardDto = adminCardService.createCard(createCardDto);

        URI location = MvcUriComponentsBuilder
                .fromMethodName(AdminCardController.class, "findAllByRsql", ObjectToRsqlConverter.toRsql(cardDto), "", null, null)
                .build()
                .encode()
                .toUri();
        log.info("The cardDto was created successfully. Admin: {}, Card ID: {}, Number: {}. Resource path: {}",
                adminUsername,
                cardDto.getId(), cardDto.getMaskedNumber(), location);
        return ResponseEntity.created(location).body(cardDto);
    }

    @GetMapping("/findAllByRsql")
    public ResponseEntity<?> findAllByRsql(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "page", required = false, defaultValue = "0" ) Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "10") Integer size
    ) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("findAllByRsql: user: {} search:{}, sort:{}, page:{}, size:{}",
                adminUsername, search, sort, page, size);
        search = UriUtils.decode(search, StandardCharsets.UTF_8);
        return ResponseEntity.ok(adminCardService.findAllByRsql(search, sort, page, size));
    }

}
