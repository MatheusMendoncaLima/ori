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

### R4. Excluir "todos os dias" pode deixar dias soltos (média-baixa)

- **Onde:** `domain/routine/RepeatingRoutineRepository.kt:149` (`delete`).
- **O que acontece:**
  - O comentário diz que só os dias concluídos ficam, como histórico.
  - Mas um dia marcado e depois desmarcado também tem documento, com `isDone = false`.
  - Depois da exclusão, esse dia aparece como uma atividade comum, não concluída, no dia dele.
- **Correção:** ao excluir a série, apagar os documentos de dias que não estão concluídos, ou ignorar no `merged` os dias de uma série que não existe mais e não foram concluídos.

### R5. Excluir uma série cujo dia foi marcado por quem acompanha (média-baixa)

- **Onde:** `RepeatingRoutineRepository.kt:149` e `firestore.rules` (`routineTasks`, `delete`).
- **O que acontece:**
  - Quando quem acompanha marca um dia como feito, o documento desse dia tem `createdBy` = quem acompanha.
  - Se a dona da rotina exclui a série olhando para esse dia, a série é apagada, mas o documento do dia não pode ser apagado por ela (regra de autoria).
  - A tela mostra erro, embora a série já tenha sumido.
- **Correção:** criar o documento do dia sempre com o autor da série, ou deixar a dona apagar documentos de dias das próprias séries (regra com teste no emulador).

### R6. Fotos de símbolos apagados por quem acompanha ficam na conta (média-baixa)

- **Onde:** `ui/app/screens/aac/AacOrganizeViewModel.kt:216` (`removeLocalPhotos`) e a regra de `photos` (`delete` só da dona).
- **O que acontece:**
  - Quem acompanha pode apagar símbolos e pranchas, mas não as fotos deles.
  - Os documentos de foto ficam na conta da pessoa e contam no limite de 100 fotos.
  - Ninguém sabe que estão lá.
- **Correção:** apagar fotos órfãs quando a dona abrir a área de organizar (comparando as fotos com os símbolos), ou permitir a quem tem `manageCommunication` apagar fotos que nenhum símbolo usa (com teste).

### R7. Apagar uma prancha sem conexão pode deixar símbolos no servidor (baixa-média)

- **Onde:** `data/aac/FirestoreAacBoardRepository.kt:95`.
- **O que acontece:**
  - Para apagar, o app primeiro lê os símbolos. Sem conexão, a leitura vem do cache.
  - Símbolos que nunca foram lidos neste aparelho não entram no lote.
  - Eles ficam no servidor, debaixo de uma prancha que não existe.
- **Correção:** só apagar uma prancha com a lista de símbolos confirmada pelo servidor; sem conexão, avisar que precisa de internet. Ou apagar no servidor (Cloud Function), quando houver o plano Blaze.

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

## 3. Gambiarras e código que engana quem lê

