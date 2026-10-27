package dev.shiori.infra.config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controlador de entrega do ponto de entrada HTML da Single Page Application (SPA)
 * do Painel Administrativo em /admin e /admin/.
 */
@Controller
public class AdminSpaController {

    private final Resource indexHtml = new ClassPathResource("static/admin/index.html");

    @GetMapping("/admin")
    public String redirectToAdminSlash() {
        return "redirect:/admin/";
    }

    @GetMapping("/admin/")
    @ResponseBody
    public ResponseEntity<Resource> serveAdminIndex() {
        if (!indexHtml.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(indexHtml);
    }
}
