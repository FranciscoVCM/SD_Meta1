package com.googol.webserver.rest;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SnippetStore {

    /** termo → lista de snippets acumulados */
    private final Map<String, List<String>> snippetsMap = new HashMap<>();

    /** devolve lista atual de snippets (até 50) */
    public synchronized List<String> getSnippets(String term) {
        return snippetsMap.getOrDefault(term, List.of());
    }

    /** adiciona snippets novos e mantém máximo 50 */
    public synchronized List<String> addSnippets(String term, List<String> newOnes) {

        if (term == null || term.isBlank()) return List.of();

        List<String> list = snippetsMap.computeIfAbsent(term, k -> new ArrayList<>());

        for (String s : newOnes) {
            if (s != null && !s.isBlank() && list.size() < 50) {
                if (!list.contains(s)) {
                    list.add(s);
                }
            }
        }

        return List.copyOf(list);
    }
}
