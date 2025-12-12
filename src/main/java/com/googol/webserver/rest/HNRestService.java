package com.googol.webserver.rest;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class HNRestService {

    private final RestTemplate http = new RestTemplate();

    private static final String SEARCH_API =
            "https://hn.algolia.com/api/v1/search?query=%s&page=%d&hitsPerPage=20";

    public HNSearchResult search(String term, int page) {
        try {
            String url = SEARCH_API.formatted(term, page);
            Map response = http.getForObject(url, Map.class);

            if (response == null) return new HNSearchResult();

            List<Map<String, Object>> hits = (List<Map<String, Object>>) response.get("hits");
            int totalPages = (int) response.getOrDefault("nbPages", 1);

            HNSearchResult out = new HNSearchResult();
            out.term = term;
            out.page = page;
            out.totalPages = totalPages;

            for (Map<String, Object> h : hits) {

                HNItem item = new HNItem(
                        (String) h.getOrDefault("title", "(sem título)"),
                        (String) h.get("url"),
                        (String) h.get("author"),
                        ((Number) h.getOrDefault("points", 0)).intValue(),
                        ((Number) h.getOrDefault("num_comments", 0)).intValue(),
                        (String) h.get("created_at")
                );

                out.items.add(item);
            }

            return out;

        } catch (Exception e) {
            return new HNSearchResult();
        }
    }
}