| Id | Onde | O problema | Correção |
|---|---|---|---|
| G1 | `domain/model/TaskStatus.kt:3` | O comentário diz "não usado por nenhuma tela", mas o tipo é usado em todo o app. O pacote `domain/model` só tem esse arquivo | Mover para `domain/routine` e corrigir o comentário |
| G2 | `data/aac/AndroidSpeechSynthesizer.kt:19` | Comentário solto ("Brazilian Portuguese…"), sobra da mudança para `AppLocale` | Apagar |
| G3 | `data/profile/DeviceProfilePhotoStore.kt:56`, `data/local/RoutedRepositories.kt:67` (e `deleteBoard`), `data/local/RoomLocalDataStore.kt:61` | Formatação quebrada pela correção automática (`this[\n photoKey(\n uid`) e um `DataResult` com o nome completo do pacote | Reescrever essas linhas |
| G4 | `data/routine/FirestoreRoutineRepository.kt:58` e `FirestoreRoutineSeriesRepository.kt` | Cada um repete o listener que `data/firestore/observeList` já faz, e os dois têm a mesma função `activitiesOf`. `enumOrDefault` também se repete nos dois repositórios do DataStore | Generalizar `observeList` para receber o tipo do resultado e reaproveitar; mover os auxiliares repetidos para um lugar só |
| G5 | `MainActivity.kt:33` | `DisposableEffect` com `onDispose {}` vazio, usado só para chamar `enableEdgeToEdge` de novo | Trocar por `LaunchedEffect(isDark)` ou `SideEffect` |
| G6 | `account/ProfileScreen.kt:114` e `preferences/SettingsScreen.kt:66` | Números de cor (`accentIndex = 3`, `5`…) escritos à mão, enquanto o resto usa `OriAreaAccents` | Usar `OriAreaAccents.PEOPLE`, `.CARE` etc. nas linhas que pertencem a uma área |
| G7 | `domain/aac/AacAccess.kt:20` e `firestore.rules:393` | Os dois dizem que as fotos "nunca saem do aparelho". Desde a sincronização de fotos isso não é verdade. O motivo real é que só a dona grava fotos (o limite e o custo são dela) | Reescrever os dois comentários |
| G8 | `firestore.rules:104` e `domain/routine/RoutineTask.kt` (`forDay`) | O estado `archived` é aceito e filtrado, mas nada no app arquiva | Decidir: remover (com migração) ou documentar como reservado |
| G9 | `functions/security-tests/test/rules.test.mjs` | 942 linhas num arquivo só | Separar por coleção, como já é feito com `aac` e `preferences` |

## 4. Acessibilidade

| Id | Onde | O problema | Correção |
|---|---|---|---|
| A1 | `ui/components/form/OriTextField.kt:131` | O botão de mostrar a senha é forçado a 36 dp, porque o `size` vem antes do tamanho mínimo do `IconButton`. O Compose costuma ampliar a área na hora do toque, mas os limites que o leitor de tela e o Scanner de Acessibilidade veem ficam em 36 dp, abaixo de 48 dp. Não foi conferido no aparelho | Tirar o `Modifier.size(36.dp)` (o botão já tem tamanho padrão) e conferir a renderização e o Scanner de Acessibilidade |

## 5. Servidor e segurança

| Id | Onde | O problema | Correção |
|---|---|---|---|
| S1 | `functions/functions/src/index.ts` | Sem App Check e sem limite de tentativas nos convites (só `maxInstances: 10`). Adivinhar um código é impraticável (32⁸ combinações, 24 h de validade), mas nada impede criar convites sem parar | App Check nas funções e um limite de convites abertos por pessoa |
| S2 | `invitations/{code}` | Convites usados ou vencidos nunca são apagados | Política de TTL do Firestore no campo `expiresAt` |
| S3 | `careInvitations.ts:222` | Os nomes guardados no vínculo são copiados quando ele é criado e não acompanham uma troca de nome | Documentar (feito em [perfil-e-conta.md](funcionalidades/perfil-e-conta.md)) ou atualizar numa Cloud Function |
| S4 | Projeto `oriteam-ori` | Há um índice `routineTasks (status, scheduledTime)` publicado que não está em `firestore.indexes.json` e que o app não usa. Ele só encarece as gravações | Apagar no console, ou publicar o arquivo com `--force` |

## 6. Testes que faltam

Estes ViewModels não têm teste próprio:

- `ReportsViewModel`;
- `CareLinksViewModel`;
- `InvitationViewModel`;
- `ProfileSetupViewModel`;
- `ProfilePhotoViewModel` (o `DeviceProfilePhotoStore` tem teste);
- `PreferencesViewModels`;
- `AacPreferencesViewModel`;
- `HomeViewModel`.

Os mais importantes são:

- **Relatórios:** recarregar ao abrir de novo, troca de período e erro de leitura.
- **Vínculos:** revogar e mudar permissões só do lado de quem é acompanhado.
- **Convites:** os erros específicos de cada código.


## 7. Ordem sugerida

1. **P1**: custo de leitura, que cresce com o uso.
2. **R4**, **R5**: dados que ficam errados.
3. **A1**, **R6**, **R7**, **R12**.
4. As gambiarras G1–G7, que são rápidas e deixam o código honesto.
5. **S1** e **S2**, junto com a publicação das Cloud Functions (plano Blaze).

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
