# Revisão de estrutura e de design

Revisão feita em 03/11/2026, lendo o código e olhando imagens de telas geradas no computador (360 × 780 dp, letras normais e maiúsculas). Nada foi visto no celular.

**Situação:** todos os itens foram feitos, menos três decididos com motivo (E18, D14 e A3). Os de código têm testes que falham sem a correção; os de aparência também foram conferidos em imagens de 360 dp. **A1 estava errado:** veja a nota na tabela de acessibilidade. Melhorias encontradas depois estão na seção 4.

- **Estrutural:** código e lógica (erros, arquitetura, duplicação, dados).
- **Design:** requisitos não funcionais (aparência, acessibilidade, uso, desempenho e custo).

Prioridade: **P1** dá resultado errado ou impede uma ação; **P2** confunde, custa caro ou dificulta mudar o código; **P3** é acabamento.

A coluna "Visto em" diz como o problema foi confirmado: código, imagem ou lint. Os itens marcados com † já estavam em [revisao-de-design.md](revisao-de-design.md) e continuam abertos.

## 1. Estrutural (código e lógica)

### Erros de lógica

| # | P | Problema | Onde | Visto em |
|---|---|---|---|---|
| E1 | ~~P1~~ feito | **Um responsável não consegue criar nem editar atividades que se repetem.** Antes de gravar, o app lê as repetições como se fosse o dono (`RoutineViewer.Owner`). Para um responsável, essa leitura não filtra `status == 'active'`, então as regras a recusam e a gravação falha com "sem permissão". Atividades comuns continuam funcionando | `RepeatingRoutineRepository.createActivity` e `updateActivity` | Código e regras |
| E2 | ~~P2~~ feito | **O Início e Tarefas ficam presos no dia em que abriram.** `onToday()` só roda quando o perfil muda. Se o app ficar aberto depois da meia-noite, "hoje" continua sendo ontem | `TodayRoute`, `TasksRoute` | Código |
| E3 | ~~P2~~ feito | **"Vezes que falou" mistura critérios:** <br>• conta quando não havia voz e a frase só apareceu em letras grandes;<br>• não conta os toques do modo "fala cada símbolo ao tocar";<br>• conta só deste aparelho, enquanto as atividades vêm da conta, então dois aparelhos mostram relatórios diferentes. **Agora** conta cada vez que a pessoa comunica uma mensagem (Falar, com ou sem voz, Mostrar, frase pronta, símbolo no modo que fala cada um); a voz de retorno ao montar não conta. O nome virou "Vezes que se comunicou", e o relatório diz que a contagem é deste aparelho | `AacCommunicateViewModel.say` e `onSymbolTap`, `RoomAacUsageRepository` | Código |
| E4 | ~~P2~~ feito | **O formulário usa uma data falsa (`LocalDate.MIN`) enquanto não começou.** Foi a causa do travamento ao adicionar atividade. Hoje uma guarda na tela evita o problema, mas um estado "carregando" explícito seria mais seguro. **Agora** a data é nula até o formulário abrir, e a tela não desenha nada antes disso | `ActivityEditorUiState`, `ActivityEditorScreen` | Código |

### Arquitetura e duplicação

