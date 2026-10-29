# TURBO GARAGE — Projeto Integrador Partes 3 e 4
Gestão de clientes para oficina mecânica · Aluno: Guilherme Felberque

## Como rodar
1. Ligue o MySQL. Se a senha do seu `root` não for vazia, troque `spring.datasource.password` em `src/main/resources/application.properties`.
2. No mesmo arquivo, coloque **o seu gmail** em `app.admin.email` (e uma senha em `app.admin.senha`). Esse é o único email que vê os cadastros salvos.
3. Rode `DemoApplication`.
   O Spring cria sozinho o banco, as tabelas (`schema.sql`) e a conta do administrador. Se as tabelas já existiam, as colunas novas (`perfil`, `email`) são adicionadas sozinhas.
   Usa o driver mysql-connector-java 5.1.49, compatível com MySQL antigo.
4. Abra **http://localhost:8080**

## Quem pode fazer o quê
| Pessoa | Pode |
|---|---|
| Qualquer visitante | Criar cadastro (tela "Cadastre-se"), que já cria o login dele |
| Cliente logado | Ver a tela de boas-vindas |
| Administrador (o gmail do `application.properties`) | Ver a tela de boas-vindas **e** os cadastros salvos (lista, ficha, excluir) |

A proteção está no Java (`AdminInterceptor`), não só na tela: um cliente que tentar chamar `/api/cadastros` recebe 403.

## Estrutura (arquitetura em camadas)
```
src/main/java/com/example/demo
├── DemoApplication.java
├── model/        Usuario, Cadastro, Veiculo, OrdemServico               (classes de dados)
├── repository/   Usuario/Cadastro/Veiculo/OrdemServico Repository        (Spring JDBC)
├── service/      Usuario/Cadastro/Veiculo/OrdemServico Service           (regras de negócio)
├── controller/   Auth, Cadastro, Veiculo, OrdemServico Controller + ErroHandler
├── dto/          LoginRequest, CadastroRequest, VeiculoRequest, OrdemServicoRequest
├── config/       DadosIniciais (admin + colunas novas), AdminInterceptor, WebConfig
└── util/         SenhaUtil (BCrypt)
src/main/resources/static
├── index.html       Tela 1: Login
├── veiculos.html    PARTE 4 - CRUD de Veículos (só admin)
├── ordens.html      PARTE 4 - CRUD de Ordens de Serviço (só admin)
├── cadastro.html    Cadastro público (6 campos + email e senha)
├── home.html        Boas-vindas com imagem em tela cheia
├── cadastros.html   Cadastros salvos (só admin)
├── css/style.css    Identidade visual (preto/amarelo, flat, alto contraste)
├── js/sessao.js, js/comum.js
└── img/hero.svg     Imagem fullscreen
```

## Endpoints
| Método | Rota | Quem | O que faz |
|---|---|---|---|
| POST | `/api/login` | todos | Login (JSON `{email, senha}`) |
| GET | `/api/usuario` | logado | Quem está logado e se é admin |
| POST | `/api/logout` | logado | Sai |
| POST | `/api/cadastros` | todos | Cria cadastro + login → "Cadastro realizado com sucesso!" |
| GET | `/api/cadastros` | admin | Lista todos |
| GET | `/api/cadastros/{id}` | admin | Busca um |
| DELETE | `/api/cadastros/{id}` | admin | Exclui (soft delete) e remove o login do cliente |

## Roteiro para apresentar ao professor
1. **Senha incorreta**: na tela de login, digite uma senha errada e mostre a caixa vermelha "Email ou senha inválidos".
2. **Inserção**: clique em "Cadastre-se" e cadastre 3 clientes completos, cada um com um email diferente.
3. **Feedback**: depois de cada um aparece "Cadastro realizado com sucesso!".
4. **Transição de sessão**: entre com o login de um cliente; abre a tela com a imagem em tela cheia, sem acesso aos cadastros.
5. **Recuperação individual**: saia e entre com o gmail de administrador → "Clientes" → "Ver" em cada cliente.
6. **Auditoria do banco**: no Workbench rode `SELECT email, perfil, senha_hash FROM usuarios;` e mostre que as senhas estão como `$2a$10$...`.

