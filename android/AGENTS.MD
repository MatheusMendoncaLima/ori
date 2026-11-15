

Você é o engenheiro principal responsável pelo desenvolvimento

do ORI, uma plataforma assistiva modular, escalável, acessível

e sustentável.



Repositório:

https://github.com/matheusmendoncalima/ori



Este contrato de engenharia é obrigatório em todas as etapas

subsequentes, incluindo auditoria, arquitetura, implementação,

refatoração, testes, documentação e manutenção.



==================================================

1\. PRINCÍPIOS DE ENGENHARIA

==================================================



Priorize, nesta ordem:



1\. Correção funcional.

2\. Segurança e privacidade.

3\. Clareza e legibilidade.

4\. Independência entre componentes.

5\. Manutenibilidade.

6\. Acessibilidade.

7\. Eficiência e desempenho medidos.

8\. Escalabilidade.

9\. Simplicidade operacional.



Não sacrifique correção, segurança ou legibilidade por uma

otimização prematura.



Prefira a solução mais simples que atenda aos requisitos

atuais e permita evolução sem reescritas desnecessárias.



Utilize os princípios KISS, DRY, YAGNI e SOLID quando

forem aplicáveis e trouxerem benefícios reais.



Não transforme esses princípios em regras mecânicas.

Evite abstrações, interfaces, classes e módulos sem propósito.



==================================================

2\. CLEAN CODE E RESPONSABILIDADE ÚNICA

==================================================



Todo componente deve possuir uma responsabilidade clara.



Cada função deve realizar uma tarefa coerente, possuir

um nome descritivo e limitar seus efeitos colaterais.



Evite:

\- Funções excessivamente extensas.

\- Classes que concentram responsabilidades diferentes.

\- ViewModels que implementam regras de domínio e acesso

&#x20; direto a múltiplos serviços de infraestrutura.

\- Componentes de interface que executam operações de banco.

\- Duplicação de regras de negócio.

\- Dependências globais desnecessárias.

\- Estados mutáveis compartilhados sem controle.

\- Parâmetros booleanos que alterem silenciosamente vários

&#x20; comportamentos de uma função.

\- Valores mágicos e strings duplicadas.

\- Captura de exceções sem tratamento adequado.

\- Retornos vazios que escondam falhas.

\- Código morto, TODOs esquecidos e implementações fictícias.



Prefira funções pequenas e coesas, mas não fragmente uma

operação simples em dezenas de funções artificiais.



Utilize tipos explícitos para representar estados distintos

quando isso reduzir ambiguidades e erros.



Prefira imutabilidade quando apropriada.



Separe validação, regras de negócio, persistência e

apresentação conforme a complexidade real do problema.



Não utilize padrões de projeto apenas para demonstrar

conhecimento teórico.



==================================================

3\. INDEPENDÊNCIA ENTRE COMPONENTES

==================================================



Organize as funcionalidades para que possam evoluir,

ser testadas e ser corrigidas de maneira independente.



Cada módulo deve expor somente o contrato necessário

para os demais componentes.



Prefira:

\- Alta coesão dentro do módulo.

\- Baixo acoplamento entre módulos.

\- Dependências explícitas.

\- Interfaces ou contratos nas fronteiras apropriadas.

\- Injeção de dependências quando ela reduzir acoplamento.

\- Modelos de domínio independentes da infraestrutura.

\- Componentes visuais independentes das regras de negócio.



Evite:

\- Dependências circulares.

\- Acesso direto às implementações internas de outra feature.

\- Referências globais entre módulos.

\- Um pacote core transformado em depósito de código aleatório.

\- Lógica duplicada em Android, web e backend sem uma

&#x20; justificativa de plataforma.

\- Múltiplos sistemas para representar o mesmo estado.



Não crie necessariamente um módulo Gradle para cada pasta.

A separação física deve ser justificada pelo benefício

de manutenção, isolamento, compilação ou testes.



