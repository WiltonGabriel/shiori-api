package dev.shiori.infra.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Configuração de entrega dos ativos estáticos e rotas cliente da SPA do Painel Administrativo.
 * Mapeia rotas de /admin/** para os recursos estáticos embutidos em classpath:/static/admin/.
 */
@Configuration
public class AdminSpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/admin/**")
                .addResourceLocations("classpath:/static/admin/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requestedResource = location.createRelative(resourcePath);
                        if (requestedResource.exists() && requestedResource.isReadable() && resourcePath.contains(".")) {
                            return requestedResource;
                        }
                        return location.createRelative("index.html");
                    }
                });
    }
}
