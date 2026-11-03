# AGENTS.md — Contrato de engenharia do ORI

Este arquivo é a fonte única das regras de engenharia do repositório.
Ele vale para pessoas e agentes de IA, em todas as etapas: auditoria,
arquitetura, implementação, refatoração, testes, documentação e manutenção.

Arquivos de instrução específicos complementam este contrato sem repeti-lo:

- `web/AGENTS.md` — aviso do Next.js (gerado por `next dev`): leia a
  documentação em `web/node_modules/next/dist/docs/` antes de escrever código web.
- `android/AGENTS.MD` (não versionado) — cópia antiga deste contrato; em caso
  de divergência, vale este arquivo.

## 0. Estrutura real do repositório

| Pasta | Conteúdo | Verificação |
|---|---|---|
| `android/` | App Android (Kotlin, Jetpack Compose, Hilt, Firebase Auth) | `./gradlew detekt testDevDebugUnitTest lintProdDebug assembleDevDebug assembleProdDebug` |
| `functions/` | Projeto Firebase: `firebase.json`, `firestore.rules`, `firestore.indexes.json` | — |
| `functions/functions/` | Cloud Functions (TypeScript): convites e vínculos | `npm run lint && npm run build` |
| `functions/security-tests/` | Testes das regras e das funções no emulador | `npm run format:check && npm test` |
| `web/` | Next.js, ainda o template inicial | `npm run format:check && npm run lint && npm run build` |
| `docs/` | Documentação em português | — |
| `.github/workflows/ci.yml` | CI que executa as verificações acima | — |

### Onde está cada assunto

| Assunto | Documento |
|---|---|
| Arquitetura, pacotes, navegação e verificações | [docs/arquitetura.md](docs/arquitetura.md) |
| Tokens visuais, componentes e layout adaptativo | [docs/design-system.md](docs/design-system.md) |
| Camada de dados, offline, custos, índices, ambientes (`dev`/`staging`/`prod`) e evolução do esquema | [docs/dados.md](docs/dados.md) |
| Contas, vínculos, permissões e regras de segurança | [docs/funcionalidades/perfis-e-permissoes.md](docs/funcionalidades/perfis-e-permissoes.md) |
| Cadastro, login e senha | [docs/funcionalidades/autenticacao.md](docs/funcionalidades/autenticacao.md) |
| Perfil, foto, sair da conta | [docs/funcionalidades/perfil-e-conta.md](docs/funcionalidades/perfil-e-conta.md) |
| Uso sem conta e passagem dos dados para a conta | [docs/funcionalidades/uso-local.md](docs/funcionalidades/uso-local.md) |
| Rotina, Tarefas, Início e atividades que se repetem | [docs/funcionalidades/rotina.md](docs/funcionalidades/rotina.md) |
| Relatórios | [docs/funcionalidades/relatorios.md](docs/funcionalidades/relatorios.md) |
| Comunicação (CAA) | [docs/funcionalidades/caa.md](docs/funcionalidades/caa.md) |
| Preferências e configurações | [docs/funcionalidades/preferencias.md](docs/funcionalidades/preferencias.md) |
| Falhas e gambiarras conhecidas, ainda não corrigidas | [docs/revisao-de-codigo.md](docs/revisao-de-codigo.md) |
| Revisões anteriores (histórico de decisões) | [docs/revisao-estrutura-e-design.md](docs/revisao-estrutura-e-design.md), [docs/revisao-de-design.md](docs/revisao-de-design.md) |

### Regras que valem em todo o código

