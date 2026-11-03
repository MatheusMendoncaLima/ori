# Perfis, vínculos e permissões

**Situação:**
- **Regras do Firestore e Cloud Functions:** implementadas e testadas no emulador (41 testes).
- **App Android:** implementado e parcialmente testado (testes de domínio e de ViewModels; telas sem teste de ponta a ponta com Firebase real).
- **Publicação:** as regras do Firestore, incluindo conta única, `routineSeries` e `photos`, foram publicadas no projeto `oriteam-ori` em 03/11/2026, e os índices em 28/10/2026. As Cloud Functions **ainda não foram publicadas**, porque isso exige o plano Blaze (ver [Como publicar](#como-publicar)). Sem elas, gerar e aceitar convites não funciona.

## 1. Identidade não é autorização

O ORI separa duas perguntas:

| Pergunta | Quem responde | Onde fica |
|---|---|---|
| **Quem é você?** (identidade) | Firebase Authentication | Token da sessão (`request.auth.uid`) |
| **O que você pode fazer?** (autorização) | Regras do Firestore e Cloud Functions | Perfil (`users/{uid}`) e vínculo (`careLinks/...`) |

Estar logado **não dá acesso a nada** por si só. Uma conta sem perfil não lê
nem grava dados do app.

A interface esconde botões que a pessoa não pode usar, mas isso serve só para
deixar a tela clara. **Quem protege os dados são as regras**, que valem também
para quem tentar acessar o banco por fora do app.

## 2. Uma só conta, dois lados de um vínculo

Não há mais login nem cadastro de responsável. **Toda conta pode tudo o que é dela:** ter rotina, usar a comunicação, acompanhar alguém e ser acompanhada. O que uma pessoa pode fazer com os dados de outra depende só do **vínculo** entre as duas.

**Por que mudou:** a mesma pessoa pode precisar de apoio e também apoiar alguém, e a escolha definitiva no cadastro não trazia segurança a mais: quem protege os dados é o vínculo com permissões.

| Lado no vínculo | Pode | Não pode |
|---|---|---|
| Quem é acompanhado (`primaryUserUid`) | Tudo nos próprios dados; concluir as atividades do responsável com `ownerCanComplete = true`; dar ou tirar as permissões `manageRoutines` e `manageCommunication`; encerrar o vínculo; aceitar convites | Alterar atividades criadas pelo responsável |
| Quem acompanha (`caregiverUid`) | Com `viewRoutines`: ver as atividades ativas da pessoa. Com `manageRoutines`: criar, editar, apagar e concluir atividades. Com `manageCommunication`: organizar pranchas e símbolos. Gerar convites. Encerrar o vínculo | Ver atividades ocultas (`hidden_from_caregiver`); ler ou alterar frases salvas; adicionar fotos a símbolos; dar permissões a si mesmo |

No app: **Perfil → Responsáveis vinculados** (quem me acompanha, e aceitar um código) e **Perfil → Acompanhados** (quem eu acompanho, e gerar um código).

**Não há papel no perfil.** Um perfil novo tem só o nome, e as regras recusam um `role` nele. Perfis antigos ainda têm `role`, que não pode ser mudado e que nada lê. No app, quem escolhe o lado do vínculo é a tela, com `CareLinkSide` (`domain/carelink`): `PRIMARY_USER` no Perfil (quem me acompanha) e `CAREGIVER` em Acompanhados (quem eu acompanho). Quem usa o app sem conta não tem perfil no Firestore: veja [uso-local.md](uso-local.md).

> **Atividades reservadas ao responsável** são as que ele criou. Com isso, uma pessoa sem responsável monta a própria rotina, e o que o responsável definiu continua do jeito combinado.
>
> **Comunicação (CAA):** as pranchas são da pessoa; o responsável só as organiza se ela permitir.

## 3. Modelos de dados

```text
users/{uid}
  displayName: string (1–100)
  createdAt: horário do servidor

users/{uid}/routineTasks/{taskId}
  title: string (1–200)            date: "AAAA-MM-DD"
  scheduledTime: "HH:MM" | null    description: string (até 500) | null
  status: "active" | "hidden_from_caregiver" | "archived"
  ownerCanComplete: bool           isDone: bool
  completedBy: uid | null          completedAt: horário do servidor | null
  createdBy: uid de quem criou     createdAt / updatedAt (horário do servidor)

careLinks/{caregiverUid}_{primaryUserUid}
  caregiverUid, primaryUserUid, caregiverName, primaryUserName
  status: "active" | "revoked"
  permissions: { viewRoutines: bool, manageRoutines: bool, manageCommunication: bool }
  (vínculos antigos sem manageCommunication contam como "sem permissão")
  createdAt, revokedAt

users/{uid}/aacBoards/{boardId}            (e /symbols/{symbolId}; veja caa.md)
users/{uid}/settings/preferences           (só o dono; veja preferencias.md)
users/{uid}/aacSavedMessages/{messageId}   (só o dono)

invitations/{código}              (só o servidor lê e grava)
  caregiverUid, caregiverName, status: "pending" | "accepted"
  createdAt, expiresAt (24 h), acceptedBy, acceptedAt
```

O id do vínculo é previsível (`cuidador_usuario`). Assim, a regra encontra o
vínculo com um único `get()`, sem consultas, que as regras não permitem.

No Android, os modelos de domínio equivalentes são `UserProfile`, `RoutineTask`,
`CareLink`/`CareLinkPermissions` e `CareInvitation`. A conversão entre documento e
modelo fica em `data/firestore/FirestoreSchema.kt`. Um documento com valor
desconhecido vira `null`, nunca um modelo pela metade. O `role` dos perfis antigos é ignorado.

## 4. Do login à decisão de autorização

```mermaid
sequenceDiagram
    participant U as Usuário
    participant A as App (OriApp)
    participant FA as Firebase Auth
    participant FS as Firestore (regras)
    participant CF as Cloud Functions

    U->>A: abre o app
    A->>FA: signedInUser (sessão restaurada pelo SDK)
    FA-->>A: uid ou null
    alt sem sessão
        A-->>U: abertura, login e cadastro
    else com sessão
        A->>FS: observa users/{uid}
        FS-->>A: perfil, ausente ou erro
        alt perfil ausente
            A-->>U: "Complete seu perfil" (sem acesso a dados)
        else erro (rede, permissão)
            A-->>U: erro com "Tentar novamente" e "Sair"
        else perfil encontrado
            A-->>U: Início, Rotina, Frases e Perfil
            U->>A: abre a rotina de alguém
            A->>FS: consulta users/{dono}/routineTasks
            FS->>FS: o chamador tem perfil?<br/>é o dono? há vínculo ativo com a permissão?
            FS-->>A: dados ou PERMISSION_DENIED
        end
    end
    U->>A: responsável gera convite
    A->>CF: createCareInvitation
    CF->>FS: valida o perfil e grava invitations/{código}
    U->>A: usuário digita o código
    A->>CF: acceptCareInvitation(código)
    CF->>FS: transação: valida papéis, validade e uso único;<br/>cria careLinks com só "ver rotina"
```

### Estados da sessão (`SessionState`)

| Estado | Quando | O que o app mostra |
|---|---|---|
| `Initializing` | O SDK restaura a sessão ou o perfil está sendo lido | Carregando |
| `SignedOut` | Não há sessão nem perfil local | Abertura, login, cadastro e "Continuar sem conta" |
| `ProfileRequired` | Há sessão, mas não há perfil | "Complete seu perfil" |
| `ProfileUnavailable` | O perfil não pôde ser lido | Erro com "Tentar novamente" e "Sair" |
| `Active` | Há perfil válido, ou perfil local (sem conta) | Início, Rotina, Frases e Perfil |

- A sessão é guardada e renovada pelo próprio SDK do Firebase. O app **não guarda
  senhas nem tokens** e **não pede login de novo** ao reabrir.
- Falha de rede **não** desloga a pessoa: com a cópia do perfil no aparelho ela segue usando o app; sem cópia, vê `ProfileUnavailable`.
- Credenciais revogadas ou expiradas fazem o SDK encerrar a sessão, e o app volta para `SignedOut`.
- **Sair** chama `signOut()`, e a raiz do app volta sozinha para a abertura.

### Permissões dentro da tela

- `canToggleCompletion()` (domínio) decide se o cartão da atividade pode ser
  marcado. A regra é a mesma do Firestore, mas serve só para a clareza da
  interface.
- Erro de permissão aparece como **erro** ("Você não tem permissão..."), nunca
  como lista vazia. Isso vale, por exemplo, quando um vínculo é encerrado com a
  tela aberta.

## 5. Operações no servidor

Criar um vínculo dá acesso aos dados de outra pessoa. Por isso essa operação
**nunca é feita pelo cliente**: as regras negam `create` em `careLinks`, e só as
Cloud Functions (Admin SDK) gravam vínculos.

| Função | Quem chama | Validações |
|---|---|---|
| `createCareInvitation` | Quem vai acompanhar | Login exigido; o perfil precisa existir; o código tem 8 caracteres, é gerado com `crypto.randomInt`, sem caracteres ambíguos, e não se repete |
| `acceptCareInvitation` | Quem será acompanhado | Login exigido; o perfil precisa existir; o código existe, está pendente e não expirou (24 h); não é um auto-vínculo; o autor ainda tem perfil; não há vínculo ativo duplicado. Tudo acontece em uma transação |

- O vínculo nasce com **menor privilégio**: só `viewRoutines`. Quem decide dar
  `manageRoutines` e `manageCommunication` é o usuário principal.
- Os erros esperados viram `HttpsError` com código (`not-found`,
  `failed-precondition` etc.). O app mostra uma mensagem própria para cada um.
  O log do servidor registra só o nome do erro.

## 6. Testes de segurança

### Regras e funções no emulador (`functions/security-tests`)

```bash
cd functions/security-tests
npm ci
npm test   # sobe o emulador do Firestore, roda os testes e desliga
```

Esse comando precisa de Java 11 ou superior e do Node 24.

| Grupo | O que é verificado |
|---|---|
| Visitante sem login | Não lê perfis, rotinas, vínculos nem convites |
| Perfis | Cria só o próprio perfil, só com o nome; **recusa qualquer `role`** (inclusive os antigos) e campos extras; exige o nome; o `role` dos perfis antigos não muda (nem por `update` nem por sobrescrita); perfil não pode ser apagado; perfis de pessoas não vinculadas são privados |
| Conta sem perfil | Não acessa nem o próprio caminho; vínculo ativo de nada vale sem perfil de responsável |
| Usuário principal | Lê a própria rotina; marca e desmarca atividade autorizada; **não** marca atividade reservada; **não** registra conclusão em nome de outra pessoa; **não** edita nem cria atividades; **não** acessa a rotina de terceiros |
| Responsável com vínculo | Lê só atividades ativas; consulta que poderia trazer ocultas é recusada; cria só como autor real; edita atividade já concluída sem reescrever a conclusão; não toca em atividades ocultas; não acessa pessoas não vinculadas |
| Permissões | "Só ver" lê, mas não gerencia; **responsável não se dá permissão**; usuário principal concede e retira; chaves de permissão desconhecidas são recusadas |
| Vínculos | Cliente **não cria** vínculo; só os dois participantes leem; **encerrar corta o acesso na hora** e não pode ser desfeito pelo cliente; responsável já revogado não tem acesso; terceiros não encerram vínculo alheio; convites são inacessíveis ao cliente |
| Comunicação (`aac.test.mjs`) | Dono organiza pranchas e símbolos com consultas limitadas; símbolo só em prancha existente; imagem só como referência curta (nada de `data:`, nada de `../`); responsável com `manageCommunication` organiza, mas **não** adiciona fotos; responsável sem a permissão, com vínculo antigo ou revogado não lê nada; **frases salvas são só do dono**; só o usuário principal concede `manageCommunication` |
| Funções de convite | Formato do código; qualquer conta com perfil cria e aceita; retentativa em colisão; aceite cria vínculo com menor privilégio; uso único; código desconhecido ou expirado; conta sem perfil; sem vínculo duplicado; vínculo revogado recriado com permissões padrão; autor que não tem mais perfil. Os perfis desses testes não têm `role` |

Para conferir que os testes detectam falhas reais, as regras foram
enfraquecidas temporariamente (papel alterável e permissões ignoradas). Os
testes "o papel não pode ser alterado" e "só ver não gerencia" falharam, como
esperado. Depois disso as regras foram restauradas. Ao tirar o papel do perfil,
a regra antiga (que exigia `role`) foi testada de novo: os dois testes de perfil
novo falharam com ela.

### App Android (`android/`)

```bash
./gradlew testDevDebugUnitTest
```

| Teste | O que garante |
|---|---|
| `AuthorizationDomainTest` | Estados da sessão; o cadastro cria o perfil; falha de conta não grava perfil; falha de perfil é separada; regra de quem marca a atividade |
| `SessionViewModelTest` | Sem sessão → `SignedOut`; sem perfil → `ProfileRequired`; perfil criado ativa a sessão sem novo login; nova tentativa e saída; trocar de conta não mostra o perfil anterior |
| `SignedInViewModelsTest` | Cada lado do vínculo vê os seus (`CareLinkSide`); erro de permissão na rotina não vira lista vazia; marcação proibida não chega ao repositório; encerrar vínculo exige confirmação; só o usuário principal muda permissões; validação e mensagens do convite; perfil exige nome |
| `AacDomainTest`, `AacOrganizeViewModelTest`, `AacScreenTest` | Quem organiza pranchas, quem usa frases salvas e fotos; responsável sem permissão não grava nada e não vê botões de mudança |
| `FirestoreSchemaTest` | Perfil sem nome é rejeitado, e o `role` antigo é ignorado; permissão ausente equivale a "não pode"; valores iguais aos das regras; mapeamento de erros |
| `LoginViewModelTest`, `RegisterViewModelTest` | Preferência "Aa" persistida; cadastro grava o perfil; conta sem perfil segue para a sessão |

## 7. Limitações e riscos

- **As Cloud Functions não estão publicadas.** Gerar e aceitar convites falha até
  o deploy das funções. O restante do app já usa as regras publicadas.
- **Rascunho de perfil no cadastro:** criar a conta e o perfil são dois passos.
  Entre eles, a tela "Complete seu perfil" pode aparecer por um instante.
- **Cache offline:** o Firestore guarda dados no aparelho. Ao trocar de conta no
  mesmo aparelho, a nova conta só consulta os próprios caminhos, mas o cache não
  é apagado ao sair.
- **Convites:** códigos expirados não são apagados automaticamente, e não há
  limite de quantos convites um responsável pode gerar.
- **Exclusão de conta** e **ocultar atividade do responsável** ainda não têm
  fluxo no app. O status `hidden_from_caregiver` já é respeitado pelas regras.
- **App Check** não está configurado. Ele reduziria chamadas feitas fora do app.

## Como publicar

**Publique antes de instalar a versão de conta única:** contas antigas com `role: caregiver` só acessam a própria rotina com as regras novas, e contas novas só geram convites com as funções novas.

**Perfil sem papel (03/11/2026):** as regras publicadas recusam `role` em perfis novos. Uma versão antiga do app, que ainda grava `role`, não consegue criar perfil: o cadastro para em "Complete seu perfil". Instale a versão nova nos aparelhos de teste.

Com a conta do projeto Firebase, dentro de `functions/`:

```bash
firebase deploy --only firestore:rules,firestore:indexes,functions
```

Antes de publicar, confira as regras atuais no console: este arquivo
**substitui** as regras existentes.
