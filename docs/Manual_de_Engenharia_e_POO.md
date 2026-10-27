# Manual de Engenharia e Orientação a Objetos: Projeto Shiori

**Escopo da Documentação:** Guia de referência teórica em Programação Orientada a Objetos (POO), arquitetura em camadas com Java e Spring Boot, modelagem de domínio e especificação técnica de todos os pacotes, classes, atributos e métodos do sistema Shiori.

---

## Capítulo 1 — Fundamentos de Java e Orientação a Objetos (POO)

A Programação Orientada a Objetos estrutura um sistema de software através de componentes que combinam estado (dados armazenados em memória) e comportamento (regras de negócio e operações).

### 1.1. Estruturas de Organização e Tipos de Arquivos

| Conceito | O que é (Definição Técnica) | Como Diferenciar na Prática | Aplicação no Projeto Shiori |
| :--- | :--- | :--- | :--- |
| **Pacote (`package`)** | Diretório lógico que agrupa arquivos `.java` com a mesma responsabilidade arquitetural. | No sistema de arquivos é uma pasta comum (ex.: `domain/model`), mas na primeira linha do código é escrito separado por pontos: `package dev.shiori.domain.model;`. | `dev.shiori.domain.model`, `dev.shiori.service`, `dev.shiori.controller`. |
| **Classe Concreta (`class`)** | Molde ou planta baixa completa de onde nascem os objetos reais na memória, contendo atributos e métodos. | Diferente da classe abstrata e da interface, permite usar a palavra reservada `new` para instanciar um objeto vivo na memória. | `Obra` e `HistoricoLeitura`. |
| **Classe Abstrata (`abstract class`)** | Molde base incompleto criado exclusivamente para que outras classes herdem seus atributos e métodos através da palavra `extends`. | O compilador proíbe o uso de `new` diretamente nela; sua função é evitar repetição de código entre classes parecidas. | `EntidadeBase` (fornece o atributo `id : Long` para `Obra` e `HistoricoLeitura`). |
| **Interface (`interface`)** | Contrato formal vazio que lista apenas as assinaturas dos métodos (o que deve ser feito), sem conter o código de implementação (como fazer). | Não armazena atributos de estado (como título ou nota) nem possui construtores. Outras classes ou o próprio Spring implementam esse contrato. | `ObraRepository` (declara a interface e o Spring Data JPA gera o código SQL automaticamente). |
| **Enumeração (`enum`)** | Tipo especial de arquivo que define uma lista fechada, fixa e imutável de valores constantes. | Enquanto uma `String` aceita qualquer texto livre (permitindo erros de digitação), o `enum` só aceita os valores pré-definidos dentro dele. | `StatusObra` (`PREVISTO`, `ANDAMENTO`, `FINALIZADO`, `HIATO`, `CANCELADO`) e `StatusLeitura` (`PLANEJADO`, `LENDO`, `PAUSADO`, `TERMINOU`, `DROPOU`). |
| **Registro (`record`)** | Estrutura moderna e imutável do Java projetada exclusivamente para transportar dados de uma camada para outra (DTO — *Data Transfer Object*). | Todos os campos são declarados no próprio cabeçalho ao lado do nome do arquivo, e o Java gera construtor e métodos de leitura automaticamente, sem conter regras de negócio mutáveis. | `ObraCadastroRequest`, `NovoCicloRequest`, `AtualizarLeituraRequest`, `HistoricoCicloResponse` e `ObraResponse`. |

---

### 1.2. Anatomia Interna de uma Classe: Dados vs. Ações

| Conceito | O que é | Como Diferenciar no Código | Exemplo no Shiori |
| :--- | :--- | :--- | :--- |
| **Atributo (ou Campo)** | Variável declarada no topo da classe (fora de qualquer método) que representa a memória permanente do objeto. | Fica diretamente dentro das chaves da `class` e utiliza o modificador `private` (sinal `-` no diagrama). | `private Double progressoAtual;` dentro da classe `Obra`. |
| **Parâmetro (ou Argumento)** | Variável declarada entre os parênteses `( )` de um método ou construtor para receber informações vindas de fora. | Mora exclusivamente dentro dos parênteses `( )` na assinatura do método e só existe enquanto o método está em execução. | Em `registrarCicloLeitura(Double capituloAtingido, Double nota, String anotacoes)`, os 3 valores são parâmetros. |
| **Variável Local** | Variável temporária criada dentro das chaves `{ }` de um método apenas para auxiliar em um cálculo interno. | Nasce e morre dentro do método, não possuindo palavras de visibilidade (`private` ou `public`) antes do tipo. | Uma variável `double somaPesos = 0.0;` criada dentro de `calcularMediaAvCiclo()`. |
| **Construtor** | Bloco especial executado no exato instante em que `new` é chamado para criar o objeto, garantindo a inicialização de dados obrigatórios e listas. | Possui obrigatoriamente o mesmo nome da classe (iniciando com letra maiúscula) e não possui tipo de retorno (nem mesmo `void`). | `public Obra(...)` executado ao criar uma nova obra. |
| **Método `void` vs. Método com Retorno** | Funções internas de uma classe que representam as ações que o objeto sabe executar. | Se possuir `void` antes do nome, executa uma ação interna sem devolver valores; se possuir um tipo (ex.: `Double`), exige a palavra `return` ao final. | `registrarCicloLeitura(...)` é `void` (altera o estado interno), enquanto `calcularMediaAvCiclo()` retorna `Double`. |

