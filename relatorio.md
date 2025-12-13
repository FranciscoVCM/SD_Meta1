RELATÓRIO – Meta 2 – Sistemas Distribuídos
Projeto SD_Meta2 – Motor de Busca Distribuído com WebServer + IA

Autores:

Francisco Martins - 2023211141 - (arquitetura distribuída, RMI, Barrels, Gateway, Downloaders, Webserver, IA, debugging final)

Gabriela Mendoza - 2022227025 (implementação do Webserver inicial, integração base SpringBoot, testes e debugging parcial)

1. Arquitetura Geral do Sistema

O projeto implementa um motor de busca distribuído, composto por múltiplos processos cooperantes que comunicam via Java RMI, executados em diferentes terminais/VMs. A Meta 2 adiciona um WebServer SpringBoot capaz de expor estatísticas, consultas de busca e integração com um modelo de IA local (Ollama + Gemma 2B).

Componentes principais
1. Gateway (RPC/RMI)

Guarda uma queue de URLs por visitar

Recebe URLs submetidas pelo utilizador

Distribui tarefas por downloaders (“getTask”)

Recolhe resultados (submitResult) e propaga para os Barrels

Executa pesquisas distribuídas acessando os Barrels

Agrega estatísticas globais

2. Downloaders (Crawler Workers)

Buscam páginas HTML reais via Jsoup

Extraem texto rico

Geram snippets, termos e outlinks

Submetem resultados ao Gateway

Propagam páginas para réplicas via ReliableMulticast

3. Barrels (Inverted Index)

Mantêm:

postings (termo - documentos)

docs com conteúdo enriquecido

inlinksMap (grafo de ligações)

snapshots automáticos para persistência

Cada Barrel responde a:

Pesquisas

Cálculo de inlinks / backlinks

Estatísticas internas

4. WebServer (SpringBoot) - Porta 8080

Fornece:

Interface HTML dinâmica (Thymeleaf)

REST endpoints

WebSocket com envio periódico de estatísticas do sistema

Integração com IA via Ollama (modelo gemma2:2b)

5. IA - Ollama

Executado em localhost:11434

Usado para gerar um resumo estável da pesquisa

Suporta agregação incremental de snippets

2. Integração SpringBoot + RPC/RMI

REST e Thymeleaf comunicam com o backend distribuído via interface:

GatewayService → RMI → GatewayServer → Barrels


O WebServer não indexa nada — delega tudo ao RPC.

Classes envolvidas:

GatewayService (cliente RMI)

SearchController, StatsController, InlinksController, BacklinksController

Templates HTML (search, stats, stream, inlinks, results)

Foi usada uma arquitetura limpa:
O WebServer - chama RMI - obtém resultados - renderiza HTML.

3. Integração WebSockets

O ficheiro:

StatsPublisher.java


envia estatísticas a cada 1s via WebSocket /stats-updates.

O Webserver usa:

WebSocketConfig

Canal /topic/stats

As páginas stats.html e stream.html recebem eventos em tempo real.

4. Integração REST WebServices

Endpoints implementados:

Método	Caminho	Função
GET	/search	Pesquisas de texto
GET	/inlinks	Inlinks de uma URL
GET	/backlinks	Backlinks de uma URL
GET	/stats	Estatísticas globais
GET	/stream	Stream de estatísticas (WebSocket)
POST	/index	Indexação submetida pelo utilizador
5. Integração com IA (Ollama)

O WebServer chama:

POST http://localhost:11434/api/generate


para gerar resumos:

Sempre em português

Máx. 120 palavras

Resumo estável:

Gerado uma vez por termo

Só regenera se receber snippets significativamente novos

Nunca se degrada ao navegar entre páginas

IAService implementa:

Cache por termo

Sanitização completa do JSON

Prompt robusto e determinista

Failover caso Ollama esteja offline

6. Testes de Software
   Tabela de Testes
   Teste	Descrição	Resultado
   T1	Indexação manual de URLs pelo WebServer	- PASS
   T2	Workers descarregam páginas e enviam ao Gateway	- PASS
   T3	Reliable Multicast guarda réplicas em múltiplos Barrels	- PASS
   T4	Pesquisa simples retorna resultados ordenados por TF + Inlinks	- PASS
   T5	Paginação funcional em /search	- PASS
   T6	IA gera resumo inicial	- PASS
   T7	IA mantém resumo ao navegar por páginas	- PASS
   T8	IA regenera se receber muitos snippets adicionais - PASS
   T9	WebSocket envia estatísticas em tempo real - PASS
   T10	Barrel snapshot é guardado a cada 1000 páginas - PASS
   T11	Carregamento de snapshot no arranque - PASS
   T12	Inlinks/backlinks funcional	- PASS
   T13	stats.html atualiza sem refresh	- PASS
   T14	webserver-keystore.p12 permite HTTPS local - PASS
   T15	Desempenho estável com 15k páginas indexadas - PASS
7. Distribuição de Tarefas
   Francisco Martins:

Toda a arquitetura RMI (Gateway, Barrel, Workers, ReliableMulticast)

InvertedIndex completo

Crawler avançado (WebCrawler)

Persistência (Snapshots)

WebServer final (controladores, thymeleaf, configs)

Integração com IA (Ollama)

WebSockets + StatsPublisher

Debugging avançado & correções finais

Design geral da arquitetura distribuída

Gabriela Mendoza:

WebServer inicial

Estrutura base dos controllers

primeiros templates HTML

Testes manuais iniciais

Debugging parcial

Documentação auxiliar

8. README EMBEBIDO (Instruções de instalação e execução)
   Requisitos

Java 17

Maven 3.8+

IntelliJ IDEA OU terminal

Ollama instalado

gemma2:2b instalado

VM Linux (VirtualBox) recomendada para workers

Executar o projeto:
1) Instalar dependências

No diretório do projeto:

mvn clean install


Na VM, para copiar Jsoup:

mvn dependency:copy-dependencies

2) Lançar Barrels

Terminal 1:

rm Barrel1.ser   # opcional, limpa snapshot
java com.googol.barrels.BarrelLauncher 1

3) Lançar Gateway

Terminal 2:

java com.googol.gateway.GatewayLauncher

4) Lançar Downloaders

Terminal 3:

java com.googol.downloaders.DownloaderStandaloneLauncher


Ou vários (em VMs diferentes):

java com.googol.downloaders.DownloaderStandaloneLauncher &

5) Lançar WebServer

In IntelliJ:
Program arguments:

--server.port=8080


Environment variables:

GATEWAY_HOST=127.0.0.1
GATEWAY_PORT=1099


Run configuration - Spring Boot - WebServerApplication

6) Lançar Ollama

Terminal 4:

ollama serve


Noutro terminal:

ollama pull gemma2:2b

7) Aceder ao website

Abrir:

http://localhost:8080

8) HTTPS opcional

SpringBoot usa:

webserver-keystore.p12
aceder https://localhost:8100

Configuração em:

src/main/resources/application.properties

9. Conclusão

O projeto cumpre todos os requisitos funcionais e técnicos da Meta 2:

Motor de busca distribuído

WebServer com REST + WebSockets + HTML dinâmico

IA integrada

Persistência automática dos barrels

Sistema robusto, escalável e modular

Documentação completa para avaliação