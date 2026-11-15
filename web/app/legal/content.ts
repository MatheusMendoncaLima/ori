/**
 * The legal texts of ORI in its three languages: the privacy policy, the terms of use and how to delete the
 * account. This file is the only source; the app links to these pages, and AGENTS.md (section 14) requires it
 * to change together with any new data.
 *
 * LEGAL_DOCUMENTS_VERSION must match the app (android: domain/legal/LegalConsent.kt). Raise both when a change
 * needs the person's consent again.
 */
export const LEGAL_DOCUMENTS_VERSION = 1;
export const LAST_UPDATED = "2026-11-09";
export const CONTACT_EMAIL = "matheusmendima03@gmail.com";
export const TEAM = [
  "Matheus Mendonça de Lima",
  "Nicollas Lopes Costa",
  "Mariana Moreira dos Santos",
];

export const LANGUAGES = ["pt", "en", "es"] as const;
export type Language = (typeof LANGUAGES)[number];

export type Section = { title: string; paragraphs: string[] };
export type LegalDocument = {
  title: string;
  intro: string;
  sections: Section[];
};

type Texts = {
  privacy: LegalDocument;
  terms: LegalDocument;
  deletion: LegalDocument;
  updated: string;
  otherLanguages: string;
};

const team = TEAM.join(", ");

