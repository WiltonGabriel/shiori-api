# Guia Prático e Simplificado do Projeto Shiori

**Objetivo deste Guia:** Explicar de forma simples, direta e sem termos difíceis tudo o que você precisa para programar o projeto Shiori por conta própria, mostrando exatamente quais pastas criar e o que colocar dentro de cada arquivo.

---

## Parte 1 — O Que é Cada Coisa no Java (Sem Complicação)

### 1.1. Os Tipos de Arquivos que Você Vai Criar

| Nome no Java | O que é na prática? | Como saber quando usar? | Onde aparece no Shiori? |
| :--- | :--- | :--- | :--- |
| **Pacote (`package`)** | É simplesmente uma pasta para organizar seus arquivos. | No computador você vê como pasta (`domain/enums`), mas na 1ª linha do arquivo o Java escreve com pontos: `package dev.shiori.domain.enums;`. | Todas as pastas dentro de `src/main/java/dev/shiori/`. |
| **Classe (`class`)** | É a ficha completa de algo do sistema: guarda informações e tem ações próprias. | Você usa quando precisa criar objetos de verdade na memória usando a palavra `new`. | `Obra` e `HistoricoLeitura`. |
| **Classe Abstrata (`abstract class`)** | É uma ficha base usada apenas para emprestar campos para outras classes (usando `extends`). | Você nunca usa `new` nela; ela só serve para você não ter que repetir o campo `id` em várias classes diferentes. | `EntidadeBase` (empresta o `id` para `Obra` e `HistoricoLeitura`). |
| **Enum (`enum`)** | É uma lista fechada de opções (como um menu de múltipla escolha). | Usa quando um campo só pode aceitar algumas palavras fixas e você quer proibir erros de digitação. | `StatusObra` e `StatusLeitura`. |
| **Record (`record`)** | É um envelope de entrega: serve apenas para levar dados de um lugar para outro (sem regras complicadas). | Você coloca os campos direto no topo ao lado do nome, e o Java já cria tudo sozinho por trás dos panos. | Os arquivos da pasta `dto` (que levam e trazem o JSON). |
| **Interface (`interface`)** | É uma lista de tarefas sem instruções: só tem o nome dos métodos, sem código dentro. | No nosso projeto, você cria a interface vazia e o Spring Boot escreve os comandos do banco de dados sozinho para você. | `ObraRepository`. |

---

### 1.2. O Que Vai Dentro de uma Classe

Imagine que uma Classe é uma ficha de controle de leitura:
* **Atributo:** Informação guardada permanentemente na ficha (ex.: `tituloPrincipal`, `progressoAtual`). Fica no topo da classe e sempre começa com `private`.
* **Construtor:** A "recepção" da classe. Roda ao chamar `new` para criar uma obra ou histórico novo, exigindo os dados iniciais obrigatórios. Tem o mesmo nome da classe.
* **Método:** Ação ou conta que a classe sabe fazer (ex.: `calcularMediaAvCiclo`).
  * Se tiver `void` antes do nome: faz o trabalho em silêncio sem devolver nada.
  * Se tiver um tipo (ex.: `Double`): calcula e devolve o resultado com `return`.
* **Parâmetro:** O que vai entre os parênteses `( )` de um método ou construtor. São os dados passados para ele trabalhar.
* **Variável Local:** Rascunho temporário dentro de um método para auxiliar num cálculo. Quando o método termina, ela desaparece.
* **Getter (`get...`):** Método simples (ex.: `getTituloPrincipal()`) que permite que outras classes leiam o valor sem alterá-lo diretamente.

---

### 1.3. Os Tipos de Dados que Você Vai Usar

* **`Long`**: Número inteiro grande para o ID do banco. Começa com maiúscula para permitir ficar vazio (`null`) antes de ser salvo.
* **`Double`**: Número decimal para capítulos fracionários (ex.: `22.5`) e notas (ex.: `9.0`). Permite `null` para campos opcionais.
* **`String`**: Qualquer texto entre aspas duplas (título, sinopse, anotações).
* **`LocalDate`**: Data de calendário (Ano, Mês e Dia). Pega a data de hoje com `LocalDate.now()`.
* **`List<HistoricoLeitura>` e `ArrayList<>()`**: `List` define que a obra possui uma coleção ordenada; `new ArrayList<>()` instancia essa lista vazia na memória.
* **`Optional<Obra>`**: Caixa de proteção retornada pelo banco ao buscar por ID. Pode conter a obra ou estar vazia (`Optional.empty()`).

---

## Parte 2 — Como Funcionam as Regras do Shiori

