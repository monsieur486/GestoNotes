package com.mr486.gestonote.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class CategoriePageController {

    @GetMapping("/categories")
    public String pageView(Model model) {
        model.addAttribute("page_active", "categories");
        return "categories";
    }
}
