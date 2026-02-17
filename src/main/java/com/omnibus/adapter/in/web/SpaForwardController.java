package com.Omnibus.adapter.in.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Forwards non-API, non-static requests to the React SPA's index.html.
 * This enables React Router to handle client-side routing on page refresh
 * and direct URL navigation (deep links).
 */
@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/",
            "/login",
            "/register",
            "/dashboard",
            "/dashboard/**",
            "/send",
            "/activity",
            "/settings",
            "/settings/**"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
