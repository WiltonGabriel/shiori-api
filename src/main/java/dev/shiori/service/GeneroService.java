package dev.shiori.service;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.shiori.domain.model.Genero;
import dev.shiori.dto.admin.GeneroResponse;
import dev.shiori.repository.GeneroRepository;

/**
 * Serviço responsável pela consulta e gestão do catálogo oficial de gêneros literários (SRP - Uncle Bob).
 */
@Service
@Transactional(readOnly = true)
public class GeneroService {

    private final GeneroRepository generoRepository;

    public GeneroService(GeneroRepository generoRepository) {
        this.generoRepository = generoRepository;
    }

    public List<GeneroResponse> listarTodos() {
        return generoRepository.findAll(Sort.by(Sort.Direction.ASC, "nome")).stream()
                .map(GeneroResponse::deEntidade)
                .toList();
    }
}