| # | P | Problema | Onde | Visto em |
|---|---|---|---|---|
| E5 | ~~P2~~ feito | **Três cópias da mesma rotina.** Início, Tarefas e Rotina criam cada um o seu `RoutineViewModel` (chaves `today-`, `tasks-`, `routine-`), e cada um abre dois listeners: atividades do dia e repetições. Trocar de dia também recria o listener das repetições, que não dependem do dia. **Agora** as telas compartilham uma leitura por pessoa e dia, e uma leitura das repetições por pessoa (`SharedRoutineReadings.kt`). Cada tela continua com o seu ViewModel, porque cada uma guarda o próprio dia escolhido | `TodayScreen`, `TasksScreen`, `RoutineScreen`, `RoutineViewModel` | Código |
| E6 | ~~P2~~ feito | **Dois fluxos para criar atividade:** Tarefas tem o seu próprio formulário e volta para a lista; a Rotina abre os detalhes da atividade salva. **Agora** as duas seguem a mesma regra (`RoutinePane.afterSave`): criar volta para a lista, e editar volta para os detalhes. A Rotina também vai para o dia da atividade salva | `TasksRoute`, `RoutineRoute` | Código |
| E7 | ~~P2~~ feito | **Três implementações de lista com detalhe:** `OriListDetailLayout` em Acompanhados; Rotina e Organizar pranchas fazem a sua com `BoxWithConstraints`, e cada uma repete `TwoPaneMinWidth = 720.dp`. **Agora** Rotina e Organizar usam o mesmo `AreaListDetailLayout`, decidido pelo espaço que a área recebe, com os 720 dp num lugar só. Acompanhados continua com `OriListDetailLayout`, porque é uma tela inteira: a janela decide e a dobradiça dos dobráveis é evitada | `RoutineScreen`, `AacOrganizeRoute`, `CareRecipientsScreen` | Código |
| E8 | ~~P2~~ feito | **Navegação feita à mão em oito lugares.** Cada área guarda "o que está aberto" em `rememberSaveable` com `BackHandler` próprio. Não há pilha comum, então voltar se comporta de jeitos diferentes e não existem links diretos para uma tela. **Agora** as áreas abrem subtelas do mesmo jeito (`rememberOpenSubscreen`): uma por vez, guardada se o app for fechado em segundo plano, e o voltar do sistema fecha a mais interna primeiro. Links diretos para telas ficaram de fora: o app ainda não tem nada que abra uma tela de fora dele | `MainScaffold`, `HomeScreen`, `ProfileScreen`, `SettingsScreen`, `AacRoute`, `AacOrganizeRoute`, `RoutineScreen`, `TasksScreen` | Código |
| E9 | ~~P2~~ feito | **O perfil depende da comunicação:**<br>• `ProfileAvatar` desenha com `SymbolPicture`;<br>• a foto de perfil usa `SymbolPhotoStore` e `PhotoImportError`, de `domain.aac`;<br>• a mensagem de erro vem de `aac.messageRes`.<br>O nome "SymbolPhotoStore" também já não descreve o que a classe faz, que inclui sincronizar com a nuvem. As fotos mereciam um pacote próprio (`domain/photos` já existe em parte). **Agora** as fotos têm o pacote `photos` (`PhotoStore`, `StoredPhoto`, `DevicePhotoStore`, `FirestoreCloudPhotoRepository`), o desenho de uma foto é `ui/components/photo/StoredPhotoImage` e as mensagens de erro de foto estão em `ui/app/screens/common`. O perfil não importa mais nada da CAA | `account/ProfileAvatar.kt`, `data/profile/DeviceProfilePhotoStore.kt` | Código |
| E10 | ~~P2~~ feito | **Papéis antigos ainda no código.** A conta é única, mas `AccountRole.PRIMARY_USER` e `CAREGIVER` ainda decidem a tela de convite e os vínculos, e o cadastro grava `role` porque a regra `isValidNewProfile` exige esse campo. **Agora** o cadastro grava só o nome, e as regras publicadas recusam `role` em perfis novos. O que restou era o lado do vínculo que cada tela mostra, renomeado para `CareLinkSide`. `UserPlan`, que não era usado, saiu | `InvitationSection`, `CareLinksViewModel`, `firestore.rules` | Código |

### Estado, dados e manutenção

