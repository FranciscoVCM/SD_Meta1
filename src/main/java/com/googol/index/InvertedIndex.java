package com.googol.index;

import com.googol.model.PageDocument;
import com.googol.model.SearchQuery;
import com.googol.model.SearchResult;
import com.googol.model.StatsSnapshot;
import com.googol.util.TextUtils;

import java.io.Serializable;
import java.util.*;

public class InvertedIndex implements Serializable {

    private final Map<String, Map<String, Integer>> postings = new HashMap<>();
    private final Map<String, Set<String>> inlinksMap = new HashMap<>();
    private final Map<String, PageDocument> docs = new HashMap<>();

    private static final int DEFAULT_PAGE_SIZE = 10;

    // =======================
    // ADD DOCUMENT
    // =======================
    public synchronized void add(PageDocument doc) {

        PageDocument old = docs.put(doc.url, doc);

        // remover backlinks antigos
        if (old != null && old.outlinks != null) {
            for (String to : old.outlinks) {
                Set<String> s = inlinksMap.get(to);
                if (s != null) {
                    s.remove(old.url);
                    if (s.isEmpty()) inlinksMap.remove(to);
                }
            }
        }

        // adicionar backlinks novos
        if (doc.outlinks != null) {
            doc.outlinksCount = doc.outlinks.size();
            for (String to : doc.outlinks) {
                inlinksMap.computeIfAbsent(to, k -> new HashSet<>()).add(doc.url);
            }
        }

        // atualizar inlinksCount do doc
        doc.inlinks = inlinks(doc.url);

        // indexação de termos
        List<String> terms = TextUtils.tokenize(doc.text);
        if (terms.isEmpty()) return;

        Map<String, Integer> tf = new HashMap<>();
        for (String t : terms) tf.merge(t, 1, Integer::sum);

        for (var e : tf.entrySet()) {
            postings.computeIfAbsent(e.getKey(), k -> new HashMap<>())
                    .put(doc.url, e.getValue());
        }
    }

    // =======================
    // RANKING SEARCH
    // =======================
    public synchronized List<String> searchUrls(List<String> rawTerms) {

        List<String> terms = new ArrayList<>();

        for (String t : rawTerms) {
            t = TextUtils.normalize(t);
            if (TextUtils.isStopWord(t)) continue;
            terms.add(t);
        }

        if (terms.isEmpty()) return List.of();

        // interseção
        Set<String> candidate = null;

        for (String t : terms) {
            Map<String, Integer> m = postings.get(t);
            if (m == null) return List.of();

            if (candidate == null) candidate = new HashSet<>(m.keySet());
            else {
                candidate.retainAll(m.keySet());
                if (candidate.isEmpty()) return List.of();
            }
        }

        if (candidate == null) return List.of();

        // scoring
        Map<String, Integer> tfScore = new HashMap<>();
        for (String url : candidate) {
            int sum = 0;
            for (String t : terms) sum += postings.get(t).getOrDefault(url, 0);
            tfScore.put(url, sum);
        }

        // ordenar
        List<String> list = new ArrayList<>(candidate);

        list.sort((a, b) -> {

            PageDocument A = docs.get(a);
            PageDocument B = docs.get(b);

            int ia = (A != null ? A.inlinks : 0);
            int ib = (B != null ? B.inlinks : 0);

            // 1) Inlinks DESC
            int cmp = Integer.compare(ib, ia);
            if (cmp != 0) return cmp;

            // 2) TF DESC
            cmp = Integer.compare(tfScore.getOrDefault(b, 0), tfScore.getOrDefault(a, 0));
            if (cmp != 0) return cmp;

            // 3) Boost no título
            boolean aTitle = (A != null && containsInTitle(A, terms));
            boolean bTitle = (B != null && containsInTitle(B, terms));

            if (aTitle && !bTitle) return -1;
            if (bTitle && !aTitle) return 1;

            // 4) determinismo
            return a.compareTo(b);
        });

        return list;
    }

    private boolean containsInTitle(PageDocument d, List<String> terms) {
        if (d.title == null) return false;
        String t = d.title.toLowerCase();
        for (String s : terms)
            if (t.contains(s)) return true;
        return false;
    }
    public synchronized StatsSnapshot stats() {
        StatsSnapshot s = new StatsSnapshot();
        s.numDocs = docs.size();
        s.numTerms = postings.size();

        int pairs = 0;
        for (Map<String,Integer> m : postings.values())
            pairs += m.size();
        s.numPostings = pairs;

        // não tem lastSearchMs no barrel, deixamos = 0
        s.lastSearchMs = 0;

        return s;
    }


    // =======================
    // FULL SEARCH
    // =======================
    public synchronized SearchResult search(SearchQuery q) {

        List<String> terms = TextUtils.tokenize(q.terms);
        List<String> urls = searchUrls(terms);

        int page = Math.max(1, q.page);
        int total = urls.size();

        int from = Math.min((page - 1) * DEFAULT_PAGE_SIZE, total);
        int to   = Math.min(from + DEFAULT_PAGE_SIZE, total);

        SearchResult out = new SearchResult();
        out.page = page;
        out.total = total;

        for (int i = from; i < to; i++) {
            String url = urls.get(i);
            PageDocument d = docs.get(url);

            SearchResult.Item it = new SearchResult.Item();
            it.url = url;
            it.title = (d != null ? d.title : url);
            it.snippet = (d != null ? d.snippet : "");

            if (d != null) {
                it.inlinks = d.inlinks;
                it.outlinks = d.outlinksCount;
            }

            out.items.add(it);
        }

        return out;
    }

    // =======================
    public synchronized int inlinks(String url) {
        if (url == null) return 0;
        return inlinksMap.getOrDefault(url, Set.of()).size();
    }

    public synchronized List<String> backlinks(String url) {
        Set<String> s = inlinksMap.get(url);
        return (s == null) ? List.of() : new ArrayList<>(s);
    }
}