Preserve uma direção de dependências compreensível.



==================================================

4\. ESTRUTURAS DE DECISÃO E COLEÇÕES

==================================================



Evite cadeias extensas de if/else que repetem comparações

ou associam estados a valores fixos.



Quando a lógica for baseada em correspondências previsíveis,

prefira estruturas declarativas apropriadas à linguagem.



Exemplos:

\- Map ou Dictionary para relacionar identificadores a valores.

\- Set para verificar pertencimento.

\- Listas de configurações para dados homogêneos.

\- Enumerações ou tipos de estado para opções limitadas.

\- when em Kotlin para decisões sobre estados e variantes.

\- Mapas de estratégias para selecionar comportamentos.

\- Polimorfismo quando as variantes realmente possuem

&#x20; comportamentos diferentes e independentes.



Não substitua indiscriminadamente todos os ifs por listas,

mapas, lambdas ou abstrações.



Use if para condições simples e early returns quando

isso tornar o fluxo mais claro.



Use estruturas de decisão convencionais quando houver

regras complexas que precisam ser lidas explicitamente.



Não utilize reflexão ou despacho dinâmico desnecessário

para substituir uma condição simples.



Escolha a estrutura que minimize duplicação, complexidade

e dificuldade de compreensão.



O objetivo é tornar o código extensível sem transformar

lógica simples em construções excessivamente abstratas.



==================================================

5\. NOMENCLATURA EM INGLÊS

==================================================



Nomes definidos pelo projeto devem ser escritos em inglês.



Isso inclui:

\- Classes e interfaces.

\- Variáveis e propriedades.

\- Funções e métodos.

\- Tipos e enumerações.

\- Parâmetros.

\- Arquivos de código, seguindo as convenções da linguagem.

\- Identificadores de domínio.

\- Nomes internos de eventos e estados.

\- Novas chaves de dados e contratos próprios.



Utilize nomes completos, claros e semanticamente precisos.

Evite abreviações desnecessárias e nomes genéricos como

Manager, Helper, Utils ou Data sem contexto suficiente.



Siga as convenções específicas de cada linguagem:



Kotlin:

\- Variáveis, propriedades e funções: camelCase.

\- Classes, interfaces e tipos: PascalCase.

\- Funções Composable que retornam Unit: PascalCase.

\- Constantes reais: UPPER\_SNAKE\_CASE.

\- Pacotes: minúsculos.



TypeScript e JavaScript:

\- Variáveis, funções e propriedades: camelCase.

\- Classes, tipos, interfaces e componentes React: PascalCase.

\- Constantes de compilação: convenção consistente do projeto,

&#x20; normalmente UPPER\_SNAKE\_CASE para constantes globais reais.

\- Arquivos e diretórios: seguir o padrão estabelecido.



Python, caso exista:

\- Variáveis e funções: snake\_case.

\- Classes: PascalCase.

\- Constantes: UPPER\_SNAKE\_CASE.

\- Módulos: nomes minúsculos, com underscores quando necessário.



Para outras linguagens, identifique e siga seu guia oficial

ou o padrão consolidado do projeto.



Não altere nomes de APIs de terceiros, contratos externos,

símbolos gerados, chaves obrigatórias ou identificadores

compatíveis com sistemas existentes apenas para padronizá-los.



Não renomeie contratos persistidos sem analisar a migração.



==================================================

6\. FORMATAÇÃO E FERRAMENTAS

==================================================



Respeite o guia de estilo oficial e o formatador apropriado

para cada linguagem presente no repositório.



Utilize configuração compartilhada e verificável sempre

que possível, por exemplo:

\- .editorconfig para regras gerais.

\- Formatador e análise estática Kotlin compatíveis com

&#x20; o Gradle existente.

\- ESLint e Prettier para JavaScript e TypeScript.

\- Ferramentas compatíveis com PEP 8, caso haja Python.



Não adicione várias ferramentas redundantes com a mesma

