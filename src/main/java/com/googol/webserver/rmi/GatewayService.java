package com.googol.webserver.rmi;

import com.googol.gateway.Gateway;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;
import org.springframework.stereotype.Service;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

@Service
public class GatewayService {

    private Gateway connect() {
        try {
            String host = System.getenv().getOrDefault("GATEWAY_HOST", "127.0.0.1");
            int port = Integer.parseInt(System.getenv().getOrDefault("GATEWAY_PORT", "1099"));

            Registry reg = LocateRegistry.getRegistry(host, port);
            return (Gateway) reg.lookup("Gateway");

        } catch (Exception e) {
            return null;
        }
    }

    public boolean isOnline() {
        try {
            Gateway gw = connect();
            if (gw == null) return false;
            gw.stats(); // ping remoto
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public SearchResult search(String terms, int page) {
        Gateway gw = connect();
        if (gw == null) return null;

        try {
            return gw.search(new SearchQuery(terms, page));
        } catch (Exception e) {
            return null;
        }
    }

    public boolean indexUrl(String url) {
        Gateway gw = connect();
        if (gw == null) return false;
        try {
            gw.indexUrl(url);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public StatsSnapshot stats() {
        Gateway gw = connect();
        if (gw == null) return null;
        try {
            return gw.stats();
        } catch (Exception e) {
            return null;
        }
    }

    public int inlinks(String url) {
        Gateway gw = connect();
        if (gw == null) return -1;
        try {
            return gw.inlinks(url);
        } catch (Exception e) {
            return -1;
        }
    }

    public java.util.List<String> backlinks(String url) {
        Gateway gw = connect();
        if (gw == null) return java.util.List.of();
        try {
            return gw.backlinks(url);
        } catch (Exception e) {
            return java.util.List.of();
        }
    }
    public long getLastSearchLatency(){
        Gateway gw = connect();
        if (gw == null) return -1;
        try {
            return gw.getLastSearchLatency();
        } catch (Exception e) {
            return -1;
        }
    }
}