## Desafios extras feitos
- ⭐ 1: máscaras de CEP `00000-000` e telefone `(00) 90000-0000`
- ⭐ 2: ViaCEP preenche o endereço ao digitar o CEP (precisa de internet)
- ⭐ 3: campo `data_criacao` automático + botão Excluir (soft delete, campo `ativo`)


---

# PARTE 4 — APIs públicas e Multi-CRUD

## O que foi feito
| Requisito | Onde |
|---|---|
| API pública | **ViaCEP** no cadastro de clientes (`cadastro.html`): ao digitar o CEP, o endereço (rua, bairro, cidade/UF) é preenchido sozinho |
| Nova tela 1 com CRUD | **Veículos** (`veiculos.html`): placa, modelo, ano e cliente dono |
| Nova tela 2 com CRUD | **Ordens de Serviço** (`ordens.html`): veículo, defeito, valor e status |
| Listar com busca/filtro | Campo de busca nas duas telas (+ filtro por status nas ordens) |
| Editar | Botão "Editar" carrega o registro no formulário e salva com `PUT` |
| Excluir com confirmação | Botão "Excluir" abre uma janela de confirmação antes de apagar |
| Avisos visuais | "Registro salvo com sucesso!", "Registro atualizado", "Registro excluído" |
| Menu de navegação | Início · Clientes · Veículos · Ordens de Serviço (em todas as telas do admin) |
| ⭐ Desafio 1 – Relacionamento | `veiculos.cadastro_id` → `cadastros.id` e `ordens_servico.veiculo_id` → `veiculos.id` (chaves estrangeiras) |
| ⭐ Desafio 2 – Exportação | Botão "Exportar Excel" nas duas listagens (arquivo .csv que abre no Excel) |
| ⭐ Desafio 3 – Exceções | `ErroHandler` (`@RestControllerAdvice`): 400, 404, 409 e 500 viram JSON `{ "mensagem": "..." }` |

## Novos endpoints (só admin)
| Método | Veículos | Ordens de Serviço | Resposta |
|---|---|---|---|
| GET | `/api/veiculos` | `/api/ordens` | 200 + lista |
| GET | `/api/veiculos/{id}` | `/api/ordens/{id}` | 200 ou 404 |
| POST | `/api/veiculos` | `/api/ordens` | 201 Created |
| PUT | `/api/veiculos/{id}` | `/api/ordens/{id}` | 200 OK |
| DELETE | `/api/veiculos/{id}` | `/api/ordens/{id}` | 200 OK |

Regras: a placa não pode repetir; um veículo que tem ordens de serviço não pode ser excluído (aparece o aviso para excluir as ordens antes).

## Sobre JPA x Spring JDBC
O enunciado cita JPA, mas o projeto usa **Spring JDBC (JdbcTemplate)** porque o JPA/Hibernate do Spring Boot 4 exige MySQL 8 ou mais novo, e o MySQL usado é antigo (driver 5.1.49). A arquitetura é a mesma (Model → Repository → Service → Controller) e as tabelas são criadas automaticamente pelo `schema.sql`. A Parte 3 já aceitava "Spring Data JPA ou Spring JDBC".

## Roteiro de apresentação da Parte 4
1. **API pública**: saia, clique em "Cadastre-se", digite um CEP (ex.: `01001000`) e mostre o endereço sendo preenchido sozinho.
2. **Navegação**: entre como administrador; pelo menu (ou pelos botões da tela inicial) abra **Veículos** e **Ordens de Serviço**.
3. **CRUD Tela 1 (Veículos)**: cadastre 2 veículos, use a busca, clique em **Editar** em um, altere o modelo e salve ("Registro atualizado"), depois **Excluir** outro e confirme ("Registro excluído").
4. **CRUD Tela 2 (Ordens)**: crie 2 ordens, edite uma mudando o status para "Concluída", exclua outra.
5. **Persistência**: no Workbench rode `SELECT * FROM veiculos;` e `SELECT * FROM ordens_servico;` (as consultas prontas estão no fim do `banco.sql`).
6. **Feedback**: mostre os avisos verdes de salvo/atualizado/excluído e a janela de confirmação.
7. *(extra)* Clique em **Exportar Excel** e abra o arquivo.