---

### 1.3. Visibilidade e Encapsulamento

O Encapsulamento protege os dados internos de um objeto, impedindo alterações diretas de fora da classe e exigindo que qualquer mudança ocorra por meio de métodos públicos validados:
* **`private` (Sinal `-`):** Apenas a própria classe pode ler ou alterar o atributo diretamente.
* **`protected` (Sinal `#`):** Apenas a própria classe, classes do mesmo pacote e classes filhas (que usam `extends`, como na herança da `EntidadeBase`) têm acesso. Usado em construtores exigidos pelo JPA.
* **`public` (Sinal `+`):** Qualquer outra classe do projeto pode invocar. Utilizado nos construtores principais e métodos de negócio.

---

### 1.4. Tipos de Dados Utilizados

* **`Long`**: Número inteiro longo para identificadores de banco de dados (`id`). Usa-se a classe Wrapper `Long` (com L maiúsculo) em vez do primitivo `long` porque, antes de salvar no banco, o ID ainda não existe e precisa assumir o valor `null`.
* **`Double`**: Número com casas decimais para suportar capítulos fracionários (ex.: `22.5`) e notas (ex.: `9.0`). Com D maiúsculo permite valor `null` para campos opcionais.
* **`String`**: Cadeia de caracteres para textos (títulos, sinopse, URL de capa e anotações).
* **`LocalDate` (`java.time.LocalDate`)**: Armazena uma data real de calendário (AAAA-MM-DD) sem fuso horário, registrando o dia do ciclo de leitura (`LocalDate.now()`).
* **`List<T>` e `ArrayList<T>`**: `List` é a interface (contrato da coleção), enquanto `ArrayList` é a implementação concreta instanciada com `new ArrayList<>()` para evitar `NullPointerException`.
* **`Optional<T>`**: Contêiner protetor retornado pelo repositório ao buscar por ID, obrigando a verificação de existência antes do uso.

---

## Capítulo 2 — Arquitetura em Camadas e Infraestrutura Spring Boot

O sistema divide-se em quatro camadas bem delimitadas:
1. **`ObraController` (Camada de Apresentação REST):** Recebe requisições HTTP com JSON, aciona os serviços de aplicação e devolve respostas HTTP com os códigos de status adequados (`201 Created`, `200 OK`, `204 No Content`, `404 Not Found`).
2. **`ObraService` (Camada de Aplicação e Orquestração):** Coordena os casos de uso, abrindo transações (`@Transactional`), consultando o repositório, instanciando e invocando métodos de negócio do domínio e comandando a persistência.
3. **Domínio (`Obra` e `HistoricoLeitura`):** Contém o estado encapsulado e toda a lógica matemática e de validação em Java puro.
4. **`ObraRepository` e PostgreSQL (Camada de Persistência):** Interface que estende `JpaRepository` para traduzir chamadas em instruções SQL nas tabelas `tb_obra` e `tb_historico_leitura`.

### 2.1. Dependências do Projeto (`pom.xml`)
* `spring-boot-starter-web`: Suporte a APIs REST e conversão automática de JSON.
* `spring-boot-starter-data-jpa`: Anotações de mapeamento objeto-relacional (`jakarta.persistence.*`) e repositórios.
* `postgresql`: Driver JDBC de conexão com o banco PostgreSQL.
* `spring-boot-starter-validation`: Validação declarativa de dados.

---

## Capítulo 3 — Modelagem de Domínio e Regras de Negócio do Shiori

### 3.1. Separação entre Metadados (DB/API) e Dados do Leitor
Na classe `Obra`, os atributos dividem-se em duas categorias:
* **Metadados da Obra (DB/API):** `tituloPrincipal`, `tituloOriginal`, `capaUrl`, `sinopse`, `notaGeral` (nota pública/externa da obra) e `statusObra` (estado editorial: `PREVISTO`, `ANDAMENTO`, `FINALIZADO`, `HIATO`, `CANCELADO`).
* **Registros Pessoais do Leitor:** `statusLeitura` (vínculo pessoal: `PLANEJADO`, `LENDO`, `PAUSADO`, `TERMINOU`, `DROPOU`), `progressoAtual` (último capítulo atingido), `avaliacaoPessoal` (nota direta de 0 a 10 atribuída pelo usuário), `mediaAvaliacaoCiclos` (média ponderada calculada pelos ciclos) e `historicos` (lista de ciclos registrados).

