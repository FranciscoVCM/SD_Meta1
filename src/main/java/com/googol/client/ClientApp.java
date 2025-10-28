package com.googol.client;

import com.googol.gateway.Gateway;
import com.googol.model.SearchQuery;
import com.googol.util.RmiUtils;

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
                if (args.length < 2) { System.out.println("search <terms...>"); return; }
                String terms = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                System.out.println(gw.search(new SearchQuery(terms, 1)));
            }
            default -> System.out.println("unknown command");
        }
    }
}
