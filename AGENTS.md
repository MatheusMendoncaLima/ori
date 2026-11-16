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
| `functions/functions/` | Cloud Functions (TypeScript) de convite, guardadas para o plano Blaze: o app faz os convites pelas regras | `npm run lint && npm run build` |
| `functions/security-tests/` | Testes das regras e das funções no emulador | `npm run format:check && npm test` |
| `web/` | Next.js, ainda o template inicial | `npm run format:check && npm run lint && npm run build` |
| `docs/` | Documentação em português | — |
| `.github/workflows/ci.yml` | CI que executa as verificações acima | — |

### Onde está cada assunto

| Assunto | Documento |
|---|---|
| Arquitetura, pacotes, navegação e verificações | [docs/arquitetura.md](docs/arquitetura.md) |
| Tokens visuais, componentes e layout adaptativo | [docs/design-system.md](docs/design-system.md) |
| Animações: níveis, peças prontas e onde cada uma aparece | [docs/design-system.md](docs/design-system.md#movimento-animações) |
| Camada de dados, offline, custos, índices, ambientes (`dev`/`staging`/`prod`) e evolução do esquema | [docs/dados.md](docs/dados.md) |
| Contas, vínculos, permissões e regras de segurança | [docs/funcionalidades/perfis-e-permissoes.md](docs/funcionalidades/perfis-e-permissoes.md) |
| Cadastro, login e senha | [docs/funcionalidades/autenticacao.md](docs/funcionalidades/autenticacao.md) |
| Perfil, foto, sair da conta | [docs/funcionalidades/perfil-e-conta.md](docs/funcionalidades/perfil-e-conta.md) |
| Uso sem conta e passagem dos dados para a conta | [docs/funcionalidades/uso-local.md](docs/funcionalidades/uso-local.md) |
| Rotina, Tarefas, Início e atividades que se repetem | [docs/funcionalidades/rotina.md](docs/funcionalidades/rotina.md) |
| Relatórios | [docs/funcionalidades/relatorios.md](docs/funcionalidades/relatorios.md) |
| Comunicação (CAA) | [docs/funcionalidades/caa.md](docs/funcionalidades/caa.md) |
| Preferências e configurações | [docs/funcionalidades/preferencias.md](docs/funcionalidades/preferencias.md) |
| ORI Home, a tela inicial opcional (launcher) | [docs/funcionalidades/launcher.md](docs/funcionalidades/launcher.md) |
| Teste do ORI Home no tablet e diferenças para o One UI | [docs/funcionalidades/launcher-teste-one-ui.md](docs/funcionalidades/launcher-teste-one-ui.md) |
| Plano de mudanças do ORI Home (girar como o One UI, correções) | [docs/funcionalidades/launcher-plano.md](docs/funcionalidades/launcher-plano.md) |
| Idiomas: como o app escolhe o idioma das datas e da voz, e como traduzir | [docs/idiomas.md](docs/idiomas.md) |
| Falhas e gambiarras conhecidas, ainda não corrigidas | [docs/revisao-de-codigo.md](docs/revisao-de-codigo.md) |
| Teste geral no tablet: erros, pontos de melhora e design duvidoso (12/11/2026); desempenho medido e design (13/11/2026); tempo para abrir cada tela (13/11/2026) | [docs/teste-geral.md](docs/teste-geral.md) |
| Revisões anteriores (histórico de decisões) | [docs/revisao-estrutura-e-design.md](docs/revisao-estrutura-e-design.md), [docs/revisao-de-design.md](docs/revisao-de-design.md) |

### Regras que valem em todo o código

- Texto na interface usa `ui/components/text/Text.kt`, nunca o `Text` do Material: é o que aplica o botão "Aa" em todas as telas.
- Diálogo usa `OriAlertDialog`, `OriConfirmationDialog` ou `OriDialog` (`ui/components/dialog`), nunca o `AlertDialog` ou o
  `Dialog` do Material: são desenhados na janela da própria tela (`OriDialogHost`, em `MainActivity` e `LauncherActivity`)
  e abrem sem o Android criar uma janela, o que custava de 50 a 90 ms a cada abertura. O detekt proíbe os do Material.
- Toda mudança em `firestore.rules` ou nas funções exige teste em `functions/security-tests`.
- Toda consulta de lista deve ter `limit` (as regras recusam listas sem limite) e nenhum listener deve viver fora do ciclo de vida da tela.
- Uma consulta que precisa de índice composto tem o índice em `functions/firestore.indexes.json`, publicado junto com as regras.
- Uma classe que **encaminha** um contrato (`Routed*`, `SharedReading*`, `RepeatingRoutineRepository`) sobrescreve **todos** os membros, inclusive os que têm implementação padrão na interface, e tem teste disso. Um membro esquecido cai no comportamento padrão sem nenhum aviso (foi o que aconteceu com `getDays`, falha R1). Use `assertForwardsEveryMember` (`src/test/.../testing/ForwardingCheck.kt`).
- Data na tela usa `rememberDateFormat` com um esqueleto de `DateSkeletons`, e nome de dia ou mês usa
  `LocalAppLocale.current`: nunca um padrão escrito à mão (`"d 'de' MMMM"`), que só serve para um idioma.
  Hora na tela usa `rememberTimeFormat`, nunca `"HH:mm"` (este só para gravar).
- Texto novo vai para o `strings_<área>.xml` da área (`aac`, `routine`, `account`, `settings`, `reports`);
  `strings.xml` fica com o que é comum. Um texto que cita um botão recebe o nome dele por parâmetro.
  O app está em português, inglês e espanhol: o texto vai para `values` e para a cópia `values-pt`, e a
  tradução para `values-en` e `values-es` (o `AppLanguagesTest` e o Lint conferem). Veja [docs/idiomas.md](docs/idiomas.md).
- Tela com o botão flutuante (`OriActionPill`) usa `ActionPillLayout`, que mede o botão: nada de espaço fixo em dp.
- Leitura de disco ou de rede nunca na thread principal. Isso vale também para `File.exists`/`isFile` dentro de um `combine` de ViewModel, que roda na thread principal.
- O banco local (Room) muda com versão nova, `Migration` e o esquema exportado em `android/app/schemas`.
- Toda animação passa por `OriMotion` e pelas peças de `ui/components/motion` e `ui/theme/OriTransitions.kt`,
  nunca por uma duração ou curva soltas: é o que faz "Animações" em Configurações valer no app inteiro.
  O valor animado é lido no desenho (`graphicsLayer`, `drawBehind`), não na composição.
- Fluidez e desempenho se medem no build `profiling` (`./gradlew assembleProdProfiling`) com
  `adb shell dumpsys gfxinfo`, nunca num build de depuração. Instalado pelo cabo, o app roda sem compilar até o
  aparelho ficar ocioso: antes de medir, compile como a loja faz (`adb shell cmd package compile -f -m speed-profile
  com.oriteam.ori`). O tempo para abrir uma tela se mede com o rastreamento do sistema (`atrace`), do dedo saindo da
  tela até o fim do primeiro quadro, com toques de uns 90 ms, como os de um dedo: veja [docs/teste-geral.md](docs/teste-geral.md).
- Os dados do aparelho e os da conta passam pelo mesmo contrato. A tela recebe um dono (`ownerUid`) e não sabe onde o dado mora (`LOCAL_OWNER_UID` = aparelho).

### Publicação (Firebase)

| O quê | Comando (em `functions/`) | Observação |
|---|---|---|
| Regras do Firestore | `firebase deploy --only firestore:rules --project oriteam-ori` | Só com os testes do emulador passando |
| Índices | `firebase deploy --only firestore:indexes --project oriteam-ori` | Um índice leva alguns minutos para ficar pronto |
| Cloud Functions | `firebase deploy --only functions --project oriteam-ori` | Não publicadas nem usadas pelo app: exigem o plano Blaze |

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
  analisar a migração. Textos exibidos ao usuário ficam nos `strings*.xml`, em português, inglês e espanhol.

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

## 14. Privacidade, LGPD e Google Play (regra permanente)

O ORI trata dados sensíveis (rotina e comunicação de pessoas com deficiência, inclusive crianças). Esta regra vale em
toda mudança, para que os documentos legais e o app nunca fiquem desatualizados. Controlador, contato e decisões:
[docs/legal/README.md](docs/legal/README.md).

- **Dado novo, campo novo ou coleção nova** (no Firestore, no banco local, no DataStore ou enviado a um serviço):
  atualize na mesma etapa a política de privacidade nos 3 idiomas (`web/app/legal/content.ts`), o mapa de dados
  e o formulário de Segurança de Dados ([docs/legal/seguranca-de-dados.md](docs/legal/seguranca-de-dados.md)),
  a exportação (`AccountDataExporter`) e a exclusão da conta (`AccountDeletion`), com teste de que o dado sai no
  arquivo exportado e some ao apagar a conta (e teste das regras no emulador).
- **Mudança relevante na política ou nos termos** (novo uso, nova finalidade, novo terceiro): suba
  `LEGAL_DOCUMENTS_VERSION`; o app pede o consentimento de novo. Registre a mudança no histórico de
  [docs/legal/README.md](docs/legal/README.md).
- **Consentimento:** nenhum dado de conta é gravado antes do consentimento (J2); menor de 18 só com a confirmação
  do responsável legal (J3). Retirar o consentimento é sempre possível e leva à exclusão dos dados.
- **Autonomia (J4):** quem é acompanhado vê e revoga, sem pedir a ninguém, tudo o que o responsável pode ver ou fazer.
  Nenhuma função de administração esconde algo do acompanhado ou bloqueia o aparelho.
- **Sem promessa de saúde (J8):** textos do app, da loja e da web nunca dizem que o ORI trata, diagnostica ou
  substitui terapia ou acompanhamento profissional.
- **Licenças (J9):** dependência, fonte, ícone ou imagem novos entram na tela de licenças (`OpenSourceLicenses`);
  nada de símbolos de CAA protegidos (PCS, Boardmaker) sem licença.
- **Logs e analytics:** nunca dados pessoais nem conteúdo do usuário.
- **Publicação:** antes de cada versão na loja, conferir a lista de [docs/legal/README.md](docs/legal/README.md#antes-de-publicar).
