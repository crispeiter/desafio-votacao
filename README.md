# API de Votação

API REST para gerenciar pautas e sessões de votação em assembleias de cooperativa. Além dos
endpoints REST comuns, tem uma camada de telas que devolve o JSON já no formato que o app
mobile sabe renderizar, conforme o Anexo 1 do enunciado. O enunciado original está em
[ENUNCIADO.md](ENUNCIADO.md).

## O que a API faz

- Cadastra e consulta pautas
- Abre uma sessão de votação em uma pauta, com duração configurável na chamada
- Recebe os votos (Sim ou Não), um por associado por pauta
- Apura o resultado
- Consulta um serviço externo de autorização de CPF antes de aceitar o voto
- Monta as telas SELECAO e FORMULARIO do app mobile

## Stack

| Para que | O que usei |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1 |
| Banco | PostgreSQL 16 |
| Migrations | Flyway |
| Documentação | springdoc-openapi 3.1 (Swagger UI) |
| Testes | JUnit 5, Mockito, Testcontainers |
| Cobertura | JaCoCo |
| Carga | k6 |

Sem Lombok. Os DTOs são `record` e as entidades são POJOs escritos à mão, então o código roda em
qualquer IDE sem plugin nenhum.

## Organização do código

Os pacotes são por funcionalidade, não por camada. Cada um leva a entidade, o repositório, o
serviço, o controller e os DTOs do seu próprio assunto.

```
com.db.votacao
├── config    Clock, propriedades externalizadas e OpenAPI
├── pauta     cadastro e consulta de pautas
├── sessao    abertura da sessão e cálculo do status
├── voto      registro do voto, apuração e a query agregada
├── cpf       validação, client HTTP e, em cpf/fake, o provedor simulado
├── tela      controller e assembler das telas do app mobile
└── comum     exceções de domínio e o tratamento centralizado de erros
```

Num domínio deste tamanho, quebrar em `application`, `domain` e `infrastructure` seria over
engineering, e o enunciado pede justamente para evitar isso. Assim, mexer em "pauta com várias
sessões" toca um pacote, e não três camadas espalhadas.

## Como rodar

Precisa de Docker e Java 21. O Maven vem no wrapper. O Docker não é só para subir o banco: os
testes também dependem dele, então vale deixar rodando.

Sobe o banco:

```
docker compose up -d
```

O contêiner publica a porta 5432. Se você já tem um PostgreSQL local nela, esse comando falha com
`port is already allocated`, e a aplicação acaba tentando falar com o seu banco local, que não
tem o usuário nem a base `votacao`. Para resolver, ou você para o serviço local, ou troca o
mapeamento de porta no `docker-compose.yml` e ajusta `spring.datasource.url` junto.

Sobe a aplicação:

```
./mvnw spring-boot:run
```

No Windows é `.\mvnw.cmd spring-boot:run`.

Depois disso:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI em JSON: http://localhost:8080/v3/api-docs
- Actuator: http://localhost:8080/actuator (health, info e metrics)

Duas coisas sobre o voto que evitam susto: o CPF precisa ser válido, porque o provedor confere o
dígito verificador (qualquer gerador de CPF serve), e um 404 dizendo que o associado não está
apto é o sorteio do provedor fazendo o que o enunciado pede, não erro da aplicação. Para desligar
o sorteio:

```
./mvnw spring-boot:run -Dspring-boot.run.arguments=--votacao.cpf.modo=SEMPRE_APTO
```

## Endpoints

