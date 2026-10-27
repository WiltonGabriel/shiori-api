package dev.shiori.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.shiori.dto.federacao.FederacaoLoteObrasDTO;
import dev.shiori.service.FederacaoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/federacao", "/api/v1/federacao"})
public class FederacaoController {

    private final FederacaoService federacaoService;

    public FederacaoController(FederacaoService federacaoService) {
        this.federacaoService = federacaoService;
    }

    @GetMapping("/obras")
    public ResponseEntity<FederacaoLoteObrasDTO> exportarObras(
            @RequestHeader(value = "X-Federation-Secret", required = false) String headerSecret,
            @RequestParam(value = "segredo", required = false) String paramSecret) {
        String segredo = headerSecret != null ? headerSecret : paramSecret;
        FederacaoLoteObrasDTO response = federacaoService.exportarObrasParaFederacao(segredo);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sincronizar")
    public ResponseEntity<Map<String, Object>> sincronizarObras(
            @RequestHeader(value = "X-Federation-Secret", required = false) String headerSecret,
            @RequestParam(value = "segredo", required = false) String paramSecret,
            @Valid @RequestBody FederacaoLoteObrasDTO lote) {
        String segredo = headerSecret != null ? headerSecret : paramSecret;
        int processadas = federacaoService.receberLoteFederado(segredo, lote);
        return ResponseEntity.ok(Map.of(
                "mensagem", "Sincronização federada concluída com sucesso",
                "obrasProcessadas", processadas
        ));
    }
}