### 3.2. Avaliação Pessoal vs. Média Oculta dos Ciclos (A Mecânica do "Olho")
1. **`avaliacaoPessoal` (Livre e Direta):** O usuário pode definir ou alterar a qualquer momento uma nota de 0.0 a 10.0 que representa o quanto ele acha subjetivamente que gostou da obra como um todo.
2. **`avaliacaoCiclo` (100% Opcional):** Ao avançar capítulos, o usuário nunca é obrigado a informar uma nota.
3. **`mediaAvaliacaoCiclos` (A "Nota Surpresa"):** Calculada automaticamente em segundo plano a partir dos ciclos que possuem nota. Na interface, permanece oculta sob um botão de revelação ("olho"): ao revelá-la, o leitor descobre se sua experiência real calculada matematicamente bate com a sua percepção subjetiva!

### 3.3. Regra Unificada de Ciclos: Maratonas, Capítulos Individuais e Ciclo Elástico
O registro `HistoricoLeitura` armazena `capituloInicial` e `capituloFinal`:
1. **Leitura em Maratona:** O leitor avança múltiplos capítulos (ex.: 4.0 ao 22.0), gerando uma extensão de 18 capítulos.
2. **Avaliação de Capítulo Único:** Quando avaliado de 1 em 1 capítulo com nota ou anotação (extensão == 1.0), o sistema identifica como capítulo individual.
3. **Avanço Diário Sem Nota e Sem Anotação (Ciclo Elástico):**
   * Ao avançar aos poucos (ex.: 4.0 -> 5.0, depois 5.0 -> 6.0) sem nota e sem anotação, o sistema **não** cria múltiplos registros vazios.
   * O método `registrarCicloLeitura` verifica se o último ciclo da lista está em aberto (sem nota e sem anotação). Se estiver, ele apenas estica o `capituloFinal` e atualiza a `dataLeitura` desse ciclo existente!
   * Caso o novo avanço tenha nota/anotação, um novo objeto `HistoricoLeitura` é criado.

### 3.4. Fórmula da Média Ponderada (`calcularMediaAvCiclo`)
Para impedir que 1 capítulo isolado com nota 10 tenha o mesmo peso de uma maratona de 20 capítulos com nota 6, o cálculo pondera pela extensão do ciclo:

$$\text{Extensão}_i = \text{capituloFinal}_i - \text{capituloInicial}_i$$

$$\text{mediaAvaliacaoCiclos} = \frac{\sum (\text{Extensão}_i \times \text{avaliacaoCiclo}_i)}{\sum \text{Extensão}_i}$$

* Ciclos sem nota (`avaliacaoCiclo == null`) são ignorados.
* Se nenhum ciclo tiver nota, retorna `null`.
* Se todas as avaliações forem de 1 em 1 capítulo, a fórmula reduz-se naturalmente à média aritmética simples.

---

## Capítulo 4 — Especificação Técnica de Pacotes e Classes

```
src/main/java/dev/shiori/
├── ShioriApplication.java
├── domain/
│   ├── enums/
│   │   ├── StatusObra.java
│   │   └── StatusLeitura.java
│   ├── exception/
│   │   ├── RecursoNaoEncontradoException.java
│   │   └── RegraDeNegocioException.java
│   └── model/
│       ├── EntidadeBase.java
│       ├── HistoricoLeitura.java
│       └── Obra.java
├── dto/
│   ├── ObraCadastroRequest.java
│   ├── NovoCicloRequest.java
│   ├── AtualizarLeituraRequest.java
│   ├── HistoricoCicloResponse.java
│   └── ObraResponse.java
├── repository/
│   └── ObraRepository.java
├── service/
│   └── ObraService.java
├── controller/
│   └── ObraController.java
└── infra/
    └── TratadorDeErrosGlobal.java
```

---

## Capítulo 5 — Roteiro Sequencial de Implementação (Checklist)

1. **Etapa 1 — Tipos Fixos (Java Puro):** Pacote `domain.enums` com `StatusObra.java` e `StatusLeitura.java`.
2. **Etapa 2 — Exceções de Domínio (Java Puro):** Pacote `domain.exception` com `RecursoNaoEncontradoException.java` e `RegraDeNegocioException.java`.
3. **Etapa 3 — Superclasse Base:** Pacote `domain.model` com a classe abstrata `EntidadeBase.java`.
4. **Etapa 4 — Núcleo de POO:** Programar `HistoricoLeitura.java` e `Obra.java` (ciclo elástico e média ponderada).
5. **Etapa 5 — Configuração do Banco e Repositório:** Configurar `application.properties` e criar a interface `ObraRepository.java`.
6. **Etapa 6 — Objetos de Transporte (DTOs):** Pacote `dto` com os 5 records.
7. **Etapa 7 — Camada de Serviço:** Programar `ObraService.java`.
8. **Etapa 8 — Camada Web e Tratamento Global de Erros:** Programar `ObraController.java` e `TratadorDeErrosGlobal.java`.

---

## Capítulo 6 — Expansões Futuras (Fase 2)
* **Velocidade de Leitura:** Previsão de dias até alcançar a publicação baseado no histórico.
* **Fórum Anti-Spoiler:** Bloqueio de tópicos além do `progressoAtual` do leitor.
* **Catálogo Automático:** Integração com APIs externas (Jikan/MyAnimeList/AniList).
* **Transição Automática de Inatividade:** Sugerir transição para `PAUSADO` se ficar 60 dias sem registrar leitura.