- Texto na interface usa `ui/components/text/Text.kt`, nunca o `Text` do Material: é o que aplica o botão "Aa" em todas as telas.
- Toda mudança em `firestore.rules` ou nas funções exige teste em `functions/security-tests`.
- Toda consulta de lista deve ter `limit` (as regras recusam listas sem limite) e nenhum listener deve viver fora do ciclo de vida da tela.
- Uma consulta que precisa de índice composto tem o índice em `functions/firestore.indexes.json`, publicado junto com as regras.
- Uma classe que **encaminha** um contrato (`Routed*`, `SharedReading*`, `RepeatingRoutineRepository`) sobrescreve **todos** os membros, inclusive os que têm implementação padrão na interface, e tem teste disso. Um membro esquecido cai no comportamento padrão sem nenhum aviso (foi o que aconteceu com `getDays`, falha R1). Use `assertForwardsEveryMember` (`src/test/.../testing/ForwardingCheck.kt`).
- Leitura de disco ou de rede nunca na thread principal. Isso vale também para `File.exists`/`isFile` dentro de um `combine` de ViewModel, que roda na thread principal.
- O banco local (Room) muda com versão nova, `Migration` e o esquema exportado em `android/app/schemas`.
- Os dados do aparelho e os da conta passam pelo mesmo contrato. A tela recebe um dono (`ownerUid`) e não sabe onde o dado mora (`LOCAL_OWNER_UID` = aparelho).

### Publicação (Firebase)

| O quê | Comando (em `functions/`) | Observação |
|---|---|---|
| Regras do Firestore | `firebase deploy --only firestore:rules --project oriteam-ori` | Só com os testes do emulador passando |
| Índices | `firebase deploy --only firestore:indexes --project oriteam-ori` | Um índice leva alguns minutos para ficar pronto |
| Cloud Functions | `firebase deploy --only functions --project oriteam-ori` | Ainda não publicadas: exigem o plano Blaze |

Publicar é uma ação externa: só faça quando a pessoa responsável pediu ou autorizou.

## 1. Princípios

Priorize, nesta ordem: correção funcional; segurança e privacidade; clareza;
independência entre componentes; manutenibilidade; acessibilidade; desempenho
medido; escalabilidade; simplicidade operacional.

- Prefira a solução mais simples que atenda aos requisitos atuais e permita evoluir.
- Use KISS, DRY, YAGNI e SOLID quando trouxerem benefício real, nunca de forma mecânica.
- Não crie abstrações, interfaces, classes ou módulos sem propósito.

## 2. Clean code e responsabilidade única

Cada componente tem uma responsabilidade clara; cada função faz uma tarefa coerente,
tem nome descritivo e limita efeitos colaterais.

Evite: funções extensas; classes com responsabilidades misturadas; ViewModels com
regras de domínio e acesso direto a infraestrutura; interface que acessa banco;
regras duplicadas; dependências globais; estado mutável compartilhado sem controle;
booleanos que mudam vários comportamentos; valores mágicos e strings duplicadas;
exceções capturadas sem tratamento; retornos vazios que escondem falhas; código
morto, TODOs esquecidos e implementações fictícias.

Prefira imutabilidade e tipos explícitos para estados distintos. Separe validação,
regra de negócio, persistência e apresentação conforme a complexidade real.

## 3. Independência entre componentes

- Alta coesão, baixo acoplamento, dependências explícitas e direção compreensível.
- Contratos (interfaces) apenas em fronteiras que precisam ser substituídas,
  por exemplo o `AuthRepository` entre telas e Firebase.
- Modelos de domínio independentes da infraestrutura; componentes visuais
  independentes das regras de negócio.
- Sem dependências circulares, sem acesso a internals de outra feature e sem um
  pacote "core" virando depósito.
- Não duplique lógica entre Android, web e backend sem justificativa de plataforma.
- Não crie módulo Gradle por pasta: a separação física precisa de benefício concreto.

## 4. Estruturas de decisão

- Use `Map`, `Set`, enums, `when` ou estratégias quando substituírem comparações
  repetidas que associam estados a valores fixos.
- Use `if` e early return para condições simples.
- Não troque condições simples por abstrações, reflexão ou despacho dinâmico.

## 5. Nomes em inglês

