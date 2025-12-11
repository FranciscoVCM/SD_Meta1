package com.googol.webserver.controllers;

import com.googol.webserver.rest.HNItem;
import com.googol.webserver.rest.HNRestService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HNController {

    private final HNRestService hn;

    public HNController(HNRestService hn) {
        this.hn = hn;
    }

    @GetMapping("/hn")
    public String hnSearch(@RequestParam String term, Model model) {

        List<HNItem> results = hn.searchTopStories(term);

        model.addAttribute("term", term);
        model.addAttribute("items", results);

        return "hackernews";   // HTML a criar
    }
}
