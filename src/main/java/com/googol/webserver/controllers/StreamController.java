package com.googol.webserver.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StreamController {

    @GetMapping("/stream")
    public String stream() {
        return "stream";
    }
}
