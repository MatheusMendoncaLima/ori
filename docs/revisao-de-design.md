# Revisão de design

> A revisão seguinte, de 03/11/2026, separa estrutura e design: [revisao-estrutura-e-design.md](revisao-estrutura-e-design.md).

Revisão feita em 02/11/2026, lendo o código das telas e olhando imagens do modo escuro geradas no computador (360 × 780 dp). **Nada aqui foi visto no celular.** Só o que está nas seções 1 e 1.1 foi corrigido; o restante é a lista de trabalho.

Prioridade: **P1** atrapalha o uso ou mostra algo errado; **P2** confunde ou quebra a consistência; **P3** é acabamento.

## 1. Paleta do modo escuro (feito)

| O que mudou | Antes | Agora | Motivo |
|---|---|---|---|
| Fundo / cartão | `#0D1B28` / `#162736` | `#0C1824` / `#142230` | Azul-marinho da marca, menos saturado |
| Camadas elevadas | Todas `#1F3F55` | `#1C2E3F` e `#243849` | Níveis distintos, sem azul forte em todo menu |
| Destaque teal (`strong`) | Menta claro `#6FD0CE` com texto escuro | Teal profundo `#12706F` com texto claro | Mesmo teal do tema claro; blocos grandes deixam de brilhar |
| Teal como linha ou texto | Usava o mesmo `strong` | Novo `strongOnSurface` `#62C4BE` | Um teal feito para preencher não serve para texto sobre o fundo |
| Fim do degradê do Perfil | `#2A7F7E` | `#0E5655` | **Corrige um erro:** o nome ficava com contraste de 3,03:1 e agora tem 8,04:1 |
| Seis cores suaves | Marrom, oliva e roxo saturados | Mesma claridade, pouca saturação | Separam as linhas sem parecer seis marcas; continua lúdico |
| Abertura do app | Janela clara | `values-night/themes.xml` escuro | Evita o clarão branco quando o sistema está escuro |

**Limitação:** se o sistema está claro e a pessoa escolheu o tema escuro no app, o clarão continua, porque a janela é criada antes de o app ler a preferência.

Os novos pares de contraste estão em `OriThemeRulesTest`. Abertura, login e o logo sobre o fundo escuro não foram conferidos em imagem.

## 1.1. Correções rápidas (feito)

| Item | Resultado |
|---|---|
| Início em carregamento ou erro | Mostra "Carregando" ou o erro com "Tentar novamente". "Nenhuma atividade" só aparece depois de uma leitura bem-sucedida |
| Contraste do tema claro | Texto de apoio `#5A7186` (4,8:1), placeholder `#5C7387` (4,93:1), fim do degradê `#2C8080` (4,66:1). Os pares perto do limite (ciano e `#5C768D`) não mudaram, pois são cores originais da marca |
| Botão "Aa" | Entrou no cabeçalho do Perfil. Em "Entrar ou criar conta" só fica o da barra; os dois cabeçalhos continuam |
| Nomes | O objeto se chama "atividade" em todo texto, e a tela mantém o nome "Tarefas" do wireframe. Navegação: "Rotina" e "Frases"; títulos: "Minha rotina" e "Formar frase". Os nomes inteiros quebravam em duas linhas em maiúsculas e desalinhavam os ícones |
| Relatórios e Perfil | "Atividades concluídas", "Do planejado", "Vezes que falou" e "Conta conectada". Responsáveis e Acompanhados agora explicam quem gera e quem digita o código |
| Ícones | Novos em `OriIcons`: balão (comunicação), pessoa (acessibilidade), paleta (tema), celular (tela), grupo (responsáveis), e coração (acompanhados). A estrela fica só para frases salvas, e o lápis para editar. Saiu o calendário repetido em Tarefas |

Nas seções abaixo, os itens listados aqui já estão resolvidos.

## 1.2. Botões (feito)

Em 03/11/2026 todas as telas passaram para os mesmos botões. Veja [design-system.md](design-system.md).