Classes, funções, variáveis, parâmetros, enums, arquivos de código, identificadores
de domínio, eventos, estados e novas chaves de dados são escritos em inglês, com
nomes completos (evite `Manager`, `Helper`, `Utils`, `Data` sem contexto).

- Kotlin: camelCase para funções e propriedades; PascalCase para tipos e para
  Composables que emitem UI; UPPER_SNAKE_CASE para constantes reais; pacotes minúsculos.
- TypeScript/JavaScript: camelCase; PascalCase para tipos e componentes React.
- Python (se existir): PEP 8.
- Não renomeie APIs de terceiros, símbolos gerados ou contratos persistidos sem
  analisar a migração. Textos exibidos ao usuário ficam em português, em `strings.xml`.

## 6. Formatação e ferramentas

| Linguagem | Ferramenta | Configuração |
|---|---|---|
| Todas | EditorConfig | `.editorconfig` |
| Kotlin | detekt + regras ktlint (`detekt-formatting`) | `android/config/detekt/detekt.yml` |
| Android (XML, recursos) | Android Lint | padrão do AGP |
| TypeScript (web) | ESLint + Prettier | `web/eslint.config.mjs`, `web/.prettierrc.json` |
| TypeScript (functions) | ESLint (estilo Google) | `functions/functions/.eslintrc.js` |

- Formatar Kotlin: `./gradlew detekt -PdetektAutoCorrect` (em `android/`).
- Formatar web: `npm run format` (em `web/`).
- Não adicione ferramentas redundantes, não atualize dependências por estética e
  não reformate arquivos inteiros sem necessidade.

## 7. Documentação para iniciantes

Documentação em português brasileiro, acessível a quem está aprendendo. Para cada
módulo, responda: o que faz; por que existe; como a execução começa; quem participa;
como os dados circulam; quais regras e permissões são verificadas; onde os dados são
lidos e gravados; o que acontece em erro; como o estado da interface muda; como testar;
como estender sem quebrar.

- Use Mermaid quando ajudar. Explique decisões e motivos.
- Comentários explicam motivos e restrições, não repetem o código.
- Não documente o que o código não faz. Atualize a documentação junto com o fluxo.

## 8. Tratamento de erros

- Toda operação relevante define sucesso, falha e estado intermediário (carregando).
- Não ignore exceções e não mostre sucesso quando algo falhou.
- Não esconda erro de autorização como se fosse ausência de conteúdo.
- Separe mensagem técnica (log) de mensagem ao usuário (`strings.xml`).
- Mensagens e logs não expõem dados privados nem detalhes internos.

## 9. Testabilidade

- Regras de domínio testáveis sem interface gráfica e sem serviços reais.
- Dependências substituíveis (fakes) nos testes; evite mocks que só repetem a implementação.
- Teste sucesso, falha, limites e autorização. Um teste só vale se detectar uma falha real.
- Organize testes por comportamento e módulo, espelhando os pacotes de `main`.
- Teste também a montagem real quando ela decide o comportamento (o que o `DataModule` liga a
  quê, o que um `Routed*` encaminha): um fake no lugar do encaminhador esconde o erro dele.
- Todo ViewModel novo tem teste das suas ações e dos estados de carregando, conteúdo, vazio e erro.

## 10. Escalabilidade

Considere consultas eficientes, paginação, índices, concorrência, limites e custos dos
serviços, idempotência, observabilidade, evolução de contratos, migrações, isolamento
de dados e falhas de rede. Explique limites e como foram verificados. Não introduza
microsserviços ou múltiplos bancos sem necessidade comprovada.

## 11. Acessibilidade e experiência

Acessibilidade cognitiva, visual e motora é requisito desde o início. Preserve a
autonomia do usuário. Adapte a interface ao espaço disponível (celular, tablet,
dobráveis, janelas redimensionáveis), com suporte a texto ampliado, tecnologias
assistivas, navegação previsível e baixa estimulação.

