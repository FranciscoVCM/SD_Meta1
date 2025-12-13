package com.googol.webserver.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class AIController {

    @GetMapping("/ai")
    public String form() {
        return "ai"; // página existe, mas não é usada pela search
    }
}
