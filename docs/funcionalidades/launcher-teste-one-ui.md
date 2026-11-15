# ORI Home no tablet: teste com gestos e comparação com o One UI (11/11/2026)

Teste de cada parte do ORI Home no tablet de teste (Galaxy Tab S9 FE, `RX2WB01E0RH`, Android 16, One UI 8.0.5,
em pé e deitado, navegação por três botões), com gestos de verdade. O documento anota o que funcionou, os problemas (claros e
de design) e onde o ORI Home é diferente da tela inicial do One UI ("One UI Home").

**Situação:** os problemas T1 a T7 e D1 a D15 deste relatório foram tratados em 11/11/2026, conforme
[launcher-plano.md](launcher-plano.md#situação-11112026). A segunda revisão, em pé, achou mais
defeitos (U1 a U16 e V1, todos corrigidos: seção 17 de [revisao-de-codigo.md](../revisao-de-codigo.md)) e mostrou que D1
estava errado sobre o One UI 8: o menu dele não escurece o fundo (é um balão compacto acima do ícone, com o nome e ⓘ);
o do ORI agora também abre acima e não escurece. O relatório
fica como foi escrito no teste.

Como ler: **funcionou** foi feito no aparelho e deu certo; **problema claro** é um erro (o app faz algo errado);
**problema de design** é algo que funciona, mas confunde, atrapalha ou destoa; **diferença** é o que o One UI faz e o
ORI não faz (ou faz de outro jeito), sem ser um erro.

## Como foi testado

- Os toques e gestos foram feitos pelo `adb` (`input tap`, `swipe`, `draganddrop` e `motionevent` para segurar,
  carregar e esperar na borda), e o que estava na tela foi lido pela estrutura de acessibilidade (`uiautomator`) e
  por capturas de tela só do ORI Home, apagadas depois.
- Tudo o que mudou na tela inicial da pessoa foi desfeito no mesmo teste (ícones movidos de volta, app adicionado
  e tirado, prévia de papel de parede cancelada). Nada foi desinstalado, nenhuma tela foi apagada, nenhum papel de
  parede foi aplicado.
- **Não foi possível verificar no aparelho (ainda):** girar o tablet e o TalkBack, que dependem de virar o
  aparelho com a mão e de uma configuração do sistema (veja "Ainda a testar"), e a letra grande do sistema. A grade
  deitada foi conferida em imagem, no Robolectric.
- Para comparar, a tela inicial do One UI foi aberta direto (`am start`), sem trocar a tela inicial padrão, e lida do
  mesmo jeito; as capturas também foram apagadas.

## O que funcionou

| Parte | Gesto ou ação | Resultado |
|---|---|---|
| Tela inicial | Ler a grade | 6 colunas × 6 linhas em pé, relógio em 2 colunas, nomes inteiros |
| Tela inicial | Arrastar para o lado | Com uma tela só, não acontece nada (certo) |
| Tela inicial | Voltar | Não faz nada, e a tela não é montada de novo (mesma janela) |
| Painel | Arrastar para a direita | Abre "Minha rotina" |
| Painel | Voltar | Volta para a primeira tela |
| Todos os apps | Arrastar para cima / voltar | Sobem com o dedo; voltar fecha o teclado e depois a lista; a busca começa vazia de novo |
| Todos os apps | Buscar "cal" | Mostra Calculadora e Calendário |
| Todos os apps | Segurar um app | Menu com Adicionar à tela inicial, Informações do app e Desinstalar |
| Todos os apps | Segurar e carregar para a tela inicial | O app cai exatamente na casa sob o dedo |
| Tela inicial | Segurar um ícone e soltar | Menu com Remover, Informações do app, Desinstalar e Editar a tela inicial |
| Tela inicial | Carregar para uma casa vazia | Cai na casa sob o dedo (em "Livres") |
| Tela inicial | Soltar sobre outro ícone | Os dois trocam de lugar |
| Tela inicial | Carregar para o dock e de volta | Entra no dock na posição do dedo; volta para a casa |
| Tela inicial | Segurar na borda direita | Depois de um instante a tela vira para uma tela nova; a área de destino aparece e o item cai nela |
| Edição | Segurar a tela vazia | Tela menor com contorno, casinha (tela principal), "Pronto" e as ferramentas embaixo |
| Edição | Arrastar para o lado | Cartão "+" para adicionar tela |
| Papel de parede | Abrir | "Como está agora", "Do aparelho" (com o histórico), degradês e cores |
| Papel de parede | Tocar num degradê / voltar | Prévia na tela inicial de verdade; voltar cancela e volta para a lista, e voltar de novo volta à edição |
| Opções | Abrir | Todas as escolhas aparecem; voltar volta à edição |

Segunda rodada, no mesmo dia (cada escolha foi desfeita depois):

| Parte | Gesto ou ação | Resultado |
|---|---|---|
| Opções | Baixa estimulação | Ícones cinza e o papel de parede escurecido; desligada, tudo volta |
| Opções | Ícones enormes | 4 colunas; em "Livres" os ícones vão para outras casas (veja D9); de volta a médios, cada um volta para a sua casa |
| Opções | Encaixados | Os ícones se juntam na ordem; de volta a "Livres", cada um volta para a sua casa |
| Opções | Mostrar botões | O botão de todos os apps aparece no dock (veja T6) |
| Opções | Painel com frases | O painel mostra as frases prontas; tocar numa fala sem erro |
| Edição | "+", casinha, lixeira | O "+" cria a tela 2; a casinha passa a tela principal para a 2 e de volta; a lixeira apaga a tela vazia na hora |
| Tela inicial | Tocar num app e voltar | O app abre; voltar mostra o ORI Home como estava (sem montar de novo) |

Terceira rodada, com o tablet deitado (2560 × 1600; a pessoa virou o tablet, a rotação automática estava ligada):

| Parte | Gesto ou ação | Resultado |
|---|---|---|
| Tela inicial | Girar | O ORI Home é montado de novo (normal: a atividade não trata a rotação sozinha) e cada ícone fica na **mesma casa** de em pé; ícones de uns 50 dp com o nome em uma linha; o relógio desenhado menor |
| Tela inicial | Carregar um ícone para uma casa vazia e de volta | Cai na casa sob o dedo, nas casas mais baixas de deitado |
| Tela inicial | Segurar um ícone | O menu abre |
| Edição | Segurar a tela vazia | Tela menor com contorno, casinha, ferramentas e "Pronto" |
| Todos os apps | Arrastar para cima | Abrem, com 6 colunas (veja D13) |
| Painel | Arrastar para a direita / voltar | A rotina em duas colunas (deitado, faz sentido); voltar volta à primeira tela |
| Papel de parede | Abrir, tocar num degradê, voltar | Funciona; a prévia de verdade aparece; T4 acontece deitado também |
| Opções | Abrir | Conteúdo centrado com largura máxima; as escolhas estavam como deixadas |

## Problemas claros

| # | Onde | O que acontece | Gravidade | Correção sugerida |
|---|---|---|---|---|
| T1 | Widgets | **Abrir "Widgets" fecha o ORI Home** (duas vezes seguidas). O Android não entrega a lista dos 77 tipos de widget instalados: `AppWidgetManager.getInstalledProvidersForProfile` lança `BadParcelableException` ("only received 24 of 77", `DeadObjectException` por falta de espaço no binder), e `AndroidWidgetHost.providers` não trata o erro | Alta | Pedir a lista por app (`getInstalledProvidersForPackage`, um app por vez), pular o app que falhar e mostrar "Não foi possível ler alguns widgets" em vez de fechar |
| T2 | Sistema | **Depois de o ORI Home fechar duas vezes, o botão Início passa a perguntar "Selecione um app de início"** (ORI Home ou One UI), mesmo com o ORI ainda registrado como tela inicial. Até a pessoa escolher "Sempre" de novo, cada toque em Início pergunta | Alta (consequência de T1) | Corrigir T1; nenhum erro de leitura do sistema deve fechar a tela inicial |
| T3 | Tela inicial | Ao soltar um item na **tela nova** criada na borda, a tela inicial **volta sozinha para a tela anterior**: o item some de vista. Causa provável (não confirmada): ao soltar, a tela "a mais" do carregar some antes de a casa nova ser gravada, e o pager volta uma tela | Média | Manter a tela a mais até a gravação chegar, ou ir para a tela do item depois de soltar |
| T4 | Papel de parede | Na prévia, **Cancelar e Aplicar ficam por cima do dock**; o ícone do dock aparece através do botão Cancelar (que é transparente) | Média | Pôr os botões acima do dock (a altura do dock é conhecida) ou numa faixa com fundo |
| T6 | Mostrar botões | "Mostrar botões" deveria servir a quem não faz gestos, mas só põe o botão de todos os apps. Editar a tela inicial, o papel de parede, os widgets e as opções continuam só com um toque longo (ou pelas ações do leitor de tela). O botão de opções do relógio existe no código (`HomeClock`, `showsOptionsButton`), mas a tela sempre o desliga: é um parâmetro sem efeito | Média | Com "Mostrar botões", mostrar o botão de opções (ou "Editar") na tela inicial; tirar o parâmetro sem uso |
| T7 | Girar | O pedido era girar "como o One UI", mas o ORI Home mantém a mesma grade deitado (casas achatadas, ícones de 50 dp), e o One UI transpõe a grade (6 × 8 em pé, 8 × 6 deitado) com um arranjo para cada posição (veja "Girar" nas diferenças) | Média | Decidir com a pessoa: grade transposta com casas de deitado guardadas à parte, ou manter |
| T5 | Papel de parede | A prévia do **papel de parede do Android** (miniatura e "Como está agora") sai num lilás médio, mas o papel de parede real é azul-marinho escuro: a cor principal que o Android informa não representa a imagem | Média | Escurecer ou clarear a prévia pelo que o Android diz do papel de parede (claro ou escuro), ou desenhar só um aviso de que é o do Android |

## Problemas de design

| # | Onde | O que acontece | Sugestão |
|---|---|---|---|
| D1 | Menus de app | Lista de texto simples, sem ícones, sem o nome do app e sem escurecer o fundo; abre por cima dos ícones vizinhos e tem pouco contraste sobre papel de parede escuro. "Remover" (ação que tira algo) vem primeiro | Cartão com o nome do app, ícones nas opções, fundo escurecido e o ícone em destaque, como no One UI; a ação de tirar por último |
| D2 | Todos os apps | Linhas de alturas diferentes: um nome de duas linhas ("Geometry Dash Lite") deixa a linha mais alta que as outras | Altura igual para todas as linhas (sempre o espaço de duas linhas) |
| D3 | Busca | Sem botão para apagar o que foi escrito; a tecla de busca do teclado não faz nada | Botão "×" no campo; a tecla de busca abre o primeiro resultado |
| D4 | Edição | O cartão "+" tem um tamanho diferente do da tela encolhida; as telas vizinhas não aparecem nas bordas | Mesmo tamanho; mostrar as bordas das telas vizinhas, como no One UI |
| D5 | Papel de parede | No tablet, "Como está agora" fica no centro e as fileiras de miniaturas ficam à esquerda; as fotos do histórico têm o mesmo nome ("Foto usada antes"), e o leitor de tela não distingue uma da outra | Alinhar tudo igual; numerar as fotos ("Foto 1 usada antes") |
| D6 | Painel | No tablet em pé, a rotina aparece em duas colunas, e a da direita fica vazia ("Escolha uma atividade"), ocupando metade do painel | No painel, uma coluna só, ou abrir a atividade por cima |
| D7 | Abrir app | O app abre com a animação padrão do Android, não "saindo do ícone" como no One UI (`startMainActivity` sem os limites do ícone); a procura da atividade (`getActivityList`) é feita na thread principal | Passar os limites do ícone e um `ActivityOptions` de crescimento a partir dele; procurar a atividade fora da thread principal |
| D8 | Tela inicial | Não há como travar a tela inicial: um toque longo sem querer pode mover ou tirar um ícone. Para quem tem dificuldade motora, isso pesa | Opção "Travar a tela inicial", como no One UI |
| D9 | Opções | Mudar o tamanho dos ícones muda o número de colunas: em "Livres", os ícones que não cabem vão para outras casas (as casas ficam guardadas e voltam com o tamanho anterior). No One UI, o tamanho do ícone e a grade são escolhas separadas | Separar o tamanho do ícone da grade, ou avisar que os ícones vão mudar de lugar |
| D10 | Encaixados | O relógio entra na ordem como um app: pode ficar no meio da primeira fileira, entre ícones | Manter o relógio no começo ao trocar para "Encaixados", ou deixar a pessoa escolher |
| D11 | Pontos das telas | Os pontos não mostram o painel à esquerda nem qual é a tela principal. O One UI mostra "≡" para a tela da esquerda e uma casinha na tela principal | Um traço para o painel e a casinha no ponto da tela principal |
| D12 | Edição | Depois de apagar a última tela, a edição fica mostrando o cartão "+", e não a tela de antes | Voltar para a tela anterior ao apagar |
| D13 | Todos os apps, deitado | Continuam 6 colunas espalhadas pela tela larga (casas de 213 dp), com os ícones de 72 dp (na tela inicial deitada eles ficam com 50 dp), e a busca ocupa a largura toda. O One UI deitado usa 8 colunas numa faixa central, com a busca pequena embaixo | Mais colunas deitado (ou a faixa central com largura máxima) e busca com largura máxima |
| D14 | Papel de parede, deitado | "Como está agora" mostra prévias deitadas e grandes, mas as miniaturas continuam em pé: as duas formas lado a lado confundem | Miniaturas no formato da tela de agora, ou prévias em pé também |
| D15 | Tela inicial, deitado | A tela fica muito espaçada para os lados (6 colunas em 2560 px), e a data do relógio, desenhada menor, fica colada ao ícone de baixo | Veja "Girar" nas diferenças: a grade transposta do One UI resolve as duas coisas |

## Diferenças para o One UI

| Parte | One UI | ORI Home |
|---|---|---|
| Pastas | Soltar um ícone sobre outro cria uma pasta (na tela de teste: Samsung, Google, Microsoft, Galaxy Picks) | Os dois trocam de lugar; não há pastas |
| Grade | A pessoa escolhe a grade da tela inicial e a da tela de apps (no tablet de teste, 6 × 8 as duas) e, à parte, o tamanho dos apps | A pessoa escolhe o tamanho do ícone; colunas e linhas vêm da tela (6 × 6 no tablet em pé, tamanho médio) |
| Tamanho dos ícones | 58 dp no tablet (113 px a 312 dpi, medido em 12/11/2026) | Desde 12/11/2026, o médio é o do One UI (58 dp); o enorme tem 120 dp, mais que o dobro, para quem tem muita dificuldade com as mãos (antes: médio 72 dp, de propósito maior) |
| Nomes dos ícones | Uma linha, com reticências ("Loja Online Sams..."); opção de esconder | Até duas linhas, com hífen |
| Selos de notificação | Número nos ícones e nas pastas (a pasta Google mostrava 4) | Desde 11/11/2026: ponto ou número, a pasta com a soma, com o "Acesso a notificações" permitido pela pessoa; sem selos na baixa estimulação |
| Menu ao segurar um ícone | Um balão pequeno acima do ícone, com o nome do app e o botão de informações (e os atalhos do app, quando há) | Lista de texto embaixo do ícone: Remover, Informações, Desinstalar e Editar |
| Widgets da tela padrão | Barra de busca do Google e o tempo no alto | Só o relógio do ORI |
| Pontos das telas | "≡" para a tela da esquerda e uma casinha na tela principal | Só pontos (D11) |
| Mover vários ícones | "Selecionar" vários e carregar juntos | Um por vez |
| Widgets | Lista em duas colunas (apps com a quantidade de widgets à esquerda, prévias de verdade à direita), "Recomendado" primeiro, busca com voz; mudar o tamanho, pilhas, arrastar da lista para a tela | Uma lista por app, com a prévia e o tamanho; sem busca; tamanho fixo; tocar adiciona no fim; e hoje fecha o app (T1) |
| Deslizar para baixo na tela inicial | Abre as notificações (opção) | Desde 11/11/2026: abre as notificações (opção, ligada por padrão) |
| Pinça (dois dedos) | Abre a edição | Só segurar a tela vazia |
| Tela à esquerda | Samsung Free ou Google Discover | Painel do ORI (rotina ou frases) |
| Todos os apps | Telas para o lado com pontos, ordem livre (arrastar), pastas, menu "⋮" (ordenar, limpar telas) e a busca **embaixo**, com microfone | Lista para baixo, em ordem alfabética, busca em cima, sem microfone |
| Busca | Finder: apps sugeridos, tópicos de configurações, arquivos, pesquisas recentes | Só apps |
| Edição das telas | Telas a uns 66 %, com as vizinhas aparecendo dos lados; casinha à esquerda e lixeira à direita da tela; "+" nos pontos; sem botão "Pronto" (sai com voltar ou tocando fora); três ferramentas: Papel de parede e estilo, Widgets, Configurações | Telas a 84 %, uma por vez; casinha e lixeira no meio; "+" como um cartão depois da última tela; botão "Pronto"; quatro ferramentas (Papel de parede, Widgets, Apps, Opções). Não dá para reordenar telas |
| Papel de parede | Prévias com as imagens reais, paleta de cores do tema, escurecer no modo escuro | Prévias pelas cores (o Android não deixa ler a imagem), sem paleta |
| Girar (tablet) | **A grade é transposta: 6 × 8 em pé e 8 × 6 deitado** (as configurações mostram "6X8" em pé e "8X6" deitado), e cada posição tem o seu arranjo: deitado, os itens da primeira tela ficam numa faixa central, perto do dock, e o widget do tempo troca de lado com o da busca; os da segunda tela também ficam em outras casas | A mesma grade de 6 × 6 nas duas posições: as casas ficam achatadas e os ícones menores, e cada ícone fica na mesma casa. **Isto não é o que o One UI faz:** a mudança de 11/11/2026 supôs que o One UI mantinha a grade, e o teste mostrou que não |
| Girar (celular) | Opção para girar (desligada por padrão) | Sempre em pé |
| Todos os apps, deitado | 8 colunas numa faixa central, fundo translúcido com o papel de parede atrás, busca pequena embaixo | 6 colunas na largura toda, fundo quase opaco, busca em cima na largura toda (D13) |
| Dock do tablet | Mostra apps recentes e sugeridos | Desde 11/11/2026: opção "Apps recentes na barra" (os dois últimos abertos pelo ORI Home; sem sugeridos) |
| Barra de tarefas (tablet) | Nos apps, a barra de tarefas vem do One UI Home | Com outra tela inicial padrão, a Samsung desliga a barra de tarefas: não há como o ORI oferecê-la |
| Travar a tela inicial | "Bloquear aparência da Tela inicial": impede remover ou mover itens | Não há (D8) |
| Novos apps | "Adic. novos apps à Tela inicial" | Desde 11/11/2026: a mesma opção |
| Pastas | Soltar um ícone sobre outro; pasta aberta com nome, "+" e cor | Desde 11/11/2026: soltar no meio de outro ícone, "Criar pasta" no menu, nome e "+" com a lista de apps; sem cor |
| Atalhos dos apps | No menu ao segurar; arrastar para a tela inicial; pedidos de outros apps | Desde 11/11/2026: no menu, com "+" para a tela inicial; pedidos de outros apps com confirmação |
| Tamanho dos widgets | Moldura com alças nos quatro lados | Desde 11/11/2026: alças à direita e embaixo, e botões de largura e altura |
| Reordenar telas | Segurar e arrastar a miniatura da tela | Desde 11/11/2026: setas para trocar com a tela anterior ou a seguinte |
| Configurações da tela inicial | Prévia no alto; tamanho dos apps; mostrar nomes dos apps e dos widgets; só tela inicial ou tela inicial e de apps; grades; tela da esquerda (Google Discover); botão de apps; apps recentes no dock (até 2); travar; novos apps; ocultar apps; selos (número); deslizar para baixo para as notificações | Baixa estimulação; painel (rotina, frases ou nenhum); encaixados ou livres; tamanho dos ícones; mostrar botões; atalhos para papel de parede e widgets; abrir o ORI; deixar de usar o ORI Home |
| Baixa estimulação | Não há na tela inicial (o tom de cinza é do sistema, no Bem-estar digital) | Ícones cinza e papel de parede mais escuro, só na tela inicial |

## Ainda a testar

- **Voltar a ficar em pé** depois de mexer deitado: conferir que cada ícone volta à mesma casa (o código guarda as
  mesmas casas; falta ver no aparelho).
- **TalkBack:** é uma configuração do sistema; a pessoa precisa ligá-lo em Configurações > Acessibilidade.

## O que fazer com isto

O plano com todas as mudanças (transpor a grade ao girar e corrigir cada problema), com as decisões que faltam, está
em [launcher-plano.md](launcher-plano.md).

T1 a T7 entram em [revisao-de-codigo.md](../revisao-de-codigo.md) (seção 16) até serem corrigidos. As diferenças são
escolhas de produto: pastas, travar a tela inicial (D8) e o menu com atalhos (D1) são as que mais aproximariam o ORI
Home do One UI sem perder a calma da tela.