const pt: Texts = {
  updated: `Versão ${LEGAL_DOCUMENTS_VERSION}, atualizada em 09/11/2026`,
  otherLanguages: "Outros idiomas",
  privacy: {
    title: "Política de privacidade do ORI",
    intro:
      "O ORI ajuda a organizar a rotina e a se comunicar com símbolos (comunicação aumentativa e alternativa). Esta política explica, em palavras simples, quais dados o app usa, para quê, com quem e como você controla tudo isso.",
    sections: [
      {
        title: "1. Quem cuida dos seus dados",
        paragraphs: [
          `O ORI é um Trabalho de Conclusão de Curso (TCC) da Etec da Zona Leste, feito pela equipe ORI: ${team}. A equipe é a controladora dos dados, nos termos da Lei Geral de Proteção de Dados (LGPD, Lei 13.709/2018).`,
          `Encarregado de dados e canal de contato: ${CONTACT_EMAIL}. Respondemos pedidos sobre dados pessoais em até 15 dias.`,
        ],
      },
      {
        title: "2. Usar sem conta",
        paragraphs: [
          'Ao tocar em "Continuar sem conta", tudo fica só no seu aparelho: a rotina, as pranchas, as frases e as preferências. Nada é enviado para a equipe nem para a nuvem. Você pode apagar tudo em Configurações > Privacidade.',
        ],
      },
      {
        title: "3. Que dados a conta usa",
        paragraphs: [
          "Conta: nome, e-mail e senha (a senha é guardada de forma protegida pelo Google Firebase Authentication; a equipe nunca a vê). Se você entrar com o Google, recebemos o nome e o e-mail da conta Google.",
          "Perfil: o nome que aparece no app e, se você quiser, uma foto.",
          "Rotina: as atividades (título, dia, horário, detalhes, se foi feita e por quem) e as que se repetem.",
          "Comunicação: as pranchas e os símbolos que você cria (palavras, frases, emoji, fotos, cor e tipo de palavra) e as frases que você salva, na ordem que você escolhe.",
          "Preferências da conta: tamanho do texto, tema, interface simplificada e animações.",
          "Vínculos: os nomes de quem acompanha e de quem é acompanhado, e as permissões que a pessoa acompanhada deu.",
          "Consentimento: a versão destes documentos que você aceitou, a data, se você tem 18 anos ou mais e, se não tiver, a confirmação do responsável legal.",
          "Ficam só no aparelho, mesmo com conta: a voz escolhida e o volume em que ela toca, o tamanho dos símbolos, a ordem das frases prontas, o espaçamento, a contagem de quantas vezes você usou a comunicação, as fotos antes de irem para a conta e, se você usar o ORI Home como tela inicial, a lista de apps do aparelho e as suas escolhas (favoritos, apps ocultos, a posição de cada ícone, se a tela inicial está travada, a grade, se o dock aparece, tamanho e formato dos ícones, o nível de estímulo, como todos os apps aparecem, se há tela inicial, fundo da tela e os widgets que você adicionar; de cada widget o ORI guarda só o número que o Android dá a ele e o tamanho, e o conteúdo é mostrado pelo próprio app do widget). Se você escolher fotos para o fundo, cópias reduzidas das últimas (até 8) ficam guardadas só no ORI deste aparelho, para escolher de novo, e são apagadas quando você as tira do histórico. Se você escolher usar um fundo também na tela de bloqueio, o ORI entrega essa imagem ao Android, que passa a guardá-la como fundo da tela de bloqueio (sem cópia de segurança na nuvem), e guarda neste aparelho qual fundo foi e o número que o Android deu a essa imagem, só para mostrar a prévia da tela de bloqueio (a foto usada lá fica guardada enquanto a tela de bloqueio a mostra). O ORI não lê a imagem do papel de parede do Android: as prévias o mostram pela própria janela do ORI. No ORI Home também ficam só no aparelho as pastas (o nome e os apps de cada uma), os atalhos de apps postos na tela inicial (só o identificador que o app dá a cada um) e as escolhas de selos, apps recentes na barra e gestos. Os selos dos ícones vêm ligados, como no One UI, mas só funcionam se você permitir o “Acesso a notificações” do Android (você pode desligá-los nas opções). Com esse acesso, o ORI Home conta quantas notificações cada app tem, para mostrar o número no ícone: ele lê só de qual app é cada notificação e quantas são, nunca o título nem o texto, e não guarda nem envia nada disso; você pode tirar o acesso quando quiser, nas configurações do Android. Os apps recentes da barra são os últimos abertos pelo próprio ORI Home e ficam só na memória, até ele fechar. A lista de apps, a contagem de notificações e a foto do fundo nunca saem do aparelho.",
          "O ORI não usa publicidade, não faz análise de uso (analytics), não vende dados e não pede localização, contatos, microfone ou câmera. As fotos são escolhidas por você no seletor do Android.",
        ],
      },
      {
        title: "4. Dados sensíveis",
        paragraphs: [
          "A rotina e a comunicação podem revelar informações sobre saúde ou deficiência. Por isso, elas só são guardadas na conta com o seu consentimento específico e destacado (LGPD, art. 11, I), pedido no primeiro uso. Você pode retirar o consentimento a qualquer momento; isso apaga os dados da conta.",
        ],
      },
      {
        title: "5. Para que usamos os dados",
        paragraphs: [
          "Só para o app funcionar para você: mostrar e guardar a sua rotina e a sua comunicação, sincronizar entre os seus aparelhos, permitir que quem você autorizou acompanhe e ajude, e manter a conta segura.",
          "Bases legais: o seu consentimento (art. 7, I e art. 11, I), a execução do serviço que você pediu (art. 7, V) e, para registros de segurança e do próprio consentimento, o cumprimento de obrigação legal e o exercício de direitos (art. 7, II e VI).",
        ],
      },
      {
        title: "6. Crianças e adolescentes",
        paragraphs: [
          "Menores de 18 anos só usam a conta com o consentimento de um responsável legal, dado no primeiro uso, sempre no melhor interesse da criança ou do adolescente (LGPD, art. 14). Coletamos apenas o necessário para o app funcionar e nunca usamos os dados para publicidade.",
        ],
      },
      {
        title: "7. Quem mais vê os dados",
        paragraphs: [
          "Responsáveis vinculados: só veem o que a pessoa acompanhada permitir, e ela pode mudar ou encerrar isso quando quiser, sem pedir a ninguém. Frases salvas e atividades marcadas como ocultas nunca são mostradas a responsáveis.",
          "Google (Firebase): guarda os dados da conta e cuida do login, como operador, seguindo os termos de tratamento de dados do Google Cloud. O banco de dados fica no Brasil (região southamerica-east1, São Paulo). O login pode ser processado pelo Google fora do Brasil, com as garantias previstas na LGPD (art. 33) e nos termos do Google.",
          "Não compartilhamos dados com mais ninguém, exceto se a lei ou uma ordem judicial exigir.",
        ],
      },
      {
        title: "8. Por quanto tempo",
        paragraphs: [
          "Enquanto a conta existir. Ao apagar a conta, os dados são apagados na hora do banco de dados; cópias de segurança do Google são eliminadas nos prazos do Google Cloud. Os códigos de convite valem por 24 horas.",
        ],
      },
      {
        title: "9. Seus direitos e como usá-los",
        paragraphs: [
          "Você pode confirmar que tratamos seus dados, acessar, corrigir, levar a outro serviço (portabilidade), pedir a eliminação, saber com quem compartilhamos, retirar o consentimento e pedir revisão (LGPD, art. 18).",
          'No app: Configurações > Privacidade tem "Baixar uma cópia dos meus dados" e "Apagar minha conta". O nome e a foto mudam em Perfil. Por e-mail: ' +
            CONTACT_EMAIL +
            ". Você também pode reclamar à Autoridade Nacional de Proteção de Dados (ANPD).",
        ],
      },
      {
        title: "10. Segurança",
        paragraphs: [
          "Os dados viajam criptografados, e regras de acesso no servidor garantem que cada pessoa só vê o que é dela ou o que foi autorizado. O app não guarda senhas e não registra conteúdo pessoal nos logs. Se acontecer um incidente que possa trazer risco, avisaremos as pessoas afetadas e a ANPD.",
        ],
      },
      {
        title: "11. O ORI não é um dispositivo médico",
        paragraphs: [
          "O ORI é uma ferramenta de apoio à organização e à comunicação. Ele não diagnostica, não trata e não substitui o acompanhamento de profissionais de saúde, terapeutas ou fonoaudiólogos.",
        ],
      },
      {
        title: "12. Mudanças nesta política",
        paragraphs: [
          "Quando algo importante mudar, o app avisa e pede o seu consentimento de novo antes de continuar. A versão e a data estão no topo desta página.",
        ],
      },
    ],
  },
  terms: {
    title: "Termos de uso do ORI",
    intro:
      "Ao usar o ORI, você concorda com estes termos. Se for menor de 18 anos, um responsável legal precisa concordar junto com você.",
    sections: [
      {
        title: "1. O que é o ORI",
        paragraphs: [
          `Um app gratuito, feito como TCC da Etec da Zona Leste pela equipe ORI (${team}), para organizar a rotina e se comunicar com símbolos.`,
        ],
      },
      {
        title: "2. Sua conta",
        paragraphs: [
          "Você é responsável por manter o acesso à sua conta seguro. Pode usar o app sem conta, com os dados só no aparelho.",
          "Responsáveis só acessam a rotina ou a comunicação de alguém com um vínculo que essa pessoa aceitou, e só o que ela permitir.",
        ],
      },
      {
        title: "3. Uso adequado",
        paragraphs: [
          "Não use o ORI para guardar conteúdo ilegal, ofensivo ou de outras pessoas sem autorização, nem para tentar acessar dados de terceiros ou atrapalhar o serviço.",
        ],
      },
      {
        title: "4. Saúde",
        paragraphs: [
          "O ORI não é um dispositivo médico e não substitui profissionais de saúde, terapia ou fonoaudiologia. Não use o app como único meio de comunicação em emergências.",
        ],
      },
      {
        title: "5. Conteúdo e licenças",
        paragraphs: [
          "O que você cria é seu. Os emoji são desenhados pela fonte do seu aparelho; as fontes, ícones e bibliotecas usadas estão listados em Configurações > Sobre o ori, com as suas licenças.",
        ],
      },
      {
        title: "6. Disponibilidade",
        paragraphs: [
          "Por ser um projeto acadêmico, o serviço pode mudar ou ser encerrado. Se for encerrado, avisaremos com antecedência e você poderá baixar uma cópia dos seus dados.",
        ],
      },
      {
        title: "7. Encerrar o uso",
        paragraphs: [
          "Você pode apagar a sua conta a qualquer momento, no app ou pelo pedido na página de exclusão da conta.",
        ],
      },
      {
        title: "8. Lei e contato",
        paragraphs: [
          `Estes termos seguem a lei brasileira. Dúvidas: ${CONTACT_EMAIL}.`,
        ],
      },
    ],
  },
  deletion: {
    title: "Como apagar a sua conta do ORI",
    intro: "Você pode apagar a conta e todos os dados dela a qualquer momento.",
    sections: [
      {
        title: "Pelo app",
        paragraphs: [
          'Abra o ORI, vá em Perfil > Configurações > Privacidade e toque em "Apagar minha conta". Antes, se quiser, toque em "Baixar uma cópia dos meus dados". Por segurança, o app pode pedir que você entre de novo.',
        ],
      },
      {
        title: "Sem o app",
        paragraphs: [
          `Envie um e-mail para ${CONTACT_EMAIL} com o assunto "Apagar conta", a partir do e-mail da conta. Confirmamos e apagamos em até 15 dias.`,
        ],
      },
      {
        title: "O que é apagado",
        paragraphs: [
          "Tudo: o perfil e a foto, a rotina, as atividades que se repetem, as pranchas, os símbolos, as frases salvas, as preferências da conta, os vínculos, o registro de consentimento e o login. Nada é mantido, exceto cópias de segurança do Google, eliminadas nos prazos do Google Cloud.",
        ],
      },
    ],
  },
};

