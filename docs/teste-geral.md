# Teste geral no tablet (12/11/2026)

Teste de todas as áreas do ORI no tablet de teste (Samsung SM-X610, deitado, Android 16, sem conta: uso só no
aparelho), com toques, segurar, arrastar e as personalizações ligadas e desligadas. O objetivo foi achar **erros**,
**pontos de melhora** e **escolhas de design duvidosas**. As falhas que ficaram abertas também estão em
[revisao-de-codigo.md](revisao-de-codigo.md#18-teste-geral-no-tablet-12112026-aberto), com onde estão e a correção
sugerida.

O app estava em inglês (Austrália) no tablet: é o idioma escolhido para o ORI nas configurações do Android, com o
sistema em português. Os textos citados abaixo estão como apareceram.

Tudo o que o teste criou foi apagado no fim (a atividade "TESTE ORI apagar", a palavra "testeori", a frase salva "I
want water"); a ordem das palavras da prancha voltou à original, e as opções do ORI Home voltaram ao que eram
(médio, quadrado, lista, tela inicial e todos os apps, sem baixa estimulação, tela destravada).

## O que foi testado

| Área | Ações | Variações |
|---|---|---|
| Início | Cartões, resumo do dia, "Aa" | Texto do sistema e "Muito grande" |
| Rotina | Criar atividade com hora e "Dias úteis", conferir sexta e sábado, abrir detalhes, marcar feita e pendente, segurar, apagar "Todos os dias", voltar com o teclado aberto e fechado | Tablet deitado (lista e detalhes lado a lado) |
| Tarefas | Abas Pendentes, Feitas, Todas | — |
| Frases (CAA) | Tocar e segurar frase pronta; montar "I want water"; letras grandes; salvar, ver e apagar a frase salva; Escrever; organizar pranchas: setas, segurar e arrastar, adicionar e apagar palavra | Texto "Muito grande" |
| Relatórios | Semana e mês, gráfico | — |
| Perfil e configurações | Perfil sem conta, Configurações, Acessibilidade (tamanho do texto, maiúsculas, interface simplificada, animações) | — |
| ORI Home | Segurar ícone (menu com o dedo na tela), mover e soltar, menu de bloco, edição, opções e avançadas, busca, tela travada | Médio e Máximo; quadrado e redondo; lista e telas; tela inicial e só todos os apps; baixa estimulação; teclado aberto |

## O que funcionou

- **Rotina:** a atividade de "Dias úteis" aparece na sexta e não no sábado; marcar feita muda o Início ("1 de 1
  atividade feita hoje") e Tarefas; apagar uma atividade que se repete pergunta "Só este dia" ou "Todos os dias".
- **Voltar:** com o teclado aberto, o primeiro voltar só fecha o teclado; sem teclado, fecha o formulário.
- **CAA:** montar uma frase por símbolos, desfazer cada palavra tocando nela, mostrar em letras grandes, salvar e
  apagar (com confirmação). Organizar: as setas mudam a ordem, segurar e arrastar também; a seta de quem já está no
  começo fica apagada; adicionar e apagar palavra (com confirmação, "não pode ser desfeito").
- **ORI Home:** menu ao segurar (com o dedo na tela) também nos blocos do Máximo e em todos os apps; mover fecha o
  menu e carrega o item; Máximo com blocos coloridos na tela inicial e em todos os apps, em lista e em telas; baixa
  estimulação deixa blocos e ícones cinza; ícones redondos e quadrados iguais para todos; só todos os apps com a hora,
  as opções e a busca; a busca acha "cal" (Calculator, Calendar); tela travada não deixa mover e avisa.
- **Acessibilidade:** "Muito grande" aumenta os textos sem cortar o Início e a CAA; o painel lateral acompanha.

## Erros

| Id | Onde | O que acontece | Gravidade |
|---|---|---|---|
| G1 | ORI Home, busca de todos os apps (e qualquer teclado aberto no ORI Home) | Abrir o teclado muda a grade: no médio, todos os apps passam de 8 para 5 colunas; no Máximo, de 4 para 3 blocos. A tela inicial por trás também é medida de novo. A grade usa as margens "seguras" do Android, que incluem o teclado (`HomeMetrics.kt`, `WindowInsets.safeDrawing`) | Média |
| G2 | Rotina, apagar uma atividade que se repete, "Todos os dias" | O aviso diz que "os dias já marcados como feitos ficam nos relatórios", mas o relatório do mês caiu de 2 para 1 atividade concluída depois de apagar a de teste (feita hoje): o dia feito saiu junto | Média |
| G3 | Nova atividade, tablet deitado com o teclado aberto | A barra "Cancelar / Salvar" sobe com o teclado e fica onde estava o campo de hora; um toque ali cancela na hora, sem perguntar, e o que foi escrito se perde (aconteceu duas vezes no teste) | Média |
| G4 | Campo de hora da atividade | Aceita qualquer texto ("abc"), com o teclado de letras; o exemplo diz "8:30 AM" (12 h), e o app mostra a hora como "09:15" (24 h) | Baixa |
| G5 | CAA, organizar a prancha pelas setas | Depois de mover, às vezes a lista rola para a palavra ficar sob o dedo e às vezes não (no começo da lista ela não rola): tocar de novo no mesmo ponto pode mover outra palavra | Baixa |

## Pontos de melhora

| Id | Onde | O que melhorar |
|---|---|---|
| P1 | CAA, frases prontas | Tocar não dá nenhum retorno visual (nem destaque, nem "falando"). Com o volume no zero (o tablet estava no silencioso), nada indica que a voz não foi ouvida. Destacar a frase enquanto fala e avisar quando o volume está no zero |
| P2 | CAA, salvar frase | Salvar uma frase igual a uma pronta ("I want water") cria uma cópia sem avisar |
| P3 | CAA, adicionar palavra | Depois de salvar, volta à lista sem confirmação e sem mostrar a palavra nova (fica no fim, fora da vista). Rolar até ela e destacá-la |
| P4 | CAA, organizar | "Says: X" em cada linha, mesmo quando o que é falado é igual ao rótulo: mostrar só quando for diferente |
| P5 | CAA com texto "Muito grande" | Os rótulos das palavras quase não crescem (a CAA tem o tamanho próprio dela). Seguir a preferência do app ou explicar onde mudar |
| P6 | Formulários com o teclado aberto no tablet deitado | Sobra uma faixa pequena: na Nova atividade, só o nome; no Escrever da CAA, "Keep on the board, with a picture" fica cortado atrás da grade |
| P7 | Rotina | Segurar uma atividade só abre os detalhes; não há ações rápidas (concluir, apagar) nem arrastar para outra hora ou dia |
| P8 | Relatórios | O gráfico de progresso tem só pontos soltos, sem linha nem barras, e só a primeira e a última data: difícil de ler |
| P9 | ORI Home, tela travada | O aviso de tela travada só aparece depois de soltar o dedo; enquanto arrasta, nada acontece. Avisar assim que o dedo se mexe |
| P10 | ORI Home, todos os apps em telas | Sobra um espaço vazio grande entre a última fileira e os pontos (as fileiras não se esticam); no Máximo, perto de um quinto da tela |
| P11 | ORI Home, Máximo | Apps de ícone branco (Play Store, Copilot, Assistant) ficam com o bloco cinza, a média das cores, e parecem desligados. Usar a cor mais viva do ícone ou o fundo do ícone adaptativo |
| P12 | Detalhes da atividade | "Repeats" aparece duas vezes: no texto ("Repeats: Monday to Friday…") e como etiqueta |

## Escolhas de design duvidosas

| Id | Onde | O que é duvidoso |
|---|---|---|
| D1 | Textos do app | A marca aparece de dois jeitos: "ori" minúsculo ("ori works without internet…", "About ori") e "ORI" (ORI Home, Abrir o ORI) |
| D2 | Tablet deitado | Larguras diferentes entre telas: Início e Perfil numa coluna de uns 650 dp, Acessibilidade mais estreita (uns 420 dp); no "só todos os apps", a hora e a busca numa coluna estreita e os blocos de ponta a ponta. Nas telas de uma coluna sobra mais da metade da tela vazia |
| D3 | Nova atividade no tablet | Duas faixas de semana ao mesmo tempo (a da lista e a do formulário), com o mesmo dia marcado: não fica claro qual escolhe o dia da atividade |
| D4 | Idioma | O app em inglês com o sistema em português (idioma por app do Android). Não é erro, mas datas do sistema em português e do app em inglês convivem; confirmar se é o que se quer no tablet |
| D5 | CAA, frases prontas | Uma frase longa ("I need to go to the bathroom") deixa a linha inteira mais alta; "with" é a única palavra sem figura entre as que têm |
| D6 | ORI Home, Máximo | Cada app com a sua cor pode ser estímulo demais; fora da baixa estimulação não há opção de uma cor só (como o Windows Phone fazia com a cor de destaque) |

## O que não foi testado

- **Celular:** só o tablet estava conectado.
- **Conta e nuvem:** o tablet usa o ORI sem conta.
- **Voz:** o tablet estava no silencioso; não deu para ouvir a fala da CAA.
- **Leitor de tela (TalkBack):** ligar é uma configuração do sistema, que fica com a pessoa.
- **Selos de notificação:** o "Acesso a notificações" ainda não foi permitido.
- **Widget no Máximo:** o redimensionar widget não foi repetido no Máximo. As pastas no Máximo foram testadas
  depois (veja abaixo).
- **Tema claro e animações desligadas** no app principal (testados depois, no segundo teste, abaixo).

## Depois do teste (12/11/2026): níveis de estímulo e pastas no Máximo

Feito no tablet, deitado, com o build `profiling`:

- **Níveis de estímulo:** "Nenhum" tirou o papel de parede do Android (fundo liso do tema) e deixou os apps cinza;
  ligar "Ícones coloridos" à mão marcou "Personalizado"; "Baixo" trouxe de volta o papel de parede e as cores. A
  diferença das animações ("Médio" e "Baixo") não aparece numa foto da tela: ela foi conferida nos testes
  (`LauncherViewModelTest`).
- **Pastas no Máximo:** soltar um bloco no meio de outro cria a pasta, que aparece como bloco transparente com os dois
  apps pequenos; tocar abre; tirar um app pelo menu deixa a pasta de um app só virar esse app. Dois problemas
  apareceram e foram corrigidos na hora:
  - a pasta aberta mostrava os apps como ícones comuns, não como blocos (agora são blocos, como na tela inicial e em
    todos os apps);
  - ao segurar um bloco sobre outro, o bloco carregado (do mesmo tamanho) cobria o de baixo, e não dava para ver que
    ia virar pasta (agora ele fica pequeno e o bloco de baixo aparece com a pasta desenhada em volta).
- **Aviso de volume no zero (CAA):** conferido nos testes e numa imagem a 360 dp; **não** no tablet, porque seria
  preciso baixar o volume de mídia do aparelho.

A tela inicial do tablet voltou como estava (nível Padrão, ícones médios, cada app no seu lugar).

## Segundo teste (13/11/2026): desempenho e design

Mesmo tablet, deitado, agora com o build `profiling` (o único que mede a fluidez de verdade: o de depuração é
várias vezes mais lento). Duas diferenças para o primeiro teste: o ORI está **com conta conectada**, e o acesso às
notificações foi permitido (os selos aparecem). O app continua em inglês, com o tema do sistema (escuro). A tela do
tablet desenha a **90 Hz**: cada quadro tem 11 ms para ficar pronto.

**Como foi medido.** Roteiros de toques pelo `adb`, cada um repetido de 3 a 6 vezes, com os contadores de
`adb shell dumpsys gfxinfo com.oriteam.ori` zerados antes de cada roteiro. Para os quadros longos,
`dumpsys gfxinfo com.oriteam.ori framestats` separa o tempo de cada fase: recompor, medir, desenhar e a espera antes
do quadro começar. Duas atividades de teste ("TESTE ORI A" e "TESTE ORI B", de hoje) foram criadas para medir a Rotina
com conteúdo e apagadas no fim; o tema e as animações voltaram a "Igual ao sistema". Os erros e as causas de lentidão,
com onde estão e a correção sugerida, também estão na seção 19 de [revisao-de-codigo.md](revisao-de-codigo.md).

### Medidas

| O quê | Resultado |
|---|---|
| Abrir o ORI Home do zero (processo parado) | 0,22 a 0,30 s |
| Abrir o app com o ORI Home já aberto | 0,08 s; com o app já aberto por trás, 0,05 a 0,07 s |
| Parado no Início ou no ORI Home (8 a 10 s) | Nenhum quadro desenhado: nada fica animando sem motivo |
| Memória (PSS) | 170 a 195 MB; o One UI Home, na mesma hora, 178 MB |

| Roteiro | Quadros com atraso | Quadros de 50 ms ou mais |
|---|---|---|
| Trocar de área pela barra lateral (12 trocas) | 5,0% | 9, até 125 ms (o pior na primeira vez) |
| Rotina: semana pelas setas / arrastando / abrir o mês | 4,0% / 1,7% / 3,1% | 0 |
| Rotina: abrir detalhes / concluir e desfazer | 4,5% / 3,0% | 0 |
| Nova atividade: abrir / teclado e digitação | 11,8% (2 de 17 quadros) / 6,9% | 0 |
| Frases: Prontas ↔ Montar (6 trocas) | 3,0% | 4, até 73 ms (na primeira vez, 133 ms) |
| Montar: tocar 5 símbolos / rolar a prancha | 4,2% / 2,8% | 0 |
| Relatórios: abrir / semana ↔ mês | 2,3% / 2,6% | 2 ao abrir, até 73 ms |
| ORI Home: passar páginas / rolar todos os apps | 2,1% / 2,4% | 0 |
| ORI Home: abrir todos os apps | 4,7% | 2, até 69 ms |
| ORI Home: abrir a busca (teclado) | 8,5% | 0 (quadros de 31 a 42 ms) |
| **ORI Home: segurar um ícone (menu)** | **16,4%** (depois da correção do M15: 5,4%) | **3 por menu, de 70 a 100 ms** (depois: nenhum) |
| ORI Home: segurar um espaço vazio (edição) | 3,2% | 0 |

**Leitura.** Rolar, arrastar e passar páginas estão fluidos: menos de 3% de atraso e nenhum quadro longo. Os atrasos
ficam no **primeiro quadro de uma tela nova** (trocar de área, abrir "Montar") e no **menu do ícone** do ORI Home.

### O que causa os quadros longos

- **Menu ao segurar um ícone (M15).** Um quadro de 35 ms recompondo o menu e, logo depois, cerca de 90 ms com a thread
  principal parada antes do quadro seguinte: é o Android criando a janela nova do menu (`AppMenu` usa um `Popup`, que é
  uma janela à parte). Acontece igual em apps sem atalhos, então não é a leitura dos atalhos (que já roda fora da
  thread principal). A edição da tela inicial, desenhada na própria janela, abre com um só quadro de atraso.
  **Corrigido no mesmo dia:** o menu passou a ser desenhado na janela da tela inicial (`MenuLayer`). Medido de novo no
  tablet, com o mesmo roteiro: 6,2% e 5,4% de quadros com atraso (antes 16,3% e 16,4%) e nenhum quadro de 50 ms ou
  mais (o pior, 37 ms). Teste: `HomeMenuLayerTest`.
- **Abrir "Montar" (M16).** Um quadro de 40 a 130 ms, quase todo em desenhar (medir e desenhar os símbolos com emoji).
  Repete a cada troca de aba, porque a prancha é montada de novo (`when (tab)` em `AacCommunicateScreen`).
  **Corrigido no mesmo dia** (terceiro teste, abaixo): de 26 a 55 ms.
- **Trocar de área (M2, ainda parcial).** O primeiro quadro da tela nova leva de 30 a 60 ms: 17 a 22 ms recompondo e 8
  a 10 ms desenhando.
- **Animações "Desligadas".** As transições somem, mas cada toque ainda desenha cerca de meio segundo de quadros: é a
  onda do toque do Material, que fica de propósito como retorno de que o toque foi recebido.

### Erros novos

| Id | Onde | O que acontece | Gravidade |
|---|---|---|---|
| G6 | Rotina no tablet, com uma atividade aberta nos detalhes | Mudar de semana deixa o painel da direita dizendo "Activity not found — It may have been deleted or be on another day", sem que nada tenha sido apagado: a atividade só está em outro dia. **Corrigido no mesmo dia:** trocar de dia fecha os detalhes (teste em `RoutineScreenLayoutTest`) | Média |
| G7 | Campos de texto, chips de escolha e o círculo de "Concluir" | A borda tem contraste abaixo de 3:1, o mínimo para controles (WCAG 1.4.11): campo e chip com 1,1:1 no tema claro e 1,5:1 no escuro; o círculo de concluir com 1,5:1 no claro. No tema claro, o campo branco sobre o fundo quase branco quase não aparece. **Corrigido no mesmo dia:** a borda de controle (`outline`) passou a #7A8C9C no claro (3,3:1 a 3,5:1) e é a de campos e chips nos dois temas (teste em `OriThemeRulesTest`) | Média |

### Ainda abertos (conferidos de novo)

- **G1:** abrir a busca de todos os apps ainda muda a grade de 8 para 5 colunas (e custa 8,5% de quadros com atraso).
- **G3:** com o teclado aberto, a barra "Cancel / Save" ainda cobre o campo de hora; o campo com foco fica pela metade
  atrás dela.
- **G4:** o exemplo da hora ainda é "8:30 AM", com o teclado de letras.

### Pontos de melhora novos

| Id | Onde | O que melhorar |
|---|---|---|
| P13 | Rotina, faixa da semana | Os pontinhos de "tem atividade" só aparecem com o mês aberto; na semana, a vista de todo dia, não há nenhum. Mostrar também na semana (são 7 dias, uma leitura menor que a do mês) |
| P14 | Tarefas | Tocar no cartão não faz nada: só o círculo marca como feita. O mesmo cartão, na Rotina, abre os detalhes. Em Tarefas, o cartão inteiro poderia marcar como feita (ou abrir os detalhes, como na Rotina) |
| P15 | Relatórios, semana e mês atuais | As atividades de hoje que ainda não chegaram contam como não feitas: às 6 h, com duas atividades marcadas para 8 h e 10h30, a semana mostrava "0%" e um ponto em 0% no gráfico. Contar só o que já passou da hora, ou só os dias que terminaram |
| P16 | Rotina, dia sem atividades | O painel da direita diz "Choose an activity — The details show up here", mas não há nada para escolher. Mostrar o convite para adicionar, ou nada |
| P17 | Frases, aba Prontas | Com frases salvas, a lista delas tem o título "Saved phrases", mas as frases que vêm com o app aparecem logo depois, sem título: não fica claro onde uma lista acaba e a outra começa |

### Design duvidoso novo

| Id | Onde | O que é duvidoso |
|---|---|---|
| D7 | Tema claro | Cartões brancos sobre um fundo azul muito claro, com borda quase da cor do fundo: os blocos da prancha e as frases prontas se destacam pouco do fundo. No tema escuro, a separação é maior |

### O que não foi testado

- **Em pé (retrato):** girar a tela é uma configuração do sistema; o tablet ficou deitado.
- **Celular, leitor de tela e voz:** pelos mesmos motivos do primeiro teste.

## Terceiro teste (13/11/2026): quanto cada tela leva para abrir

Pedido: conferir **todas** as telas e corrigir as que levavam mais de 100 ms para abrir, incluindo todos os apps do
ORI Home. Mesmo tablet, deitado, build `profiling`, ORI com conta, tema escuro, app em inglês.

**Como foi medido.** Com o rastreamento do sistema (`adb shell atrace`, categorias `gfx`, `input` e `view`), o tempo
conta do momento em que o dedo sai da tela (o toque que dispara a ação) até o fim do primeiro quadro desenhado depois
dele: é quando a tela nova aparece. Cada abertura foi repetida de 2 a 3 vezes, e também logo depois de o app reiniciar
("primeira vez"). Três cuidados mudaram os números, e valem para toda medida futura (estão no AGENTS.md):

- **Compilar como a loja.** Instalado pelo cabo, o app ficava com o código sem compilar (`status=verify`) até o aparelho
  ficar ocioso; pela Play Store, o perfil de inicialização é aplicado na instalação. Sem compilar, abrir Frases pela
  primeira vez levava 217 ms; compilado (`adb shell cmd package compile -f -m speed-profile com.oriteam.ori`), 100 ms.
  Todas as medidas abaixo são com o app compilado.
- **Tocar como um dedo.** O `input tap` do `adb` solta a tela em poucos milissegundos. Parado, o tablet desenha a 30 Hz
  e leva uns 60 ms para voltar a 90 Hz, o que um dedo de verdade (uns 90 ms na tela) já cobre. Os toques foram feitos
  com 90 ms de duração.
- **Contar o conteúdo, não só o primeiro quadro.** Quando o conteúdo chega depois (dados lidos, uma janela nova), o
  tempo vai até o quadro em que ele aparece.

### Todas as telas

Antes e depois das correções (ms, do dedo saindo da tela até a tela aparecer). Abaixo de 100 ms, a tela já abria no
tempo pedido e não foi mexida.

| Tela | Antes | Depois |
|---|---|---|
| **ORI Home** | | |
| Todos os apps, deslizando para cima | 83 a 123 (157 logo depois de reiniciar) | **31 a 51**, também logo depois de reiniciar |
| Todos os apps, pelo botão "Apps" da edição | 63 | 26 |
| Busca de todos os apps (teclado) | 60 (quadros de até 26 ms enquanto o teclado sobe: G1) | igual |
| Pasta aberta | 35 a 55 | igual |
| "Adicionar apps" de uma pasta (diálogo) | 83 a 93 (365 sem compilar) | **48 a 55** |
| "Ocultar apps" (diálogo) | 143 a 154 | **66** |
| Edição da tela inicial (segurar o vazio) | um quadro de 29 ms depois que o toque vira "segurar" | igual |
| Opções / Opções avançadas | 50 a 64 / 57 a 83 | igual |
| Papel de parede / prévia de um papel | 40 a 60 / 60 | igual |
| Widgets | 244 na primeira vez (a lista chegava aos 196 ms), 82 depois | **79** na primeira vez, 65 depois |
| Painel à esquerda (Minha rotina) | 42 a 44 | igual |
| Menu de um ícone | corrigido antes (M15) | — |
| "Abrir no ORI" (painel) | 169 até o primeiro quadro, com o app sempre recriado | **100** com o app fechado; aberto, ele volta em 28 e fica pronto em uns 120 |
| Abrir o ORI Home do zero (processo parado) | 220 a 300 (segundo teste) | não mexido: é o Android criando o processo |
| **App: áreas** | | |
| Início / Rotina / Perfil | 20 a 55 | igual |
| Frases (área) | 95 depois da primeira vez; 127 a 165 na primeira vez | **57 a 60** na primeira vez, 62 a 71 depois (M21) |
| Frases: aba Montar | 66 a 73; 136 a 144 na primeira vez | **30 a 55** na primeira vez, 26 a 44 depois (M16) |
| Frases: aba Prontas | 47 | 27 a 30 |
| Frases: voltar de Organizar pranchas ou de Preferências | 82 a 120 | **24 a 62** |
| **App: telas de dentro** | | |
| Tarefas | 28 a 43 | igual |
| Relatórios | 60; 205 a 279 na primeira vez (os números chegam da internet) | igual: depende da rede (M22) |
| Rotina: semana, mês, detalhes, nova atividade, editar | 22 a 45 | igual |
| Perfil: editar perfil, cuidadores, pessoas que apoio, configurações | 46 a 51 | igual |
| Configurações: as 7 telas (de Acessibilidade a Sobre) | 28 a 66 | igual |
| Organizar pranchas, uma prancha, editor de símbolo, novo símbolo | 36 a 62 | igual |
| Preferências da comunicação | 66 | igual |
| "Escrever" (teclado da CAA) | 45 | igual |
| Menus "Mais" da CAA e das opções da prancha (menus do Material) | 68 a 98 | igual (abaixo de 100) |
| **App: diálogos** | | |
| Escolher prancha | 156 | **19 a 27** |
| Mostrar em letras grandes | 159 | **30** |
| Emoji de uma frase salva | 155 | **39 a 43** (a grade de emojis pronta aos 78 a 91) |
| Horário de uma atividade | 128 | **56 a 62** |
| Confirmar que apaga (atividade, frase) | 126 | **55 a 59** |
| Nova prancha | 93 | **33 a 37** |

### O que foi corrigido

- **Diálogos (M17).** Todo `AlertDialog` e `Dialog` do Material é uma janela à parte, e o Android criava essa janela a
  cada abertura: uns 20 ms esperando o sistema registrar a janela, 8 ms ajustando o tamanho dela e um ou dois quadros de
  espera. Agora os diálogos são desenhados na janela da própria tela (`OriDialogHost`, `OriDialog`, `OriAlertDialog`).
  Eles continuam se comportando como diálogos: o leitor de tela anuncia o título e deixa de ler o que está atrás, voltar
  e um toque fora fecham, e o teclado de um campo atrás se fecha. O fundo escurece igual ao do Android (60% de preto,
  medido nas capturas). O escurecimento é desenhado numa camada própria, com a transparência aplicada na cor: na
  primeira versão, cada quadro do escurecimento redesenhava a tela inteira e ocupava a GPU de 10 a 14 ms.
- **Todos os apps (M18).** Ao deslizar para cima, o ORI Home montava de uma vez os cerca de 43 ícones visíveis, com
  nome (60 ms só na thread principal). Agora a lista é montada antes, escondida, assim que a tela inicial termina de
  desenhar e a thread principal fica livre; o deslize só a move. Fechada, ela não é desenhada nem lida pelo leitor de
  tela, volta para o topo e solta o teclado da busca. Mudanças da tela inicial que não mexem na lista (um selo, uma
  mensagem) não a refazem. Cada ícone só monta o menu dele quando o menu abre.
- **Widgets (M19).** A lista de widgets do aparelho é lida quando a edição da tela inicial começa (a um toque de
  "Widgets"), e uma leitura nova não troca mais a lista já mostrada por "Carregando".
- **"Abrir no ORI" (M20).** O painel do ORI Home abria o app com `FLAG_ACTIVITY_CLEAR_TASK`, que fechava e recriava a
  tela principal a cada vez. Agora o app que já está aberto volta como estava, e só a área muda (`onNewIntent`), como
  acontece ao tocar no ícone do app. Conferido no tablet: com o app em Frases, "Abrir no ORI" volta para a mesma janela,
  em "Minha rotina".

- **Frases na primeira vez (M21).** Marcas de rastreamento temporárias em cada parte da tela (retiradas depois)
  mostraram que o desenho das frases era barato (16 frases, menos de 4 ms). O custo estava em três lugares:
  - o motor de voz era criado na thread principal (24 ms consultando o Android); agora é criado numa thread própria;
  - o primeiro quadro abria em "Montar", vazia, e Prontas só aparecia no quadro seguinte (mais 41 ms); agora o
    primeiro quadro já é Prontas;
  - montar a área toda levava de 50 a 94 ms, e agora leva de 7 a 15 ms.
- **A aba Montar (M16).** Os símbolos da prancha de Montar continuam lidos com Prontas aberta, e cada aba, depois de
  aparecer, fica montada fora de vista enquanto a outra está aberta: nem desenhada, nem tocada, nem lida pelo leitor de
  tela. Montar também é montada antes, escondida, quando a tela fica parada, e a tela de comunicação fica montada por
  baixo de Organizar, Preferências e Nova palavra. Trocar de aba ou voltar passou a só mostrar o que já estava pronto.

### Ainda abertos

- **Relatórios na primeira vez (M22).** A tela abre em 36 a 60 ms com "Carregando"; os números chegam da internet em
  150 a 200 ms. Mostrar antes os números guardados no aparelho poderia exibir números incompletos que mudam em seguida.
- **Menus do Material na CAA.** O "Mais" da mensagem e as opções da prancha ainda são janelas à parte: abrem em 68 a
  98 ms e, ao fechar, gastam um quadro de uns 20 ms descartando a janela.

### O que não foi medido

- **Telas antes da conta:** abertura, entrar, criar conta, consentimento e criação do perfil. Para vê-las seria preciso
  sair da conta, e entrar de novo pede a senha, que só a pessoa digita. "Conectar uma conta" só aparece no uso sem conta.
- **Adicionar um widget e o pedido de outro app para fixar um atalho** (`WidgetStepActivity`, `PinRequestActivity`):
  mudariam a tela inicial da pessoa.
- **Leitor de tela nos diálogos novos:** ligar o TalkBack é uma configuração do sistema. O anúncio do título e o fundo
  escondido estão cobertos por `OriDialogLayerTest`.
- **Celular:** só o tablet foi medido.