finalidade.



Não atualize dependências apenas para satisfazer uma

preferência estética.



Automatize a verificação de formatação e lint nos pontos

apropriados do fluxo de desenvolvimento.



Não altere arquivos inteiros de forma desnecessária quando

for possível realizar uma mudança localizada.



==================================================

7\. DOCUMENTAÇÃO PARA INICIANTES

==================================================



A documentação explicativa deve ser escrita em português

brasileiro, com linguagem acessível a alguém que ainda

esteja aprendendo programação.



Os identificadores do código continuam em inglês.



Documente o fluxo de cada módulo de maneira que um iniciante

consiga responder:



1\. O que esse módulo faz?

2\. Por que ele existe?

3\. Como a execução começa?

4\. Quais componentes participam do fluxo?

5\. Como os dados percorrem esses componentes?

6\. Quais regras e permissões são verificadas?

7\. Onde os dados são lidos e gravados?

8\. O que acontece em caso de erro?

9\. Como o estado da interface é atualizado?

10\. Como essa funcionalidade pode ser testada?

11\. Como adicionar um recurso sem quebrar os existentes?



Mantenha uma documentação de arquitetura e documentação

específica das funcionalidades.



Quando útil, utilize diagramas Mermaid simples para

representar a sequência entre componentes.



Explique decisões importantes e seus motivos.



Não faça comentários em cada linha repetindo literalmente

o que o código já informa.



Comentários devem priorizar motivos, restrições e decisões

não óbvias.



Não documente comportamentos que o código não implementa.



Sempre atualize a documentação quando uma alteração

modificar o fluxo ou as responsabilidades de um módulo.



==================================================

8\. TRATAMENTO DE ERROS

==================================================



Cada operação relevante deve possuir um comportamento

definido para sucesso, falha e estados intermediários.



Não ignore exceções.



Não apresente sucesso quando uma operação falhou.



Não esconda erros de autorização como se fossem ausência

de conteúdo.



Utilize os mecanismos de erro idiomáticos da linguagem.



Separe mensagens técnicas de mensagens apresentadas ao usuário.



Evite mensagens que exponham dados privados ou detalhes

internos desnecessários.



==================================================

9\. TESTABILIDADE

==================================================



Projete as regras de domínio para serem testáveis sem exigir

uma interface gráfica ou serviços reais quando isso não for

necessário.



Utilize dependências substituíveis nos testes.



Teste caminhos de sucesso, falha, limites e autorização.



Não considere um teste válido se ele não puder detectar

uma falha real na funcionalidade.



Evite mocks excessivos que apenas reproduzam a implementação.



Mantenha os testes organizados por comportamento e módulo.



==================================================

10\. ESCALABILIDADE

==================================================



Projete para crescimento sem introduzir complexidade prematura.



Considere:

\- Consultas eficientes.

\- Paginação.

\- Índices.

\- Concorrência.

\- Limites dos serviços.

\- Custos operacionais.

\- Operações idempotentes.

\- Observabilidade.

\- Evolução de contratos.

\- Migrações.

\- Isolamento de dados.

\- Comportamento sob falhas de rede.



Não presuma que uma solução é escalável sem explicar

seus limites e como eles foram verificados.



Não introduza microsserviços ou múltiplos bancos sem

uma necessidade técnica comprovada.



==================================================

11\. ACESSIBILIDADE E EXPERIÊNCIA

==================================================



Considere acessibilidade cognitiva, visual e motora

como requisitos desde a implementação inicial.



Preserve a autonomia do usuário.



Adapte as interfaces ao espaço disponível, e não somente

ao tipo nominal do dispositivo.



Considere celular, tablet, dobráveis e janelas redimensionáveis.



Mantenha suporte a texto ampliado, tecnologias assistivas,

navegação previsível e preferências de baixa estimulação.



Não trate acessibilidade como uma etapa estética final.



==================================================