const en: Texts = {
  updated: `Version ${LEGAL_DOCUMENTS_VERSION}, updated on 2026-11-09`,
  otherLanguages: "Other languages",
  privacy: {
    title: "ORI privacy policy",
    intro:
      "ORI helps organize the routine and communicate with symbols (augmentative and alternative communication). This policy explains, in plain words, which data the app uses, why, with whom, and how you control it.",
    sections: [
      {
        title: "1. Who takes care of your data",
        paragraphs: [
          `ORI is a final course project (TCC) of Etec da Zona Leste, Brazil, made by the ORI team: ${team}. The team is the data controller under Brazil's General Data Protection Law (LGPD, Law 13,709/2018).`,
          `Data protection officer and contact: ${CONTACT_EMAIL}. We answer requests about personal data within 15 days.`,
        ],
      },
      {
        title: "2. Using it without an account",
        paragraphs: [
          'With "Continue without an account", everything stays only on your device: the routine, the boards, the phrases and the preferences. Nothing is sent to the team or to the cloud. You can erase it all in Settings > Privacy.',
        ],
      },
      {
        title: "3. Which data the account uses",
        paragraphs: [
          "Account: name, email and password (the password is kept protected by Google Firebase Authentication; the team never sees it). If you sign in with Google, we receive the name and email of the Google account.",
          "Profile: the name shown in the app and, if you want, a photo.",
          "Routine: the activities (title, day, time, details, whether and by whom it was done) and the repeating ones.",
          "Communication: the boards and symbols you create (words, phrases, emoji, photos, color and kind of word) and the phrases you save, in the order you choose.",
          "Account preferences: text size, theme, simplified interface and animations.",
          "Links: the names of the caregiver and of the person supported, and the permissions the supported person gave.",
          "Consent: the version of these documents you accepted, the date, whether you are 18 or older and, if not, the confirmation of the legal guardian.",
          "Stay only on the device, even with an account: the chosen voice and the volume it plays at, the size of the symbols, the order of the ready-made phrases, the spacing, the count of how often you used communication, photos before they go to the account and, if you use ORI Home as the home screen, the list of apps on the device and your choices (favorites, hidden apps, where each icon is, whether the home screen is locked, the grid, whether the dock shows, icon size and shape, the stimulation level, how all the apps show, whether there is a home screen, background and the widgets you add; of each widget ORI keeps only the number Android gives it and its size, and its content is shown by the widget's own app). If you choose photos for the background, reduced copies of the latest ones (up to 8) are kept only inside ORI on this device, to choose again, and are deleted when you remove them from the history. If you choose to also use a background on the lock screen, ORI hands that picture to Android, which keeps it as the lock screen wallpaper (with no cloud backup), and keeps on this device which background it was and the number Android gave that picture, only to show the preview of the lock screen (a photo used there is kept while the lock screen shows it). ORI does not read the picture of Android's wallpaper: the previews show it through ORI's own window. ORI Home also keeps only on the device the folders (the name and apps of each), the app shortcuts put on the home screen (only the identifier the app gives each one) and the choices of badges, recent apps in the dock and gestures. The icon badges come on, as on One UI, but they only work if you allow Android's “Notification access” (you can turn them off in the options). With that access, ORI Home counts how many notifications each app has, to show the number on its icon: it reads only which app each notification belongs to and how many there are, never the title or the text, and it neither keeps nor sends any of it; you can take the access back at any time, in Android's settings. The recent apps of the dock are the last ones opened from ORI Home itself and stay only in memory, until it closes. The list of apps, the count of notifications and the background photo never leave the device.",
          "ORI shows no ads, does no usage analytics, does not sell data and does not ask for location, contacts, microphone or camera. Photos are chosen by you in the Android picker.",
        ],
      },
      {
        title: "4. Sensitive data",
        paragraphs: [
          "The routine and communication may reveal health or disability information. So they are only stored in the account with your specific and highlighted consent (LGPD, art. 11, I), asked on first use. You can withdraw it at any time; that deletes the account data.",
        ],
      },
      {
        title: "5. What we use the data for",
        paragraphs: [
          "Only for the app to work for you: showing and keeping your routine and communication, syncing your devices, letting the people you authorized follow and help, and keeping the account secure.",
          "Legal bases: your consent (art. 7, I and art. 11, I), providing the service you asked for (art. 7, V) and, for security records and the consent record itself, legal obligation and the exercise of rights (art. 7, II and VI).",
        ],
      },
      {
        title: "6. Children and teenagers",
        paragraphs: [
          "People under 18 only use an account with the consent of a legal guardian, given on first use, always in the best interest of the child or teenager (LGPD, art. 14). We collect only what the app needs and never use the data for advertising.",
        ],
      },
      {
        title: "7. Who else sees the data",
        paragraphs: [
          "Linked caregivers: they only see what the supported person allows, and the person can change or end it at any time without asking anyone. Saved phrases and activities marked as hidden are never shown to caregivers.",
          "Google (Firebase): stores the account data and handles sign-in as a processor, under the Google Cloud data processing terms. The database is in Brazil (southamerica-east1, São Paulo). Sign-in may be processed by Google outside Brazil, with the safeguards of the LGPD (art. 33) and Google's terms.",
          "We share data with no one else, unless the law or a court order requires it.",
        ],
      },
      {
        title: "8. For how long",
        paragraphs: [
          "While the account exists. When you delete the account, the data is erased from the database at once; Google's backups are removed within Google Cloud's periods. Invitation codes are valid for 24 hours.",
        ],
      },
      {
        title: "9. Your rights and how to use them",
        paragraphs: [
          "You may confirm that we process your data, access it, correct it, take it to another service (portability), ask for its deletion, learn with whom we share it, withdraw consent and ask for review (LGPD, art. 18).",
          'In the app: Settings > Privacy has "Download a copy of my data" and "Delete my account". The name and photo change in Profile. By email: ' +
            CONTACT_EMAIL +
            ". You may also complain to Brazil's National Data Protection Authority (ANPD).",
        ],
      },
      {
        title: "10. Security",
        paragraphs: [
          "Data travels encrypted, and access rules on the server make sure each person only sees what is theirs or what was authorized. The app keeps no passwords and logs no personal content. If an incident may bring risk, we will notify the people affected and the ANPD.",
        ],
      },
      {
        title: "11. ORI is not a medical device",
        paragraphs: [
          "ORI is a tool to support organization and communication. It does not diagnose, treat or replace health professionals, therapists or speech therapists.",
        ],
      },
      {
        title: "12. Changes to this policy",
        paragraphs: [
          "When something important changes, the app tells you and asks for your consent again before going on. The version and date are at the top of this page.",
        ],
      },
    ],
  },
  terms: {
    title: "ORI terms of use",
    intro:
      "By using ORI you agree to these terms. If you are under 18, a legal guardian must agree with you.",
    sections: [
      {
        title: "1. What ORI is",
        paragraphs: [
          `A free app, made as a final course project of Etec da Zona Leste by the ORI team (${team}), to organize the routine and communicate with symbols.`,
        ],
      },
      {
        title: "2. Your account",
        paragraphs: [
          "You are responsible for keeping access to your account safe. You may use the app without an account, with the data only on the device.",
          "Caregivers only access someone's routine or communication through a link that person accepted, and only what the person allows.",
        ],
      },
      {
        title: "3. Proper use",
        paragraphs: [
          "Do not use ORI to store illegal or offensive content, or other people's content without permission, nor to try to access others' data or disrupt the service.",
        ],
      },
      {
        title: "4. Health",
        paragraphs: [
          "ORI is not a medical device and does not replace health professionals, therapy or speech therapy. Do not use the app as the only means of communication in emergencies.",
        ],
      },
      {
        title: "5. Content and licenses",
        paragraphs: [
          "What you create is yours. Emoji are drawn by your device's font; the fonts, icons and libraries used are listed in Settings > About ori, with their licenses.",
        ],
      },
      {
        title: "6. Availability",
        paragraphs: [
          "As an academic project, the service may change or end. If it ends, we will tell you in advance and you will be able to download a copy of your data.",
        ],
      },
      {
        title: "7. Stopping",
        paragraphs: [
          "You can delete your account at any time, in the app or through the request on the account deletion page.",
        ],
      },
      {
        title: "8. Law and contact",
        paragraphs: [
          `These terms follow Brazilian law. Questions: ${CONTACT_EMAIL}.`,
        ],
      },
    ],
  },
  deletion: {
    title: "How to delete your ORI account",
    intro: "You can delete the account and all its data at any time.",
    sections: [
      {
        title: "In the app",
        paragraphs: [
          'Open ORI, go to Profile > Settings > Privacy and tap "Delete my account". Before that, if you want, tap "Download a copy of my data". For security, the app may ask you to sign in again.',
        ],
      },
      {
        title: "Without the app",
        paragraphs: [
          `Send an email to ${CONTACT_EMAIL} with the subject "Delete account", from the account's email. We confirm and delete within 15 days.`,
        ],
      },
      {
        title: "What is deleted",
        paragraphs: [
          "Everything: the profile and photo, the routine, the repeating activities, the boards, the symbols, the saved phrases, the account preferences, the links, the consent record and the sign-in. Nothing is kept, except Google's backups, removed within Google Cloud's periods.",
        ],
      },
    ],
  },
};

