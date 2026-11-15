# Formulário de Segurança de Dados do Google Play (J10)

As respostas que a Play Console pede, conforme o que o app faz hoje. A regra do [AGENTS.md](../../AGENTS.md) (seção 14)
manda atualizar este arquivo junto com qualquer dado novo, e a Play Console em seguida.

## Visão geral

| Pergunta | Resposta |
|---|---|
| O app coleta ou compartilha dados do usuário? | Sim (coleta, com conta). Sem conta, nada sai do aparelho |
| Os dados são criptografados em trânsito? | Sim (HTTPS/TLS do Firebase) |
| O usuário pode pedir a exclusão dos dados? | Sim: no app (Configurações > Privacidade > Apagar minha conta) e pela página `/pt/delete-account/` |
| Compartilha dados com terceiros? | Não. O Google (Firebase) é prestador de serviço (operador), o que não conta como compartilhamento |
| Publicidade ou analytics? | Não |

## Dados coletados (só com conta)

"Obrigatório" quer dizer: sem ele a conta não funciona. Nenhum dado é usado para publicidade, marketing ou
personalização de anúncios.

| Categoria da Play | Dado no ORI | Obrigatório? | Finalidade |
|---|---|---|---|
| Informações pessoais > Nome | Nome do perfil, nomes nos vínculos | Sim | Funcionalidade do app |
| Informações pessoais > Endereço de e-mail | E-mail do login | Sim | Gerenciamento da conta |
| Informações pessoais > IDs do usuário | Identificador da conta (Firebase) | Sim | Gerenciamento da conta, funcionalidade |
| Fotos e vídeos > Fotos | Foto do perfil e fotos de símbolos | Não | Funcionalidade do app |
| Informações de saúde e condicionamento físico > Informações de saúde | Rotina e comunicação podem revelar saúde ou deficiência | Sim | Funcionalidade do app |
| Mensagens > Outras mensagens no app | Frases salvas (com o emoji e a ordem delas) e texto das pranchas | Não | Funcionalidade do app |
| Atividade no app > Outro conteúdo gerado pelo usuário | Atividades, séries, pranchas, símbolos, preferências da conta | Sim | Funcionalidade do app |

## Não coletado

Localização, contatos, agenda, arquivos (o arquivo exportado é salvo pela própria pessoa), áudio (a voz é gerada no
aparelho), informações financeiras, histórico de navegação, IDs do aparelho para publicidade, dados de diagnóstico e
falhas (o app não usa Crashlytics nem analytics).

## Ficam só no aparelho (não contam como coleta)

Voz escolhida e o volume em que ela toca, tamanho dos símbolos, ordem das frases prontas (`aac_ready_phrase_order`), espaçamento, contagem de uso da comunicação, tudo do uso sem conta, a cópia do
consentimento, o registro de que a abertura do logo já foi vista e, no ORI Home, a lista de apps instalados
(lida com `<queries>`, sem `QUERY_ALL_PACKAGES`) e as escolhas de favoritos, dock, apps ocultos, nível de estímulo (animações, papel de parede, cores e enfeites), botões, posição dos ícones (em pé e com o tablet deitado), número de telas e tela principal, tela travada, grade escolhida, painel,
tamanho dos ícones, fundo da tela e widgets (só o número do widget no Android e o tamanho; o conteúdo é do app do
widget e não passa pelo ORI). O painel à esquerda mostra a rotina ou as frases já guardadas pelo ORI: nenhum dado novo. A foto
escolhida para o fundo (pelo seletor de fotos do sistema, sem permissão de armazenamento) fica só como uma cópia
reduzida na pasta privada do app (`launcher_wallpaper`); as últimas 8 ficam num histórico para escolher de novo e cada
uma é apagada ao sair dele; nunca são enviadas. "Desinstalar" (permissão `REQUEST_DELETE_PACKAGES`) só abre a
confirmação do Android; nada é coletado. "Tela de bloqueio" (permissão `SET_WALLPAPER`) entrega a imagem escolhida ao
Android como fundo da tela de bloqueio, sem marcar para cópia de segurança; nada sai do aparelho pelo ORI.

Também no ORI Home, só no aparelho: as pastas (`launcher_folders`: nome e apps de cada uma), os atalhos de apps na
tela inicial (a chave guarda só o identificador que o app dá ao atalho; o nome e a figura são lidos do Android a cada
vez), e as escolhas de gestos e opções (`launcher_swipe_down_notifications`, `launcher_shows_labels`,
`launcher_adds_new_apps`, `launcher_turns_on_phone`, `launcher_badges`, `launcher_recent_apps`,
`launcher_drop_preview`, `launcher_shows_dock`, `launcher_icon_outline`, `launcher_drawer_style`, `launcher_screen_mode`), e qual fundo o próprio ORI pôs na tela de bloqueio com o número que o Android deu a essa
imagem (`launcher_lock_wallpaper`, `launcher_lock_wallpaper_id`), só para desenhar a prévia; a foto usada lá fica na
pasta privada enquanto a tela de bloqueio a mostra. O ORI não lê a imagem do papel de parede do Android: as prévias o
mostram pela própria janela, sem permissão. Os **selos dos
ícones** (com o número, como no One UI, desde 12/11/2026; antes desligados até a pessoa escolher) usam o "Acesso a
notificações" do Android (`BadgeNotificationListener`, só com a permissão dada pela pessoa nas configurações do
Android, e ligado só enquanto o ORI Home está ligado; sem essa permissão nada é contado): de cada notificação o ORI lê o app, o
perfil, o número e se ela conta para o selo; nunca título, texto ou anexos. A contagem fica só na memória e não é
gravada, registrada em log nem enviada. Os **apps recentes** da barra são os últimos abertos pelo ORI Home, só na
memória (não usa o "Acesso ao uso" do Android). Os pedidos de "Adicionar à tela inicial" de outros apps só são
aceitos depois do sim da pessoa. Nada disso é coletado no sentido da Google Play (nada sai do aparelho).
