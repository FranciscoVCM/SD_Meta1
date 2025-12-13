package com.googol.webserver.controllers;

import com.googol.webserver.rest.HNRestService;
import com.googol.webserver.rest.HNSearchResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class HNController {

    private final HNRestService hn;

    public HNController(HNRestService hn) {
        this.hn = hn;
    }

    @GetMapping("/hn")
    public String search(
            @RequestParam(defaultValue = "") String term,
            @RequestParam(defaultValue = "0") int page,
            Model model
    ) {

        HNSearchResult result;

        if (term.isBlank()) {
            // devolve resultado vazio, sem crashar
            result = new HNSearchResult();
            result.term = "";
            result.page = 0;
            result.totalPages = 0;
        } else {
            result = hn.search(term, page);
        }

        model.addAttribute("term", term);
        model.addAttribute("result", result);

        return "hackernews";
    }
}
