package com.googol.client;

import com.googol.gateway.Gateway;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;   // <- NOVO
import com.googol.util.RmiUtils;

import java.util.Arrays;               // <- NOVO

public class ClientApp {
    public static void main(String[] args) throws Exception {
        Gateway gw = RmiUtils.lookup("Gateway", Gateway.class);
        if (args.length == 0) {
            System.out.println("usage: index <url> | search <terms...>");
            return;
        }
        switch (args[0]) {
            case "index" -> {
                if (args.length < 2) { System.out.println("index <url>"); return; }
                gw.indexUrl(args[1]);
            }
            case "search" -> {
                if (args.length < 2) {
                    System.out.println("usage: search [page] <terms...>");
                    return;
                }

                int page = 1;
                int termsStart = 1;

                // se o 2º token for um número, interpretamos como página
                try {
                    if (args.length >= 3) {
                        page = Integer.parseInt(args[1]);
                        termsStart = 2;
                    }
                } catch (NumberFormatException ignored) { /* continua com page=1 */ }

                String terms = String.join(" ", Arrays.copyOfRange(args, termsStart, args.length));
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
            }
            case "stats" -> {
                var gaw = RmiUtils.lookup("Gateway", Gateway.class);
                var s = gaw.stats();
                System.out.println("pagesIndexed = " + s.pagesIndexed);
                System.out.println("urlsInQueue  = " + s.urlsInQueue);
                System.out.println("activeDl     = " + s.activeDownloaders);
                System.out.println("numDocs      = " + s.numDocs);
                System.out.println("numTerms     = " + s.numTerms);
                System.out.println("numPostings  = " + s.numPostings);
            }
            case "inlinks" -> {
                if (args.length < 2) { System.out.println("usage: inlinks <url>"); return; }
                var gow = RmiUtils.lookup("Gateway", Gateway.class);
                System.out.println("inlinks(" + args[1] + ") = " + gow.inlinks(args[1]));
            }
            case "backlinks" -> {
                if (args.length < 2) { System.out.println("usage: backlinks <url>"); return; }
                var gaw = RmiUtils.lookup("Gateway", Gateway.class);
                var list = gaw.backlinks(args[1]);
                System.out.println("Backlinks (" + list.size() + "):");
                for (String u : list) System.out.println(" - " + u);
            }

            default -> System.out.println("unknown command");
        }
    }
}
