# Plano de finalização do ORI (15/11/2026)

Este documento junta, num lugar só, **o que já foi feito** na rodada de finalização e **tudo o que ainda falta**:
defeitos, recursos pedidos (modo foco, notificações, alto contraste, gráfico), acessibilidade, ideias novas e o que
depende de alguém fora do código. Os detalhes de cada defeito continuam em [revisao-de-codigo.md](revisao-de-codigo.md)
e [teste-geral.md](teste-geral.md); aqui fica o mapa.

**Como ler a situação de cada item** (a mesma classificação do [AGENTS.md](../AGENTS.md), seção 12):

| Marca | Quer dizer |
|---|---|
| ✅ | Implementado e testado (teste automático e, quando indicado, no aparelho) |
| 🟡 | Implementado e parcialmente testado |
| 🔒 | Bloqueado por configuração externa (console, loja, pessoa responsável) |
| ⬜ | Não implementado |

---

## 1. O que foi feito nesta rodada

### 1.1 Commits

| Commit | O que entrou |
|---|---|
| `2412e2a` | Todo o trabalho que estava sem commit (517 arquivos): ORI Home, documentos legais, idiomas e correções das revisões anteriores |
| `0dbef5b` | Convites sem Cloud Functions |
| `5e0d927` | R5, R6 e R7 |
| `94bc63e` | G2 e G3 |
| `e3af486` | G1 e G4 |
| *(sem commit ainda)* | R12, R13, o teste com duas contas registrado e este documento |

### 1.2 Parte de cuidador: estava parada e agora funciona