| Método | Rota | O que faz | Status |
|---|---|---|---|
| POST | `/api/v1/pautas` | Cria uma pauta | 201, 400 |
| GET | `/api/v1/pautas` | Lista as pautas | 200 |
| GET | `/api/v1/pautas/{id}` | Busca uma pauta | 200, 404 |
| POST | `/api/v1/pautas/{pautaId}/sessao` | Abre a sessão de votação | 201, 400, 404, 409 |
| GET | `/api/v1/pautas/{pautaId}/sessao` | Consulta a sessão | 200, 404 |
| POST | `/api/v1/pautas/{pautaId}/votos` | Registra um voto | 201, 400, 404, 409, 500 |
| GET | `/api/v1/pautas/{pautaId}/resultado` | Apura a votação | 200, 404 |
| GET | `/api/v1/telas/pautas` | Tela SELECAO com as pautas em votação | 200 |
| POST | `/api/v1/telas/pautas/{pautaId}/votacao` | Tela FORMULARIO para votar | 200 |
| POST | `/api/v1/telas/pautas/{pautaId}/voto` | Registra o voto e devolve a confirmação | 200 |
| POST | `/api/v1/telas/pautas/{pautaId}/resultado` | Tela com a apuração | 200 |
| GET | `/fake/autorizacao/{cpf}` | Provedor de autorização simulado | 200, 404 |

Os erros dos endpoints REST seguem RFC 7807, no formato `ProblemDetail`. Os endpoints de tela
são a exceção, e explico o motivo mais abaixo.

## As telas do app

Essa parte é o Anexo 1 do enunciado e foi onde gastei mais tempo pensando. O app não sabe nada
sobre pauta ou sessão: ele recebe uma tela e renderiza.

A tela inicial é a única em GET, porque o app precisa buscá-la sem uma ação prévia do usuário.
O `GET /api/v1/telas/pautas` devolve algo assim:

```json
{
  "tipo": "SELECAO",
  "titulo": "Pautas em votação",
  "itens": [
    {
      "titulo": "Reforma do estatuto social",
      "descricao": "Encerra em 17/08/2026 14:05",
      "url": "http://localhost:8080/api/v1/telas/pautas/1/votacao",
      "body": { "pautaId": 1 }
    }
  ]
}
```

Tocando no item, o app faz POST na `url` com aquele `body` e recebe o formulário de votação, com
o campo de CPF e os botões Sim e Não. Cada botão carrega a URL e o corpo do POST que ele deve
disparar, e o app junta o CPF digitado a esse corpo. O anexo diz que o FORMULARIO tem um ou dois
botões na parte inferior, e votação tem exatamente duas opções.

Só aparecem na lista as pautas com sessão aberta. Mostrar uma pauta sem sessão levaria o
associado a um formulário que só falharia.

Duas coisas que o anexo não define e eu precisei decidir:

- Os tipos de item. Criei `TEXTO` para entrada de dados e `INFORMACAO` para texto somente
  leitura, usado nas telas de confirmação, resultado e erro.
- As URLs de callback saem de `votacao.callback-base-url`, que por padrão aponta para
  `http://localhost:8080`. Dá para trocar por variável de ambiente
  (`VOTACAO_CALLBACK_BASE_URL`), que é o que resolve o caso do emulador contra o aparelho
  físico citado nas dicas.

## As decisões que mais pesaram

**As telas sempre devolvem 200, mesmo em erro.** Se `/telas/**` devolvesse `ProblemDetail`, o app
quebrava, porque ele só sabe renderizar tela. Então voto duplicado, sessão encerrada e CPF não
apto viram uma tela FORMULARIO com a mensagem do erro, e o status continua 200. Os endpoints REST
continuam devolvendo 400, 404 e 409 normalmente. São dois contratos diferentes na mesma
aplicação, e isso é de propósito.

**Não faço `select` antes de inserir o voto.** A unicidade é da constraint
`uk_voto_sessao_associado`, e a violação vira 409. Um `SELECT` seguido de `INSERT` é TOCTOU:
duas requisições simultâneas do mesmo CPF passam as duas pelo select e gravam dois votos. Deixar
o banco decidir é o único jeito correto sob concorrência, e ainda economiza uma ida ao banco por
voto. Tem um teste de repositório que tenta o segundo insert de verdade e verifica que quem
barrou foi a constraint, pelo nome dela na mensagem de erro.