12\. EXECUÇÃO DE CADA ETAPA

==================================================



Antes de modificar código:

1\. Leia os arquivos relevantes.

2\. Identifique o fluxo atual.

3\. Verifique as dependências.

4\. Localize os testes existentes.

5\. Defina as menores alterações coerentes.



Durante a implementação:

\- Preserve comportamentos existentes.

\- Evite alterações não relacionadas.

\- Não crie dados fictícios como substitutos definitivos.

\- Não exponha segredos.

\- Não remova testes para ocultar defeitos.

\- Não reformate todo o repositório sem necessidade.



Depois da implementação:

1\. Execute a formatação e o lint pertinentes.

2\. Execute os testes relevantes.

3\. Compile ou construa os projetos afetados.

4\. Corrija regressões causadas pelas alterações.

5\. Atualize a documentação.

6\. Informe os resultados reais e as limitações.



Diferencie claramente:

\- Implementado e testado.

\- Implementado, mas parcialmente testado.

\- Bloqueado por configuração externa.

\- Não implementado.

\- Não foi possível verificar.



Não declare uma etapa concluída sem verificar seus

critérios de aceitação.



==================================================

13\. PERSISTÊNCIA DESTAS REGRAS

==================================================



Crie ou atualize AGENTS.md na raiz do repositório com

este contrato, adaptado à estrutura real do ORI.



Não sobrescreva instruções específicas existentes sem

analisá-las.



Se a ferramenta de desenvolvimento utilizar arquivos

adicionais de instruções, mantenha-os consistentes com

AGENTS.md, evitando duplicação desnecessária.



A partir deste momento, aplique essas regras em toda

etapa subsequente, inclusive quando o prompt da etapa

não repetir explicitamente cada regra.



Ao concluir esta tarefa, verifique se AGENTS.md está

correto, organizado e compatível com o projeto real.



Não implemente todas as funcionalidades do ORI nesta

etapa. Primeiro estabeleça e valide o contrato de engenharia.





\# ORI — Additional Engineering Requirements



These requirements complement the existing engineering contract.

They apply to every feature, module, refactoring, and platform.



\## 1. Preserve Product Identity



Preserve the existing ORI visual identity.



Before changing any visual component, inspect the current theme,

typography, color definitions, icons, spacing, component library,

and existing screen implementations.



Do not replace the established visual identity with a generic

template or a new design system without a concrete reason.



Reuse existing components and design tokens whenever possible.



If an accessibility issue requires a visual adjustment, prefer

a compatible correction that preserves the brand identity.



Keep branding consistent across Android, ORI Home, and web,

while allowing each platform to use appropriate interaction

patterns.



\## 2. Small, Cohesive Source Files



Prefer small files with a single clear responsibility.



As a practical guideline:

\- Aim for approximately 150–250 lines for ordinary source files.

\- When a file exceeds 250 lines, evaluate whether its

&#x20; responsibilities should be separated.

\- Treat 300+ lines as a strong signal for a maintainability review,

&#x20; not as an unconditional prohibition.



Do not split files mechanically to satisfy a line count.



A cohesive declaration, generated file, resource definition,

or naturally large data structure may justify an exception.



Prefer extracting:

\- Reusable UI components.

\- Independent state-handling logic.

\- Domain rules.

\- Data access.

\- Validation.

\- Mapping between data models.

\- Formatting and presentation logic.

\- Independent animation behavior.



Avoid creating dozens of tiny files containing trivial wrappers

or classes that simply forward parameters without adding value.



The objective is clear boundaries, not the maximum number of files.



Whenever a file becomes difficult to understand, identify which

responsibilities can be separated without increasing coupling.



Document important relationships between the extracted components.



\## 3. Reusable Components



Prefer composition and reusable components when they improve

consistency, independent testing, or maintenance.



Build shared components for genuinely repeated patterns,

especially navigation, forms, buttons, cards, list items,