| Item | Situação | Onde está |
|---|---|---|
| **Convites sem Cloud Functions.** Gerar e aceitar código dependiam de funções que exigem o plano Blaze e nunca foram publicadas, então nenhum vínculo podia ser criado. Agora o app grava e as regras do Firestore conferem tudo: autor real, nome do perfil, código de 8 caracteres, 24 h, uso único, último código do autor, sem vínculo consigo mesmo, menor privilégio. Um código por vez por pessoa, com 30 s entre um e outro | ✅ 16 testes no emulador (com as regras enfraquecidas de propósito, para provar que os testes pegam a falha), `InvitationCodesTest`, `InvitationViewModelTest`. **Regras publicadas** no `oriteam-ori` | [perfis-e-permissoes.md](funcionalidades/perfis-e-permissoes.md#5-convites-o-app-grava-as-regras-conferem) |
| O app deixou de depender do Firebase Functions (dependência, injeção e o erro "serviço indisponível" saíram) | ✅ | `data/carelink/FirestoreInvitationRepository.kt` |
| Exportar e apagar a conta incluem o código de convite | ✅ (regras no emulador) | `data/account/FirestoreAccountData.kt` |
| **Teste com duas contas no projeto real** (celular e tablet): gerar, aceitar, dar permissão, criar, concluir e excluir atividade, encerrar o vínculo com a tela aberta. Tudo sincronizou na hora | ✅ no aparelho | [perfis-e-permissoes.md](funcionalidades/perfis-e-permissoes.md#teste-com-duas-contas-no-projeto-real-15112026) |
| **R5.** Apagar "todos os dias" de uma série com um dia marcado por quem acompanha dava erro | ✅ regra publicada, teste no emulador e no app | revisão, "Corrigidas" |
| **R6.** Fotos de símbolos apagados por quem acompanha ficavam na conta | ✅ regra publicada, testes | idem |
| **R7.** Apagar prancha sem internet podia deixar símbolos órfãos | 🟡 conferido no código (depende do Firestore real) | idem |

### 1.3 Outros defeitos corrigidos

| Item | Situação |
|---|---|
| **G2.** Apagar "todos os dias" olhando um dia feito apagava o histórico do relatório | ✅ |
| **G3.** "Cancelar" ficava sob o dedo com o teclado e descartava sem perguntar. Agora pergunta "Descartar as mudanças?" (Cancelar, seta e voltar do sistema), e a barra de ações espera o teclado fechar quando falta altura (`FormLayout`, também no formulário de símbolo, o que resolve o **P6**) | ✅ e conferido no tablet |
| **G1.** O teclado mudava a grade do ORI Home (8 → 5 colunas) | 🟡 sem teste automático; não conferido no aparelho (o ORI Home está desligado no tablet) |
| **G4.** Campo de hora: exemplo e relógio no formato do aparelho, teclado de números com 24 h, "830"/"0830" aceitos, aviso ao sair do campo | ✅ e conferido no tablet (o teclado de números não foi conferido) |
| **R12.** Falha do banco ao apagar os dados do aparelho fechava o app; contagem da comunicação mostrava "0 vezes" quando falhava | ✅ |
| **R13.** O cadastro aparecia como erro quando só o nome do Firebase Auth falhava | 🟡 conferido no código |

### 1.4 Verificação feita

Em cada passo: detekt, os testes unitários (893 no último), Android Lint, builds `dev`, `prod` e `profiling`, e os testes
das regras no emulador (126). Nenhum falha.

---

## 2. Pendências que dependem de alguém fora do código 🔒

| O quê | Quem | Por quê |
|---|---|---|
| Ligar a **política de TTL** do Firestore em `invitations.expiresAt` (console → Firestore → Time-to-live) | Pessoa com acesso ao console | Apaga sozinho os convites vencidos (S2). Passo a passo em [perfis-e-permissoes.md](funcionalidades/perfis-e-permissoes.md#como-publicar) |
| Apagar o índice `routineTasks (status, scheduledTime)` que sobra no projeto (S4) | Idem, ou eu com `--force`, se autorizado | Encarece as gravações sem uso |
| **App Check** no Firestore (S1) | Console + chave do Play Integrity | Reduz gravações feitas fora do app |
| Lista "Antes de publicar" de [legal/README.md](legal/README.md#antes-de-publicar): SHA do certificado de publicação (login com Google), Play Console (política, exclusão, Segurança de Dados, público e Famílias), termos de tratamento de dados do Google Cloud, **revisão jurídica** dos textos | Equipe | Exigências da loja e da LGPD |
| Ligar o **ORI Home** no tablet de teste, para eu conferir o G1 e testar o modo foco | Você | Ligar muda a tela inicial do aparelho, então não faço sozinho |

---

## 3. Defeitos ainda abertos

### 3.1 Achados no teste com duas contas (15/11/2026) ⬜

| Id | O que acontece | Correção sugerida |
|---|---|---|
| T1 | Depois de aceito, o código continua na tela de quem convidou, como se valesse | Esconder o código quando a pessoa aparece na lista (ou mostrar "Usado por …") |
| T2 | Depois de encerrar o vínculo, o tablet ainda mostra "Done! You are now linked with…" | Limpar a mensagem de sucesso quando a lista de vínculos muda |
| T3 | "Acompanhados" vazio diz "Gere um código de convite em **Conta**", e a área se chama "Perfil" (e o botão está na mesma tela) | Trocar o texto nos 4 idiomas, citando o botão pelo nome |
| T4 | O formulário de **símbolo** ainda fecha sem perguntar quando há algo escrito | Usar `rememberLeaveWithConfirmation`, como no de atividade |

### 3.2 Falhas da revisão de código ⬜

| Id | Resumo | Gravidade |
|---|---|---|
| R8 | Com 100 fotos, não dá para trocar uma foto existente; a contagem roda fora de `runDataOperation` | Baixa |
| R9 | A foto antiga de um símbolo é apagada antes de o servidor confirmar a edição | Baixa |
| R10 | Desligar a repetição sem conseguir ler as séries tenta editar um documento que não existe | Baixa |
| R11 | Passagem para a conta compara títulos de um jeito diferente do formulário (pode duplicar) | Baixa |
| R14 | O texto do contrato promete criação idempotente que só vale enquanto a gravação está pendente | Baixa |
| G5 | Nas setas de organizar a prancha, a palavra movida nem sempre fica sob o dedo | Baixa |
| S3 | O nome guardado no vínculo não acompanha uma troca de nome | Baixa |
| C2 | "Hoje" usa o fuso do aparelho de quem acompanha, não o da pessoa | Baixa (pede decisão: muda o perfil e as regras) |
| C1 | Cada pessoa aberta deixa ViewModels (e um motor de voz) vivos até o app fechar | Média (muda a arquitetura de navegação) |

### 3.3 Desempenho e custo ⬜

| Id | Resumo |
|---|---|
| P1 (revisão) | Fotos que a conta não tem são procuradas de novo a cada atualização da lista de símbolos |
| P2 (revisão) | `photoStore.pathOf` consulta o disco na thread principal |
| P3 (revisão) | O motor de voz de cada pessoa fica ligado até o app fechar (junto com C1) |
| P4 (revisão) | `getDays` corta em 31 páginas sem registrar |
| M2 | Trocar de área ainda tem um primeiro quadro de 30 a 60 ms (parcial) |
| M7 | Avisos e o calendário ainda animam a altura (parcial) |
| M10 | Mapeamento dos ViewModels na thread principal (parcial) |
| M14 | Fontes Nunito baixadas no primeiro uso; o texto é medido de novo |
| M22 | Relatórios na primeira vez esperam o servidor (150–200 ms) |
| M23 | Menus do Material na CAA ("Mais", opções da prancha) ainda abrem como janela à parte |

### 3.4 Pontos de melhora do teste geral ⬜

| Id | Onde | Melhora |
|---|---|---|
| P1 | Frases prontas | Destacar a frase enquanto fala; avisar quando o volume está no zero |
| P2 | Salvar frase | Avisar quando a frase já existe entre as prontas |
| P3 | Adicionar palavra | Rolar até a palavra nova e destacá-la |
| P4 | Organizar | Mostrar "Diz: X" só quando for diferente do rótulo |
| P5 | CAA com texto "Muito grande" | Os rótulos seguirem o "Aa" (ou explicar onde mudar) |
| P7 | Rotina | Ações rápidas ao segurar (concluir, apagar) e arrastar para outra hora ou dia |
| P8 | Relatórios | Gráfico legível (é a Fase 4, abaixo) |
| P9 | ORI Home travado | Avisar assim que o dedo se mexe, não só ao soltar |
| P10 | ORI Home, todos os apps em telas | Espaço vazio entre a última fileira e os pontos |
| P11 | ORI Home, Máximo | Apps de ícone branco ficam com o bloco cinza; usar a cor mais viva do ícone |
| P12 | Detalhes da atividade | "Repete" aparece duas vezes |
| P13 | Rotina, semana | Mostrar os pontinhos de "tem atividade" também na semana |
| P14 | Tarefas | Tocar no cartão não faz nada; marcar como feita ou abrir os detalhes |
| P15 | Relatórios | Atividades de hoje que ainda não chegaram contam como não feitas (é a Fase 4) |
| P16 | Rotina, dia vazio | O painel da direita diz "Escolha uma atividade" sem ter o que escolher |
| P17 | Frases prontas | Separar com título a lista das salvas da lista que vem com o app |

### 3.5 Design duvidoso, a decidir ⬜

| Id | Questão |
|---|---|
| D1 | A marca aparece como "ori" e "ORI": escolher uma (sugestão: sempre "ORI") |
| D2 | Larguras diferentes entre telas de uma coluna no tablet deitado |
| D3 | Duas faixas de semana ao mesmo tempo no tablet (a da lista e a do formulário) |
| D4 | App em inglês com o sistema em português: confirmar se é o desejado |
| D5 | Frase longa deixa a linha das prontas mais alta; "with" sem figura |
| D6 | No Máximo, cada app com a sua cor pode ser estímulo demais; opção de uma cor só |
| D7 | Tema claro: cartões quase da cor do fundo (ligado ao alto contraste, Fase 5) |

---

## 4. Recursos novos pedidos

### 4.1 Gráfico de relatório arrumado (Fase 4) ⬜

- Barras por dia (0–100%), grade em 0/50/100%, datas legíveis conforme a largura, dia sem atividade marcado como vazio
  (diferente de 0%), hoje destacado.
- **P15:** a semana e o mês atuais contam só o que já passou da hora.
- Acessível: o gráfico continua escondido do leitor de tela, com os números nos detalhes, e uma lista "por dia" para
  o TalkBack.
- Onde: `ui/app/screens/reports/DailyChart.kt`, `domain/reports`.

### 4.2 Alto contraste (Fase 5) ⬜

- Preferência "Alto contraste" em Configurações → Acessibilidade, sugerida quando o sistema já usa "Texto em alto
  contraste".
- Temas claro e escuro com texto e controles em 7:1 (AAA), bordas sólidas e cores de área ajustadas. Vale no app e no
  ORI Home.
- Testes de contraste em `OriThemeRulesTest` e imagens a 360 dp. Se a preferência for sincronizada com a conta, entra
  nas regras, na exportação e na política (AGENTS §14).

### 4.3 Notificações: lembrete de atividade (Fase 6) ⬜

- "Lembrar" na atividade com hora (na hora, 5, 15 ou 30 min antes), com um padrão nas preferências.
- Local, sem servidor: `AlarmManager` exato quando o Android permite e inexato quando não, reagendado no boot, ao mudar
  o fuso e quando a rotina muda. Repetidas agendam só a próxima.
- Permissão de notificação pedida só ao ligar o primeiro lembrete, com explicação antes.
- Notificação com "Feito" e "Abrir", **sem o conteúdo na tela de bloqueio** (é dado sensível).
- Fora do escopo: avisar quem acompanha quando a pessoa conclui algo, porque isso exige push pelo servidor (FCM +
  Functions, plano Blaze).
- O campo novo do lembrete precisa da atualização legal (AGENTS §14) e pode pedir para subir `LEGAL_DOCUMENTS_VERSION`.

### 4.4 Modo foco no ORI Home, com horário programado (Fase 7) ⬜

- **O que faz:** com o foco ligado, os apps marcados como "distraem" somem da tela inicial, do dock, de todos os apps e
  da busca. O papel de parede some, e os selos desses apps também. Rotina e Comunicação ficam sempre à vista.
- **Escolha:** uma lista própria do foco, separada dos apps ocultos, com a mesma lista de marcar (`AppChecklist`).
- **Ligar e desligar:** botão no ORI Home e em Opções, com um aviso discreto na tela inicial ("Modo foco até 17:00").
- **Programação:** um ou mais períodos (dias da semana, início e fim), reaproveitando o `RepeatPicker`. Disparo pelo
  mesmo agendador dos lembretes e conferência ao abrir a tela inicial, para um alarme perdido não deixar o estado errado.
- **Autonomia (J4/H6):** não bloqueia nada. A pessoa sempre desliga, Configurações e outros launchers continuam
  acessíveis, e não há administrador do dispositivo nem serviço de acessibilidade. Só vale com o ORI Home como tela
  inicial, e a tela diz isso.
- **Futuro, só com consentimento e visível ao acompanhado (N10):** quem acompanha sugerir um horário de foco.
- **Dados:** só no aparelho (DataStore do launcher), mas entram no mapa de dados.
- Depende de ligar o ORI Home no tablet de teste para conferir (seção 2).

---

## 5. Acessibilidade ainda não conferida (Fase 8) ⬜

| O quê | Como |
|---|---|
| **TalkBack** em Início, Rotina, formulário, Frases, Relatórios, Perfil, convite e diálogos | No aparelho (você liga o TalkBack) |
| **Texto ampliado** (sistema em 200% e "Aa" em Muito grande), sem corte nem sobreposição | No aparelho e em imagens Robolectric |
| **Tema claro** e **animações desligadas** no app inteiro | Idem |
| **Celular em pé a 360 dp** e **tablet em pé** | Idem |
| Accessibility Scanner do Google | No aparelho |
| Testes Compose de área de toque (48 dp) e descrição onde faltam | Automático |

---

## 6. Ideias novas (para decidir) ⬜

Ideias que conversam com o propósito do ORI. Nenhuma foi pedida ainda; ficam aqui para escolher. Todas respeitam o
nível de animação e a baixa estimulação, e nenhuma promete efeito de saúde (J8).

### 6.1 Acessibilidade e autonomia

| Ideia | Para quê |
|---|---|
| **Timer visual** ("falta pouco") na atividade em andamento: um disco que diminui, sem números | Noção de tempo para quem não lê horas |
| **Quadro "Primeiro / Depois"** na Rotina | Apoio comum em rotinas visuais: a próxima coisa e a que vem depois |
| **Rotina em figuras**: um modo que mostra cada atividade com emoji ou foto grande, como as pranchas | Quem não lê acompanha o dia sozinho |
| **Ditado por voz** no título da atividade e no "Escrever" da CAA | Menos digitação para quem tem dificuldade motora |
| **Varredura (switch access)** na prancha: destacar um símbolo por vez e escolher com um toque em qualquer lugar | Acesso motor; o Android tem o "Acesso com interruptor", mas uma varredura própria da prancha é mais simples |
| **Tamanho do alvo** ajustável na prancha (além do "Aa"): poucos símbolos grandes ou muitos pequenos | Motor e visão |
| **Confirmação ao falar** opcional ("Falar 'quero água'?") | Evita fala acidental para quem toca sem querer |
| **Atalhos de tela inicial** do Android (pinned shortcuts) para uma frase pronta ou "Falar" | Comunicação em um toque, mesmo sem o ORI Home |
| **Widget** "Próxima atividade" e "Frases" fora do ORI Home | Ver o dia sem abrir o app |
| **Modo de emergência**: um cartão com "Preciso de ajuda", o nome e um contato, mostrado em letras grandes | Comunicação em situação difícil, sem rede |
| **Frases por lugar ou hora** (sugerir as frases de "escola" de manhã) | Menos rolagem para achar o que dizer |

### 6.2 Recompensa e diversão ("gimmick"), sempre desligáveis

| Ideia | Para quê |
|---|---|
| **Celebração ao concluir o dia**: confete curto ou um selo, respeitando "Animações" (no Reduzido, só um ✓ grande) | Reforço positivo |
| **Sequência de dias** ("3 dias seguidos com a rotina feita"), sem punição quando quebra | Motivação, sem pressão |
| **Selos/adesivos** colecionáveis por marcos (primeira frase salva, primeira semana) | Diversão para crianças |
| **Mascote** do ORI que reage às conclusões (opcional, desligado na baixa estimulação) | Identidade e vínculo com o app |
| **Sons de conclusão** escolhíveis (ou nenhum) | Retorno não visual |
| **Temas de cor** além do teal (com contraste conferido) | Personalização |
| **Prancha de emoções** pronta ("Como estou?"), com registro opcional só no aparelho | Expressão de sentimentos; nunca enviado a ninguém sem escolha da pessoa |

---

## 7. Ordem sugerida a partir daqui

1. Commitar o que está pronto (R12, R13, o teste registrado e este documento).
2. **T1 a T4**, achados do teste com duas contas: rápidos e visíveis para quem usa o vínculo.
3. Terminar o teste do cuidador: comunicação (pranchas), atividade oculta e aceitar de novo depois de encerrar.
4. **Fase 4:** gráfico de relatório (pedido direto).
5. **Fase 5:** alto contraste (pedido direto, também resolve o D7).
6. **Fase 6:** lembretes; **Fase 7:** modo foco, que usa o agendador dos lembretes.
7. Defeitos restantes (R8–R11, R14, G5, S3) e desempenho (P1–P4 da revisão, M14, M23).
8. **Fase 8:** acessibilidade conferida no aparelho.
9. Escolher entre as ideias da seção 6.
10. Pendências externas (seção 2) antes de publicar na loja.
