# ORI Home: plano de mudanças (11/11/2026)

Tudo o que precisa mudar no ORI Home depois dos testes no tablet: transpor a grade ao girar, como o One UI faz, e
corrigir cada problema encontrado. Os detalhes de cada teste estão em [launcher-teste-one-ui.md](launcher-teste-one-ui.md)
e os problemas em [revisao-de-codigo.md](../revisao-de-codigo.md) (seção 16).

## Situação (11/11/2026)

Feito, com as decisões recomendadas (A1: até 8 linhas em pé; B1: deitado, ícone menor com o nome em uma linha; C1:
primeiro arranjo deitado na ordem de leitura de em pé, tela por tela). Cada item tem teste; o que cada teste confere
está em [launcher.md](launcher.md#como-testar).

| Item | Situação |
|---|---|
| Transposição (seção 2), T7, D13, D15 | Feito e testado (`HomeMetricsTest`, `HomeLayoutTest`, `HomePagesTest`, `HomeScreenSizeTest`, `LauncherScreenTest`, `DataStoreLauncherSettingsRepositoryTest`) |
| T1, T2 | Feito e testado: a lista lida app por app quando falha (`WidgetListTest`) e, depois da segunda revisão (U1), as imagens e o adicionar procuram na lista lida (`KnownWidgetsTest`); conferido no tablet em pé |
| T3, T4, T6 | Feito e testado (`HomeScreenSizeTest`, `WallpaperScreenTest`, `LauncherScreenTest`); os testes de T3 e T4 falhavam antes |
| T5, D5, D14 | Feito e testado (`SystemWallpaperColorsTest`, `WallpaperScreenTest`) |
| D1, D2, D3, D4, D11, D12 | Feito; D1 e D12 testados (`LauncherScreenTest`; o de D12 falhava antes); D4 e D11 são desenho, conferidos no tablet. Revendo em pé, o menu (D1) e a edição (D4) foram aproximados do One UI 8 (U6, U12) |
| D6, D7, D8, D10 | Feito e testado (`AreaListDetailLayoutTest`, `AppOpeningTest`, `HomeLockTest`, `HomeGesturesTest`, `LauncherViewModelTest`) |
| D9 | Feito e testado: "Grade da tela inicial" separa a grade do tamanho do ícone, como no One UI (`HomeMetricsTest`, `LauncherScreenTest`) |

O resto deste documento é o plano como foi escrito antes de começar.

## 1. Decisões antes de começar

| # | Decisão | Opções | Recomendação |
|---|---|---|---|
| A | **Grade em pé.** O One UI usa 6 × 8 em pé. O ORI usa no máximo 6 linhas, e no tablet em pé fica 6 × 6. Transpor 6 × 6 dá 6 × 6 de novo: nada mudaria | (1) Subir o teto para 8 linhas: no tablet de teste, com ícones médios, cabem 7 (6 × 7 em pé, 7 × 6 deitado). (2) Deixar a pessoa escolher a grade, como no One UI ("Grade da tela inicial"), com as opções que cabem no tamanho de ícone escolhido | (1) agora; (2) depois, se fizer falta |
| B | **Ícone deitado.** Deitado, as linhas viram as colunas de em pé (6 no tablet). Seis linhas numa altura de uns 550 dp dão uns 92 dp por linha: um ícone de 72 dp com o nome não cabe. O One UI não tem esse problema porque os ícones dele têm uns 52 dp | (1) Ícone menor deitado (uns 50 dp) com o nome em uma linha, como hoje. (2) Ícone do mesmo tamanho deitado, sem os nomes (o leitor de tela continua lendo). (3) Menos linhas deitado (aí não é mais a transposição exata) | (1), que mantém os nomes |
| C | **Primeiro arranjo deitado.** Quando a pessoa gira pela primeira vez, ainda não há casas de deitado | (1) Mesma ordem de leitura, tela por tela, de cima para baixo. (2) Como o One UI fez no tablet: os itens no pé da tela, no centro | (1), mais previsível |

## 2. Transpor a grade ao girar (tablet)

O celular continua só em pé. No tablet, a grade deitada é a de em pé com colunas e linhas trocadas (6 × 7 em pé →
7 × 6 deitado), e cada posição guarda o seu arranjo: mover um ícone deitado não muda onde ele fica em pé.

| Onde | Mudança |
|---|---|
| `ui/launcher/HomeMetrics.kt` | A grade em pé vem da largura e da altura em pé (decisão A). Deitado, `colunas = linhas de em pé` e `linhas = colunas de em pé`. O tamanho do ícone deitado segue a decisão B. Sai a "sala em pé" que hoje mantém a mesma grade nas duas posições. O relógio passa a ser medido nas casas de cada posição (`scaledToFit` fica só como proteção) |
| `domain/launcher/HomeLayout.kt` | `HomeGrid` ganha a posição da tela (em pé ou deitada), para todas as regras saberem qual arranjo usar sem mudar as chamadas |
| `domain/launcher/LauncherModel.kt` | `LauncherSettings` ganha as casas de deitado (`positionsTurned`), ao lado das de em pé. Tirar um item (`removedFromHome`) tira das duas |
| `domain/launcher/HomeEditing.kt` | `arranged`, `dropped`, `stepped`, `canDrop` e `keepingStoredCells` usam as casas da posição da grade. `droppedOnDock` tira das duas. Nova função para o primeiro arranjo deitado (decisão C), tela por tela: a grade transposta tem o mesmo número de casas, então cada tela continua cabendo na mesma tela |
| `domain/launcher/HomePages.kt` | As telas (quantas, qual é a principal) são as mesmas nas duas posições. Apagar uma tela tira os itens dela na posição mostrada e desloca as telas seguintes nos dois arranjos |
| `ui/launcher/HomeWidgetEditor.kt` | Tirar um widget tira as casas dele dos dois arranjos |
| `ui/launcher/HomeBars.kt` (dock) | Cabem no dock as colunas de em pé nas duas posições, para o dock não transbordar ao girar |
| `ui/launcher/AppDrawer.kt` | Todos os apps também transpõem: no tablet de teste, 6 colunas em pé e 8 deitado (como o One UI); ícone do mesmo tamanho da tela inicial em cada posição; busca com largura máxima (D13) |
| `data/launcher/DataStoreLauncherSettingsRepository.kt` | Nova chave `launcher_positions_turned`, no mesmo formato de `launcher_positions` |
| Privacidade (AGENTS.md, seção 14) | "A posição de cada ícone" já está na política e fica só no aparelho: sem mudança de versão. Registrar no histórico de [legal/README.md](../legal/README.md) |
| Testes | `HomeMetricsTest`: grade transposta e ícone deitado. `HomeLayoutTest`: um arranjo por posição, primeiro arranjo deitado, mover deitado não muda em pé. `HomePagesTest`: apagar uma tela deitado. `DataStoreLauncherSettingsRepositoryTest`: a chave nova volta igual. Teste de tela: em pé e deitado, cada ícone na casa da sua posição |
| Documentação | [launcher.md](launcher.md) (grade e girar; persistência) e o relatório de teste (T7, D13 e D15 resolvidos) |
| No aparelho | Girar em pé → deitado → em pé, com o ORI Home e o One UI lado a lado |

## 3. Correções dos problemas encontrados

### Problemas claros

| # | Problema | Onde | O que mudar |
|---|---|---|---|
| T1 | Abrir "Widgets" fecha o ORI Home (o Android não entrega a lista dos 77 widgets) | `data/launcher/AndroidWidgetHost.kt` (`providers`), `ui/launcher/HomeWidgetEditor.kt` | Pedir a lista app por app (`getInstalledProvidersForPackage`), pular o que falhar e mostrar "Alguns widgets não puderam ser lidos"; teste com um `HomeWidgetHost` que falha |
| T2 | Depois de fechar duas vezes, o botão Início pergunta qual tela inicial usar | Consequência de T1 | Corrigir T1; e tratar como erro com mensagem toda leitura do sistema (apps, widgets, papel de parede) que hoje pode fechar a tela inicial |
| T3 | Soltar um item numa tela nova faz a tela inicial voltar para a anterior | `ui/launcher/LauncherHome.kt` (`homePageCount`, pager) | Manter a tela a mais até a casa nova ser gravada, ou ir para a tela do item depois de soltar; teste de tela |
| T4 | Na prévia do papel de parede, Cancelar e Aplicar ficam sobre o dock (em pé e deitado) | `ui/launcher/WallpaperPreview.kt` | Pôr os botões acima do dock (`HomeMetrics.bottomHeight`) ou numa faixa com fundo |
| T5 | A prévia do papel de parede do Android sai num lilás claro para um papel azul-marinho escuro | `ui/launcher/WallpaperNow.kt` (`SystemWallpaperPicture`) | Ajustar o brilho pelo que o Android diz (`colorHints`: claro ou escuro), ou mostrar só um aviso de que é o papel do Android |
| T6 | "Mostrar botões" não dá botão para editar nem para as opções; o botão de opções do relógio nunca aparece | `ui/launcher/HomePage.kt`, `HomeCarry.kt`, `HomeClock.kt` | Com "Mostrar botões", um botão de opções (ou "Editar") na tela inicial; tirar o parâmetro sem efeito |
| T7 | O tablet deitado não segue o One UI | Seção 2 | A transposição |

### Problemas de design

| # | Problema | O que mudar |
|---|---|---|
| D1 | Menus de app só com texto, sem o nome do app, por cima dos vizinhos, com "Remover" primeiro | Balão como o do One UI: nome do app e ícones nas opções, fundo escurecido, ícone em destaque, a ação de tirar por último (`HomeItemMenu.kt`, `AppDrawer.kt`, `AppMenuActions.kt`) |
| D2 | Em todos os apps, linhas de alturas diferentes | Altura de duas linhas de nome em todas as linhas (`AppDrawer.kt`) |
| D3 | Busca sem botão de apagar; a tecla de busca não faz nada | Botão "×"; a tecla de busca abre o primeiro resultado (`AppDrawer.kt`) |
| D4 | Na edição, o cartão "+" tem outro tamanho; as telas vizinhas não aparecem | Mesmo tamanho da tela encolhida; mostrar as bordas das vizinhas (`HomePageEditing.kt`, `LauncherHome.kt`) |
| D5 | Papel de parede: alinhamentos diferentes no tablet; fotos do histórico com o mesmo nome | Tudo com o mesmo alinhamento; "Foto 1 usada antes", "Foto 2…" (`WallpaperPicker.kt`) |
| D6 | Painel em pé com a segunda coluna vazia | Uma coluna só no painel (`OriPanel.kt`) |
| D7 | Apps abrem sem a animação "saindo do ícone"; a procura da atividade é feita na thread principal | Passar os limites do ícone e `ActivityOptions` a `startMainActivity`; procurar fora da thread principal (`AndroidInstalledApps.kt`) |
| D8 | Não há como travar a tela inicial | Opção "Travar a tela inicial" (como o One UI): sem carregar nem tirar itens enquanto travada. Escolha nova no DataStore: entra na política (seção 14) |
| D9 | Mudar o tamanho dos ícones muda as colunas e, em "Livres", os ícones mudam de casa | Com a transposição e a decisão A, separar tamanho do ícone da grade, ou avisar antes de mudar |
| D10 | Em "Encaixados", o relógio pode ficar no meio da primeira fileira | Relógio no começo ao trocar para "Encaixados" |
| D11 | Os pontos das telas não mostram o painel nem a tela principal | Traço para o painel e casinha na tela principal, como o One UI (`HomeBars.kt`) |
| D12 | Depois de apagar a última tela, a edição mostra o cartão "+" | Voltar para a tela anterior (`LauncherHome.kt`) |
| D13 | Todos os apps deitado: 6 colunas, ícones grandes, busca na largura toda | Seção 2 (transpor também todos os apps) |
| D14 | Papel de parede deitado: prévias deitadas e miniaturas em pé | Miniaturas no formato da tela de agora (`WallpaperThumbnails.kt`) |
| D15 | Tela inicial deitada muito espaçada; data do relógio colada ao ícone de baixo | Seção 2 |

### Limitações já conhecidas (sem correção prevista)

- Prévias do papel de parede do Android só pelas cores: o Android não deixa ler a imagem sem uma permissão especial.
- Widget carregado aparece como um cartão: o Android desenha um widget num lugar só.
- Hífen nos nomes ainda não conferido num celular.
- Celular só em pé (o One UI tem uma opção para girar).
- Com outra tela inicial padrão, a Samsung desliga a barra de tarefas dos apps no tablet.

### Ainda a testar

- Voltar a ficar em pé e conferir que cada ícone volta à sua casa (depois da transposição, as casas de em pé).
- TalkBack: precisa ser ligado pela pessoa (é configuração do sistema).

## 4. Diferenças para o One UI que seriam recursos novos

**Situação em 11/11/2026: os nove foram feitos** (veja "Recursos do One UI" em [launcher.md](launcher.md)), mais
"Ocultar apps", "Configurar o widget" e os pedidos de "Adicionar à tela inicial" de outros apps. Ficaram de fora: cor
da pasta, "Somente tela inicial", sugestões do Finder e Google Discover.

Não são erros; são escolhas de produto, da que mais aproxima o ORI Home do One UI para a que menos:

1. Pastas (soltar um ícone sobre outro).
2. Travar a tela inicial (D8).
3. Atalhos do app no menu (com D1).
4. Mudar o tamanho de um widget e busca na lista de widgets.
5. Reordenar telas na edição.
6. Deslizar para baixo na tela inicial para abrir as notificações.
7. Mostrar ou esconder os nomes dos ícones.
8. Selos de notificação (exige acesso às notificações: entra na política, seção 14).
9. Apps recentes no dock do tablet e pôr novos apps na tela inicial.

## 5. Ordem sugerida

1. **T1 e T2:** a falha que fecha a tela inicial vem primeiro.
2. **Transposição** (T7, D13, D15, D9), com as decisões A, B e C.
3. **T3, T4 e T6:** erros claros.
4. **T5, D5 e D14:** papel de parede.
5. **D1, D2, D3, D4, D11 e D12:** acabamento das telas.
6. **D6, D7, D8 e D10.**
7. Recursos da seção 4, conforme a decisão de produto.

Cada etapa termina com a verificação completa (detekt, testes, Lint, builds), a documentação e a instalação no tablet.
