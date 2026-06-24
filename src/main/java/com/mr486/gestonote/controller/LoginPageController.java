package com.mr486.gestonote.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * LoginPageController is a Spring MVC Controller that handles requests for the login page.
 */
@Controller
public class LoginPageController {

    /**
     * This method is mapped to the "/login" URL and is responsible for handling the login requests.
     * It sets the active page attribute to "login" and returns the name of the view to be rendered.
     *
     * @param model the Model object which can be used to add attributes to the view
     * @return the name of the view to be rendered
     */
    @RequestMapping(value = "/login")
    public String pageView(Model model) {
        model.addAttribute("page_active", "login");
        return "login";
    }
}