| Onde | Problema | Agora |
|---|---|---|
| Novo símbolo | "Escolher foto" e "Remover imagem" com larguras diferentes | Mesma largura e altura |
| Dados pessoais | Um contornado e um só de texto, de tamanhos diferentes | Mesma largura e altura |
| Organizar pranchas | Três botões soltos, baixos e de larguras diferentes | Principal em cima; Renomear e Excluir iguais |
| Sem pranchas | O de baixo mais baixo que o de cima | Mesma altura |
| Abertura | 50 dp e outro estilo de texto | Padrão do app |
| Login | Borda própria, "Esqueci a senha" menor | Padrão do app |
| Cadastro, Complete seu perfil, formulários | "Cancelar" e "Sair" menores e encolhidos | Mesma altura, largura total |
| Perfil | "Editar perfil" com 48 dp, "Entrar" com 52 dp | 52 dp |
| Estados vazio e de erro | Botão sem altura mínima | Padrão do app |
| "Tentar de novo" em cartões | Tamanhos diferentes a cada tela | Padrão do app |

## 2. Pontos que você levantou

**Feito depois desta revisão:** os itens 1, 4, 6 e 7 desta tabela. Veja [rotina.md](funcionalidades/rotina.md), [caa.md](funcionalidades/caa.md) e [uso-local.md](funcionalidades/uso-local.md).

| # | Pedido | O que o código faz hoje | Caminho sugerido | Prioridade |
|---|---|---|---|---|
| 1 | Título abaixo da data | `ActivityEditorScreen` tem a ordem título, semana, horário | Ordem data, horário, título, descrição | P2 |
| 2 | CAA mais coesa | São seis camadas: topo com 3 ícones, abas de pranchas, frase, grade, 4 ações e a navegação do app | Ver seção 3 | P1 |
| 3 | Palavras viram sentenças | "Dor" mostra uma palavra e fala "Estou com dor" (`phrase` na `StarterBoard`) | Separar palavra de frase pronta (itens 4 e 5) | P1 |
| 4 | Foto de perfil | `UserProfile` só tem `uid` e `displayName`; o ícone é fixo | Foto em Dados pessoais. Decidir se fica no aparelho, como as fotos de símbolos, ou se vai para a conta (isso exigiria Storage, regras e testes) | P2 |
| 5 | Frases prontas | "Frases salvas" começa vazia | Trazer frases comuns já na primeira abertura, editáveis | P1 |
| 6 | Arrastar a semana | `WeekStrip` só tem as setas | `HorizontalPager` por semana, mantendo as setas para quem não consegue arrastar | P2 |
| 7 | Repetição de atividades | `RoutineTask` tem uma única `date`; não existe repetição | Modelo próprio: uma regra (dias da semana, último dia, último dia útil) gera as ocorrências, e cada ocorrência guarda se foi feita. Muda Firestore, regras, Room e migração para a conta | P1, a maior mudança da lista |
| 8 | "eu estou estou com sono estou com dor" | `AacMessage.text` só junta os trechos com espaço, e os trechos misturam palavra e frase inteira | Ver seção 3 | P1 |

## 3. Proposta para a CAA (itens 2, 3, 5 e 8)

**Feito**, com três diferenças em relação à proposta abaixo: as abas se chamam "Montar" e "Prontas"; as frases prontas não são editáveis; e pranchas antigas não precisam de migração, porque a regra de fala resolve a repetição. Detalhes em [funcionalidades/caa.md](funcionalidades/caa.md).

O conflito do item 8 vem de um único erro de modelo: a mesma grade mistura **palavras para montar** ("eu", "quero") com **frases completas** ("Estou com dor"). Nenhuma regra de texto resolve isso de forma confiável, então a separação precisa estar na tela:

1. **Duas abas na mesma área:** "Frases prontas" (um toque fala) e "Montar frase" (palavras). As frases salvas entram na primeira.
2. **Na grade de montar, só palavras:** "Dor" passa a falar "dor", então "estou" + "dor" não repete nada. Adicionar "com" para que a frase soe natural.
3. **Topo mais simples:** organizar e preferências ficam dentro de "Mais", e o título e os ícones saem do caminho.

**Riscos:** pranchas já criadas continuam com as frases antigas e precisariam de migração. A escolha de vocabulário deveria passar por alguém de fonoaudiologia.

