# ori

<p align="center">
  <img src="https://github.com/user-attachments/assets/601fab7e-89b4-49e8-95cf-670578e08200" alt="Logo do ORI" width="180">
</p>

<p align="center">
  Aplicativo de suporte assistivo e acessibilidade para pessoas neurodivergentes.
</p>

<p align="center"> <img src="https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin"> <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android"> <img src="https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose"> <img src="https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase"> <img src="https://img.shields.io/badge/Firestore-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Cloud Firestore"> <img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white" alt="Git"> </p>

<p align="center">
  <img
    src="https://github.com/user-attachments/assets/217a7725-cedc-4123-8d57-333c985d01bc"
    alt="Tela inicial do ORI"
    width="300"
  >
</p>

---

## Sobre o Projeto

O **ori** é um aplicativo mobile desenvolvido para oferecer suporte assistivo a pessoas neurodivergentes, com foco em **autonomia, acessibilidade e personalização da experiência do usuário**.

A proposta do projeto é centralizar recursos que auxiliem em atividades do cotidiano, reduzindo barreiras relacionadas à organização, comunicação e interação com o aplicativo.

O MVP é estruturado em três pilares principais:

* **Rotina:** organização e acompanhamento de atividades do dia a dia.
* **Comunicação:** recursos de apoio à comunicação e expressão das necessidades do usuário.
* **Personalização:** adaptação da interface e dos recursos de acordo com as necessidades e preferências individuais.

O projeto também considera diferentes níveis de autonomia, permitindo que determinados recursos e vínculos de acompanhamento sejam configurados conforme o perfil e as permissões do usuário.

---

> **Status:** Em desenvolvimento (MVP).

---

## Objetivos

O ori tem como principais objetivos:

* oferecer uma experiência acessível e simples de utilizar;
* apoiar a organização da rotina diária;
* fornecer recursos de comunicação assistiva;
* permitir personalização de interface e experiência;
* preservar a autonomia do usuário;
* estabelecer uma estrutura segura para gerenciamento de usuários e vínculos de acompanhamento.

O projeto prioriza uma abordagem **autonomy-first**, evitando que mecanismos de suporte substituam desnecessariamente a tomada de decisão do próprio usuário.

---

## Stack Principal

### Mobile