1. **As 3 Notas da Obra:**
   * `notaGeral`: Nota pública externa da obra (catálogos ou sites).
   * `avaliacaoPessoal`: Nota direta de 0.0 a 10.0 que o usuário dá de cabeça para a obra quando quiser.
   * `mediaAvaliacaoCiclos` (A Nota Surpresa do "Olhinho"): Calculada automaticamente somando as notas ponderadas dos ciclos. Fica oculta sob o botão do olho para ser revelada depois!
2. **Dar Nota nos Capítulos é 100% Opcional:** O usuário nunca é obrigado a dar nota ao atualizar onde parou.
3. **Ciclo de Leitura Flexível e Ciclo Elástico:**
   * Se leu vários capítulos de uma vez (ex.: 4 ao 22): registra extensão de 18 capítulos.
   * Se avaliou de 1 em 1 capítulo com nota ou anotação (ex.: cap 1, 2 e 3): extensão de 1.0, tratado como capítulo único.
   * Se avançou de pouco em pouco sem nota e sem anotação (ex.: 4 -> 5, depois 5 -> 6): para não poluir o histórico, a classe `Obra` apenas estica o `capituloFinal` do último ciclo em aberto!
4. **Média Justa (Ponderada):**
   * Multiplica a extensão de cada ciclo com nota pela respectiva nota e divide pela soma total das extensões desses ciclos avaliados.

---

## Parte 3 — Estrutura Completa por Pacote (O Que Criar em Cada Arquivo)

### PACOTE 1: `dev.shiori.domain.enums`

#### Arquivo 1.1: `StatusObra.java`
* **Tipo:** `public enum StatusObra`
* **Constantes:** `PREVISTO`, `ANDAMENTO`, `FINALIZADO`, `HIATO`, `CANCELADO`.

#### Arquivo 1.2: `StatusLeitura.java`
* **Tipo:** `public enum StatusLeitura`
* **Constantes:** `PLANEJADO`, `LENDO`, `PAUSADO`, `TERMINOU`, `DROPOU`.

---

### PACOTE 2: `dev.shiori.domain.exception`

#### Arquivo 2.1: `RecursoNaoEncontradoException.java`
* **Tipo:** `public class RecursoNaoEncontradoException extends RuntimeException`
* **Construtor:** Recebe `(String mensagem)` e chama `super(mensagem);`.

#### Arquivo 2.2: `RegraDeNegocioException.java`
* **Tipo:** `public class RegraDeNegocioException extends RuntimeException`
* **Construtor:** Recebe `(String mensagem)` e chama `super(mensagem);`.

---

### PACOTE 3: `dev.shiori.domain.model`

