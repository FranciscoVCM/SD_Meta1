package com.googol.webserver.controllers;

import com.googol.webserver.rest.AIService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AIController {

    private final AIService ai;

    public AIController(AIService ai) {
        this.ai = ai;
    }

    @GetMapping("/ai")
    public String aiForm() {
        return "ai";
    }

    @PostMapping("/ai/summary")
    public String summarize(@RequestParam String text, Model model) {

        String result = ai.summarize(text);

        model.addAttribute("input", text);
        model.addAttribute("summary", result);

        return "ai";
    }
}
