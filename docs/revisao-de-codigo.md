# Revisão do código (03/11/2026)

Revisão de todo o repositório depois da lista [revisao-estrutura-e-design.md](revisao-estrutura-e-design.md),
feita no commit `2b2055f`. Foram lidas a camada de dados, o domínio e a injeção de dependências do
Android, os ViewModels e as telas, as regras do Firestore, as Cloud Functions, os testes de segurança
e a CI.

Este documento registra o que foi encontrado e ainda não foi corrigido. O que já foi corrigido sai da lista e vai para a seção "Corrigidas", no fim, com o teste que garante a correção.
Cada item diz onde está, o que acontece para quem usa o app e o que mudar. Os números de linha valem
para o commit acima.

**Como ler a gravidade:**

- **Alta:** o app faz algo diferente do que diz, com custo ou perda visível.
- **Média:** acontece num caso comum, ou deixa dados errados.
- **Baixa:** caso raro, ou só atrapalha quem mantém o código.

## O que está em ordem

Antes das falhas, o que a revisão procurou e **não** encontrou:

- `TODO`, `runBlocking`, `GlobalScope`, `printStackTrace` ou `Thread.sleep`;
- consulta de lista sem `limit` ou listener sem `awaitClose`;
- `Text` do Material em vez do `Text` do ORI, ou texto fixo fora do `strings.xml`;
- `collectAsState` sem ciclo de vida;
- cor escrita fora de `ui/theme`;
- exceção engolida sem motivo escrito.

As regras negam por padrão, e cada coleção tem dono e limites.

## 1. Falhas de comportamento

### R8. No limite de fotos, trocar uma foto existente é recusado (baixa)

- **Onde:** `data/photos/FirestoreCloudPhotoRepository.kt:74` (`isAtPhotoLimit`).
- **O que acontece:**
  - Com 100 fotos, a foto do perfil não pode ser trocada, mesmo sem aumentar o número de fotos.
  - Além disso, a contagem roda fora de `runDataOperation`: uma exceção que não seja do Firestore escaparia.
- **Correção:** não contar quando o documento já existe; colocar a contagem dentro de `runDataOperation`.

### R9. A foto antiga de um símbolo some antes da confirmação (baixa)

- **Onde:** `ui/app/screens/aac/SymbolEditorViewModel.kt:176`.
- **O que acontece:**
  - Ao trocar a foto, a antiga é apagada assim que a gravação é aceita no aparelho, mesmo pendente.
  - Se o servidor recusar a edição depois, o símbolo volta apontando para uma foto que já foi apagada.
- **Correção:** apagar a foto antiga só com `WriteStatus.SYNCED`; com `PENDING_SYNC`, deixar para a próxima vez que o símbolo for lido do servidor.

### R10. Desligar a repetição sem conseguir ler as séries (baixa)

- **Onde:** `domain/routine/RepeatingRoutineRepository.kt:112`.
- **O que acontece:**
  - Se as séries não puderam ser lidas e a pessoa desligou a repetição, o app supõe que era uma atividade comum.
  - Ele então tenta editar um documento que não existe. Sem conexão, o erro só aparece mais tarde.
- **Correção:** sem a leitura das séries, recusar a gravação com o erro da leitura, como já é feito quando a repetição está ligada.

### R11. Passagem para a conta: comparações diferentes das do formulário (baixa)

- **Onde:** `domain/local/MoveLocalDataToAccount.kt:98` e `:190`.
- **O que acontece:**
  - Uma tarefa só é considerada repetida com o título idêntico, enquanto o formulário ignora maiúsculas e espaços. Isso pode criar duplicatas.
  - As frases salvas da conta são lidas por `observeSavedMessages().first()`, que não diz se a resposta veio do cache, ao contrário da leitura das pranchas.
- **Correção:** usar a mesma comparação do formulário (`isDuplicateOf`); ler as frases com uma leitura que informe a origem.

### R12. Falhas do banco local que não aparecem (baixa)

- **Onde:**
  - `ui/app/session/SessionViewModel.kt:131` (`eraseLocalData`);
  - `data/local/RoomLocalDataStore.kt:61` (`countBetween`).
- **O que acontece:**
  - Apagar os dados do aparelho não passa por `runLocalWrite`: um erro do banco fecharia o app.
  - A contagem de comunicação devolve 0 quando falha, e o relatório mostra "0 vezes" como se fosse verdade.
- **Correção:** usar `runLocalWrite` e mostrar a falha; devolver `DataResult` na contagem.

### R13. O cadastro falha por causa de um nome que o app não usa (baixa)

- **Onde:** `data/auth/FirebaseAuthRepository.kt:58`.
- **O que acontece:**
  - Depois de criar a conta, o app grava o nome também no Firebase Auth. Se só isso falhar, o cadastro inteiro aparece como erro, mas a conta já existe e está conectada.
  - O app usa o nome do perfil no Firestore. O do Auth só preenche "Complete seu perfil".
- **Correção:** não tratar essa falha como erro do cadastro (registrar no log e seguir).

### R14. Criar duas vezes com o mesmo id só funciona enquanto a primeira gravação está pendente (baixa)

- **Onde:**
  - `domain/routine/RoutineRepository.kt` (`newActivityId`);
  - `firestore.rules` (`routineTasks`, `update`).
- **O que acontece:**
  - O contrato promete que criar duas vezes com o mesmo id guarda uma atividade só.
  - Isso vale enquanto a primeira gravação ainda está na fila do aparelho.
  - Depois que o servidor confirmou, um segundo `set()` vira uma edição, e as regras o recusam, porque `createdAt` mudaria.
  - As atividades repetidas já tratam isso: uma série que já existe é atualizada (correção do R3).
  - Nas atividades comuns, hoje só a passagem para a conta poderia repetir uma criação, e ela compara o título antes de criar.
- **Correção:**
  - Corrigir o texto do contrato.
  - Se um dia houver outra repetição de criação, ler o documento antes, ou gravar com `merge` apenas os campos editáveis quando ele já existe.

## 2. Desempenho e custo

| Id | Onde | O que acontece | Correção |
|---|---|---|---|
| P1 | `aac/AacBoardContent.kt:82` e `aac/AacOrganizeViewModel.kt:86` | `fetchMissing` roda a cada atualização da lista de símbolos, inclusive as de metadados. Uma foto que a conta não tem (por exemplo, acima do limite, em outro aparelho) custa uma leitura de novo a cada vez | Lembrar, enquanto a tela estiver aberta, quais fotos já foram procuradas |
| P2 | `aac/AacSymbolUiModel.kt:32` | `photoStore.pathOf` consulta o disco dentro do `combine` do ViewModel, que roda na thread principal, para cada símbolo e cada pedaço da frase | Resolver o caminho em `Dispatchers.IO` (por exemplo, `flowOn` no fluxo dos símbolos) ou deixar o `StoredPhotoImage` resolver |
| P3 | `ui/app/screens/aac/AacRoute.kt` e demais `hiltViewModel(key = …)` | Os ViewModels vivem enquanto a Activity vive. Cada pessoa acompanhada aberta na Comunicação mantém um motor de voz (`TextToSpeech`) até o app fechar | Fechar a voz quando a tela sai (`DisposableEffect`) e abrir de novo ao voltar, ou usar um motor compartilhado |
| P4 | `data/routine/FirestoreRoutineRepository.kt:30` | `getDays` corta em 31 páginas sem avisar ninguém | Registrar no log quando o corte acontecer |

## 3. Gambiarras e código que engana quem lê — corrigidas em 09/11/2026

- **G1:** `TaskStatus` foi para `domain/routine` (o pacote `domain/model` deixou de existir), com o comentário certo.
- **G2:** o comentário solto já não existia.
- **G3:** `DeviceProfilePhotoStore.pathFor`, `RoutedAacBoardRepository.deleteBoard` e `RoomLocalDataStore.countBetween`
  reescritos (este agora importa `DataResult` e diz por que uma leitura que falha conta zero).
- **G4:** os dois repositórios da rotina usam o `observeList` comum (com `mapList` para o tipo de cada um) e a mesma
  `routineTasksOf`; `enumOrDefault` está num lugar só (`data/preferences/EnumPreferences.kt`).
- **G5:** `MainActivity` usa `LaunchedEffect(isDark)` em vez de um `DisposableEffect` com `onDispose {}` vazio.
- **G6:** já resolvido pela revisão das cores (`areaAccent`).
- **G7:** os comentários de `AacAccess.canUseDevicePhotos` e das regras dizem o motivo real: a foto vai para a conta da
  dona, cujo espaço e limite são dela.
- **G8:** `archived` fica, documentado como reservado em `TaskStatus` e nas regras: nada arquiva hoje, mas o app esconde
  uma atividade arquivada. Removê-lo mudaria dados guardados e as regras sem ganho agora.
- **G9:** `rules.test.mjs` virou `profiles`, `routine`, `photos` e `careLinks.test.mjs`, com a preparação comum em
  `test/support/rulesFixture.mjs`. Os 101 testes continuam passando.

## 4. Acessibilidade — corrigida em 09/11/2026

- **A1:** o botão de mostrar a senha tem 48 dp (`OriDimensions.MinTouchTarget`). O teste mostrou que tirar os 36 dp não
  bastava: o tamanho padrão do botão é 40 dp nos limites que o leitor de tela vê. Teste: `PasswordTextFieldTest`.

## 5. Servidor e segurança

