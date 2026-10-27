package dev.shiori.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.shiori.dto.feed.AtividadeFeedResponse;
import dev.shiori.infra.security.CurrentUser;
import dev.shiori.infra.security.UsuarioPrincipal;
import dev.shiori.service.FeedService;

@RestController
@RequestMapping({"/api/feed", "/api/v1/feed"})
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public ResponseEntity<Page<AtividadeFeedResponse>> obterTimeline(
            @CurrentUser UsuarioPrincipal usuarioLogado,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AtividadeFeedResponse> feed = feedService.obterFeed(usuarioLogado.getId(), pageable);
        return ResponseEntity.ok(feed);
    }
}
