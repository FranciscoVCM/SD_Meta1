package com.googol.webserver.controllers;

import com.googol.webserver.rmi.GatewayService;
import com.googol.webserver.rest.AIService;
import com.googol.model.SearchResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class SearchController {

    private final GatewayService gateway;
    private final AIService ai;

    public SearchController(GatewayService gateway, AIService ai) {
        this.gateway = gateway;
        this.ai = ai;
    }

    @GetMapping("/search")
    public String search(
            @RequestParam(defaultValue = "") String terms,
            @RequestParam(defaultValue = "1") int page,
            Model model
    ) {
        // Se não escreveu nada → não chama gateway, mostra página vazia
        if (terms.isBlank()) {
            model.addAttribute("terms", "");
            model.addAttribute("result", null);
            model.addAttribute("analysis", "");
            return "search";
        }

        SearchResult result = gateway.search(terms, page);

        // === Análise AI ===
        String analysis = "";
        try {
            List<String> snippets =
                    result.items.stream()
                            .map(i -> i.snippet == null ? "" : i.snippet)
                            .limit(5)
                            .toList();

            analysis = ai.analyzeSearch(terms, snippets);

        } catch (Exception e) {
            analysis = "(IA indisponível no momento)";
        }

        model.addAttribute("terms", terms);
        model.addAttribute("result", result);
        model.addAttribute("analysis", analysis);

        return "search";
    }
}