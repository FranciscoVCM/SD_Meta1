package com.googol.client;

import com.googol.gateway.Gateway;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.util.RmiUtils;

import java.util.Arrays;
import java.util.Scanner;

public class ClientApp {

    // ===== Helper seguro para conectar à Gateway =====
    private static Gateway connect() {
        try {
            String gwHost = System.getenv().getOrDefault("GATEWAY_HOST", "192.168.1.79");
            int gwPort = Integer.parseInt(System.getenv().getOrDefault("GATEWAY_PORT", "1099"));
            return RmiUtils.lookup(gwHost, gwPort, "Gateway", Gateway.class);
        } catch (Exception e) {
            return null;  // Gateway offline
        }
    }

    private static void showMenu() {
        System.out.println("\n========= Googol Client =========");
        System.out.println("1 - Indexar URL");
        System.out.println("2 - Pesquisar");
        System.out.println("3 - Estatísticas");
        System.out.println("4 - Inlinks");
        System.out.println("5 - Backlinks");
        System.out.println("0 - Sair");
        System.out.print("Escolha uma opção: ");
    }

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);

        while (true) {

            showMenu();

            String choice = sc.nextLine().trim();

            switch (choice) {

                case "1" -> {  // INDEXAR URL
                    Gateway gw = connect();
                    if (gw == null) {
                        System.out.println("[Erro] A gateway está offline — tente novamente mais tarde.");
                        break;
                    }
                    System.out.print("URL a indexar: ");
                    String url = sc.nextLine().trim();
                    try {
                        gw.indexUrl(url);
                        System.out.println("Submetido: " + url);
                    } catch (Exception e) {
                        System.out.println("[Erro] Falha ao enviar pedido para a gateway.");
                    }
                }

                case "2" -> {  // PESQUISAR
                    Gateway gw = connect();
                    if (gw == null) {
                        System.out.println("[Erro] A gateway está offline — tente novamente mais tarde.");
                        break;
                    }

                    System.out.print("Termos da pesquisa: ");
                    String terms = sc.nextLine().trim();
                    System.out.print("Página (1 por defeito): ");
                    String p = sc.nextLine().trim();

                    int page = 1;
                    try { page = Integer.parseInt(p); } catch (Exception ignored) {}

                    try {
                        SearchQuery q = new SearchQuery(terms, page);
                        SearchResult res = gw.search(q);

                        System.out.println("\n=== Search Results ===");
                        System.out.println("Total: " + res.total + " (page " + res.page + ")");
                        int i = 1 + (res.page - 1) * 10;
                        for (SearchResult.Item it : res.items) {
                            System.out.println((i++) + ". " + it.title + " — " + it.url);
                            if (it.snippet != null && !it.snippet.isBlank())
                                System.out.println("   " + it.snippet);
                            System.out.println();
                        }
                    } catch (Exception e) {
                        System.out.println("[Erro] Falha ao contactar a gateway.");
                    }
                }

                case "3" -> {  // ESTATÍSTICAS
                    Gateway gw = connect();
                    if (gw == null) {
                        System.out.println("[Erro] A gateway está offline — tente novamente mais tarde.");
                        break;
                    }

                    try {
                        var s = gw.stats();
                        System.out.println("pagesIndexed = " + s.pagesIndexed);
                        System.out.println("urlsInQueue  = " + s.urlsInQueue);
                        System.out.println("activeDl     = " + s.activeDownloaders);
                        System.out.println("numDocs      = " + s.numDocs);
                        System.out.println("numTerms     = " + s.numTerms);
                        System.out.println("numPostings  = " + s.numPostings);

                        if (s.topQueries != null && !s.topQueries.isEmpty()) {
                            System.out.println("\nTop queries:");
                            int i = 1;
                            for (String qstr : s.topQueries) {
                                System.out.println("  " + (i++) + ". " + qstr);
                            }
                        }

                        if (s.barrelNumDocs != null && !s.barrelNumDocs.isEmpty()) {
                            System.out.println("\nBarrels (numDocs):");
                            s.barrelNumDocs.forEach((label, docs) ->
                                    System.out.println("  " + label + " = " + docs));
                        }

                        if (s.barrelAvgLatencySec != null && !s.barrelAvgLatencySec.isEmpty()) {
                            System.out.println("\nLatência média por barrel (segundos):");
                            s.barrelAvgLatencySec.forEach((label, sec) -> {
                                String val = (sec == null || sec < 0) ? "N/A" : String.format("%.1f s", sec);
                                System.out.println("  " + label + " = " + val);
                            });
                        }

                    } catch (Exception e) {
                        System.out.println("[Erro] Falha ao pedir estatísticas à gateway.");
                    }
                }

                case "4" -> {  // INLINKS
                    Gateway gw = connect();
                    if (gw == null) {
                        System.out.println("[Erro] A gateway está offline — tente novamente mais tarde.");
                        break;
                    }
                    System.out.print("URL: ");
                    String url = sc.nextLine().trim();
                    try {
                        System.out.println("inlinks(" + url + ") = " + gw.inlinks(url));
                    } catch (Exception e) {
                        System.out.println("[Erro] Falha ao pedir inlinks.");
                    }
                }

                case "5" -> {  // BACKLINKS
                    Gateway gw = connect();
                    if (gw == null) {
                        System.out.println("[Erro] A gateway está offline — tente novamente mais tarde.");
                        break;
                    }
                    System.out.print("URL: ");
                    String url = sc.nextLine().trim();
                    try {
                        var list = gw.backlinks(url);
                        System.out.println("Backlinks (" + list.size() + "):");
                        for (String u : list) System.out.println(" - " + u);
                    } catch (Exception e) {
                        System.out.println("[Erro] Falha ao pedir backlinks.");
                    }
                }

                case "0" -> {
                    System.out.println("A sair...");
                    return;
                }

                default -> System.out.println("Opção inválida.");
            }
        }
    }
}
