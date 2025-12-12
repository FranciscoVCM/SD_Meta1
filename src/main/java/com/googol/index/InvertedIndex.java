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

    private static final int PAGE_SIZE = 10;

    public synchronized void add(PageDocument doc) {

        doc.normalizedUrl = TextUtils.normalizeUrl(doc.url);
        doc.timestamp = System.currentTimeMillis();

        PageDocument old = docs.put(doc.normalizedUrl, doc);

        if (old != null && old.outlinks != null)
            for (String to : old.outlinks)
                inlinksMap.getOrDefault(to, Set.of()).remove(old.normalizedUrl);

        if (doc.outlinks != null) {
            doc.outlinksCount = doc.outlinks.size();
            for (String raw : doc.outlinks) {
                String to = TextUtils.normalizeUrl(raw);
                inlinksMap.computeIfAbsent(to, k -> new HashSet<>()).add(doc.normalizedUrl);
            }
        }

        doc.inlinks = inlinks(doc.normalizedUrl);

        List<String> terms = TextUtils.tokenize(doc.text);
        if (terms.isEmpty()) return;

        Map<String, Integer> tf = new HashMap<>();
        for (String t : terms) tf.merge(t, 1, Integer::sum);

        for (var e : tf.entrySet()) {
            postings.computeIfAbsent(e.getKey(), k -> new HashMap<>())
                    .put(doc.normalizedUrl, e.getValue());
        }
    }

    public synchronized List<String> searchUrls(List<String> rawTerms) {

        List<String> terms = new ArrayList<>();
        for (String t : rawTerms) {
            t = TextUtils.normalize(t);
            if (!t.isBlank() && !TextUtils.isStopWord(t))
                terms.add(t);
        }

        if (terms.isEmpty()) return List.of();

        Set<String> candidate = null;

        for (String t : terms) {
            Map<String,Integer> m = postings.get(t);
            if (m == null) return List.of();

            if (candidate == null) candidate = new HashSet<>(m.keySet());
            else {
                candidate.retainAll(m.keySet());
                if (candidate.isEmpty()) return List.of();
            }
        }

        if (candidate == null) return List.of();

        Map<String, Integer> tfScore = new HashMap<>();
        for (String url : candidate) {
            int score = 0;
            for (String t : terms)
                score += postings.get(t).getOrDefault(url, 0);
            tfScore.put(url, score);
        }

        List<String> list = new ArrayList<>(candidate);

        list.sort((a,b)->{
            PageDocument A = docs.get(a);
            PageDocument B = docs.get(b);

            int la = A != null ? A.inlinks : 0;
            int lb = B != null ? B.inlinks : 0;

            int cmp = Integer.compare(lb, la);
            if (cmp != 0) return cmp;

            cmp = Integer.compare(tfScore.get(b), tfScore.get(a));
            if (cmp != 0) return cmp;

            boolean aTitle = A != null && A.title != null &&
                    terms.stream().anyMatch(t -> A.title.toLowerCase().contains(t));
            boolean bTitle = B != null && B.title != null &&
                    terms.stream().anyMatch(t -> B.title.toLowerCase().contains(t));

            if (aTitle && !bTitle) return -1;
            if (bTitle && !aTitle) return 1;

            return Long.compare(B.timestamp, A.timestamp);
        });

        return list;
    }

    public synchronized SearchResult search(SearchQuery q) {
        List<String> terms = TextUtils.tokenize(q.terms);
        List<String> urls = searchUrls(terms);

        int total = urls.size();
        int page = Math.max(1, q.page);

        int from = Math.min((page - 1) * PAGE_SIZE, total);
        int to   = Math.min(from + PAGE_SIZE, total);

        SearchResult out = new SearchResult();
        out.page = page;
        out.total = total;

        for (int i = from; i < to; i++) {
            String url = urls.get(i);
            PageDocument d = docs.get(url);

            SearchResult.Item it = new SearchResult.Item();
            it.url = d.url;
            it.title = d.title;
            it.snippet = d.snippet;
            it.inlinks = d.inlinks;
            it.outlinks = d.outlinksCount;

            out.items.add(it);
        }

        return out;
    }

    public synchronized int inlinks(String url) {
        url = TextUtils.normalizeUrl(url);
        return inlinksMap.getOrDefault(url, Set.of()).size();
    }

    public synchronized List<String> backlinks(String url) {
        url = TextUtils.normalizeUrl(url);
        return new ArrayList<>(inlinksMap.getOrDefault(url, Set.of()));
    }

    public synchronized StatsSnapshot stats() {
        StatsSnapshot s = new StatsSnapshot();
        s.numDocs = docs.size();
        s.numTerms = postings.size();

        int pairs = 0;
        for (var m : postings.values())
            pairs += m.size();

        s.numPostings = pairs;
        return s;
    }
}