const es: Texts = {
  updated: `Versión ${LEGAL_DOCUMENTS_VERSION}, actualizada el 09/11/2026`,
  otherLanguages: "Otros idiomas",
  privacy: {
    title: "Política de privacidad de ORI",
    intro:
      "ORI ayuda a organizar la rutina y a comunicarse con símbolos (comunicación aumentativa y alternativa). Esta política explica, con palabras simples, qué datos usa la app, para qué, con quién y cómo controlas todo.",
    sections: [
      {
        title: "1. Quién cuida tus datos",
        paragraphs: [
          `ORI es un Trabajo de Conclusión de Curso (TCC) de la Etec da Zona Leste, Brasil, hecho por el equipo ORI: ${team}. El equipo es el responsable del tratamiento según la Ley General de Protección de Datos de Brasil (LGPD, Ley 13.709/2018).`,
          `Encargado de datos y contacto: ${CONTACT_EMAIL}. Respondemos solicitudes sobre datos personales en hasta 15 días.`,
        ],
      },
      {
        title: "2. Usar sin cuenta",
        paragraphs: [
          'Con "Continuar sin cuenta", todo queda solo en tu dispositivo: la rutina, los tableros, las frases y las preferencias. No se envía nada al equipo ni a la nube. Puedes borrarlo todo en Ajustes > Privacidad.',
        ],
      },
      {
        title: "3. Qué datos usa la cuenta",
        paragraphs: [
          "Cuenta: nombre, correo y contraseña (la contraseña la guarda protegida Google Firebase Authentication; el equipo nunca la ve). Si entras con Google, recibimos el nombre y el correo de la cuenta de Google.",
          "Perfil: el nombre que aparece en la app y, si quieres, una foto.",
          "Rutina: las actividades (título, día, hora, detalles, si se hizo y quién) y las que se repiten.",
          "Comunicación: los tableros y símbolos que creas (palabras, frases, emoji, fotos, color y tipo de palabra) y las frases que guardas, en el orden que eliges.",
          "Preferencias de la cuenta: tamaño del texto, tema, interfaz simplificada y animaciones.",
          "Vínculos: los nombres de quien acompaña y de quien es acompañado, y los permisos que dio la persona acompañada.",
          "Consentimiento: la versión de estos documentos que aceptaste, la fecha, si tienes 18 años o más y, si no, la confirmación del responsable legal.",
          "Quedan solo en el dispositivo, incluso con cuenta: la voz elegida y el volumen en que suena, el tamaño de los símbolos, el orden de las frases listas, el espaciado, el conteo de cuántas veces usaste la comunicación, las fotos antes de ir a la cuenta y, si usas ORI Home como pantalla de inicio, la lista de apps del dispositivo y tus elecciones (favoritas, apps ocultas, la posición de cada ícono, si la pantalla de inicio está bloqueada, la cuadrícula, si se muestra el dock, tamaño y forma de los íconos, el nivel de estímulo, cómo se muestran todas las apps, si hay pantalla de inicio, fondo de pantalla y los widgets que añadas; de cada widget ORI guarda solo el número que Android le da y su tamaño, y su contenido lo muestra la propia app del widget). Si eliges fotos para el fondo, copias reducidas de las últimas (hasta 8) se guardan solo dentro de ORI en este dispositivo, para elegirlas de nuevo, y se borran cuando las quitas del historial. Si eliges usar un fondo también en la pantalla de bloqueo, ORI le entrega esa imagen a Android, que la guarda como fondo de la pantalla de bloqueo (sin copia de seguridad en la nube), y guarda en este dispositivo qué fondo fue y el número que Android le dio a esa imagen, solo para mostrar la vista previa de la pantalla de bloqueo (una foto usada allí se guarda mientras la pantalla de bloqueo la muestra). ORI no lee la imagen del fondo de pantalla de Android: las vistas previas lo muestran a través de la propia ventana de ORI. En ORI Home también quedan solo en el dispositivo las carpetas (el nombre y las apps de cada una), los accesos directos de apps puestos en la pantalla de inicio (solo el identificador que la app da a cada uno) y las elecciones de insignias, apps recientes en la barra y gestos. Las insignias de los iconos vienen activadas, como en One UI, pero solo funcionan si permites el “Acceso a notificaciones” de Android (puedes desactivarlas en las opciones). Con ese acceso, ORI Home cuenta cuántas notificaciones tiene cada app, para mostrar el número en su icono: lee solo de qué app es cada notificación y cuántas hay, nunca el título ni el texto, y no guarda ni envía nada de eso; puedes quitar el acceso cuando quieras, en los ajustes de Android. Las apps recientes de la barra son las últimas abiertas desde el propio ORI Home y quedan solo en la memoria, hasta que se cierra. La lista de apps, el conteo de notificaciones y la foto del fondo nunca salen del dispositivo.",
          "ORI no muestra publicidad, no hace análisis de uso (analytics), no vende datos y no pide ubicación, contactos, micrófono ni cámara. Las fotos las eliges tú en el selector de Android.",
        ],
      },
      {
        title: "4. Datos sensibles",
        paragraphs: [
          "La rutina y la comunicación pueden revelar información sobre salud o discapacidad. Por eso solo se guardan en la cuenta con tu consentimiento específico y destacado (LGPD, art. 11, I), pedido en el primer uso. Puedes retirarlo cuando quieras; eso borra los datos de la cuenta.",
        ],
      },
      {
        title: "5. Para qué usamos los datos",
        paragraphs: [
          "Solo para que la app funcione para ti: mostrar y guardar tu rutina y tu comunicación, sincronizar tus dispositivos, permitir que quien autorizaste acompañe y ayude, y mantener la cuenta segura.",
          "Bases legales: tu consentimiento (art. 7, I y art. 11, I), la prestación del servicio que pediste (art. 7, V) y, para registros de seguridad y del propio consentimiento, el cumplimiento de obligación legal y el ejercicio de derechos (art. 7, II y VI).",
        ],
      },
      {
        title: "6. Niños y adolescentes",
        paragraphs: [
          "Los menores de 18 años solo usan la cuenta con el consentimiento de un responsable legal, dado en el primer uso, siempre en el mejor interés del niño o adolescente (LGPD, art. 14). Recogemos solo lo necesario y nunca usamos los datos para publicidad.",
        ],
      },
      {
        title: "7. Quién más ve los datos",
        paragraphs: [
          "Responsables vinculados: solo ven lo que la persona acompañada permite, y ella puede cambiarlo o terminarlo cuando quiera, sin pedir a nadie. Las frases guardadas y las actividades ocultas nunca se muestran a los responsables.",
          "Google (Firebase): guarda los datos de la cuenta y gestiona el inicio de sesión como encargado, según los términos de tratamiento de datos de Google Cloud. La base de datos está en Brasil (southamerica-east1, São Paulo). El inicio de sesión puede procesarse fuera de Brasil, con las garantías de la LGPD (art. 33) y de los términos de Google.",
          "No compartimos datos con nadie más, salvo que la ley o una orden judicial lo exija.",
        ],
      },
      {
        title: "8. Por cuánto tiempo",
        paragraphs: [
          "Mientras exista la cuenta. Al borrar la cuenta, los datos se borran de inmediato de la base de datos; las copias de seguridad de Google se eliminan en los plazos de Google Cloud. Los códigos de invitación valen 24 horas.",
        ],
      },
      {
        title: "9. Tus derechos y cómo ejercerlos",
        paragraphs: [
          "Puedes confirmar que tratamos tus datos, acceder, corregir, llevarlos a otro servicio (portabilidad), pedir su eliminación, saber con quién los compartimos, retirar el consentimiento y pedir revisión (LGPD, art. 18).",
          'En la app: Ajustes > Privacidad tiene "Descargar una copia de mis datos" y "Borrar mi cuenta". El nombre y la foto cambian en Perfil. Por correo: ' +
            CONTACT_EMAIL +
            ". También puedes reclamar ante la Autoridad Nacional de Protección de Datos de Brasil (ANPD).",
        ],
      },
      {
        title: "10. Seguridad",
        paragraphs: [
          "Los datos viajan cifrados, y reglas de acceso en el servidor garantizan que cada persona solo ve lo suyo o lo autorizado. La app no guarda contraseñas ni registra contenido personal en los logs. Si ocurre un incidente que pueda traer riesgo, avisaremos a las personas afectadas y a la ANPD.",
        ],
      },
      {
        title: "11. ORI no es un dispositivo médico",
        paragraphs: [
          "ORI es una herramienta de apoyo a la organización y la comunicación. No diagnostica, no trata y no sustituye a profesionales de la salud, terapeutas ni fonoaudiólogos.",
        ],
      },
      {
        title: "12. Cambios en esta política",
        paragraphs: [
          "Cuando algo importante cambie, la app te avisará y pedirá tu consentimiento de nuevo antes de continuar. La versión y la fecha están al principio de esta página.",
        ],
      },
    ],
  },
  terms: {
    title: "Términos de uso de ORI",
    intro:
      "Al usar ORI, aceptas estos términos. Si eres menor de 18 años, un responsable legal debe aceptarlos contigo.",
    sections: [
      {
        title: "1. Qué es ORI",
        paragraphs: [
          `Una app gratuita, hecha como TCC de la Etec da Zona Leste por el equipo ORI (${team}), para organizar la rutina y comunicarse con símbolos.`,
        ],
      },
      {
        title: "2. Tu cuenta",
        paragraphs: [
          "Eres responsable de mantener seguro el acceso a tu cuenta. Puedes usar la app sin cuenta, con los datos solo en el dispositivo.",
          "Los responsables solo acceden a la rutina o la comunicación de alguien mediante un vínculo que esa persona aceptó, y solo a lo que ella permita.",
        ],
      },
      {
        title: "3. Uso adecuado",
        paragraphs: [
          "No uses ORI para guardar contenido ilegal u ofensivo, o de otras personas sin permiso, ni para intentar acceder a datos ajenos o perjudicar el servicio.",
        ],
      },
      {
        title: "4. Salud",
        paragraphs: [
          "ORI no es un dispositivo médico y no sustituye a profesionales de la salud, terapia ni fonoaudiología. No uses la app como único medio de comunicación en emergencias.",
        ],
      },
      {
        title: "5. Contenido y licencias",
        paragraphs: [
          "Lo que creas es tuyo. Los emoji los dibuja la fuente de tu dispositivo; las fuentes, íconos y bibliotecas usadas están en Ajustes > Acerca de ori, con sus licencias.",
        ],
      },
      {
        title: "6. Disponibilidad",
        paragraphs: [
          "Al ser un proyecto académico, el servicio puede cambiar o terminar. Si termina, avisaremos con antelación y podrás descargar una copia de tus datos.",
        ],
      },
      {
        title: "7. Dejar de usar",
        paragraphs: [
          "Puedes borrar tu cuenta cuando quieras, en la app o con la solicitud de la página de eliminación de cuenta.",
        ],
      },
      {
        title: "8. Ley y contacto",
        paragraphs: [
          `Estos términos siguen la ley brasileña. Dudas: ${CONTACT_EMAIL}.`,
        ],
      },
    ],
  },
  deletion: {
    title: "Cómo borrar tu cuenta de ORI",
    intro: "Puedes borrar la cuenta y todos sus datos cuando quieras.",
    sections: [
      {
        title: "En la app",
        paragraphs: [
          'Abre ORI, ve a Perfil > Ajustes > Privacidad y toca "Borrar mi cuenta". Antes, si quieres, toca "Descargar una copia de mis datos". Por seguridad, la app puede pedir que inicies sesión de nuevo.',
        ],
      },
      {
        title: "Sin la app",
        paragraphs: [
          `Envía un correo a ${CONTACT_EMAIL} con el asunto "Borrar cuenta", desde el correo de la cuenta. Confirmamos y borramos en hasta 15 días.`,
        ],
      },
      {
        title: "Qué se borra",
        paragraphs: [
          "Todo: el perfil y la foto, la rutina, las actividades que se repiten, los tableros, los símbolos, las frases guardadas, las preferencias de la cuenta, los vínculos, el registro del consentimiento y el inicio de sesión. No se conserva nada, salvo las copias de seguridad de Google, eliminadas en los plazos de Google Cloud.",
        ],
      },
    ],
  },
};

export const legalTexts: Record<Language, Texts> = { pt, en, es };

export function isLanguage(value: string): value is Language {
  return (LANGUAGES as readonly string[]).includes(value);
}