**O provedor de CPF é uma integração HTTP de verdade.** A tarefa se chama integração com sistemas
externos, e um fake em processo não demonstra integração nenhuma. Então o serviço simulado é um
controller em `/fake/autorizacao/{cpf}`, fora do `/api/v1`, e a aplicação chama ele por HTTP com
um client declarativo (`@HttpExchange` com `RestClient`), com timeout de conexão e de leitura
configurados. Trocar pelo provedor real é mudar `votacao.cpf.base-url`, não código. A validação
de formato e dígito verificador é determinística, quem é sorteado é só o ABLE ou UNABLE, que é o
que o enunciado descreve.

## Outras decisões

- **Uma sessão por pauta.** O enunciado fala em abrir "uma sessão de votação em uma pauta", no
  singular. Modelar 1:N traria a pergunta "qual sessão apurar?" sem requisito que justificasse.
- **Duração em segundos, de 1 a 86400**, com default de 60. Em segundos porque dá para abrir uma
  sessão de 10s e ver o encerramento sem esperar um minuto. O mínimo evita sessão que nasce
  encerrada e o máximo evita sessão eterna por erro de digitação.
- **`UNABLE_TO_VOTE` devolve 404.** Semanticamente eu usaria 403, já que o CPF existe e só não
  está autorizado, mas o enunciado pede "retornar 404 no client tb" e eu segui o enunciado.
- **Resultado com sessão aberta vem parcial**, com `statusSessao: "ABERTA"`, o que permite
  acompanhar em tempo real. Se a regra fosse sigilo até o fechamento, bastaria zerar as contagens.
- **`SEM_VOTOS` não é `EMPATE`.** Ninguém votou é diferente de deu empate.
- **`Clock` injetado no lugar de `Instant.now()`**, que é o que deixa os testes de janela de tempo
  determinísticos. Na suíte inteira só existe um `Thread.sleep`, no teste que abre uma sessão de
  1 segundo e confirma que o voto depois disso dá 409.
- **Datas em `Instant` e `timestamptz`**, sem `LocalDateTime`, para não ter ambiguidade de fuso.
  O único lugar com fuso fixo é a descrição da tela, que mostra o horário de encerramento em
  horário de Brasília, porque é texto para o associado ler.
- **Log de negócio em INFO e nenhum CPF registrado**, que é dado pessoal. Tem também o contador
  `votacao.votos.registrados` com tag da opção, em
  `/actuator/metrics/votacao.votos.registrados`.

## Versionamento da API

Escolhi versionar por URI, o `/api/v1` que está em todas as rotas.

O cliente aqui é um app mobile, e versão antiga de app fica instalada por meses. Precisar manter
`v1` e `v2` no ar ao mesmo tempo durante a migração da base instalada é o cenário normal, e
versão no path resolve isso de um jeito que qualquer gateway roteia sem esforço. Some a isso o
fato de aparecer no log e no cache da CDN, o que ajuda muito na hora de investigar problema.

As alternativas que considerei:

- Header customizado (`X-API-Version`): fica invisível na URL, o que atrapalha debug e cache.
- Media type (`Accept: application/vnd.votacao.v1+json`): é o mais purista, mas custa mais para o
  time mobile sem ganho proporcional.

Vale dizer que o Spring Boot 4 já traz suporte nativo a versionamento de API. Preferi manter o
versionamento por URI, que é explícito e não depende de recurso específico de versão do
framework.

E a regra prática: só sobe versão em breaking change. Campo novo em response é retrocompatível e
não vira `v2`.

## Testes

```
./mvnw test
```

Precisa do Docker rodando, porque os testes de repositório e de integração sobem PostgreSQL com
Testcontainers. São 99 testes, divididos assim:

- **Unitários** dos serviços, com Mockito puro e `Clock.fixed`, sem subir contexto Spring.
- **De repositório**, com `@DataJpaTest` e Testcontainers, um por entidade. O de voto é o mais
  importante da suíte, porque prova que a unicidade vive no banco.