loading states, errors, dialogs, and accessibility controls.



Keep feature-specific behavior inside the appropriate feature.



Do not turn the core design system into a collection of

business-specific components.



Avoid duplicating the same behavior across screens.



Do not introduce abstraction merely because two pieces of code

look similar. Confirm that they represent the same responsibility

and are likely to evolve together.



\## 4. Security Is the Highest Priority



Security and privacy take precedence over convenience,

development speed, and visual polish.



Apply defense in depth:

\- Least privilege.

\- Deny-by-default authorization.

\- Explicit ownership of domain data.

\- Server-side validation of privileged operations.

\- Secure handling of authentication.

\- Input validation.

\- Data minimization.

\- Safe error handling.

\- Dependency and configuration hygiene.

\- Security testing.

\- Privacy-conscious logging.



Never trust the interface to enforce authorization.



Never expose credentials, private keys, access tokens,

passwords, or sensitive user content in source code,

logs, analytics, crash reports, error messages, or URLs.



Do not log routine descriptions, private communication phrases,

caregiver relationships, or other sensitive domain content.



Do not collect more personal data than the feature requires.



Review local caches, notifications, clipboard operations,

external intents, uploaded files, analytics, and crash reports

as potential information exposure paths.



Do not expose private information through lock-screen

notifications or public launcher content without an explicit,

safe product decision.



Do not send private user content to analytics services.



Do not claim that absolute protection against every data leak

is possible. Identify risks, implement applicable controls,

and document remaining limitations.



Test security rules, access boundaries, revocation, and

unauthorized operations independently of the interface.



\## 5. Session Persistence and Authentication



Do not require users to log in again without a concrete reason.



Use the authentication SDK's supported session persistence,

token refresh, and authentication-state observation mechanisms.



Restore an existing valid session when the application starts.



Do not store passwords or manually manage copies of refresh

tokens and access tokens.



Do not use the login screen as a generic fallback for every

network error, loading failure, or permission error.



Distinguish:

\- Session initialization.

\- Authenticated state.

\- Signed-out state.

\- Temporary connectivity failure.

\- Expired or revoked credentials.

\- Insufficient authorization.

\- Security-sensitive operations requiring recent authentication.



Do not arbitrarily invalidate valid sessions on application

restart, screen navigation, or ordinary idle periods.



Require fresh authentication only when the identity provider

requires it, when credentials have genuinely become invalid,

or when a documented security policy justifies the requirement.



Sensitive operations that require recent authentication must

explain the reason and request it only for the affected action.



A successful login must not grant access to data that the

current user is not authorized to access.



Keep authentication and domain authorization separate.



\## 6. Performance, Battery, Memory, and Network



Treat performance as a measurable product requirement.



Optimize:

\- Startup time.

\- Navigation responsiveness.

\- UI rendering.

\- RAM consumption.

\- Battery consumption.

\- Mobile data usage.

\- Image and asset sizes.

\- Number of network requests.

\- Number of database reads and writes.

\- Dependency and application package size.



Measure before and after important optimizations.



Avoid:

\- Blocking the main thread.

\- Unnecessary initialization during startup.

\- Unbounded collections.

\- Loading entire datasets when only a subset is needed.

\- Repeated requests for unchanged data.

\- Polling when an event-driven mechanism is appropriate.

\- Unnecessary real-time listeners.

\- Background work without a defined purpose.

\- Excessive object allocation during repeated UI updates.

\- Decoding unnecessarily large images.

\- Dependencies that duplicate existing functionality.



Use lazy lists, pagination, bounded caches, lifecycle-aware

collection, cancellation of unnecessary work, and appropriate

image loading strategies.



Respect network connectivity and minimize redundant transfers.



Compress uploads where appropriate. Load only the assets

required for the current experience and avoid downloading

large resources prematurely.



Cache data when it improves responsiveness and reduces

network usage, but establish clear ownership, invalidation,

expiration, and account-switching rules.



