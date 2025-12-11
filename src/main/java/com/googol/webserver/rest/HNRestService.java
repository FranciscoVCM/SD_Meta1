package com.googol.webserver.rest;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Service
public class HNRestService {

    private final RestTemplate http = new RestTemplate();

    private static final String TOP_STORIES =
            "https://hacker-news.firebaseio.com/v0/topstories.json";

    private static final String ITEM =
            "https://hacker-news.firebaseio.com/v0/item/%d.json";

    public List<HNItem> searchTopStories(String term) {

        // 1. obter IDs dos top stories
        Integer[] ids = http.getForObject(TOP_STORIES, Integer[].class);
        if (ids == null) return List.of();

        List<HNItem> results = new ArrayList<>();

        // limitar a 40 items para velocidade
        int limit = Math.min(40, ids.length);

        for (int i = 0; i < limit; i++) {

            String url = ITEM.formatted(ids[i]);
            HNItem item = http.getForObject(url, HNItem.class);

            if (item == null) continue;

            // filtro simples
            if (item.title() != null && item.title().toLowerCase().contains(term.toLowerCase())) {
                results.add(item);
            }
        }

        return results;
    }
}
