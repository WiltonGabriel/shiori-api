package dev.shiori.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import dev.shiori.domain.model.Genero;
import dev.shiori.dto.admin.GeneroResponse;
import dev.shiori.repository.GeneroRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários: GeneroService")
class GeneroServiceTest {

    @Mock
    private GeneroRepository generoRepository;

    @InjectMocks
    private GeneroService generoService;

    @Test
    @DisplayName("Deve listar todos os gêneros ordenados por nome")
    void deveListarTodosGenerosOrdenados() {
        Genero g1 = new Genero("Ação", "Obras de ação eletrizante");
        Genero g2 = new Genero("Fantasia", "Mundos mágicos e épicos");

        when(generoRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"))).thenReturn(List.of(g1, g2));

        List<GeneroResponse> resultado = generoService.listarTodos();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).nome()).isEqualTo("Ação");
        assertThat(resultado.get(0).slug()).isEqualTo("acao");
        assertThat(resultado.get(1).nome()).isEqualTo("Fantasia");
        assertThat(resultado.get(1).slug()).isEqualTo("fantasia");
    }
}
