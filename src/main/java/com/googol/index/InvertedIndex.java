package com.googol.index;

import com.googol.model.PageDocument;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;
import com.googol.util.TextUtils;

import java.io.Serializable;
import java.util.*;


public class InvertedIndex implements Serializable {

    // termo -> (url -> tf)
    private final Map<String, Map<String, Integer>> postings = new HashMap<>();

    // URL -> { páginas que apontam para ela }
    private final Map<String, Set<String>> inlinksMap = new HashMap<>();

    // url -> documento
    private final Map<String, PageDocument> docs = new HashMap<>();

    // paginação default
    private static final int DEFAULT_PAGE_SIZE = 10;

    /** Adiciona (ou substitui) o documento e actualiza as ocorrências dos termos. */
    public synchronized void add(PageDocument doc) {
        // obter doc antigo (se existir) antes de substituir
        PageDocument old = docs.put(doc.url, doc);

        // retirar arestas antigas deste doc
        if (old != null && old.outlinks != null) {
            for (String to : old.outlinks) {
                Set<String> S = inlinksMap.get(to);
                if (S != null) {
                    S.remove(old.url);
                    if (S.isEmpty()) inlinksMap.remove(to);
                }
            }
        }

        // adicionar arestas novas (backlinks)
        if (doc.outlinks != null) {
            for (String to : doc.outlinks) {
                inlinksMap.computeIfAbsent(to, k -> new HashSet<>()).add(doc.url);
            }
        }

        List<String> terms = TextUtils.tokenize(doc.text);
        if (terms.isEmpty()) return;

        // tf por doc
        Map<String, Integer> tf = new HashMap<>();
        for (String t : terms) {
            tf.merge(t, 1, Integer::sum);
        }

        // acrescentar ao índice
        for (Map.Entry<String, Integer> e : tf.entrySet()) {
            postings
                    .computeIfAbsent(e.getKey(), k -> new HashMap<>())
                    .put(doc.url, e.getValue());
        }
    }

    /** Pesquisa URLs (AND) com scoring = soma das tf. */
    public synchronized List<String> searchUrls(List<String> rawTerms) {
        List<String> terms = new ArrayList<>();
        for (String t : rawTerms) {
            if (t == null || t.isBlank()) continue;
            t = TextUtils.normalize(t);
            if (TextUtils.isStopWord(t)) continue;
            terms.add(t);
        }
        if (terms.isEmpty()) return Collections.emptyList();

        // interseção dos conjuntos de URLs
        Set<String> candidate = null;
        for (String t : terms) {
            Map<String, Integer> m = postings.get(t);
            if (m == null) return Collections.emptyList();
            if (candidate == null) candidate = new HashSet<>(m.keySet());
            else {
                candidate.retainAll(m.keySet());
                if (candidate.isEmpty()) return Collections.emptyList();
            }
        }

        // scoring (tf) para desempate
        Map<String, Integer> score = new HashMap<>();
        for (String url : candidate) {
            int s = 0;
            for (String t : terms) {
                s += postings.get(t).getOrDefault(url, 0);
            }
            score.put(url, s);
        }

        List<String> urls = new ArrayList<>(candidate);
        urls.sort((a, b) -> {
            int ia = inlinks(a);
            int ib = inlinks(b);
            int cmp = Integer.compare(ib, ia); // mais inlinks primeiro
            if (cmp != 0) return cmp;

            int sa = score.getOrDefault(a, 0);
            int sb = score.getOrDefault(b, 0);
            cmp = Integer.compare(sb, sa);     // depois TF desc
            if (cmp != 0) return cmp;

            return a.compareTo(b);             // determinismo
        });
        return urls;
    }

    /** Pesquisa completa, devolvendo SearchResult paginado. */
    public synchronized SearchResult search(SearchQuery q) {
        List<String> terms = TextUtils.tokenize(q.terms);
        List<String> urls = searchUrls(terms);

        int page = (q.page <= 0) ? 1 : q.page;
        int pageSize = DEFAULT_PAGE_SIZE;
        int total = urls.size();

        int from = Math.min((page - 1) * pageSize, total);
        int to   = Math.min(from + pageSize, total);

        SearchResult res = new SearchResult();
        res.total = total;
        res.page  = page;
        res.items.clear();

        for (int i = from; i < to; i++) {
            String url = urls.get(i);
            PageDocument d = docs.get(url);

            SearchResult.Item it = new SearchResult.Item();
            it.url = url;
            it.title = (d != null && d.title != null && !d.title.isBlank()) ? d.title : url;
            it.snippet = (d != null) ? d.snippet : null;
            res.items.add(it);
        }
        return res;
    }

    /** Número de inlinks (páginas do índice que têm outlink para esta URL). */
    public synchronized int inlinks(String url) {
        if (url == null) return 0;
        return inlinksMap.getOrDefault(url, Collections.emptySet()).size();
    }

    /** Pequeno snapshot de estatísticas do índice. */
    public synchronized StatsSnapshot stats() {
        StatsSnapshot s = new StatsSnapshot();
        s.numDocs = docs.size();
        s.numTerms = postings.size();
        int pairs = 0;
        for (Map<String,Integer> m : postings.values()) pairs += m.size();
        s.numPostings = pairs;
        return s;
    }

    public synchronized List<String> backlinks(String url) {
        if (url == null) return List.of();
        Set<String> s = inlinksMap.get(url);
        if (s == null || s.isEmpty()) return List.of();
        return new ArrayList<>(s);
    }

    public synchronized PageDocument getDoc(String url) { return docs.get(url); }
    public int defaultPageSize() { return DEFAULT_PAGE_SIZE; }
}