## 4. Outros problemas encontrados

### P1: mostra algo errado ou atrapalha o uso

| Problema | Onde | Por que importa |
|---|---|---|
| **Carregando ou com erro, o Início diz "Nenhuma atividade para hoje"** | `TodayRoute`: `(state.content as? Content)?.value.orEmpty()` | Informa algo falso e viola a regra de não esconder erro como ausência de conteúdo |
| **A cor das linhas da CAA depende da posição, não do tipo de palavra** | `AacBoardGrid`: `index / spec.columns` | Ao trocar o tamanho dos símbolos, mudam as colunas e as cores deixam de indicar o tipo. Sugestão: guardar a categoria no símbolo, como a chave de Fitzgerald, padrão em CAA |
| **O padrão da CAA é o menor símbolo** | `symbolSize` SMALL | Priorizei os 4 colunas do wireframe, mas o público pode ter dificuldade motora, e sobra uma faixa vazia entre a grade e as ações. Os símbolos poderiam crescer até ocupar a altura |
| **A frase mostra só o fim** | `MessagePieces` rola uma linha | Em frases longas o começo some, e a pessoa não confere o que vai ser dito |
| **"Limpar" apaga tudo com um toque, ao lado de "Apagar"** | `AacMessageActions` | Toque errado perde a frase, sem como desfazer. Mantive por causa do wireframe, mas recomendo um "Desfazer" |
| **"Sair da conta" não pede confirmação** | `ProfileScreen` | Um toque encerra a sessão, e é preciso internet para entrar de novo |
| **Contraste baixo no tema claro** (o teste só cobre o escuro) | `Color.kt` | Ver a tabela abaixo |

| Par no tema claro | Contraste | Mínimo AA |
|---|---|---|
| Texto de apoio `#8A9BAA` no fundo (login) | 2,71 | 4,5 |
| Placeholder `#A0B2C0` em branco | 2,18 | 4,5 |
| Branco sobre o fim do degradê do Perfil `#4FA3A3` | 2,96 | 4,5 ("Conta ori" é texto pequeno) |
| Ciano `#0082A3` no fundo | 4,21 | 4,5 |
| Branco sobre o botão ciano `#0082A3` | 4,45 | 4,5 |
| Texto `#5C768D` no fundo | 4,49 | 4,5 |

Os três primeiros são claramente insuficientes. Os outros ficam perto do limite, mas envolvem cores originais da marca, então a decisão é sua.

### P2: inconsistências

**Nomes**

- O mesmo objeto tem três nomes: "Nova tarefa", "Adicionar atividade" e "Nova atividade". Em Tarefas > Todas, o texto vazio manda usar "Adicionar atividade", mas o botão diz "Nova tarefa".
- A mesma área se chama "Comunicar" na navegação e "Formar frase" no título. A rotina é "Rotina" e "Minha rotina".
- "Rotinas cumpridas" é, na verdade, a porcentagem de atividades concluídas.
- "Uso de CAA (vezes)" usa um termo técnico.
- "Conta ori" está em minúscula.
- "Acompanhados" e "Responsáveis vinculados" não deixam claro quem gera e quem digita o código: quem acompanha gera, quem é acompanhado digita. Depois da conta única, essa direção não é óbvia.

**Ícones**

- A estrela significa Acessibilidade e também "Salvar frase" e "Frases salvas".
- O lápis significa Comunicar, Formar frase, Preferências de comunicação e organizar pranchas. Em quase todo app, ele quer dizer editar.
- Tema e Tela usam a mesma ferramenta.
- Responsáveis e Acompanhados usam o mesmo rosto.
- Em Tarefas, todo cartão tem o mesmo calendário, que ocupa espaço sem informar nada.

**Estrutura e caminhos repetidos**

- Acessibilidade e Comunicação aparecem no Perfil e dentro de Configurações.
- As preferências da CAA têm um terceiro caminho, pela engrenagem da tela.
- Os atalhos do Início repetem a navegação inferior (Rotina e Formar frase). Mídia e Relatórios só existem ali.
- Tarefas e Rotina mostram os mesmos dados, mas só a Rotina abre detalhes e muda de dia.