| # | P | Problema | Onde | Visto em |
|---|---|---|---|---|
| E11 | ~~P3~~ feito | **Estado mutável fora do `StateFlow`:**<br>• `latestTasks`, `latestSymbols` e `latestBoards` são atualizados como efeito colateral dentro de `map`;<br>• `hasItemsOverLimit` é um campo de uma classe injetada;<br>• `isMoving` é um `var` comum. **Agora** o que foi lido fica junto do estado no próprio fluxo (`Day`, `LowerContent`, `Listed`); a passagem para a conta guarda o que aprende numa tentativa (`Attempt`); a sessão guarda o trabalho em andamento (`Job`) | `RoutineViewModel`, `AacCommunicateViewModel`, `AacOrganizeViewModel`, `MoveLocalDataToAccount`, `SessionViewModel` | Código |
| E12 | ~~P3~~ feito | **A foto de perfil grava arquivo dentro do `map` de um Flow:** cada coleta repete a gravação. **Agora** o caminho vem só do que está guardado no aparelho, e a cópia acompanha a conta num trabalho separado, enquanto a foto é mostrada | `DeviceProfilePhotoStore.observePhoto` | Código |
| E13 | ~~P3~~ feito | **Português fixo no código:**<br>• `BrazilianPortuguese` nos calendários;<br>• a voz em `pt-BR`;<br>• os verbos e estados da gramática;<br>• o formato de data dos Relatórios, sem locale.<br>Hoje o app é só em português, mas isso impede traduzir. **Agora** o idioma fica num lugar só (`AppLocale`): calendários, datas e voz o seguem. A gramática da CAA é do português por natureza e precisaria de tabelas próprias | `WeekStrip`, `RepeatPicker`, `AndroidSpeechSynthesizer`, `SentencePiece`, `ReportsScreen` | Código |
| E14 | ~~P3~~ feito | **Banco local sem histórico de esquema** (`exportSchema = false`): as migrações são SQL escrito à mão, e só o teste atual as confere. **Agora** o Room exporta o esquema para `app/schemas` a partir da versão 3; as versões 1 e 2 não existem mais para exportar | `OriDatabase` | Código |
| E15 | ~~P3~~ feito | **Arquivos acima de 250 linhas:** `LocalRepositories` (282), `AacCommunicateViewModel` (279) e `AacOrganizeViewModel` (260). **Agora** `LocalRepositories` virou três arquivos (comum, rotina e comunicação), a leitura da CAA saiu para `AacBoardContent`, a mensagem para `MessageDraft`, as listas do Organizar para `Listed.kt`, o dia do calendário para `DayCell.kt` e a conversão do cartão para `RoutineStepUiModel.kt`. Nenhum arquivo passa de 250 linhas | — | Contagem |
| E16 | ~~P3~~ feito | **Recursos sem uso:**<br>• textos `day_go_to_today`, `day_next`, `day_previous`, `preferences_*_group*` e `reports_period`;<br>• cores do modelo (`purple_*`, `teal_*`, `black`, `white`);<br>• `ic_keyboard`. **Agora** foram removidos, junto com as cores do modelo e o ícone | `res/` | Lint |
| E17 | ~~P3~~ feito | **Gravações em duas etapas, sem atomicidade:** ligar ou desligar a repetição e marcar o primeiro dia de uma série. A forma nova é gravada antes da antiga ser apagada, então nada se perde, mas pode sobrar algo. **Agora** ligar e desligar a repetição é uma gravação só (lote na nuvem, transação no aparelho). Marcar um dia pela primeira vez continua em dois passos: as regras exigem que uma atividade nova comece não concluída | `RepeatingRoutineRepository` | Código (documentado) |
| E18 | decidido | **Limite de 100 fotos sem garantia:** só o app confere e, sem internet, aceita a foto mesmo assim. **Decidido: não fazer agora.** Um contador nas regras exigiria transação, que não funciona offline, então salvar foto sem internet pararia de funcionar. A mesma falta de limite existe em todas as coleções; o lugar certo é o servidor (Cloud Functions, plano Blaze) | `FirestoreCloudPhotoRepository` | Código (documentado) |

## 2. Design (requisitos não funcionais)

### Aparência e consistência