[Kotlin](https://kotlinlang.org/) · [Android](https://developer.android.com/) · [Jetpack Compose](https://developer.android.com/jetpack/compose)

### Backend & Infrastructure

[Firebase](https://firebase.google.com/) · [Firebase Authentication](https://firebase.google.com/docs/auth) · [Cloud Firestore](https://firebase.google.com/docs/firestore)

### Development Tools

[Android Studio](https://developer.android.com/studio) · [Git](https://git-scm.com/) · [GitHub](https://github.com/)

---

## Arquitetura

O aplicativo é desenvolvido utilizando **Kotlin** e **Jetpack Compose**, seguindo uma estrutura voltada para separação de responsabilidades e manutenção do código.

A camada de apresentação organiza as telas a partir de **estados de interface e ViewModels**, mantendo a lógica de apresentação separada dos componentes visuais sempre que possível.

A autenticação dos usuários é realizada através do **Firebase Authentication**, utilizando o identificador `firebaseUid` como referência externa da conta.

A aplicação evita manter credenciais sensíveis diretamente no banco de dados, delegando o gerenciamento de autenticação ao Firebase.

A estrutura de permissões e vínculos entre usuários é tratada separadamente da autenticação, permitindo diferenciar:

* identidade do usuário;
* dados do perfil;
* vínculos de acompanhamento;
* permissões de acesso aos recursos.

---

## Funcionalidades

### Implementado

* Autenticação de usuários;
* Cadastro e acesso à conta;
* Interface adaptada para acessibilidade;
* Personalização da experiência do usuário;
* Gerenciamento de perfil.

### Em desenvolvimento

* Organização e acompanhamento da rotina;
* Recursos de apoio à comunicação;
* Gerenciamento de vínculos entre usuários;
* Configurações avançadas de acessibilidade;
* Refinamento do sistema de permissões;
* Melhorias de experiência e usabilidade.

---

## Acessibilidade

A acessibilidade é um dos princípios centrais do ORI e influencia diretamente as decisões de interface e experiência do aplicativo.

Entre os aspectos considerados no desenvolvimento estão:

* hierarquia visual clara;
* navegação simplificada;
* personalização da interface;
* redução de elementos desnecessários;
* adaptação às diferentes necessidades dos usuários;
* maior previsibilidade das interações.

A interface é desenvolvida com foco em **clareza, consistência e baixo atrito cognitivo**, evitando excesso de informações ou interações desnecessárias.

---

## Firebase

O ORI utiliza serviços do Firebase para componentes de infraestrutura, autenticação e armazenamento de dados.

### Authentication

Responsável pelo gerenciamento das credenciais e autenticação dos usuários.

O aplicativo utiliza o identificador fornecido pelo Firebase para relacionar a conta autenticada aos dados correspondentes dentro da aplicação.

### Cloud Firestore

Responsável pelo armazenamento dos dados relacionados aos usuários e às funcionalidades da aplicação.

Os dados da aplicação são mantidos separados das credenciais de autenticação, permitindo uma arquitetura mais organizada e evitando a implementação de mecanismos próprios para armazenamento e gerenciamento de senhas.

---

## Como rodar

### Pré-requisitos

* Android Studio instalado;
* Android SDK configurado;
* JDK compatível com o projeto;
* Git instalado;
* dispositivo Android com depuração USB habilitada ou emulador configurado;
* projeto Firebase configurado para o ambiente de desenvolvimento.

### Clone o repositório

```bash
git clone <url-do-repositorio>
cd ori
```

### Abra o projeto

Abra a pasta do projeto no **Android Studio** e aguarde a sincronização do Gradle.

### Execute

Conecte um dispositivo Android ou inicie um emulador e execute o projeto pelo Android Studio.

Também é possível gerar o APK pelo terminal:

```bash
./gradlew assembleDebug
```

No Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

O APK de debug será gerado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

---

## Estrutura do Projeto

Uma visão simplificada da organização:

```text
ori/
├── app/
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/
│           │       └── oriteam/
│           ├── res/
│           └── AndroidManifest.xml
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew
```

A organização interna é dividida por responsabilidades, mantendo componentes de interface, navegação, dados e lógica de negócio desacoplados sempre que possível.

---

## Desenvolvimento

O projeto utiliza **Git** e **GitHub** para controle de versão e desenvolvimento colaborativo.

Cada integrante trabalha preferencialmente em branches específicas para suas funcionalidades, reduzindo conflitos e permitindo que alterações sejam integradas de forma controlada.

Exemplo:

```bash
git checkout -b feature/login
```

Após a implementação:

```bash
git add .
git commit -m "feat: implementa tela de login"
git push origin feature/login
```

A integração com a branch principal deve ser realizada após a revisão das alterações.

---

## Equipe

### Desenvolvimento

* **Mariana dos Santos Moreira** - [GitHub](https://github.com/hanjimeu)
* **Matheus Mendonca de Lima** - [GitHub](https://github.com/MatheusMendoncaLima)
* **Nicollas Lopes Costa** - [GitHub](https://github.com/nicollassss)

### Orientação

* **Jeferson Roberto de Lima**

---

## Contexto Acadêmico

O ORI é desenvolvido no contexto do curso de **Desenvolvimento de Sistemas da ETEC Zona Leste**, integrado ao modelo **AMS (Articulação da Formação Profissional Média e Superior)**.

O projeto também está relacionado à proposta de formação **P-Tech**, com parceria institucional envolvendo a **IBM**.

---

## Escopo do MVP

Para manter o projeto viável e consistente com o objetivo inicial, determinadas funcionalidades permanecem fora do escopo principal do MVP.

Entre elas:

* inteligência artificial para geração automática de rotinas;
* monitoramento avançado de comportamento;
* funcionalidades relacionadas a diagnósticos ou acompanhamento clínico;
* gamificação;
* geração de relatórios complexos;
* sistemas avançados de notificações.

Esses recursos podem ser avaliados posteriormente, mas não fazem parte do núcleo necessário para validar a proposta inicial do ORI.

---

## Princípios do Projeto

### Acessibilidade primeiro

A experiência deve ser compreensível e utilizável pelo maior número possível de pessoas.

### Autonomia

Os recursos de suporte devem complementar a capacidade do usuário, e não substituí-la sem necessidade.

### Simplicidade

O aplicativo deve resolver problemas concretos sem adicionar complexidade desnecessária.

### Privacidade e segurança

Dados de autenticação e informações do usuário devem ser tratados de forma responsável.

### Evolução incremental

O projeto é desenvolvido por etapas, priorizando funcionalidades essenciais antes de recursos secundários.

---

## Licença

Este projeto foi desenvolvido para fins **acadêmicos e educacionais**.

© 2026 ORI. Todos os direitos reservados.
