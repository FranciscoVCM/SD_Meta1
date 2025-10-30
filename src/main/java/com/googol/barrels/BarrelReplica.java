package com.googol.barrels;

import com.googol.index.InvertedIndex;
import com.googol.index.LinkGraph;
import com.googol.model.CrawlResult;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BarrelReplica extends UnicastRemoteObject implements Barrel {

    private final InvertedIndex index = new InvertedIndex();
    @SuppressWarnings("unused")
    private final LinkGraph linkGraph = new LinkGraph(); // usaremos mais tarde
    // Título/snippet simples por URL (para mostrar nos resultados)
    private final java.util.Map<String, String> titles = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<String, String> snippets = new java.util.concurrent.ConcurrentHashMap<>();

    public BarrelReplica() throws RemoteException { super(); }

    @Override
    public synchronized void append(CrawlResult r) throws RemoteException {
        if (r == null || r.url == null) return;
        index.addDocument(r.url, r.terms != null ? r.terms : List.of());
        if (r.title != null)   titles.put(r.url, r.title);
        if (r.snippet != null) snippets.put(r.url, r.snippet);
        // (inlinks/outlinks ficam para a próxima etapa)
    }

    @Override
    public synchronized SearchResult search(SearchQuery q) throws RemoteException {
        SearchResult out = new SearchResult();
        if (q == null || q.terms == null || q.terms.isBlank()) return out;

        List<String> tokens = com.googol.util.TextUtils.tokenize(q.terms);
        Set<String> urls = index.searchAny(tokens);

        // paginação 10-a-10
        int page = Math.max(q.page, 1);
        int pageSize = 10;
        List<String> all = new ArrayList<>(urls);
        int total = all.size();
        int from = Math.min((page - 1) * pageSize, total);
        int to   = Math.min(from + pageSize, total);

        out.total = total;
        out.page = page;

        for (int i = from; i < to; i++) {
            String url = all.get(i);
            SearchResult.Item it = new SearchResult.Item();
            it.url = url;
            it.title = titles.getOrDefault(url, url);
            it.snippet = snippets.getOrDefault(url, "");
            out.items.add(it);
        }
        return out;
    }

    @Override
    public synchronized int inlinks(String url) throws RemoteException {
        return 0; // implementaremos quando fizermos LinkGraph
    }

    @Override
    public synchronized StatsSnapshot stats() throws RemoteException {
        StatsSnapshot s = new StatsSnapshot();
        s.pagesIndexed = titles.size();
        s.urlsInQueue = 0; // atualizaremos quando ligarmos ao Gateway/Queue
        s.activeDownloaders = 0;
        return s;
    }
}
