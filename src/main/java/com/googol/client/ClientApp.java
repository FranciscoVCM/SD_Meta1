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
                    System.out.println("usage: search <terms>");
                    return;
                }
                String terms = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                SearchQuery q = new SearchQuery(terms, 1);
                SearchResult res = gw.search(q);

                System.out.println("\n=== Search Results ===");
                System.out.println("Total: " + res.total + " (page " + res.page + ")");
                int i = 1;
                for (SearchResult.Item it : res.items) {
                    System.out.println(i++ + ". " + it.title + " — " + it.url);
                    if (it.snippet != null && !it.snippet.isBlank())
                        System.out.println("   " + it.snippet);
                    System.out.println();
                }
            }

            default -> System.out.println("unknown command");
        }
    }
}