| # | P | Problema | Onde | Visto em |
|---|---|---|---|---|
| D1 | ~~P2~~ feito | **Os cartões do resumo de Relatórios têm alturas diferentes.** "Do planejado" cabe em uma linha e fica menor que "Atividades concluídas" e "Vezes que falou", que quebram em duas. Em maiúsculas todos quebram e ficam iguais. Os cartões ficam em um `FlowRow` sem altura comum | `ReportsScreen.SummaryTile` | Imagem |
| D2 | ~~P2~~ feito | **Margens laterais diferentes:** Organizar pranchas e Acompanhados usam 16 dp fixos, e as outras telas usam a margem da janela (24 dp no celular). "Nova prancha" fica mais largo que os botões das outras telas. **Agora** as duas usam `CenteredContent`, como as outras | `OrganizerPanes`, `CareRecipientsScreen` | Imagem e código |
| D3 | ~~P2~~ feito | **O título da barra começa em 16 dp, e o conteúdo em 24 dp.** † **Agora** o título, ou a seta de voltar, começa na margem do conteúdo, e o "Aa" termina nela | `OriTopBar` | Imagem |
| D4 | ~~P2~~ feito | **Cabeçalhos diferentes:** a maioria usa `OriTopBar`, mas o Perfil tem um cabeçalho colorido, Frases tem abas no lugar do título e Login e Cadastro mostram "ori". O botão "Aa" muda de lugar a cada um. **Agora** o Perfil usa a barra comum, com o cartão colorido logo abaixo, e o "Aa" fica sempre no mesmo canto. Frases mantém as abas no lugar do título, de propósito, para os símbolos terem altura; Login e Cadastro mantêm a marca, por serem a entrada do app | `ProfileScreen`, `AacCommunicateScreen`, `BrandHeader` | Imagem |
| D5 | ~~P2~~ feito | **Quatro desenhos de linha de lista:**<br>• menus, com ícone colorido e seta;<br>• `OriListItem`, em Acompanhados e vínculos;<br>• pranchas no Organizar, cartões altos com setas ↑↓ e nada indicando que abrem;<br>• cartões de frase.<br>**Agora** menus e listas são desenhados sobre a mesma base (`OriRow`), com o fundo, a borda e os cantos de `OriCard`, e o que abre outra tela termina com a seta (`OriOpenArrow`), inclusive as pranchas. Os cartões de frase já usavam `OriCard` e continuam maiores, porque são o botão de falar | `OriMenuRow`, `OriListItem`, `OrganizerPanes`, `AacPhrasesPane` | Imagem |
| D6 | ~~P2~~ feito | **A mesma atividade com dois desenhos:** na Rotina, horário à esquerda e faixa colorida; em Tarefas, cartão branco com o horário embaixo e sem cor. **Agora** Tarefas usa a linha do tempo da Rotina, e a cor vem da atividade, não da posição | `RoutineTimeline`, `RoutineStepCard` | Imagem |
| D7 | ~~P2~~ feito | **Dois estilos de seleção:** as abas (`OriFilterTabs`) ficam em teal cheio; os chips de pranchas e de repetição (`FilterChip`) usam o azul-claro do Material. **Agora** todos usam `OriChoiceChip`, em teal. As opções da repetição também passaram a ser anunciadas como opções de um grupo, e as pranchas como abas | `OriFilterTabs`, `BoardSelector`, `RepeatPicker` | Imagem |
| D8 | ~~P3~~ feito | **Cantos diferentes para coisas parecidas:**<br>• botões em pílula;<br>• abas e ações da CAA com canto médio;<br>• cartões com canto grande, menos os da linha do tempo, com canto médio. **Agora** a regra é: o que mostra conteúdo (cartões, linhas, blocos de resumo) tem canto grande; controles (campos, abas, escolhas) têm canto médio; botões são pílulas. A linha do tempo e os blocos de Relatórios passaram para o canto grande | vários | Código |
| D9 | ~~P3~~ feito | **Dois estilos de título de seção:** `SectionHeader`, pequeno e cinza (Frases salvas, Imagem), e o título próprio de Relatórios, maior e escuro (Resumo geral), que ainda aparecia centralizado. **Agora** há só `SectionHeader`, no estilo de Relatórios e alinhado à esquerda, sem recuo. As linhas de Configurações também perderam o recuo de 16 dp, para começar na mesma borda | `ReportsScreen.SectionTitle` | Imagem |
| D10 | ~~P3~~ feito | **Quatro componentes para uma linha de aviso:** `AacNote`, `RoutineNotice`, `LinksNote` e `FormMessage`. **Agora** há um só `OriNotice`; ele é anunciado pelo leitor de tela só quando avisa algo que muda (conexão, limite). `FormMessage` continua para o retorno de uma ação, num tamanho legível | — | Código |
| D11 | ~~P3~~ feito | **Abas de Tarefas cortadas:** em 360 dp, "Todas" encosta na borda e perde o contorno da direita, e nada indica que a fila rola. **Agora** as abas quebram para a linha de baixo quando não cabem, e nenhuma fica fora da tela | `OriFilterTabs` | Imagem |
| D12 | ~~P3~~ feito | **Cor escolhida pela posição, sem significado:** cartões de Relatórios (`accents[0]`, `[5]`, `[3]`), linha do tempo e linhas da CAA †. Na linha do tempo, a cor agora vem da atividade e não muda de uma tela para outra, mas continua sem significado. **Agora** a cor tem significado: nos menus e em Relatórios, cada área tem a sua (`OriAreaAccents`); nas pranchas, pessoas em amarelo, ações em verde e sentimentos em azul, como nas chaves de cor de CAA, e as outras palavras ficam sem cor; na linha do tempo, a cor vem da atividade | `ReportsScreen`, `RoutineTimeline`, `AacSymbolTile` | Código |
| D13 | ~~P3~~ feito | **Medidas soltas fora de `OriSpacing`:** Cadastro (9), Login (8), Relatórios (5), Complete seu perfil (5) e outras † **Agora** os espaços usam os degraus de `OriSpacing`; ficaram só medidas que são tamanho de algo (a altura do gráfico, a faixa de cor, o ponto do dia) | — | Código |
| D14 | decidido | **Organizar pranchas:** o cartão não mostra quantos símbolos a prancha tem, e só dá para reordenar com setas. **Decidido: não fazer agora.** Contar os símbolos custaria uma leitura por prancha a cada abertura (ou um contador mantido nas regras), e as setas são o jeito de reordenar que funciona para todos, inclusive com leitor de tela | `OrganizerPanes` | Imagem |