#### Arquivo 3.1: `EntidadeBase.java`
* **Tipo:** `public abstract class EntidadeBase`
* **Anotação de classe:** `@MappedSuperclass`
* **Atributo:** `private Long id;` anotado com `@Id` e `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
* **Métodos:** `getId()`, `equals` e `hashCode` baseados no `id`.

#### Arquivo 3.2: `HistoricoLeitura.java`
* **Tipo:** `public class HistoricoLeitura extends EntidadeBase`
* **Anotações:** `@Entity`, `@Table(name = "tb_historico_leitura")`
* **Atributos:**
  1. `private Double capituloInicial;` (`@Column(nullable = false)`)
  2. `private Double capituloFinal;` (`@Column(nullable = false)`)
  3. `private Double avaliacaoCiclo;` (opcional)
  4. `private String anotacoes;` (`@Column(columnDefinition = "TEXT")`)
  5. `private LocalDate dataLeitura;` (`@Column(nullable = false)`)
  6. `private Obra obra;` (`@ManyToOne`, `@JoinColumn(name = "obra_id", nullable = false)`)
* **Construtores:**
  * `protected HistoricoLeitura()`: vazio para o JPA.
  * `public HistoricoLeitura(Obra obra, Double capituloInicial, Double capituloFinal, Double avaliacaoCiclo, String anotacoes)`:
    * Valida se `obra`, `capituloInicial` e `capituloFinal` não são nulos.
    * Valida se `capituloInicial >= 0.0` e `capituloFinal > capituloInicial`.
    * Se `avaliacaoCiclo != null`, valida se está entre 0.0 e 10.0.
    * Atribui aos campos e define `this.dataLeitura = LocalDate.now();`.
* **Métodos de Domínio:**
  * `calcularTamanhoCiclo()`: retorna `capituloFinal - capituloInicial`.
  * `isCapituloUnico()`: retorna se tamanho é `1.0`.
  * `isCicloEmAberto()`: retorna `true` se `avaliacaoCiclo == null` e `anotacoes == null || anotacoes.isBlank()`.
  * `estenderCicloAte(Double novoCapituloFinal)`: valida se é maior, atualiza `capituloFinal` e data com `LocalDate.now()`.
  * Getters para todos os atributos.

#### Arquivo 3.3: `Obra.java`
* **Tipo:** `public class Obra extends EntidadeBase`
* **Anotações:** `@Entity`, `@Table(name = "tb_obra")`
* **Atributos:**
  1. `private String tituloPrincipal;` (`@Column(nullable = false)`)
  2. `private String tituloOriginal;`
  3. `private String capaUrl;`
  4. `private String sinopse;` (`@Column(columnDefinition = "TEXT")`)
  5. `private Double notaGeral;`
  6. `private StatusObra statusObra;` (`@Enumerated(EnumType.STRING)`)
  7. `private StatusLeitura statusLeitura;` (`@Enumerated(EnumType.STRING)`)
  8. `private Double progressoAtual;`
  9. `private Double avaliacaoPessoal;`
  10. `private Double mediaAvaliacaoCiclos;`
  11. `private List<HistoricoLeitura> historicos = new ArrayList<>();` (`@OneToMany(mappedBy = "obra", cascade = CascadeType.ALL, orphanRemoval = true)`)
* **Construtores:**
  * `protected Obra()`: vazio para JPA.
  * `public Obra(...)`: valida `tituloPrincipal` não vazio, zera `progressoAtual = 0.0`, define `statusLeitura = StatusLeitura.LENDO` se vier nulo, valida `avaliacaoPessoal` entre 0.0 e 10.0 se informada, inicializa lista de históricos.
* **Métodos de Regra de Negócio:**
  * `registrarCicloLeitura(Double capituloAtingido, Double nota, String anotacoes)`:
    * Valida se `capituloAtingido > progressoAtual` e se `nota` (quando presente) está entre 0.0 e 10.0.
    * Valida se para registrar anotações é obrigatório atribuir uma nota (`anotacoes != null && !anotacoes.isBlank() && nota == null`).
    * Se sem nota e último ciclo estiver aberto: estende o último ciclo.
    * Senão: cria novo `HistoricoLeitura` e adiciona à lista.
    * Atualiza `progressoAtual` e recalcula `mediaAvaliacaoCiclos`.
  * `calcularMediaAvaliacaoCiclos()`: calcula média ponderada das notas dos ciclos pelo tamanho ou retorna `null`.
  * `atualizarDadosLeitura(StatusLeitura novoStatus, Double novaAvaliacaoPessoal)`: atualiza se não nulos, validando nota entre 0 e 10.
  * Getters (sendo `getHistoricos()` com `Collections.unmodifiableList`).

---

### PACOTE 4: `dev.shiori.dto`

1. `ObraCadastroRequest`: dados de entrada para criação da obra.
2. `NovoCicloRequest`: dados para registro de um novo ciclo (`capituloAtingido`, `nota`, `anotacoes`).
3. `AtualizarLeituraRequest`: atualização de status e avaliação pessoal.
4. `HistoricoCicloResponse`: dados do ciclo para envio via API (com construtor que mapeia a partir de `HistoricoLeitura`).
5. `ObraResponse`: dados completos da obra e sua lista de ciclos convertidos.

---

### PACOTE 5: `dev.shiori.repository`

* `ObraRepository`: estende `JpaRepository<Obra, Long>` e adiciona `findByStatusLeitura(StatusLeitura statusLeitura);`.

---

### PACOTE 6: `dev.shiori.service`

* `ObraService`: classe com `@Service` e injeção de `ObraRepository`.
  * `cadastrarObra(ObraCadastroRequest dados)`: instancia obra, registra ciclo inicial se houver, salva e retorna DTO.
  * `registrarCiclo(Long obraId, NovoCicloRequest dados)`: busca obra (lança 404 se não achar), registra ciclo, salva e retorna DTO.
  * `listarTodas(StatusLeitura statusFiltro)`: busca filtrada ou todas.
  * `buscarPorId(Long obraId)`: busca por ID ou lança exceção.
  * `atualizarLeitura(Long obraId, AtualizarLeituraRequest dados)`: atualiza dados e salva.
  * `excluirObra(Long obraId)`: verifica existência e exclui.

---

### PACOTE 7: `dev.shiori.controller`

* `ObraController`: `@RestController` mapeado em `/obras`.
  * `POST /obras`: Cadastra obra (`201 Created`).
  * `POST /obras/{id}/ciclos`: Registra ciclo de leitura (`200 OK`).
  * `GET /obras`: Lista obras (com filtro opcional).
  * `GET /obras/{id}`: Busca obra por ID.
  * `PATCH /obras/{id}/leitura`: Atualiza status ou avaliação pessoal.
  * `DELETE /obras/{id}`: Remove obra (`204 No Content`).

---

### PACOTE 8: `dev.shiori.infra`

* `TratadorDeErrosGlobal`: `@RestControllerAdvice` capturando:
  * `RecursoNaoEncontradoException` ➔ status `404 Not Found`.
  * `RegraDeNegocioException` ➔ status `400 Bad Request`.