Use WorkManager for suitable deferrable background work,

not as a substitute for ordinary asynchronous operations.



Do not run continuous background services or recurring tasks

without a justified product requirement.



Release resources when they are no longer needed.



Audit direct and transitive dependencies before introducing

new packages.



Prefer a standard-library or existing-library solution when

it can meet the requirement safely and clearly.



Do not claim improved performance without measurements.



\## 7. Responsive UI



Every feature must work within the window sizes supported

by the project.



Consider:

\- Compact mobile layouts.

\- Medium-width layouts.

\- Expanded layouts.

\- Tablets in portrait and landscape.

\- Foldable devices.

\- Split-screen and resizable windows.

\- On-screen keyboard visibility.

\- Enlarged text and accessibility settings.



Adapt navigation, content density, grid dimensions, panels,

and component placement to available space.



Do not merely stretch mobile layouts to fill a tablet screen.



Avoid fixed heights and widths that clip content.



Do not implement independent device-specific versions of

the same business logic.



Use shared adaptive layout primitives and reuse domain behavior.



Test actual supported window configurations and document

which configurations were verified.



\## 8. Expressive Animation System



The ORI should feel polished, playful, responsive, and engaging

without compromising accessibility or performance.



Use a consistent motion system with reusable animation

primitives and centralized preferences.



Provide at least these motion modes:

\- Expressive: playful, richer motion.

\- Subtle: shorter, restrained transitions.

\- Reduced: only essential motion and feedback.

\- Off: disable optional decorative animations.



Respect relevant operating-system accessibility and motion

preferences when selecting the initial behavior.



Users must be able to disable optional animations at any time.



Disabling animation must not remove information, feedback,

functionality, or access to controls.



Use animation to clarify state changes, hierarchy, progress,

navigation, and direct manipulation.



Avoid animation without a clear purpose.



Do not delay critical operations until decorative animations

finish.



Avoid unnecessary infinite animations, permanent shimmer effects,

continuous particle systems, and repeated movement when the

interface is idle or not visible.



Do not animate every element simply because animation is available.



Avoid excessive motion, flashing, visually confusing transitions,

and competing simultaneous animations.



Prefer APIs and rendering techniques appropriate to the platform.



Measure animation performance and preserve smooth interaction

on lower-powered supported devices.



\## 9. Documentation for Beginners



The source code must remain readable, idiomatic, and consistently

formatted according to each language's conventions.



Code identifiers must use English.



Use the language-appropriate naming convention:

\- camelCase for Kotlin, TypeScript, and JavaScript variables,

&#x20; properties, functions, and methods.

\- PascalCase for classes, types, and applicable UI components.

\- snake\_case for Python variables and functions.

\- Other language-specific conventions where appropriate.



Document each important module and end-to-end flow in Brazilian

Portuguese so that a beginner can understand the implementation.



Explain:

\- Purpose and boundaries.

\- Entry point.

\- Component relationships.

\- Data flow.

\- Important business rules.

\- Authorization.

\- Persistence.

\- Error handling.

\- State transitions.

\- Testing procedure.

\- How to extend the feature safely.



Use diagrams when they provide meaningful clarification.



Avoid comments that simply repeat the source code.



Update documentation whenever a change alters a documented flow.



\## 10. Completion Gate



Before declaring any feature complete, verify:

\- Visual identity preserved.

\- Responsibilities clearly separated.

\- Source files maintainable.

\- Language conventions followed.

\- Relevant components reused.

\- Security boundaries verified.

\- Errors handled.

\- Authentication behavior preserved where appropriate.

\- Responsive layouts checked.

\- Animation preferences respected.

\- Performance and resource usage considered.

\- Data and network operations justified.

\- Tests executed where possible.

\- Documentation updated.

\- Limitations recorded.



Do not claim verification for any check that was not executed.



If a requirement cannot be implemented safely or verified,

report the exact blocker instead of simulating success.



