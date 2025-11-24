package pl.learnedge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InfoController {

    @GetMapping("/info/about")
    public String about() {
        return "info/about";
    }

    @GetMapping("/info/contact")
    public String contact() {
        return "info/contact";
    }
}
