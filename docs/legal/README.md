# Privacidade, LGPD e Google Play

Este guia explica como o ORI cumpre a Lei Geral de Proteção de Dados (LGPD) e as políticas do Google Play, e o que
precisa ser feito a cada mudança. A regra permanente está no [AGENTS.md](../../AGENTS.md), seção 14.

> Não é um parecer jurídico. Antes de publicar na loja, vale uma revisão por alguém de direito digital (por exemplo,
> o professor orientador do TCC ou um núcleo de prática jurídica).

## Quem é quem

| Papel | Quem |
|---|---|
| Controlador (decide sobre os dados) | Equipe ORI, TCC da Etec da Zona Leste: Matheus Mendonça de Lima, Nicollas Lopes Costa e Mariana Moreira dos Santos |
| Encarregado (DPO) e canal do titular | matheusmendima03@gmail.com (resposta em até 15 dias) |
| Operador (trata em nome da equipe) | Google (Firebase Authentication e Cloud Firestore), pelos termos de tratamento de dados do Google Cloud |
| Onde ficam os dados | Firestore em `southamerica-east1` (São Paulo); o login é processado pelo Google, também fora do Brasil |

## Onde estão os documentos

- **Texto, nos 3 idiomas:** `web/app/legal/content.ts` (uma fonte só). Páginas: `/{pt,en,es}/privacy/`,
  `/terms/` e `/delete-account/`, publicadas com o Firebase Hosting em `https://oriteam-ori.web.app`.
- **No app:** a tela de consentimento, Configurações > Privacidade e Configurações > Sobre o ori abrem essas páginas
  (`LegalLinks`).
- **Versão:** `LEGAL_DOCUMENTS_VERSION` no app (`domain/legal/LegalConsent.kt`) e na web (`content.ts`), sempre iguais.

## Como cada requisito é cumprido

| Requisito | Como | Onde e teste |
|---|---|---|
| J1. Política e termos | Páginas públicas nos 3 idiomas, links no app | `web/app/legal`, `AboutScreen`, `PrivacyScreen` |
| J2. Consentimento para dados sensíveis | Tela "Antes de começar" no primeiro uso, com interruptores separados para os termos e para os dados sensíveis; o registro guarda versão, data, idade e responsável; retirar é apagar | `ConsentGate`, `ConsentViewModelTest`, `accountDeletion.test.mjs` |
| J3. Crianças | "Tenho menos de 18 anos" exige a confirmação do responsável legal; as regras recusam o registro sem ela | `ConsentViewModelTest`, `accountDeletion.test.mjs` |
| J4. Autonomia do acompanhado | Só o acompanhado muda as permissões do responsável e qualquer um dos dois encerra o vínculo; frases salvas e atividades ocultas nunca aparecem para o responsável | `CareLinksViewModelTest`, `careLinks.test.mjs`, `aac.test.mjs` |
| J5. Acesso, portabilidade e exclusão | "Baixar uma cópia dos meus dados" (JSON) e "Apagar minha conta" (tudo, pelo próprio app, sem servidor); página web de exclusão para quem não tem o app | `PrivacyViewModelTest`, `AccountExportTest`, `accountDeletion.test.mjs` |
| J6. Encarregado e incidentes | Contato na política e no app; plano abaixo | Este documento |
| J7. Transferência internacional | Firestore no Brasil; login citado como processado fora, com as garantias do art. 33 | Política, seção 7 |
| J8. Não é dispositivo médico | Aviso em Sobre, nos termos e na política | `AboutScreen` |
| J9. Licenças | Lista em Sobre; emoji vêm da fonte do aparelho; nenhum símbolo de CAA protegido | `OpenSourceLicenses` |
| J10. Segurança de Dados do Google Play | Respostas em [seguranca-de-dados.md](seguranca-de-dados.md) | — |

### Como a exclusão da conta funciona

1. O app confere se o login é recente (o Firebase exige isso para apagar uma conta). Se não for, pede para sair e
   entrar de novo, sem apagar nada.
2. Marca o perfil com `deletingAt`. Com essa marca, as regras deixam a dona apagar também as atividades que um
   responsável reservou (sem ela, essas continuam protegidas).
3. Apaga, em lotes: símbolos e pranchas, atividades, séries, frases salvas, fotos, preferências da conta; encerra e
   remove os vínculos (que guardam nomes); apaga o consentimento e o perfil; por último, o login.
4. Sem conexão, para em 60 segundos e avisa; o que já foi enviado continua.

**Limite conhecido:** os códigos de convite ficam no servidor (só as Cloud Functions mexem neles) e não são apagados
pela conta; valem 24 horas e guardam o identificador e o nome de quem os criou. Apagá-los automaticamente pede
uma política de TTL no Firestore (item S2 da revisão). Hoje as funções de convite não estão publicadas, então nenhum
convite é criado.

## Plano para incidentes (J6, LGPD art. 48)

1. Quem perceber avisa a equipe no mesmo dia; registre o que aconteceu, quando e quais dados.
2. Contenha: troque chaves, publique regras corrigidas, desative o que estiver vazando.
3. Avalie o risco às pessoas. Havendo risco ou dano relevante, avise a ANPD (formulário no site da ANPD) e as pessoas
   afetadas em até 3 dias úteis, dizendo o que aconteceu, quais dados, o que foi feito e o que elas podem fazer.
4. Corrija a causa e registre a lição em [revisao-de-codigo.md](../revisao-de-codigo.md).

## Antes de publicar

- [x] As páginas legais publicadas (`firebase deploy --only hosting`, em `functions/`; o deploy gera o site e o
      copia para `functions/public`). Publicadas em 09/11/2026 e conferidas nos 3 idiomas.