**Componentes e cores**

- **Dois destaques competem:** ciano (`primary`) em Falar e Salvar; teal (`strong`) em abas e "+ Adicionar". Fica mais claro escolher um para ação principal.
- **Dois estilos de seleção:** abas de `OriFilterTabs` e `FilterChip` nas pranchas.
- **Formatos de botão diferentes:** pílula (`LoadingButton`) e canto médio (ações da CAA). A documentação chama as abas de "pílula", mas elas têm canto médio.
- **"Aa" duplicado em Entrar ou criar conta:** `OriTopBar` e `BrandHeader` trazem o botão, com dois cabeçalhos na mesma tela.
- **"Aa" ausente na tela principal do Perfil:** ela não tem `OriTopBar`, embora o componente prometa o botão "de qualquer lugar".
- **Sair em vermelho:** a cor sugere perigo, mas a ação não apaga nada.

**Layout**

- O título da barra começa em 16 dp, e o conteúdo em 24 dp, então não alinham (visível no Início).
- O cabeçalho do Perfil vai até as bordas, enquanto o resto tem margem.
- Mídia e Acompanhados usam margem fixa de 16 dp, em vez de seguir a janela.
- Em Acompanhados, o cartão de convite fica fora da lista rolável. Com texto ampliado ou janela baixa, ele toma o espaço dos nomes.

**Formulários**

- Horário é digitado à mão ("08:30"). Um seletor evita erro e ajuda quem tem dificuldade motora.
- Só dá para escolher a data semana a semana, com setas. Falta um calendário.
- O emoji do símbolo é um campo de texto, além das sugestões.
- Editores têm voltar e "Cancelar" juntos, e "Cancelar" tem estilo menor que as outras ações.

**Outros**

- **Abertura:** espera fixa de 5 segundos antes de mostrar as opções, em toda primeira abertura. "Entrar" e "Criar conta" têm o mesmo peso, e "Continuar sem conta" é o mais discreto, embora seja o caminho sem barreira.
- **Tocar em um trecho da frase o remove sem aviso visual:** nada indica isso, e um toque errado apaga a palavra.
- **Emojis como símbolos:** mudam de desenho entre aparelhos, e vários são arbitrários (🌙 "estou", 💡 "preciso", 🥛 é copo de leite para "água"). Sugestão: pictogramas como ARASAAC, verificando a licença.
- **Rótulos misturam maiúscula e minúscula:** "Ajuda" e "quero". Falta ouvir como a voz real lê "Ele/Ela", porque a barra pode soar estranha (não testado).
- **Mídia:** o erro chama `onRetry = {}`, então "Tentar novamente", se aparecer, não faz nada. O nome é obrigatório antes de salvar, mesmo para quem só quer guardar a imagem.
- **Relatórios:** o gráfico não tem escala de porcentagem, e a linha liga dias não seguidos como se fossem contínuos. Barras por dia seriam mais honestas.

### P3: acabamento

- Espaçamentos fora de `OriSpacing` em Login, Cadastro e Abertura (10, 12, 14, 20 e 28 dp).
- Botões da abertura com 50 dp, contra o padrão de 52 dp.
- `88.dp` repetido em três telas, e `64.dp` e `44.dp` soltos em componentes.
- `accentIndex` escolhido à mão em cada menu.
- Botões sem altura mínima em `NoBoardsState` e `PictureButtons`.
- Cores que sobraram do template em `colors.xml` (roxo e teal).
- `error` e `errorText`, além de `tertiary` e `successText`, duplicam papéis.
- Partes de [design-system.md](design-system.md) estão desatualizadas: tabela de títulos por papel, frase dizendo que a CAA não tem tela e "três colunas" como padrão.

## 5. Ordem sugerida

1. **Correções rápidas:** Início mentindo em erro, contraste do tema claro, "Aa" no Perfil e duplicado, nomes e ícones.
2. **Reforma da CAA (seção 3).**
3. **Editor de atividades:** ordem dos campos, seletor de horário e calendário, arrastar a semana.
4. **Foto de perfil**, depois de decidir onde ela fica.
5. **Repetição de atividades:** por último, por ser uma mudança de dados e regras.
