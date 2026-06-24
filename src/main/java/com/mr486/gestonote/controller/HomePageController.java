package com.mr486.gestonote.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomePageController {

    @GetMapping("/")
    public String pageView(Model model) {
        model.addAttribute("page_active", "home");
        return "home";
    }
}
