package dev.shiori.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.shiori.dto.ObraResponse;
import dev.shiori.dto.hub.PareamentoResponse;
import dev.shiori.dto.hub.SolicitacaoPareamentoRequest;
import dev.shiori.dto.hub.SubmissaoObraAnonimaRequest;
import dev.shiori.service.PareamentoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/hub/pareamento", "/api/v1/hub/pareamento"})
public class PareamentoController {

    private final PareamentoService pareamentoService;

    public PareamentoController(PareamentoService pareamentoService) {
        this.pareamentoService = pareamentoService;
    }

    @PostMapping("/solicitar")
    public ResponseEntity<PareamentoResponse> solicitarPareamento(@Valid @RequestBody SolicitacaoPareamentoRequest request) {
        PareamentoResponse response = pareamentoService.solicitarPareamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/status/{instanceUuid}")
    public ResponseEntity<PareamentoResponse> consultarStatus(@PathVariable String instanceUuid) {
        PareamentoResponse response = pareamentoService.consultarStatus(instanceUuid);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/submeter-obra")
    public ResponseEntity<ObraResponse> submeterObraAnonima(@Valid @RequestBody SubmissaoObraAnonimaRequest request) {
        ObraResponse response = pareamentoService.submeterObraAnonima(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
