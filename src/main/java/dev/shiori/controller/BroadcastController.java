package dev.shiori.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.shiori.dto.admin.BroadcastResponse;
import dev.shiori.service.AdminService;

@RestController
@RequestMapping({"/api/broadcasts", "/api/v1/broadcasts"})
public class BroadcastController {

    private final AdminService adminService;

    public BroadcastController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/ativos")
    public ResponseEntity<List<BroadcastResponse>> listarBroadcastsAtivos() {
        return ResponseEntity.ok(adminService.listarBroadcastsAtivos());
    }
}