| Id | Onde | O problema | Correção |
|---|---|---|---|
| S1 | Convites (`firestore.rules`, `invitations`) | Sem App Check. Desde 15/11/2026, cada pessoa tem um código por vez e espera 30 s para gerar outro (regra com teste), e adivinhar um código continua impraticável (32⁸ combinações, 24 h de validade) | App Check no Firestore |
| S2 | `invitations/{code}` | Convites usados ou vencidos ficam no banco (um código novo já apaga o anterior da mesma pessoa) | Política de TTL do Firestore no campo `expiresAt`, no console (passo em [perfis-e-permissoes.md](funcionalidades/perfis-e-permissoes.md#como-publicar)) |
| S3 | `careInvitations.ts:222` | Os nomes guardados no vínculo são copiados quando ele é criado e não acompanham uma troca de nome | Documentar (feito em [perfil-e-conta.md](funcionalidades/perfil-e-conta.md)) ou atualizar numa Cloud Function |
| S4 | Projeto `oriteam-ori` | Há um índice `routineTasks (status, scheduledTime)` publicado que não está em `firestore.indexes.json` e que o app não usa. Ele só encarece as gravações | Apagar no console, ou publicar o arquivo com `--force` |

## 6. Testes que faltavam — escritos em 09/11/2026

`ReportsViewModelTest` (o período numa leitura, vazio, erro e nova tentativa, troca de período, reler ao voltar),
`CareLinksViewModelTest` (lista, vazio, erro, encerrar só com confirmação, só o acompanhado muda permissões, pendente),
`InvitationViewModelTest` (gerar, serviço ausente, código limpo e curto, a mensagem de cada recusa),
`ProfileSetupViewModelTest`, `ProfilePhotoViewModelTest`, `AacPreferencesViewModelTest` e `HomeViewModelTest`. As
preferências gerais já tinham `PreferencesViewModelTest`.

## 7. Ordem sugerida

1. **P1**: custo de leitura, que cresce com o uso.
2. **R5**: erro mostrado depois de uma exclusão que funcionou.
3. **A1**, **R6**, **R7**, **R12**.
4. As gambiarras G1–G7, que são rápidas e deixam o código honesto.
5. **S1** e **S2**, junto com a publicação das Cloud Functions (plano Blaze).

## 8. Desempenho da interface e das animações (07/11/2026)

As animações foram percebidas como "travadas" no tablet. A primeira medição, num build de depuração, foi:

| Medida | Antes (build de depuração) |
|---|---|
| Quadros desenhados | 1484 |
| Quadros com atraso | 75 (5,05%) |
| Tempo do quadro: 50% / 90% / 95% / 99% | 9 / 18 / 23 / 200 ms |
| Quadros lentos na thread principal | 68 |
| GPU: 99% dos quadros | 16 ms |

Tablet Samsung SM-X610, deitado, a 60 Hz, `adb shell dumpsys gfxinfo com.oriteam.ori`. A placa de vídeo dava
conta; o gargalo era a thread principal. A maior parte do que foi encontrado já foi corrigida (veja
"Corrigidas"). Depois das correções, no build `profiling`, com os contadores zerados e uso livre da pessoa no tablet:

| Medida | Antes (depuração) | Depois (`profiling`) |
|---|---|---|
| Quadros desenhados | 1484 | 3062 |
| Quadros com atraso | 5,05% | 2,55% |
| Tempo do quadro: 50% / 90% / 95% / 99% | 9 / 18 / 23 / 200 ms | 9 / 11 / 15 / 23 ms |
| Quadros lentos na thread principal | 68 (4,6%) | 58 (1,9%) |
| Quadros de 50 ms ou mais | cerca de 30, até 550 ms | 3, até 97 ms |

**Limite da comparação:** o uso não foi um roteiro igual nas duas medições, e a primeira foi num build de
depuração. Mesmo assim, a cauda (99% dos quadros em 23 ms, em vez de 200 ms) mostra a melhora que se percebe.

Ficaram em aberto:

### M2. A tela nova ainda é montada inteira no primeiro quadro da transição (média, parcial)

- **O que já mudou:**
  - o build de medição otimizado (M1) e o código mais leve em cada quadro (M8 a M13);
  - menos `AnimatedContent` aninhados (o resumo do Início não tem mais o seu);
  - o conteúdo novo começa a aparecer sem espera (M6).
- **O que falta:** se a medição no build `profiling` ainda mostrar quadros longos ao trocar de área, preparar a tela nova antes de animar, ou não animar quando o primeiro quadro passar do orçamento.

### M7. Avisos e o calendário ainda animam a altura (baixa, parcial)

- **O que já mudou:** o resumo do Início não anima mais a altura.
- **O que ficou:**
  - os avisos que vêm e vão (`OriTransientNotice`) abrem o próprio espaço. São raros (sem conexão, alterações pendentes), e um salto sem animação seria pior para a leitura;
  - a abertura do mês (`animateContentSize`) também continua.
- **Se a medição apontar estes dois:** avisos por cima do conteúdo, e o mês aberto com recorte em vez de altura.

### M10. O mapeamento nos ViewModels ainda roda na thread principal (média, parcial)

- **O que já mudou:**
  - o Firestore entrega os resultados numa thread de fundo própria, então os documentos viram modelos fora da thread que desenha;
  - o caminho de uma foto conhecida não consulta mais o disco.
- **O que falta:** `merged`, `forDay` e `toUiModel` rodam no `viewModelScope`. Movê-los para `Dispatchers.Default` exige injetar o dispatcher nos ViewModels (para os testes continuarem determinísticos).

### M14. Fontes baixadas no primeiro uso (baixa, não corrigida)

- **Onde:** `ui/theme/Type.kt` (Nunito pelo provedor do Google Fonts).
- **O que acontece:** na primeira abertura, a fonte chega depois do primeiro desenho, e os textos são medidos de novo.
- **Correção:** declarar as fontes como pré-carregadas (`preloaded_fonts` no manifesto) ou incluir os arquivos no app.

## 9. Bugs, falhas visuais e de design (07/11/2026)

Percorrido no tablet SM-X610 deitado (tema escuro, sem conta, build `prodProfiling`) e lido no código das
telas. Os números de linha valem para o código atual, ainda não commitado. Cada item diz como foi confirmado:

- **no aparelho:** visto na tela;
- **no código:** lido no código, não reproduzido;
- **possível:** depende de algo que não foi testado.

Todos os itens desta seção (B3 a B6, V1 a V8 e D1 a D13) foram corrigidos em 07/11/2026; veja "Corrigidas".

### O que foi e o que não foi verificado

- **No aparelho:** tablet SM-X610 deitado, tema escuro, sem conta, em Frases, Montar, preferências da CAA, Rotina, mês, formulário, Relatórios e Perfil.
- **No código:** as telas de Frases, Rotina, Tarefas, Início, Relatórios, Perfil e Configurações.
- **Não verificado:**
  - Configurações e Tarefas no aparelho (o tablet bloqueou no meio do percurso);
  - o celular, que não estava conectado;
  - texto ampliado e leitor de tela no aparelho;
  - tema claro.

## 10. Segunda varredura: idiomas, crescimento e melhorias (07/11/2026)

Feita depois da correção de B1 e B2, com foco no que impede o app de crescer, principalmente em
**inglês e espanhol**. Os números de linha valem para o código atual, ainda não commitado.

**Resumo para quem vai traduzir:** os textos já estão todos no `strings.xml`. Desde a correção de L1 e L2,
as datas, os nomes dos dias e a voz seguem o idioma dos textos (veja [idiomas.md](idiomas.md)). Ainda presos
ao português: a semana (L5), a gramática das frases da CAA (L3), o idioma das pranchas (L4) e o horário de
24 horas (L6). Os quatro foram corrigidos depois (veja "Corrigidas"), e em 08/11/2026 o app passou a ter inglês e
espanhol, com a gramática da CAA de cada um (veja [idiomas.md](idiomas.md)).

### Crescimento (C)

#### C1. Cada pessoa aberta deixa ViewModels vivos até o app fechar (média) — no código

- **Onde:** os `hiltViewModel(key = …)` por pessoa (`routine-$ownerUid`, `today-…`, `tasks-…`, os da Comunicação e dos formulários). Amplia o P3, que só falava da voz.
- **O que acontece:**
  - As chaves ficam no `ViewModelStore` da Activity. Quem acompanha muitas pessoas acumula um conjunto por pessoa visitada: estado, cópias das listas e, na Comunicação, um motor de voz.
  - As leituras param quando a tela sai, mas a memória só volta quando o app fecha.
- **Correção:** um `ViewModelStoreOwner` por pessoa, limpo ao sair dela, ou navegação por `NavBackStackEntry`, em que os ViewModels morrem com a tela.

#### C2. "Hoje" depende do fuso de cada aparelho (baixa) — no código

- **Onde:** `RoutineViewModel.today()` (218), `ReportsViewModel` (76) e `AacCommunicateViewModel.recordCommunication` (229): todos usam `LocalDate.now(clock)`.
- **O que acontece:** as atividades são gravadas por data, sem fuso. Quem acompanha de outro fuso vê o "hoje" do próprio aparelho: Tarefas e o Início viram o dia em horas diferentes para cada pessoa.
- **Correção:** guardar o fuso da pessoa acompanhada no perfil e calcular o "hoje" dela por ele.

### Feito nesta etapa

- **Início:** a saudação usa só o primeiro nome ("Olá, Ana!", em vez de "Olá, Ana Maria da Silva!"). Sem nome, continua "Olá!". O primeiro nome é `UserProfile.firstName`, e os testes são `UserProfileTest` e `TodayScreenTest`.

### O que ficou aberto, e por quê

L3, L5 a L7, L9, C3, B7 e V9 foram corrigidos em 07/11/2026, e L4 em 08/11/2026 (veja "Corrigidas").
Ficaram dois, que mudam dados guardados ou a arquitetura e pedem uma decisão antes:

- **C2** acrescenta o fuso ao perfil: muda as regras de `users/{uid}` e a forma de calcular "hoje" para quem
  acompanha.
- **C1** muda onde os ViewModels de cada pessoa vivem (um dono por pessoa, limpo ao sair dela). Mal feito, perde o
  estado na rotação ou relê dados a cada troca de aba.

## Corrigidas

### R1. Relatórios e marcas do mês faziam uma consulta por dia (alta) — corrigida em 03/11/2026

- **O que acontecia:**
  - O `RoutedRoutineRepository` não encaminhava `getDays`, então rodava a implementação padrão da interface: um `getDay` por dia.
  - A consulta por intervalo de `FirestoreRoutineRepository.getDays` nunca rodava no app.
  - Um relatório do mês fazia até 31 consultas; o mês da Rotina, uma para cada dia mostrado.
- **Correção:**
  - O `RoutedRoutineRepository` agora encaminha `getDays`, `setDone` e `delete`, os três membros com implementação padrão.
  - Os quatro `Routed*` recebem os contratos no construtor principal. O Hilt usa um construtor secundário com as classes concretas.
- **Testes:**
  - `RoutedRepositoriesTest` chama cada membro de cada contrato, com conta e sem conta, e confere que a implementação certa recebe aquele mesmo membro. A conferência é feita por `assertForwardsEveryMember`, em `src/test/.../testing/ForwardingCheck.kt`.
  - Sem a linha do `getDays`, o teste da rotina falha.
  - `SharedRoutineReadingsTest` confere o mesmo para as leituras compartilhadas, que encaminham com `by`.
  - No emulador, "a caregiver reads a period only with the active filter" confere que as regras aceitam a consulta por intervalo de quem acompanha, que agora roda de verdade.
- **Não verificado:**
  - Não foi conferido no aparelho.
  - Que o índice `routineTasks (status, date)` está pronto no projeto também não foi conferido; sem ele, só as marcas do mês de quem acompanha deixam de aparecer.

### R2. Tocar noutro dia do mês lia o mês inteiro de novo (média, custo) — corrigida em 03/11/2026

- **O que acontecia:** a releitura do mês dependia dos ids das atividades do dia escolhido. Trocar de dia trocava esses ids, então cada toque num dia do mês aberto lia o mês todo de novo.
- **Correção:**
  - `RoutineViewModel` conta as mudanças do dia escolhido (`DayEdits`). Uma mudança só conta quando a data continua a mesma e os ids mudam.
  - O dia ainda carregando não conta.
  - O mês é lido de novo quando outro mês aparece, numa nova tentativa, ou quando uma atividade entra ou sai do dia escolhido.
- **Testes:**
  - Em `RoutineViewModelTest`, "choosing another day of the open month does not read the month again": antes da correção, foram 3 leituras; agora é 1.
  - "an activity added to the chosen day reads the open month again" garante que a releitura necessária continua.
  - O fake da rotina agora registra cada leitura de período (`periodReads`) e pode dar a cada dia as suas atividades (`routineByDay`).
- **Não verificado:** no aparelho.

### R3. Uma atividade repetida acima do limite travava a passagem para a conta (média) — corrigida em 03/11/2026

- **O que acontecia:** com a conta já cheia (50 atividades repetidas), a criação devolvia `PRECONDITION_FAILED`. A passagem tratava isso como falta de conexão e parava em "aguardando conexão" para sempre, prendendo no aparelho também o que vinha depois.
- **Correção:**
  - Em `MoveLocalDataToAccount`, uma série que não cabe fica no aparelho, conta como "alguns itens não couberam" e a passagem continua.
  - Os dias marcados nessa série também ficam no aparelho; senão, a conta os mostraria como atividades soltas.
  - Durante a correção apareceu um segundo problema: repetir a criação de uma série que a conta já tinha recebido seria recusado pelas regras (a criação mudaria `createdAt`). Agora `RepeatingRoutineRepository.createActivity` atualiza a série que já existe, mesmo com a conta no limite.
- **Testes:**
  - Em `MoveLocalDataToAccountTest`, "repeating activities over the limit of the account stay here with their days, and the rest moves": antes, terminava em `WAITING_FOR_CONNECTION`.
  - Em `RepeatingRoutineRepositoryTest`, "creating again a repeating activity that is already there updates it, even at the limit": antes, era recusado.
- **Não verificado:** no aparelho, nem com uma conta real cheia.

### R4. Excluir "todos os dias" deixava dias soltos (média-baixa) — corrigida em 07/11/2026

- **O que acontecia:** um dia marcado e depois desmarcado tem documento próprio, com `isDone = false`. Depois de excluir a série inteira, esse dia aparecia como uma atividade comum, não concluída, no dia dele.
- **Correção:**
  - Em `RepeatingRoutineRepository.merged`, que o dia, a semana, o mês e os relatórios usam, um documento de dia cuja série não existe mais só aparece se estiver concluído (histórico).
  - Os outros continuam guardados, mas não aparecem; nenhuma gravação nova é feita, então também funciona sem conexão e em todos os aparelhos.
  - O mesmo vale para quem acompanha, quando a série foi escondida dele depois de um dia ter sido marcado.
- **Teste:** em `RepeatingRoutineRepositoryTest`, "after a series is deleted only its days marked as done stay, as history", no dia e na leitura de vários dias. Sem a correção, o dia desmarcado voltava.
- **Não verificado:** no aparelho.
- **Limitação:** os documentos desses dias continuam no banco. Ocupam pouco, mas contam no limite de 100 atividades por dia.

### R5, R6 e R7. Vínculo e comunicação — corrigidas em 15/11/2026

As regras de R5 e R6 foram publicadas no `oriteam-ori` em 15/11/2026.

| Item | O que acontecia | Correção | Teste |
|---|---|---|---|
| R5 | Ao excluir "todos os dias" de uma série, um dia marcado por quem acompanha (autor do documento do dia) não podia ser apagado pela dona: a série sumia e a tela mostrava erro | Regra `isDayOfOwnSeries`: a dona apaga o dia de uma série dela, enquanto a série existe. O app apaga o dia **antes** da série; se o dia for recusado, a série fica | `routine.test.mjs` ("the owner removes a day a caregiver marked…", falha sem a regra e sem a conferência do autor da série); `RepeatingRoutineRepositoryTest` ("deleting all days removes the day's document before the series…", falha com a ordem antiga) |
| R6 | Quem acompanha apagava símbolos, mas as fotos deles ficavam na conta da pessoa, contando no limite de 100 | Quem tem `manageCommunication` apaga fotos de símbolos (nunca a do perfil), e o app apaga a foto junto com o símbolo ou a prancha | `photos.test.mjs` (apaga a do símbolo, não a do perfil; sem permissão, não apaga); `AacOrganizeViewModelTest` ("a caregiver with permission removes the photo of a symbol they delete…") |
| R7 | Sem conexão, apagar uma prancha lia os símbolos do cache e podia deixar no servidor os que este aparelho nunca leu | A lista dos símbolos vem do servidor (`Source.SERVER`); sem conexão, a exclusão para com "Sem conexão" | Sem teste automático: depende do Firestore real. Conferido no código |

### Desempenho das animações (M1, M3–M6, M8, M9, M11–M13) — corrigidas em 07/11/2026

| Id | O que foi feito | Como foi conferido |
|---|---|---|
| M1 | R8 ligado no release (`isMinifyEnabled`, `isShrinkResources`); perfil de baseline em `app/src/main/baseline-prof.txt` com todo o código do ORI (o `profileinstaller` já vinha pelo Compose); build `profiling` (`./gradlew assembleProdProfiling`), o release assinado com a chave de depuração | O build `profiling` compila e instala (3,8 MB, contra 20 MB do de depuração). A medição de fluidez ainda falta |
| M3 | `loadingAfter`: ao trocar de dia ou de prancha, a lista anterior fica até a nova chegar, e "Carregando" só aparece depois de 300 ms. A data muda na hora. O detalhe aberto espera o novo dia em vez de dizer "não encontrada" | `RoutineViewModelTest`: "another day replaces the list without passing through Carregando" (com leitura de 40 ms) e "a day that takes long says Carregando after a moment". Os dois falham se o "Carregando imediato" voltar. `RoutineScreenLayoutTest` foi ajustado para a espera |
| M4 | A linha do tempo e a grade de símbolos ganharam um `key` do dia ou da prancha (`contentDate`, `symbolsKey`): outra lista aparece de uma vez, sem esmaecer item por item | Testes de interface existentes; o efeito visual só no aparelho |
| M5 | Molas do Material: `playful` com amortecimento 0,6 e rigidez 800; itens de lista com 0,9 e 700 | `OriThemeRulesTest` |
| M6 | O conteúdo novo começa a aparecer sem espera (subtelas, áreas, troca de estado); durações menores (300 ms nas subtelas). Os cartões do Início só chegam animados na primeira visita, e cada área guarda o próprio estado entre as abas (`rememberSaveableStateHolder`) | `MotionLevelsTest` |
| M8 | Valores animados lidos no desenho: o ✓ e o seu círculo (`drawBehind`), o "Falar" respirando, o fundo das linhas e das escolhas | Testes de interface existentes |
| M9 | `Modifier.pressScale` e `Modifier.entrance` com `Modifier.Node` (animam no próprio nó, sem recompor); `itemAnimation` virou extensão de `Modifier`. Os 4 avisos do Lint sumiram | Lint |
| M11 | A barrinha colorida do cartão da Rotina é desenhada (`drawBehind`), sem `IntrinsicSize.Min` | Imagem renderizada do cartão |
| M12 | O gráfico monta a linha uma vez por tamanho e dados (`drawWithCache`) e, a cada quadro, só corta o trecho | Testes de interface existentes |
| M13 | A foto esmaece sozinha (`graphicsLayer`), sem compor a foto e o ícone ao mesmo tempo | Testes existentes |

### Botão "Adicionar atividade" sob a barra de tarefas do tablet — corrigido em 07/11/2026

- **O que acontecia:** com trilho ou gaveta de navegação (tablets), a área principal não se afastava da barra de tarefas embaixo, e o botão ficava metade embaixo dela.
- **Correção:** `MainScaffold` aplica também a margem de baixo de `WindowInsets.safeDrawing` quando a navegação não é a barra de baixo. O teclado não é contado duas vezes, porque a margem aplicada por fora é consumida.
- **Conferido:** no tablet SM-X610, com uma captura da tela da Rotina. Não há teste automatizado, porque o Robolectric não simula a barra de tarefas.

### B1. O primeiro toque numa frase pronta mostrava letras grandes em vez de falar (alta) — corrigida em 07/11/2026

- **O que acontecia:** o motor de voz leva um momento para ligar quando Frases abre (`PREPARING`). Nesse tempo `speak` devolvia `false`, e a tela entendia "sem voz": abria as letras grandes com "Fechar". No modo "montar com retorno", a palavra tocada não era dita, e em Configurações → Voz o exemplo não tocava.
- **Correção:** `SpeechWhenReady` (`ui/app/screens/aac`). Um toque com a voz ligando espera por ela e é falado assim que ela fica pronta; só o último toque espera. As letras grandes aparecem só sem voz, ou se ela não ligar em 3 segundos. Usado por `AacCommunicateViewModel` (Falar, frases, retorno de cada símbolo) e `VoiceSettingsViewModel` (exemplo).
- **Testes:**
  - `AacCommunicateViewModelTest`: "a phrase tapped while the voice is starting is said when it starts…" e "a voice that does not start in time, or cannot start, gives way to the large letters";
  - `VoiceSettingsViewModelTest`: "a choice made while the voice is still starting is heard when it starts".
  - O primeiro e o último falham com o comportamento antigo.
- **Não verificado:** o som no aparelho; o tablet estava bloqueado.

### B2. Um rascunho de formulário abandonado com o voltar do sistema reaparecia (média) — corrigida em 07/11/2026

- **O que acontecia:** só "Cancelar" e "Salvar" limpavam o rascunho, e o ViewModel do formulário vive mais que o formulário. Fechado pelo voltar do sistema, o rascunho voltava na próxima "Adicionar atividade", com o texto e **o dia** antigos: em Tarefas, a atividade podia ser criada num dia que já passou. Ao editar a mesma atividade de novo, apareciam as mudanças abandonadas, e salvar sobrescrevia o que estava gravado. O formulário de símbolos fazia o mesmo e deixava no aparelho as fotos importadas.
- **Correção:** `rememberFormOpening` (`ui/components/form/FormOpening.kt`) dá um nome a cada abertura, guardado com `rememberSaveable`. O `start` de `ActivityEditorViewModel` e de `SymbolEditorViewModel` mantém só o rascunho da mesma abertura (rotação, app fechado pelo sistema, visita a outra área) e descarta o resto. No caso dos símbolos, descarta também as fotos importadas, como "Cancelar".
- **Testes:**
  - `ActivityEditorViewModelTest`: "a form left with the system back opens again empty, on the day chosen now" e "editing again shows what is saved, not the changes left with the system back";
  - `SymbolEditorViewModelTest`: "a form left with the system back opens again empty, and its imported photo is removed";
  - `FormOpeningTest`: o nome se mantém ao recriar a tela e muda ao abrir de novo.
  - Todos falham com o comportamento antigo.
- **Limite:** se um rascunho antigo existir, ele pode aparecer por um quadro antes de o `start` o trocar, durante a entrada animada do formulário.

### L1, L2 e L8. Datas e voz presas ao português — corrigidas em 07/11/2026

- **O que acontecia:**
  - o idioma das datas, dos dias e da voz era uma constante (`pt-BR`), e não o idioma dos textos;
  - os formatos de data eram escritos à mão (`"EEEE, d 'de' MMMM"`, `"dd/MM"`);
  - as vozes eram filtradas também pelo país.
  - Com uma tradução, as telas sairiam meio traduzidas, e quem usa `es-ES` poderia ficar sem voz num aparelho que só tem `es-US`.
- **Correção:**
  - `appLocaleFor` escolhe o idioma como o Android escolhe os textos, mantendo a região do aparelho;
  - o `MainActivity` o fornece às telas por `LocalAppLocale`;
  - as datas usam esqueletos (`rememberDateFormat`, `DateSkeletons`);
  - a voz usa o mesmo idioma, primeiro as vozes da região do aparelho;
  - os idiomas aparecem no Android 13+ (`res/xml/locales_config.xml`).
  - Detalhes em [idiomas.md](idiomas.md).
- **Testes:**
  - `AppLanguagesTest`: escolha do idioma; pastas, lista e `locales_config.xml` batem;
  - `AppDatesTest`: a mesma data em português, inglês e espanhol. Os 4 testes de data falham com um padrão escrito à mão.
- **Mudança visível em português:** "Novembro de 2026" no título do calendário (era "Novembro 2026") e "04/11" no resumo da repetição (era "4/11").
- **Limite:** hoje só há português, então para quem usa o app nada muda além disso. Não foi conferido no aparelho em outro idioma.

### Segunda rodada: bugs, design, idiomas e privacidade — corrigidos em 07/11/2026

| Item | O que mudou | Teste |
|---|---|---|
| B3 | O voltar do sistema fecha o detalhe aberto ao lado da lista, também em duas colunas | `RoutineScreenLayoutTest` |
| B4 | "A seguir" é a próxima atividade de verdade; as de mais cedo não feitas são contadas à parte | `TodayScreenTest` |
| B5 | Uma mensagem que o motor aceitou e não disse aparece em letras grandes (`SpeechSynthesizer.failures`) | `AacCommunicateViewModelTest` |
| B6 | Em "Pendentes", a tarefa marcada fica alguns segundos, para desfazer | `TasksScreenTest`, `TasksAndWeekTest` |
| B7 | Uma gravação atrasada não mexe no formulário aberto depois | `ActivityEditorViewModelTest` |
| V1 | Rótulos dos números em `plurals` | `ReportTilesTest` |
| V2, V3 | Gráfico por data, sem ligar dias vazios, com 0/50/100%, pontos e linha juntos | `DailyChartTest` |
| V4 | Toque redondo no dia | Imagem a 360 dp |
| V5 | Espaço do botão flutuante medido (`ActionPillLayout`), também no estado vazio | Imagem a 360 dp |
| V6 | "Parar" flutua sobre a grade | Imagem a 360 dp |
| V7 | Frases prontas deslizam quando as salvas chegam | — (animação) |
| V8 | O cartão inteiro abre a atividade | Testes da Rotina existentes |
| V9 | A área que sai some em 50 ms | — (duração da animação) |
| D1 | Letras grandes no centro, maiores em telas largas; "Fechar" com largura de formulário | Não renderizado (é um diálogo) |
| D2 | Ações da CAA no centro; "Falar" desabilitado legível; estado sem pranchas centralizado | Imagem no tablet (1280 dp) |
| D3 | Vozes numa lista que abre ao tocar | `VoiceSettingsSectionTest` |
| D4, D5 | Um "Aa" só em duas colunas; botão flutuante some com o formulário aberto; "Dia da atividade"; "Opcional, ex.: 08:30"; dias com três letras; "Ver o mês inteiro" visível | `RoutineScreenLayoutTest`, `WeekSwipeAndRepeatTest`, imagem a 360 dp |
| D6 | "Nada neste dia" e a ação com o nome do botão | Imagem a 360 dp |
| D7 | A sexta semana toda do mês seguinte fica vazia | `WeekSwipeAndRepeatTest` |
| D8 | Nota da contagem alinhada | Imagem a 360 dp |
| D9 | Título com a largura e o centro do conteúdo | Não conferido no tablet |
| D10 | Um caminho só para editar o perfil | — |
| D11 | Linhas que não são área ficam neutras | Imagem a 360 dp |
| D12 | Rail em vez de gaveta | `WindowLayoutTest` |
| D13 | "Apagar" na confirmação; "Sobre" rola | — |
| L3 | Gramática por idioma (`SentenceGrammar`) | `SentenceGrammarTest` |
| L5 | Semana da tela pelo idioma; semana das repetições fixa | `WeekStartTest` |
| L6 | Hora no formato do idioma e do aparelho; campo aceita AM/PM e "7h30" | `RoutineDomainTest`, `TodayScreenTest` |
| L7 | Textos separados por área; formatos não traduzíveis; botões citados por parâmetro; pseudo-idiomas na depuração | Conferência das 485 entradas, lint |
| L9 | `lang="pt-BR"` na web | `npm run lint`, `npm run build` |
| C3 | Sair da conta apaga a cópia do perfil; o cache do Firestore fica (documentado) | `SessionViewModelTest` |

**Conferido no tablet SM-X610** (deitado, tema escuro, sem conta, build `prodProfiling`), em 07/11/2026:

- **Rotina:** um "Aa" só nas duas colunas; "Ver o mês inteiro" e "Voltar para a semana" escritos; a sexta
  semana de dezembro vazia; toque redondo no dia; "Nada neste dia" acima do botão flutuante; o botão some com o
  formulário aberto; "Dia da atividade", "Opcional, ex.: 08:30" e os dias com três letras; o voltar do
  sistema fecha o formulário sem sair da Rotina (B3); aberto de novo noutro dia, o formulário vem vazio e no dia
  escolhido (B2); tocar na faixa colorida abre a atividade (V8).
- **Início e Tarefas:** com a atividade das 05:00 pendente às 19h47, nenhuma "A seguir" e "1 atividade de mais
  cedo ainda não foi feita" (B4); em "Pendentes", a tarefa marcada ficou à vista e saiu alguns segundos depois
  (B6). A atividade foi marcada como pendente e concluída de novo só para isso: terminou como estava.
- **Relatórios:** "1 Atividade concluída", pontos pela data sem ligar os dias vazios, 0/50/100%, nota alinhada.
- **Frases:** com o app recém-aberto, o primeiro toque em "Sim" falou (o registro do sistema mostra a voz tocando)
  e as letras grandes não abriram (B1); "Parar" flutua sem mexer na grade; letras grandes no centro (D1);
  "Montar" sem pranchas centralizado (D2); lista de vozes recolhida, com "Em uso: Voz 3" (D3).
- **Perfil e Configurações:** um caminho só para editar o perfil; títulos alinhados com os cartões; ícones neutros;
  "Apagar" na confirmação de Privacidade (cancelada, nada foi apagado); trilho em vez da gaveta.

**Corrigido a partir do tablet:**

- o ponto de 0% do gráfico ficava em cima da data "01/11": a linha e os pontos agora ficam dentro de uma margem;
- "Parar" se esticava pela largura do tablet: agora tem a largura de um formulário, no centro;
- o "Falar" sem mensagem ficou sem fundo e mais claro que "Apagar" e "Salvar", parecendo o único ativo: agora
  mantém o fundo e tem o mesmo tom discreto (55%) das outras ações sem mensagem.

**Não conferido no aparelho:**

- o primeiro nome no Início, porque o perfil do tablet não tem nome e não foi mudado;
- a voz que falha sem internet (B5), a gravação atrasada (B7) e as frases salvas deslizando (V7), que pediriam
  mudar configurações ou criar dados;
- outro idioma, texto ampliado e o celular (não estava conectado). Esses têm testes automatizados.

### L4. O conteúdo não dizia em que idioma estava (média) — corrigida em 08/11/2026

- **O que acontecia:** pranchas e frases salvas não tinham idioma; a voz e a gramática usavam o do app. Trocado o
  idioma do aparelho, ou aberta a prancha por quem acompanha num aparelho em outro idioma, uma prancha em português
  seria lida por uma voz em inglês, e uma pessoa bilíngue não conseguia ter uma prancha em cada idioma.
- **Correção:**
  - um campo `language` (`pt-BR`, `es`…) em prancha e frase salva, no Firestore e no Room. Uma prancha nova recebe
    o idioma do app (`appLanguageTag`); uma frase salva, o da prancha em que foi montada; o que é de antes fica sem
    o campo e é lido como português (`OLD_CONTENT_LANGUAGE`): até 08/11/2026 o app só existia em português. No
    começo, o que era de antes seguia o idioma do app; com o inglês e o espanhol, isso faria uma voz em inglês ler
    uma prancha em português, e foi corrigido no mesmo dia (teste em `AacCommunicateViewModelTest`, com o app em inglês);
  - a voz (`SpeechSynthesizer.speak(texto, idioma)`), a gramática (`AacMessage.inLanguage`, `grammarFor`) e as
    cores dos símbolos seguem a prancha aberta; uma frase salva é dita no idioma dela. Sem voz nesse idioma, a
    mensagem aparece em letras grandes, em vez de ser lida por uma voz de outro idioma;
  - as regras aceitam o campo ausente ou um código de idioma (`hasValidLanguage`), publicadas em 08/11/2026;
  - o Room foi para a versão 4 (`MIGRATION_3_4`, esquema em `android/app/schemas`);
  - a passagem para a conta leva o idioma de cada prancha e frase.
  - Detalhes em [caa.md](funcionalidades/caa.md#idioma-de-cada-prancha-e-frase-08112026).
- **Testes:**
  - no emulador, em `aac.test.mjs`: "a board may record its language, as a language tag" e "may record the
    language they are in". Os dois falham com as regras de antes;
  - `AacCommunicateViewModelTest`: cinco casos (prancha em espanhol monta "Estou dor", fala e salva em `es`;
    prancha de antes em `pt-BR`; sem voz em espanhol, letras grandes; frase salva no idioma dela, pronta no do app;
    prancha sugerida em `pt-BR`);
  - `AacOrganizeViewModelTest`, `AacDocumentMappingTest`, `LocalRepositoriesTest`, `MoveLocalDataToAccountTest`
    e `OriDatabaseMigrationTest` (versão 1 até a 4, sem perder dados).
- **Conferido no tablet SM-X610** (build `prodProfiling`, sem conta), em 08/11/2026: o app abriu depois da
  atualização, com a migração do banco, sem erro no registro do sistema; Frases e "Frases salvas" abriram; "Sim"
  foi falado pela voz pt-BR; a prancha que já existia (sem idioma) manteve as cores do português e montou "eu estou
  com dor". A frase foi apagada depois, e nada foi criado.
- **Não conferido no aparelho:** uma prancha em outro idioma, porque o app só tem português e criar uma exigiria
  mudar dados; o celular.
- **Limite:** não há como escolher o idioma de uma prancha na tela; ela recebe o do app ao ser criada.

## 11. Revisão de interface: construção, não erros (09/11/2026)

Feita olhando o app no celular (modo escuro), imagens renderizadas e os wireframes (`wireframe/`). O foco foi o que
deixava a interface menos profissional sem estar errado, mantendo a identidade (cores, Nunito, ícones, cantos). Os dez
itens foram corrigidos no mesmo dia; as regras que ficaram estão em
[design-system.md](design-system.md#construção-da-interface-09112026).

### I1 a I10 — corrigidos em 09/11/2026

| Item | O que acontecia | Correção | Teste |
|---|---|---|---|
| **I1.** Interruptor desligado quase invisível (`SettingsSwitchItem`) | Trilho e borda com a mesma cor clara: 1,25:1 no tema claro e 2,1:1 no escuro, abaixo do 3:1 de um controle | Borda e bolinha na cor do texto de apoio | `OriThemeRulesTest` ("a switch that is off can be seen") |
| **I2.** Duas cores principais para o mesmo papel | O ciano (`primary`) e o teal (`strong`) lado a lado: "Escolher" ciano ao lado do dia escolhido teal, rádios ciano e abas teal | `OriAppColors`, em volta de `MainScaffold`, faz do teal o `primary` dentro do app; o ciano fica nas telas de acesso | `OriThemeRulesTest` ("inside the app the teal is the main color") |
| **I3.** "Salvar" só no fim do formulário | No celular, "Salvar" e "Cancelar" ficavam abaixo de Repetir e das opções, fora da tela | `FormActionBar`: barra fixa embaixo, no formulário de atividade e no de símbolo | `ActivityEditorScreenTest` ("save and cancel are on a small phone screen without scrolling") |
| **I4.** Dias da semana sem explicação (`RepeatPicker`) | Os círculos vinham antes das opções, sem rótulo, mesmo com "Não repete" | Opções primeiro, depois "Ou escolha os dias da semana" e os círculos | `ActivityEditorScreenTest` ("the days of the week come after the options…") |
| **I5.** O mesmo ícone "i" em toda tela vazia | O círculo com "i" lembra um aviso | Cada `EmptyState` com o ícone do assunto: calendário, ✓, balão, lista, gráfico, pessoas, cadeado | Conferido em imagem (um teste só repetiria o código) |
| **I6.** Títulos longos quebravam em duas linhas | "Configurações de acessibilidade" e "Preferências de comunicação" na barra do celular | "Acessibilidade" e "Comunicação", na barra e no menu | `PreferencesScreenTest` ("the title of every settings page fits in one line of a phone") |
| **I7.** Três nomes para a mesma área | Barra "Frases", cartão do Início "Formar frase" | "Frases" na barra, no Início e em "Abrir o app em"; "Prontas" e "Montar" seguem como abas | `PreferencesScreenTest` ("Frases") |
| **I8.** Organizar empilhava botões antes da lista | Adicionar, Acrescentar, Renomear e Excluir; a lista começava perto da metade da tela | Renomear e Excluir no menu "⋮" da barra de título; "Acrescentar" só quando falta palavra | `AacScreenTest` ("renaming and deleting a board wait in the menu of the title bar", "who cannot manage a board has no menu…") |
| **I9.** "Escrever" era um "+" sem texto | Quem vê só achava um "+", que podia ser "adicionar símbolo" | Ícone de teclado com a palavra "Escrever" embaixo, área de toque de 48 dp | `AacMessageBarTest` ("writing is a button with a full touch target") |
| **I10.** Detalhes | "Sair da conta" em ciano; dois quadros de Relatórios com a mesma cor; o cartão vazio do Início não dizia que abre a Rotina | "Sair da conta" em vermelho; "Do planejado" com a cor de Relatórios; o cartão diz "Toque para planejar o dia em "Rotina"" | `TodayScreenTest` ("an empty day says so and leads to the routine"); as cores, em imagem |

### O que foi e o que não foi verificado

- **Antes:** no celular, Rotina, formulário de atividade, Tarefas, Relatórios, Frases (Prontas e Montar), Perfil,
  Configurações e as páginas de ajuste; os wireframes de Configurações, Perfil, CAA, Rotina, Relatórios e Início.
- **Depois das correções, em imagem renderizada a 360 dp (Robolectric), nos temas claro e escuro:** o formulário de
  atividade com a barra fixa (numa tela de 740 dp e numa alta, com Repetir inteiro), Organizar com o "⋮", a barra de
  Montar com "Escrever", o Início sem atividades, o resumo de Relatórios, duas telas vazias e "Sair da conta".
- **No celular RQCX6026CSZ (build `profiling`, modo escuro, português), em 09/11/2026:** Rotina vazia (ícone de
  calendário), Início (cartão vazio com a dica, "Frases"), Perfil ("Sair da conta" vermelho), Configurações e as
  páginas Acessibilidade e Comunicação (título numa linha, rádios teal, interruptor desligado visível), o formulário
  de atividade (barra fixa no topo e no fim da rolagem; Repetir com o rótulo), Relatórios (três cores), Montar
  ("Escrever" com teclado) e Organizar (o "⋮" aberto, com "Excluir prancha" em vermelho). O formulário foi
  cancelado e o menu fechado sem tocar em nada: nenhum dado foi criado ou mudado.
- **Não visto no aparelho:** o tema claro (seria preciso mudar a escolha da pessoa), o tablet (desconectado), o texto
  ampliado e o leitor de tela.

### I11. Dois tons de teal no modo escuro — corrigido em 09/11/2026

- **Correção:** escolhas (`OriChoiceChip`), o dia escolhido (`DayCell`), os dias de Repetir e o "+ Adicionar
  atividade" usam a cor principal (`primary`/`onPrimary`), a mesma dos botões. No tema claro nada muda. Teste:
  `OneMainColorTest` (compara os pixels do chip escolhido e do botão, nos dois temas).

#### Como estava

- **Onde:** `OriDarkAppColorScheme` (`primary` #62C4BE) e `OriChoiceChip` (escolhido em `strong`, o teal profundo).
- **O que acontece:** no modo escuro, um botão cheio ("Salvar", "Adicionar símbolo") é teal claro com texto escuro, e
  uma escolha ("Não repete", a aba "Montar", o dia escolhido) é teal profundo com texto branco, lado a lado. No tema
  claro as duas são o mesmo teal. Visto no celular, no formulário de atividade.
- **Por que não é só trocar o `primary`:** o `primary` também é a cor do texto de "Escolher" e "Cancelar"; o teal
  profundo como texto sobre o fundo escuro não tem contraste.
- **Correção sugerida:** no modo escuro, a escolha também em teal claro com texto escuro (`strongOnSurface` e
  `onPrimary`), conferindo o contraste em `OriThemeRulesTest`.

## 12. Pontos de melhora pedidos (09/11/2026, abertos)

- **N1. Repetir todo mês no mesmo dia** — feito em 09/11/2026 ([rotina.md](funcionalidades/rotina.md#todo-mês-no-mesmo-dia-09112026)).
- **N2. Montar: arrastar símbolos para reordenar** — feito em 09/11/2026 (veja N6–N8 em
  [caa.md](funcionalidades/caa.md#pedidos-de-09112026-emoji-das-frases-arrastar-cor-e-tipo-de-palavra)).
- **N3. Montar: escolher a cor de um cartão** — feito em 09/11/2026, no formulário do símbolo.
- **N4. Animação detalhada do logo** — feito em 09/11/2026 ([design-system.md](design-system.md#abertura-do-logo-09112026)).
- **N5. Entrar e cadastrar com conta Google** — feito em 09/11/2026 ([autenticacao.md](funcionalidades/autenticacao.md#entrar-com-o-google-09112026)).
  Falta cadastrar a impressão do certificado de publicação antes de ir para a loja.
- **N6. Frases salvas: escolher e trocar o emoji** — feito em 09/11/2026.
- **N7. Gramática mais ampla em inglês e espanhol** — feito em 09/11/2026 (tabelas maiores; ainda é uma tabela,
  não um analisador: palavras fora dela ficam como foram tocadas).
- **N8. Classificar palavras novas para a gramática** — feito em 09/11/2026: a pessoa escolhe o "Tipo de palavra" no
  formulário do símbolo; sem escolha, valem as tabelas.
- **N9. "Gerar código de convite" não funcionava** — corrigido em 15/11/2026: os convites são feitos pelo app e
  conferidos pelas regras, sem Cloud Functions ([perfis-e-permissoes.md](funcionalidades/perfis-e-permissoes.md#5-convites-o-app-grava-as-regras-conferem)).
  Testes: `careInvitations.test.mjs` (emulador), `InvitationCodesTest` e `InvitationViewModelTest`. Regras publicadas em 15/11/2026.
- **N10. Mais administração de acompanhados** — aberto: falta escolher quais funções (veja a seção 14, J3/J4: só com
  consentimento e visível para o acompanhado).

## 13. Requisitos do modo ORI Home (launcher opcional) — implementado em 10/11/2026

Como cada requisito foi atendido, os testes e as limitações: [funcionalidades/launcher.md](funcionalidades/launcher.md).
H5 guarda o launcher anterior e volta a ele; H12 tem mover antes/depois (sem arrastar); H13 não tem pastas.

O ORI continua sendo um app comum; o launcher é opcional, a pessoa escolhe e pode desfazer.

**Registro e ativação**
- **H1. Atividade própria:** `LauncherActivity`, separada da `MainActivity`, com `intent-filter` `MAIN` + `HOME` +
  `DEFAULT`, `launchMode="singleTask"`, `stateNotNeeded`, `excludeFromRecents` e tema próprio (opção de mostrar o
  papel de parede).
- **H2. Desligada por padrão:** o componente nasce com `android:enabled="false"` e só é ligado
  (`PackageManager.setComponentEnabledSetting`) quando a pessoa ativa o modo. Assim o ORI não aparece como opção de
  tela inicial para quem não quer.
- **H3. Pedido oficial:** `RoleManager.ROLE_HOME` (`createRequestRoleIntent`, com `isRoleAvailable`/`isRoleHeld`)
  pela Activity Result API; abaixo do Android 10 ou se o pedido não estiver disponível, abrir
  `Settings.ACTION_HOME_SETTINGS`. Nunca forçar nem repetir o pedido sem a pessoa tocar.
- **H4. Resultado:** aprovado → ORI vira a tela inicial; negado/cancelado → tudo continua como antes, com aviso e
  opção de tentar depois. O estado vem sempre do sistema (`isRoleHeld`), não de uma preferência guardada.
- **H5. Desativar e voltar ao launcher anterior:** guardar o pacote do launcher anterior antes de ativar; em
  Configurações, "Voltar ao launcher anterior" abre o pedido do sistema/`ACTION_HOME_SETTINGS` e depois desliga o
  componente. Se o ORI for desinstalado ou o componente desligado, o Android volta ao padrão sozinho.
- **H6. Sem bloqueio:** sem `startLockTask`, modo quiosque, administrador do dispositivo ou serviço de acessibilidade
  para impedir a saída; Configurações e o voltar do sistema continuam acessíveis.

**Lista de apps**
- **H7. Visibilidade de pacotes:** `<queries>` com `MAIN` + `LAUNCHER` e `LauncherApps.getActivityList`; sem
  `QUERY_ALL_PACKAGES` e sem nenhuma permissão nova. Ícones com `getBadgedIcon` (perfil de trabalho).
- **H8. Apps removidos ou mudados:** `LauncherApps.Callback` (adicionado, removido, mudado, indisponível) e uma
  conferência ao abrir: favorito de um app removido some da grade sem erro; app reinstalado pode voltar.
- **H9. Desempenho:** listar e carregar ícones fora da thread principal, com cache em memória e tamanho limitado;
  abrir a tela inicial rápido mesmo com muitos apps.
- **H10. Privacidade:** a lista de apps fica só no aparelho; nunca vai para o Firestore, logs ou analytics.

**Tela e navegação**
- **H11. Favoritos:** fixar e desfixar apps; ordem salva.
- **H12. Organização personalizada:** reordenar (arrastar e, para quem não arrasta, botões mover antes/depois),
  ocultar apps da gaveta, talvez pastas simples.
- **H13. Gaveta de apps:** lista completa em ordem alfabética, com busca.
- **H14. Grade adaptativa:** colunas pela largura (`WindowLayout`), pelo texto ampliado e pela densidade; celular,
  tablet, dobrável e paisagem.
- **H15. Atalhos para Rotina e CAA:** abrem a `MainActivity` na área certa (intent explícita com a área), também
  "próxima atividade" e "Falar".
- **H16. Baixa estimulação:** menos itens, sem badges e enfeites, cores suaves, animações por `OriMotion`
  (respeitando "Animações" e a interface simplificada).
- **H17. Botões do sistema:** Home na própria tela inicial volta ao topo; voltar não sai do launcher nem fecha nada
  por engano; voltar preditivo.

**Dados e arquitetura**
- **H18. Preferências locais do aparelho:** DataStore (ou Room) do aparelho, sem conta e sem sincronizar: favoritos,
  ordem, ocultos, modo ativo, launcher anterior.
- **H19. Independência:** a `LauncherActivity` não depende de Firebase, sessão, rotina ou CAA para abrir; o atalho de
  Rotina e CAA carrega o domínio só quando tocado. `Application.onCreate` leve, sem rede.
- **H20. Componentes compartilhados:** `OriTheme`, `Text`, tokens, `OriMotion`, `WindowLayout`; sem duplicar regra
  de domínio.

**Testes, documentação e limitações**
- **H21. Testes:** ativação aprovada e negada (resultado do `RoleManager`), abertura normal do app sem launcher,
  favoritos (fixar, ordenar, app removido), reinício (preferências mantidas, `isRoleHeld` relido), layouts a 360 dp,
  tablet e texto ampliado (Robolectric com `ShadowPackageManager`/`LauncherApps` falsos), e no aparelho.
- **H22. Documentação:** `docs/funcionalidades/launcher.md` com registro no Manifest, ativação, navegação, estados,
  persistência e limitações; textos nos 3 idiomas.
- **H23. Limitações a documentar:** fabricantes que ignoram ou escondem o pedido de papel; sem widgets e sem badges
  de notificação (exigiriam permissões a mais); o Android sempre deixa a pessoa trocar de launcher; política do Google
  Play para apps que listam apps instalados (o uso como launcher é aceito).

## 14. Segurança, privacidade e riscos jurídicos — requisitos (09/11/2026, abertos)

Não é parecer jurídico: antes de publicar, revisar com advogado(a) de proteção de dados. O ORI trata dado sensível
(rotina e comunicação de pessoas com deficiência, possivelmente crianças), o que pede o cuidado mais alto da LGPD.

**Lei e documentos (J)**
- **J1. Política de privacidade e termos de uso** em pt, en e es, com link no app (cadastro e Sobre) e na loja: que
  dados, para quê, base legal, com quem (Google/Firebase), onde ficam, por quanto tempo, direitos e contato.
- **J2. Base legal de dado sensível (LGPD art. 11):** consentimento específico e destacado no cadastro para rotina,
  comunicação e fotos; registrar quando e qual versão foi aceita; permitir retirar.
- **J3. Crianças e adolescentes (LGPD art. 14):** consentimento de um responsável legal, no melhor interesse da
  criança; se houver público infantil, seguir a política de Famílias do Google Play (e COPPA/GDPR se for aos EUA/UE).
- **J4. Autonomia da pessoa com deficiência (Lei 13.146, Convenção da ONU):** adulto acompanhado decide o que o
  responsável vê e faz; permissões visíveis para ele e revogáveis por ele (já é assim nos vínculos: manter e testar).
- **J5. Direitos do titular:** ver, exportar (portabilidade) e apagar os dados pelo app; **apagar a conta** dentro do
  app e por um link na web (exigência do Google Play), apagando também fotos, pranchas, séries e vínculos (ligado a
  R6, R7 e S2).
- **J6. Encarregado (DPO) e canal de contato**; plano de resposta a incidente com aviso à ANPD e às pessoas (art. 48).
- **J7. Transferência internacional:** os dados ficam no Firebase (Google, fora do Brasil?): conferir a região do
  projeto, citar na política e aceitar os termos de tratamento de dados do Google Cloud.
- **J8. Não é dispositivo médico:** não prometer tratamento, diagnóstico ou terapia na loja e no app (evita
  enquadramento na ANVISA como software médico); aviso de que a CAA não substitui acompanhamento profissional.
- **J9. Licenças de terceiros:** tela "Licenças" em Sobre (Nunito/Nunito Sans OFL, Material Icons e ícones Apache 2.0,
  emojis do sistema, bibliotecas); não usar símbolos de CAA protegidos (PCS/Boardmaker) sem licença; frases prontas
  e imagens próprias ou livres.
- **J10. Formulário de Segurança de Dados** do Google Play fiel ao que o app faz; revisar a cada mudança.

**Segurança técnica (T)**
- **T1. App Check** no Firestore, Storage e Functions; limite de tentativas nos convites (S1).
- **T2. Regras negam por padrão**, com teste no emulador para cada mudança (já é regra); revisar acesso de quem
  acompanha a fotos e símbolos (R6).
- **T3. Dados no aparelho:** `allowBackup`/`dataExtractionRules` sem banco local, fotos e preferências sensíveis no
  backup na nuvem do Android; nada sensível em `SharedPreferences` sem necessidade.
- **T4. Fotos:** tirar EXIF (localização) antes de gravar e enviar; limite de tamanho e tipo.
- **T5. Logs e falhas:** sem nome, e-mail, texto da CAA ou rotina em log, Crashlytics ou analytics.
- **T6. Sessão:** só a persistência do SDK; sair apaga cópias locais da conta (já feito para o perfil: estender a
  rotina e a CAA em cache).
- **T7. Dependências e alvo:** atualizar bibliotecas com falha conhecida; `targetSdk` exigido pela loja; R8 ligado.
- **T8. Custos e abuso:** orçamento e alerta no Blaze; limites por usuário (já há em pranchas, fotos e frases);
  TTL de convites (S2).
- **T9. Retenção:** apagar convites vencidos, dados órfãos e contas inativas conforme a política.

**Itens da lista que viram prioridade por segurança ou lei**
- J5 depende de **R6, R7, S2** (nada pode sobrar depois de apagar).
- **S1, N9** (convites), **N5** (Google: tela de consentimento OAuth e marca "Entrar com Google" conforme as regras
  do Google), **N10** (administração de acompanhados: só com consentimento e aviso visível ao acompanhado, nunca
  monitoramento escondido; nada de bloqueio do aparelho), **H6/H10** (launcher sem bloqueio e lista de apps só no
  aparelho), **C2** (fuso: datas erradas em dado de cuidado), **R12/R13** (falhas silenciosas).

### J1 a J10 — implementados em 09/11/2026 (pendências externas abaixo)

O que o app e a web fazem está em [legal/README.md](legal/README.md), com os testes de cada item. Ficam para a equipe,
fora do código: publicar as páginas (Firebase Hosting), preencher a Play Console (política, exclusão, Segurança de
Dados, público e Famílias), aceitar os termos de tratamento de dados do Google Cloud, cadastrar o SHA do certificado
de publicação e pedir uma revisão jurídica dos textos.

## 15. ORI Home: grade e código em vários tamanhos de tela — corrigido em 10/11/2026

Revisão da grade, do arrastar, de código repetido e de processos lentos do ORI Home, em 11 tamanhos de tela
(celular em pé e deitado, dobrável, tablet nos dois sentidos, letra grande, ícones de todos os tamanhos). Cada falha
ganhou um teste que falhava antes da correção. A tabela completa, com a correção e o teste de cada uma, está em
[funcionalidades/launcher.md](funcionalidades/launcher.md#falhas-corrigidas-em-10112026).

As mais graves:

- **O soltar usava a grade da primeira montagem da tela** (alta): com ícones enormes no tablet, a casa calculada
  era a de 6 colunas, não a de 4, e o item caía noutro lugar que o mostrado. Os gestos agora leem o estado na hora
  (`rememberUpdatedState`). Teste: `HomeGesturesTest`.
- **Celular deitado com linhas espremidas** (média): nomes cortados e ícones encolhidos. Linhas nunca menores que o
  ícone com o nome. Testes: `HomeMetricsTest`, `HomeGesturesTest`.
- **Barras do sistema dos lados ignoradas** (média): a última coluna ficava por baixo da barra de navegação. Teste:
  `HomeGesturesTest`.

Segunda rodada (11/11/2026), em grade e tamanho de tela: na lista que rola, a metade de baixo de um tablet em pé
não recebia itens (alta); a lista não rolava com um item na borda; numa lista rolada o item caía linhas acima do
dedo; em "Livres", virar o tablet e mover um ícone mudava de lugar os das últimas linhas; com a barra de navegação ao
lado, a borda que vira a tela era inalcançável; e com letra em dobro "Remover" cobria a primeira linha. Testes:
`HomeScreenSizeTest`, `HomeLayoutTest`, `HomeMetricsTest`.

Terceira rodada (11/11/2026, pedidos da pessoa): voltar na tela inicial fechava o ORI Home e o Android o abria de
novo (alta); voltar de uma aba remontava a tela inicial na primeira tela. Corrigidos com a tela inicial sempre
montada por baixo das abas e voltar sem efeito nela (`HomeBackTest`). A lista que rola saiu, e a grade passou a ser
a mesma em pé e deitado, como no One UI (`HomeMetricsTest`, `HomeScreenSizeTest`).

Ficaram como limitação, sem falha conhecida aberta: celular só em pé, prévias do papel de parede do Android pelas
cores, o widget carregado como cartão, e o hífen dos nomes, que só pode ser conferido num celular (veja "Limitações"
no mesmo documento). Em 12/11/2026, as prévias passaram a mostrar o papel de parede do Android de verdade, pela
janela, e o widget passou a ser carregado como está (`WallpaperScreenTest`, `ShownWidgetViewsTest`, `HomeDropTest`;
conferido no tablet).

## 16. ORI Home no tablet: teste com gestos (11/11/2026) — corrigido em 11/11/2026

Teste com gestos de verdade no tablet de teste (Android 16, One UI 8.0.5), em pé e deitado. O relatório completo está
em [funcionalidades/launcher-teste-one-ui.md](funcionalidades/launcher-teste-one-ui.md) e o plano das correções, com
a situação de cada item, em [funcionalidades/launcher-plano.md](funcionalidades/launcher-plano.md).

| Falha | Correção | Teste |
|---|---|---|
| T1. Abrir "Widgets" fechava o ORI Home (o Android não entregava a lista dos 77 widgets: `BadParcelableException`) | A lista inteira, quando falha, é lida app por app, com aviso; e (seção 17, U1) as imagens e o adicionar procuram na lista lida, sem pedir a lista inteira de novo | `WidgetListTest`, `KnownWidgetsTest` |
| T2. Depois de fechar duas vezes, o Android voltava a perguntar qual tela inicial usar | Consequência de T1 | `KnownWidgetsTest`; no tablet, a lista rolada até o fim sem fechar |
| T3. Soltar um item numa tela nova voltava para a tela anterior | A tela a mais espera a gravação (`landingPage`) | `HomeScreenSizeTest` (falhava antes) |
| T4. Cancelar e Aplicar da prévia do papel de parede ficavam sobre o dock | Num cartão acima dos pontos e do dock | `WallpaperScreenTest` (falhava antes) |
| T5. A prévia do papel de parede do Android saía clara para um papel escuro | As cores com o brilho que o Android informa (`colorHints`) | `SystemWallpaperColorsTest` |
| T6. "Mostrar botões" não dava botão para editar; o parâmetro do relógio não tinha efeito | Botão "Editar a tela inicial" no alto; o parâmetro saiu | `LauncherScreenTest` |
| T7. O tablet deitado não seguia o One UI | Grade transposta, com um arranjo por posição (`positionsTurned`) | `HomeMetricsTest`, `HomeLayoutTest`, `HomePagesTest`, `HomeScreenSizeTest` |

Ainda em aberto, de design: D9 em parte (mudar o tamanho do ícone muda as colunas; veja as limitações em
[funcionalidades/launcher.md](funcionalidades/launcher.md#limitações)).

## 17. ORI Home: segunda revisão no tablet, em pé (11/11/2026) — corrigido em 11/11/2026

Todos os testes de novo com o tablet em pé, depois das correções da seção 16, e a comparação com o One UI em pé. Os
16 defeitos achados foram corrigidos e conferidos no tablet em pé (build `profiling`), cada um com teste:

| Defeito | Correção | Teste |
|---|---|---|
| U1. Abrir "Widgets" fechava o ORI Home em pé: a imagem de cada widget e o "adicionar" pediam a lista inteira dos 77 widgets, sem proteção, uma vez por linha | Procura na lista lida por último (`KnownWidgets`); o que faltar, só no app do widget, protegido. No tablet: a lista inteira rolada até o fim, sem fechar | `KnownWidgetsTest`, `WidgetListTest` |
| U2. Apagar a busca deixava a lista rolada até o primeiro app achado | A lista volta ao começo a cada busca | `AllAppsSearchTest` (falhava antes) |
| U3. Faixa escura atrás da barra de três botões sobre degradês e fotos | `isNavigationBarContrastEnforced = false` | No tablet (prévia de "Entardecer": a cor vai até o pé) |
| U4. O item segurado ficava num retângulo claro de cantos retos | O menu não escurece mais a tela (U6), então não há recorte | No tablet |
| U5. O menu do relógio e dos widgets não tinha título | "Relógio do ORI" e o nome do widget (lido fora da thread principal) | `WidgetMenuTest`; no tablet, o do relógio |
| U6. O menu não era como o do One UI 8 | Balão acima do item (embaixo sem espaço), sem escurecer a tela; as escolhas continuam por extenso | `MenuPositionTest`, `LauncherScreenTest` |
| U7. Com "Mostrar botões", o botão de todos os apps não ficava no meio do dock | O botão no centro, os apps dos dois lados | `LauncherScreenTest`; no tablet, centro exato |
| U8. Com ícones maiores, os que não cabem embaralhavam a ordem | Vão para as casas livres na ordem de leitura das casas guardadas | `HomeLayoutTest` (falhava antes); no tablet, com "Enorme" |
| U9. Duas barras no alto do painel | "Abrir no ORI" na barra do título da rotina | `RoutineTopBarActionsTest`; no tablet |
| U10. "Travar a tela inicial" em "Aparência" | Seção "Organização" (posição dos ícones e trava) | No tablet |
| U11. O aviso de tela travada parecia erro e ocupava a largura toda | Tipo de mensagem neutro (`FormMessageType.INFO`, `infoRes`) e largura máxima | `LauncherViewModelTest`, `HomeGesturesTest`; no tablet |
| U12. A edição ficava longe da do One UI | Tela a 70 % como cartão, papel de parede escurecido, vizinhas à vista, casinha e lixeira nos cantos, "+" nos pontos | `LauncherScreenTest` (tamanho e cantos); no tablet |
| U13. A prévia "Como está agora" não era a tela da pessoa | `MiniHome`: a grade, os itens da tela principal e o dock | `MiniHomeTest`; no tablet |
| U14. Arrastar da gaveta um app que já está na tela move o ícone | Decisão: um ícone por app (documentado em launcher.md) | — |
| U15. Avisos do compilador nos testes | Nome sem aspas e `@OptIn` | Verificação completa sem avisos no launcher |
| U16. A prévia do papel de parede do Android saía mais clara | Teto de luminosidade escura 0,2 | `SystemWallpaperColorsTest`; no tablet |

### Depois da reverificação (11/11/2026)

| Defeito | Correção | Teste |
|---|---|---|
| V1. Na lista de widgets, os do Android 12+ que só oferecem um desenho de prévia apareciam com o ícone do app, e cinco "Current weather and forecast" 2 × 2 ficavam idênticos | A imagem do app primeiro; sem ela, o desenho de prévia do app (`previewLayout`) como uma view na lista, por cima do ícone (um desenho que não funciona fora da tela inicial de quem o fez deixa o ícone à vista); widgets iguais em nome e tamanho mostram a descrição de cada um, ou um número. No tablet: a lista rolada até o fim, sem caixas vazias nem fechamento | `WidgetVariantTest`, `WidgetListTest`; no tablet |
| D9. Mudar o tamanho dos ícones mudava as colunas e as linhas | "Grade da tela inicial" (Opções > Organização): com uma grade escolhida, o ícone cabe na casa e nada sai do lugar; "Automática" segue o tamanho dos ícones | `HomeMetricsTest`, `LauncherScreenTest`, `DataStoreLauncherSettingsRepositoryTest`; no tablet |

## 18. Teste geral no tablet (12/11/2026) — aberto

Teste de todas as áreas no tablet deitado, sem conta, com as personalizações ligadas e desligadas. O relatório
inteiro (o que funcionou, pontos de melhora e escolhas de design duvidosas) está em [teste-geral.md](teste-geral.md);
aqui ficam os erros, com onde estão e a correção sugerida.

| Defeito | Onde | O que acontece | Correção sugerida | Gravidade |
|---|---|---|---|---|
| G1. O teclado muda a grade do ORI Home | `ui/launcher/HomeMetrics.kt`, `rememberHomeMetrics`: `WindowInsets.safeDrawing.asPaddingValues()` | O `safeDrawing` inclui o teclado: com a busca de todos os apps aberta, a altura "útil" cai e a grade é calculada de novo (médio: 8 → 5 colunas; Máximo: 4 → 3 blocos), também a da tela inicial por trás | Medir com as barras do sistema e o recorte da câmera, sem o teclado (`WindowInsets.systemBars.union(WindowInsets.displayCutout)`); o teclado só empurra a busca. Teste: a grade igual com e sem `ime` | Média — **corrigido em 15/11/2026**: a grade, as telas e o arrastar medem só as barras do sistema e o recorte da câmera (`homeBars`, em `HomeMetrics.kt`); o teclado cobre o pé da grade, como no One UI. Sem teste automático (o Robolectric não simula o teclado) e não conferido no aparelho: o ORI Home está desligado no tablet de teste |
| G2. Apagar "Todos os dias" leva os dias feitos | `domain/routine/RepeatingRoutineRepository.kt` (`DeleteScope.ALL_DAYS` → `deleteSeries`), `data/local/LocalRoutineRepositories.kt` (`dao.delete(seriesId)`); aviso `activity_delete_repeating_message` | O aviso diz que os dias marcados como feitos ficam nos relatórios, mas o relatório do mês caiu de 2 para 1 concluída depois de apagar a série de teste (feita hoje) | Ver onde os dias feitos de uma série são guardados e mantê-los ao apagar a série (ou trocar o aviso). Teste: série com um dia feito, apagar todos os dias, o relatório continua contando | Média — **corrigido em 15/11/2026**: apagar todos os dias olhando um dia feito mantém esse dia (o documento ia junto com a série). Teste: `RepeatingRoutineRepositoryTest`, "deleting all days while looking at a day already done keeps that day as history" |
| G3. "Cancelar" sob o dedo, sem confirmação | `ui/app/screens/routine` (editor de atividade, barra de ações com o teclado) | No tablet deitado com o teclado aberto, a barra "Cancelar / Salvar" sobe e fica onde estava o campo de hora; um toque ali fecha o formulário sem perguntar e o que foi escrito se perde | Pedir confirmação ao cancelar (e ao voltar) com algo escrito; não deixar a barra cobrir os campos (rolar o campo com foco para cima dela) | Média — **corrigido em 15/11/2026**: com algo escrito, "Cancelar", a seta e o voltar do sistema perguntam antes (`rememberLeaveWithConfirmation`); com o teclado aberto e menos de 440 dp, a barra espera o teclado fechar (`FormLayout`, também no formulário de símbolo, o que resolve o P6). Testes: `ActivityEditorScreenTest` e `ActivityEditorViewModelTest`. Conferido no tablet SM-X610 deitado (build `profiling`, em inglês): com o teclado aberto a barra some e os campos ocupam a altura; "Cancel" com "Teste" escrito pergunta "Discard your changes?", e "Discard" fecha sem criar nada. O formulário de símbolo ainda fecha sem perguntar |
| G4. Hora em texto livre | Editor de atividade, campo "Hora (opcional)" | Aceita "abc", com o teclado de letras; o exemplo está em 12 h ("8:30 AM") e a hora aparece em 24 h ("09:15") | Teclado numérico (ou o seletor como padrão), validação, exemplo no formato de hora do aparelho | Baixa — **corrigido em 15/11/2026**: o exemplo, o relógio e a hora que ele escreve seguem o formato do aparelho; com 24 h, teclado de números, e "830" ou "0830" valem; uma hora que não dá para ler é avisada ao sair do campo, com o exemplo. Teste: `RoutineDomainTest` ("time input takes the numbers of a number keyboard…"). Conferido no tablet (inglês, 24 h): "abc" e sair do campo mostram "That time is not valid. Write it like 08:30"; o teclado de números não foi conferido |
| G5. Setas da organização às vezes deixam a palavra sob o dedo | CAA, organizar prancha (lista com setas) | Depois de mover, a lista às vezes rola para a palavra ficar sob o dedo e às vezes não (no começo da lista): tocar de novo no mesmo ponto pode mover outra palavra | Sempre manter a palavra movida sob o dedo, ou destacá-la e mostrar as setas só nela | Baixa |

## 19. Segundo teste no tablet: desempenho e design (13/11/2026) — aberto

Medido no tablet deitado, com o build `profiling` e o ORI com conta. As medidas, o que funcionou e os pontos de melhora
estão em [teste-geral.md](teste-geral.md) ("Segundo teste"); aqui ficam os defeitos, com onde estão e a correção
sugerida.

**M16** (abrir "Montar" monta a prancha de novo) foi corrigido em 13/11/2026: veja a seção 20.

**M2 continua parcial:** trocar de área pela barra lateral ainda tem um primeiro quadro de 30 a 60 ms (17 a 22 ms
recompondo a tela nova e 8 a 10 ms desenhando), 3 a 5% de quadros com atraso.

**Corrigidos no mesmo dia (13/11/2026):**

- **G6. "Atividade não encontrada" ao mudar de semana.** `RoutineRoute` (`RoutineScreen.kt`) fecha os detalhes quando a
  pessoa escolhe outro dia; salvar numa data diferente continua abrindo os detalhes no dia novo. Teste:
  `RoutineScreenLayoutTest`, "choosing another week closes the open details instead of saying the activity is gone".
  Conferido no tablet: mudar de semana com uma atividade aberta mostra "Escolha uma atividade".
- **G7. Bordas de controles com pouco contraste.** `outline` passou a ser a borda do que se toca (`OriControlBorder`,
  #7A8C9C no claro; #5E7488 no escuro) e é usada na borda sem foco de `OriTextField` e de `OriChoiceChip`, além do anel
  de `DoneToggle`, que já a usava; `outlineVariant` ficou só para decoração (cartões, linhas). Teste: `OriThemeRulesTest`,
  "the edge of a field, a choice or the done circle can be seen, in both themes" (falha com o #C3D5E3 de antes).
  Conferido em imagem a 360 dp (Robolectric) e no tablet: a borda do campo mede 3,3:1 no claro (antes 1,1:1) e 3,7:1
  no escuro (antes 1,5:1).
- **M15. O menu do ícone do ORI Home travava ao abrir.** O menu deixou de ser uma janela própria (`Popup`): `AppMenu`
  pede o menu com `MenuRequest`, e `MenuLayerHost`, no alto de `LauncherScreen`, desenha o cartão na mesma janela
  (`MenuLayer.kt`), ao lado do item, com toque fora e voltar fechando, como antes. O conteúdo do menu é guardado como
  dado (`MenuContent`), para um nome que chega depois (o de um widget) aparecer. Testes: `HomeMenuLayerTest` (nenhum
  `Popup`, ao lado do ícone, toque fora fecha sem abrir o app de baixo, voltar fecha, uma escolha funciona) e
  `WidgetMenuTest`. Medido no tablet (build `profiling`, 3 menus): quadros com atraso de 16,3% para 6,2% (app sem
  atalhos) e de 16,4% para 5,4% (com atalhos); quadros de 50 ms ou mais, de 12 e 9 para nenhum (o pior, 37 ms).

## 20. Tempo para abrir cada tela (13/11/2026) — em parte aberto

Todas as telas que dava para abrir com a conta conectada foram medidas no tablet, do dedo saindo da tela até a tela
aparecer, com o build `profiling` compilado como a loja faz. A tabela completa, antes e depois, e o método estão em
[teste-geral.md](teste-geral.md) ("Terceiro teste").

| Defeito | Onde | O que acontece | Correção sugerida | Gravidade |
|---|---|---|---|---|
| M22. Relatórios na primeira vez | `ReportsViewModel` / `FirestoreRoutineRepository.getDays` (`get()` vai ao servidor) | A tela abre em 36 a 60 ms com "Carregando"; os números chegam em 150 a 200 ms, da internet | Só se valer o risco: mostrar antes o que está guardado no aparelho, marcado como provisório, e trocar pelos números do servidor | Baixa |
| M23. Menus do Material na CAA | `AacMessageActions` ("Mais") e `OrganizerPanes` (opções da prancha): `DropdownMenu`, uma janela à parte | Abrem em 68 a 98 ms e, ao fechar, gastam um quadro de uns 20 ms descartando a janela | Desenhá-los na janela da tela, como `MenuLayer` (ORI Home) e `OriDialogHost` | Baixa |

**Corrigidos no mesmo dia (13/11/2026):**

- **M17. Diálogos abriam em 125 a 160 ms.** Todo `AlertDialog`/`Dialog` do Material é uma janela própria, e o Android a
  criava a cada abertura (uns 20 ms esperando o sistema registrar a janela, 8 ms no primeiro ajuste de tamanho, um ou
  dois quadros de espera). Os diálogos passaram a ser desenhados na janela da tela: `OriDialogHost` (em `MainActivity` e
  `LauncherActivity`) guarda os diálogos abertos, `OriDialog` pede um e `OriAlertDialog` tem a aparência do
  `AlertDialog` (`ui/components/dialog`). O conteúdo é desenhado com os valores da tela que pediu
  (`currentCompositionLocalContext`), e a tela atrás fica fora do leitor de tela (`clearAndSetSemantics`) enquanto o
  diálogo está aberto. O escurecimento usa `CompositingStrategy.ModulateAlpha`, para não redesenhar a tela inteira à
  parte a cada quadro. O detekt passou a proibir `AlertDialog`, `BasicAlertDialog` e `Dialog` (`ForbiddenImport`); a
  exceção é `PinRequestDialog`, que já é a janela inteira de `PinRequestActivity`. Teste: `OriDialogLayerTest` (8
  casos: na mesma janela e lido como diálogo com título; tela atrás escondida do leitor e de volta ao fechar; toque fora
  fecha sem tocar a tela atrás; toque no diálogo não fecha; voltar fecha o diálogo e não a tela; o conteúdo acompanha a
  tela; os valores da tela chegam ao diálogo; o de tela inteira não fecha com um toque). Conferido em imagem a 360 dp
  (claro e escuro) e no tablet. Medido: de 125 a 160 ms para 19 a 62 ms.
- **M18. Todos os apps do ORI Home abriam em 83 a 157 ms.** O primeiro quadro do deslize compunha os cerca de 43 apps
  visíveis. Agora `LauncherHome` monta a lista escondida quando a thread principal fica livre pela primeira vez
  (`rememberScreenRested`, em `ui/components/state/KeptAhead.kt`), e ela segue montada: fechada, não é desenhada (alfa 0), não é lida
  (`clearAndSetSemantics`), volta ao topo e solta o teclado da busca. Escondida, ela só acompanha os apps e as opções
  (`rememberStateWhileShown`). `DrawerApp` recebe as escolhas do menu como função e só monta o menu ao abri-lo, para um
  app igual ser pulado ao recompor. Medido: 31 a 51 ms, também logo depois de reiniciar. Os testes do ORI Home
  (gaveta, busca, pastas, menus) passam sem mudança.
- **M19. Widgets levavam 244 ms na primeira vez.** `LauncherViewModel.onEditingChange` lê a lista de widgets quando a
  edição começa, se ainda não foi lida, e `HomeWidgetEditor.loadProviders` só mostra "Carregando" quando não há lista.
  Medido: 79 ms na primeira vez e 65 ms depois.
- **M20. "Abrir no ORI" recriava o app.** `LauncherActivity.openOri` usava `FLAG_ACTIVITY_CLEAR_TASK`. Agora usa
  `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP`, e `MainActivity.onNewIntent` troca só a área (`AreaRequest`,
  lido por `MainScaffold`). Medido: 100 ms até o primeiro quadro com o app fechado (antes, 169 ms); com ele aberto, volta
  em 28 ms. Conferido no tablet (com o app em Frases, o painel volta para a mesma janela, em "Minha rotina"). Sem teste
  automático: a troca depende do `onNewIntent` de uma atividade real.
- **M21. Frases levava de 127 a 165 ms na primeira vez.** Um rastreamento com marcas temporárias por parte da tela
  (retiradas depois) mostrou três causas, nenhuma no desenho das frases (16 frases custam menos de 4 ms):
  - o motor de voz (`TextToSpeech`) era criado na thread principal junto com o ViewModel: 24 ms consultando o Android
    sobre os motores instalados. `AndroidSpeechSynthesizer` agora o cria numa thread própria (`engineMaking`); nada
    fala antes de ele avisar que está pronto, e parar ou fechar antes espera por ele;
  - o primeiro quadro abria em "Montar" vazia (o estado inicial não tinha a aba), e Prontas só era composta no quadro
    seguinte, mais 41 ms. `AacCommunicateViewModel.firstState` já começa na aba e na prancha pedidas;
  - montar a área toda (ViewModel, rotas) levava de 50 a 94 ms; agora, de 7 a 15 ms.
  Medido: de 57 a 60 ms na primeira vez depois de reiniciar, em um quadro só; de 62 a 71 ms nas seguintes. Testes:
  `AacCommunicateViewModelTest` (a tela abre em Prontas).
- **M16. "Montar" era montada de novo a cada troca de aba (66 a 144 ms).** `AacBoardContent` separou as duas fontes:
  os símbolos ficam ligados à prancha de "Montar" (`buildBoard`: a escolhida ou, com Prontas aberta, a última usada),
  e as frases salvas continuam lidas só com Prontas aberta, guardando a lista lida por último. Na tela, `AacTabs`
  mantém montada a aba que já apareceu, fora de vista quando a outra está aberta (`outOfSightUnless`, em
  `ui/components/state/KeptAhead.kt`: medida, mas sem lugar na tela, então não é desenhada, tocada nem lida), e monta
  "Montar" antes, escondida, quando a tela fica parada (`rememberScreenRested`: nada a compor, nenhuma animação, a
  thread principal livre), que agora também prepara todos os apps do ORI Home. A tela de comunicação também fica
  montada por baixo de Organizar, Preferências e Nova palavra (`KeptUnder`, em `AacRoute`). Medido: Montar em 30 a
  55 ms na primeira vez e de 26 a 44 ms depois; Prontas em 27 a 30 ms; voltar de Organizar ou de Preferências, de 24
  a 62 ms (antes, 95 a 120). Testes: `AacCommunicateViewModelTest` (os símbolos ficam com Prontas aberta, sem ler de
  novo; as frases lidas ficam com Montar aberta, sem ser lidas), `AacScreenTest` ("Montar" feita antes, fora de
  alcance, e mostrada na hora) e `KeptAheadTest` (fora de vista não é lida nem tocada; mostrada de novo, nada é
  montado; coberta, sai e volta sem ser montada de novo; a tela só conta como parada depois dos primeiros quadros).