- Área de toque de pelo menos 48 dp. Não reduza o tamanho de `IconButton` com `Modifier.size`.
- Texto com número usa `plurals` (`pluralStringResource`), nunca "1 atividades".
- Espaços só nos degraus de `OriSpacing`; cor de uma área com `OriAreaAccents`, nunca um número solto.
- Um elemento só visual (ícone ao lado de um texto, gráfico com os números nos detalhes) fica
  escondido do leitor de tela; o que tem significado é lido como uma coisa só.

## 12. Execução de cada etapa

Antes: leia os arquivos, identifique o fluxo, as dependências e os testes; defina as
menores alterações coerentes.

Durante: preserve comportamentos, evite mudanças não relacionadas, não crie dados
fictícios definitivos, não exponha segredos, não remova testes para esconder defeitos.

Depois: formate e rode lint; rode os testes; compile; corrija regressões; atualize a
documentação; informe resultados reais e limitações, classificando cada item como
**implementado e testado**, **implementado e parcialmente testado**, **bloqueado por
configuração externa**, **não implementado** ou **não foi possível verificar**.

Uma falha encontrada e não corrigida na mesma etapa entra em
[docs/revisao-de-codigo.md](docs/revisao-de-codigo.md), com onde está, o que acontece e a
correção sugerida. Ao corrigir, tire de lá e diga onde está o teste.

Edição automática (script, expressão regular) só em arquivo versionado ou com cópia de
segurança: parte do código usa CRLF, e uma substituição errada pode apagar um arquivo inteiro.

## 13. Requisitos adicionais do ORI

1. **Identidade visual.** Preserve tema, tipografia (Nunito/Nunito Sans), cores `Ori*`,
   ícones e espaçamentos. Reutilize componentes e tokens existentes. Correções de
   acessibilidade devem ser compatíveis com a marca.
2. **Arquivos pequenos e coesos.** Mire em 150–250 linhas; acima de 250, avalie separar;
   300+ é sinal forte de revisão. Não divida mecanicamente.
3. **Componentes reutilizáveis** para padrões realmente repetidos (formulários, botões,
   mensagens, carregamento). Comportamento específico fica na feature.
4. **Segurança em primeiro lugar.** Menor privilégio, autorização negada por padrão,
   dono explícito dos dados, validação no servidor de operações privilegiadas, dados
   mínimos, logs sem conteúdo sensível. Nunca confie na interface para autorizar.
   Nunca exponha credenciais, tokens ou conteúdo do usuário em código, logs, analytics,
   mensagens de erro ou URLs. Teste regras de segurança independentemente da interface.
5. **Sessão.** Use a persistência de sessão do SDK, restaure sessões válidas ao abrir o
   app, não guarde senhas nem tokens manualmente e não use a tela de login como
   resposta genérica a erros de rede ou permissão. Separe autenticação de autorização.
6. **Desempenho, bateria, memória e rede.** Meça antes e depois; evite bloquear a thread
   principal, listeners e trabalho em segundo plano sem propósito e dependências redundantes.
7. **Interface responsiva.** Evite alturas e larguras fixas que cortem conteúdo; teste as
   configurações de janela suportadas e documente quais foram verificadas.
8. **Animações.** Sistema consistente com modos Expressivo, Sutil, Reduzido e Desligado,
   respeitando as preferências do sistema; desativar animações não remove informação.
9. **Documentação para iniciantes** conforme a seção 7.
10. **Critério de conclusão.** Antes de declarar algo pronto, verifique identidade visual,
    separação de responsabilidades, convenções, reutilização, segurança, erros, sessão,
    responsividade, animações, desempenho, testes executados, documentação e limitações.
    Não declare verificado o que não foi executado.
11. **Verificação visual.** Teste de posição não basta para layout. Uma mudança visual é conferida
    numa imagem renderizada na largura de um celular (360 dp, com Robolectric) e, quando possível,
    no aparelho. Diga qual das duas foi feita.
