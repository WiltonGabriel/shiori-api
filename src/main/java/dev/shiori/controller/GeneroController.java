package dev.shiori.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.shiori.dto.admin.GeneroResponse;
import dev.shiori.service.GeneroService;

/**
 * Controlador REST público para disponibilização da taxonomia oficial de gêneros literários (SRP - Uncle Bob).
 */
@RestController
@RequestMapping({"/generos", "/api/generos"})
public class GeneroController {

    private final GeneroService generoService;

    public GeneroController(GeneroService generoService) {
        this.generoService = generoService;
    }

    @GetMapping
    public ResponseEntity<List<GeneroResponse>> listarTodos() {
        List<GeneroResponse> generos = generoService.listarTodos();
        return ResponseEntity.ok(generos);
    }
}