- [ ] Na Play Console: URL da política (`/pt/privacy/`), URL de exclusão de conta (`/pt/delete-account/`),
      formulário de Segurança de Dados conforme [seguranca-de-dados.md](seguranca-de-dados.md), classificação de
      conteúdo e público-alvo incluindo menores (política de Famílias do Google Play).
- [ ] No console do Google Cloud do projeto: aceitar os termos de tratamento de dados (Data Processing Terms).
- [ ] A impressão SHA-1 do certificado de publicação cadastrada no Firebase (login com Google).
- [ ] `LEGAL_DOCUMENTS_VERSION` igual no app e na web; texto da loja sem promessa de tratamento ou terapia.
- [ ] Revisão jurídica dos textos.

## Histórico dos documentos

| Versão | Data | O que mudou |
|---|---|---|
| 1 | 09/11/2026 | Primeira versão: política, termos e página de exclusão nos 3 idiomas |
| 1 | 10/11/2026 | ORI Home: a política cita a lista de apps e as escolhas do launcher, que ficam só no aparelho. Sem mudança de versão: nada novo é coletado |
| 1 | 10/11/2026 | ORI Home: tamanho dos ícones, botões e fundo da tela, com a foto do fundo guardada só no aparelho. Sem mudança de versão: nada sai do aparelho, então nada novo é coletado |
| 1 | 10/11/2026 | ORI Home: telas, posição livre dos ícones, painel com a rotina ou as frases, papel de parede do Android e widgets (só o número e o tamanho de cada um ficam no aparelho). Sem mudança de versão: nada novo sai do aparelho |
| 1 | 10/11/2026 | ORI Home: histórico de até 8 fotos de fundo, guardado só no aparelho, e "Desinstalar" (abre a confirmação do Android). Sem mudança de versão: nada sai do aparelho |
| 1 | 10/11/2026 | ORI Home: o fundo pode ir também para a tela de bloqueio do Android (a pedido da pessoa; sem cópia de segurança). Sem mudança de versão: nada sai do aparelho |
| 1 | 11/11/2026 | ORI Home: a posição dos ícones com o tablet deitado (`launcher_positions_turned`) e "Travar a tela inicial" (`launcher_layout_locked`), escolhas guardadas só no aparelho, como as outras do ORI Home (não entram na exportação nem na exclusão da conta, que são da conta). Sem mudança de versão: nada novo sai do aparelho |
| 1 | 11/11/2026 | ORI Home: "Grade da tela inicial" (`launcher_grid`, colunas × linhas escolhidas), guardada só no aparelho como as outras escolhas do ORI Home. Sem mudança de versão: nada novo sai do aparelho |
| 1 | 11/11/2026 | ORI Home como o One UI: pastas (`launcher_folders`), atalhos de apps e pedidos de "Adicionar à tela inicial", selos com o "Acesso a notificações" (só a contagem por app, na memória; nunca o conteúdo), apps recentes na barra (só na memória), "Ocultar apps" e opções de gestos (`launcher_swipe_down_notifications`, `launcher_shows_labels`, `launcher_adds_new_apps`, `launcher_turns_on_phone`, `launcher_badges`, `launcher_recent_apps`, e depois `launcher_drop_preview`; em 12/11/2026, o fundo que o ORI pôs na tela de bloqueio e o número que o Android deu à imagem, `launcher_lock_wallpaper` e `launcher_lock_wallpaper_id`, para a prévia; e os selos passaram a vir com o número, como no One UI, mas só contam depois que a pessoa permite o "Acesso a notificações" no Android; e o formato dos ícones, como todos os apps aparecem e se há tela inicial: `launcher_icon_shape` (depois `launcher_icon_outline`, com três formatos), `launcher_drawer_style`, `launcher_screen_mode`; e o nível de estímulo no lugar da "Baixa estimulação", escolha por escolha: `launcher_stimulus_motion`, `launcher_stimulus_wallpaper`, `launcher_stimulus_colors`, `launcher_stimulus_simplified`, com `launcher_low_stimulation` apagada na primeira mudança). A política cita tudo nos 3 idiomas. Na comunicação, o aviso de volume no zero só lê o volume do Android na hora, sem permissão e sem guardar nada; e o volume em que a voz toca, mídia ou toque (`aac_voice_sound`), fica guardado só no aparelho como as outras preferências da voz. Sem mudança de versão, como nas mudanças anteriores do ORI Home: nada novo sai do aparelho, e o acesso às notificações é pedido pelo próprio Android, a pessoa decide lá e pode tirar quando quiser |
| 1 | 12/11/2026 | Comunicação: a ordem das frases salvas (`position` em `aacSavedMessages` e no aparelho), que a pessoa muda segurando e arrastando. Sai na exportação e some com a frase ao apagar a conta; a política cita a ordem nos 3 idiomas. Sem mudança de versão: faz parte das frases que a pessoa já salva, sem novo uso, finalidade ou terceiro |
| 1 | 12/11/2026 | ORI Home: "Mostrar o dock" (`launcher_shows_dock`), escolha guardada só no aparelho como as outras do ORI Home; a política cita se o dock aparece entre as escolhas, nos 3 idiomas. Sem mudança de versão: nada novo sai do aparelho |
| 1 | 12/11/2026 | Comunicação: a ordem das frases que vêm com o app (`aac_ready_phrase_order`), escolha guardada só no aparelho como as outras preferências da comunicação (não vai para a conta nem para a exportação); a política cita a ordem das frases prontas nos 3 idiomas. Sem mudança de versão: nada novo sai do aparelho |