### Acessibilidade

| # | P | Problema | Onde | Visto em |
|---|---|---|---|---|
| A1 | — | **Não era defeito.** O botão de mostrar senha é desenhado com 36 dp, mas o Android já amplia a área de toque para 48 dp. Um teste com o botão antigo confirmou isso, então nada foi mudado | `OriTextField.PasswordVisibilityButton` | Teste |
| A2 | ~~P2~~ feito | **Os círculos dos dias na repetição têm 40 dp.** Sete círculos de 48 dp não cabem em 360 dp, então precisam de outro arranjo, como duas linhas ou círculos que ocupem a largura. **Agora** cada dia ocupa uma fatia igual da linha, com 48 dp de altura (44 dp de largura num celular de 360 dp); o círculo continua com 40 dp | `RepeatPicker.DayCircle` | Código |
| A3 | decidido | **Dias de outro mês com 30% de opacidade:** ficaram abaixo do contraste AA, como pedido, mas continuam tocáveis. Vale confirmar se é aceitável. **Decidido: manter a opacidade**, como pedido. O que faltava era o leitor de tela: agora um dia de outro mês é anunciado com o mês ("domingo, 1 de novembro") | `DayCell` | Código |
| A4 | ~~P3~~ feito | **"Bem-vindo(a)!" marca gênero,** enquanto o resto do app evita. **Agora** é "Boas-vindas!" | `login_welcome_title` | Texto |

### Uso