- **De integração**, com `@SpringBootTest` e MockMvc: fluxo completo, voto duplicado, sessão
  expirada, payload inválido, rota inexistente, método não suportado e a chamada real ao
  provedor de CPF pelo client HTTP.
- **Das telas**, incluindo os três caminhos de erro que precisam devolver 200 com corpo de tela.

Para gerar o relatório de cobertura:

```
./mvnw clean verify
```

O relatório sai em `target/site/jacoco/index.html`. Hoje está em 98% de instruções. O que ficou
descoberto é o `main` da aplicação, o ramo aleatório do provedor simulado (os testes usam o modo
sempre apto de propósito) e o caminho de erro 500 genérico.

## Teste de carga

O script está em [performance/carga-votos.js](performance/carga-votos.js) e as instruções de
execução estão no topo dele. Resumindo: sobe o banco, sobe a aplicação com
`votacao.cpf.modo=SEMPRE_APTO`, cria uma pauta com sessão longa e roda:

```
k6 run -e PAUTA_ID=1 performance/carga-votos.js
```

A rampa vai até 200 usuários virtuais e cada iteração gera um CPF válido e vota. Os thresholds
são p95 abaixo de 300ms e menos de 1% de erro.

O resultado da execução que fiz está em [performance/RESULTADO.md](performance/RESULTADO.md).
Foram 84.459 votos em 2min30, com p95 de 72,41ms e nenhum erro, rodando aplicação, banco e
gerador de carga na mesma máquina.

O que ajuda nesse número:

- Constraint de unicidade no lugar do select prévio, que corta uma consulta por voto
- Índice `(sessao_id, opcao)` cobrindo a query de apuração
- Apuração por `count` agregado no banco, sem carregar voto nenhum para a memória
- Nenhum `@OneToMany` de sessão para voto, para não existir caminho acidental que traga a tabela
  inteira
- `open-in-view` desligado e leituras com `@Transactional(readOnly = true)`
- Pool de conexões em 20

## Configurações

Tudo em `application.yml`, e tudo sobrescrevível por variável de ambiente.

| Propriedade | Default | Para que serve |
|---|---|---|
| `votacao.sessao.duracao-padrao` | `60s` | Duração quando a chamada não informa |
| `votacao.cpf.modo` | `ALEATORIO` | `SEMPRE_APTO` desliga o sorteio do provedor |
| `votacao.cpf.base-url` | `http://localhost:8080` | Onde vive o provedor de autorização |
| `votacao.cpf.timeout-conexao` | `2s` | Timeout de conexão do client |
| `votacao.cpf.timeout-leitura` | `3s` | Timeout de leitura do client |
| `votacao.callback-base-url` | `http://localhost:8080` | Base das URLs que vão nas telas |

## O que deixei de fora de propósito

- **Entidade `Associado`.** O enunciado diz que o associado é identificado por um id único, e o
  CPF já cumpre esse papel. Um CRUD de associado seria escopo que ninguém pediu.
- **Várias sessões por pauta**, pelo motivo que expliquei lá em cima.
- **Autenticação.** O enunciado autoriza abstrair a segurança das interfaces.
- **Camada de mapper dedicada.** A conversão entre entidade e DTO é de uma linha e cabe no
  controller. Uma biblioteca de mapeamento aqui seria dependência a mais para resolver problema
  que não existe.

## O que eu faria numa versão de verdade

- Autenticação e autorização, com o associado vindo do token em vez do corpo da requisição
- Idempotência por chave de requisição, para o app poder repetir um POST sem medo depois de perder
  a conexão
- Cache do resultado depois que a sessão encerra, já que o valor vira imutável
- Notificação de encerramento da sessão por fila, e ingestão assíncrona dos votos se o volume
  crescer a ponto de precisar desacoplar a escrita
- Particionamento da tabela de votos por sessão, se a base chegar em algumas dezenas de milhões
  de linhas