| # | P | Problema | Onde | Visto em |
|---|---|---|---|---|
| U1 | ~~P2~~ feito | **Tarefas só mostra hoje.** Não dá para trocar de dia, e "Todas" significa "todas de hoje". **Agora** Tarefas diz o dia ("Hoje, quarta-feira, 4 de novembro"); trocar de dia continua sendo papel da Rotina | `TasksScreen` | Código |
| U2 | ~~P2~~ feito | **O mês não marca os dias com atividades,** ao contrário do calendário da Samsung. **Agora** o mês aberto marca com um ponto os dias que têm atividades, e o leitor de tela diz "tem atividades". É uma leitura por intervalo quando o mês aparece; para quem acompanha, ela usa o índice de `firestore.indexes.json` | `RoutineCalendar` | Código (documentado) |
| U3 | ~~P2~~ feito | **Sair da conta não pede confirmação** e aparece em vermelho † **Agora** pede confirmação e não é mais vermelho: sair não apaga nada | `ProfileScreen` | Código |
| U4 | ~~P2~~ feito | **Configurações repete Acessibilidade e Comunicação,** que também estão no Perfil † **Agora** Acessibilidade e Comunicação ficam só em Configurações | `SettingsScreen`, `ProfileScreen` | Código |
| U5 | ~~P3~~ feito | **O horário é digitado à mão,** em vez de escolhido em um seletor † **Agora** o campo tem "Escolher", que abre o relógio do sistema; digitar continua possível | `ActivityEditorScreen` | Código |
| U6 | ~~P3~~ feito | **A frase montada mostra só o fim** quando é longa † **Agora** "…" no começo mostra que há palavras roladas para a esquerda. A frase continua numa linha só, para os símbolos não saírem de baixo do dedo; "Mostrar" e o leitor de tela dão a frase inteira | `AacMessageBar` | Imagem anterior |

### Desempenho e custo

| # | P | Problema | Onde | Visto em |
|---|---|---|---|---|
| C1 | ~~P2~~ feito | **Listeners repetidos** (ver E5): até seis abertos para o mesmo dia, e o das repetições recriado a cada troca de dia. Quanto isso custa depende de o Firestore aproveitar o cache, o que não foi medido. **Agora** há um listener por dia olhado e um das repetições por pessoa; os testes contam quantos são abertos. O efeito na conta do Firestore continua sem medição | `RoutineViewModel` | Código |
| C2 | ~~P3~~ feito | **Relatórios releem tudo:** o período inteiro (até 31 dias e as repetições) a cada abertura e a cada troca entre semana e mês, sem guardar o resultado. **Agora** o período é lido numa consulta por intervalo, e a tela se atualiza ao ser aberta de novo, mantendo os números enquanto lê (antes ela podia mostrar números antigos) | `ReportsViewModel` | Código |

## 3. Ordem sugerida

1. ~~E1, D1 e E2~~ feitos. A1 não era defeito.
2. ~~E5, C1 e E6~~ feitos.
3. ~~D2, D3, D5, D6, D7 e D9~~ feitos.
4. ~~E9 e E10~~ feitos; as regras foram publicadas.
5. ~~O restante~~ feito, com E18, D14 e A3 decididos.

## 4. Melhorias encontradas depois

| # | O que era | O que mudou |
|---|---|---|
| N1 | Relatórios não se atualizavam ao serem abertos de novo: o ViewModel sobrevivia e ignorava o mesmo dono, então os números podiam ser antigos | Atualizam a cada abertura, mantendo os números na tela enquanto leem (ver C2) |
| N2 | Uma foto de símbolo era decodificada de novo cada vez que voltava à tela ao rolar a grade, com o ícone neutro piscando | As fotos decodificadas recentes ficam num cache limitado a 1/16 da memória do app (`DecodedPhotos`) |
| N3 | "1 de 1 atividades concluídas": textos com número sem plural (lint) | `plurals` em Início e Relatórios |
| N4 | Os botões tinham "largura total" escondida no `Modifier` padrão, e duas telas passavam `Modifier` vazio para desligá-la (lint) | Parâmetro explícito `isFullWidth` |
| N5 | Três imagens PNG numa pasta sem densidade e um rótulo repetido no manifesto (lint) | Imagens em `drawable-nodpi`, rótulo removido |
| N6 | Um dia de outro mês aparecia apagado, mas o leitor de tela dizia só "segunda, 1" | O anúncio inclui o mês (ver A3) |
| N7 | Mensagens de erro de formulário em letra muito pequena (`labelSmall`) | Mesmo tamanho do texto comum (`bodyMedium`) |
