package com.medsimple.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the single-page app shell for client-side routes.
 * Any deep link into a SPA zone (e.g. /login, /app/episodes/123) is forwarded to
 * the built index.html so React Router can take over. API paths (/api/**) and real
 * static files (/assets/**, /banners_logo/**) are handled before this and never reach it.
 */
@Controller
public class SpaForwardController {

    @GetMapping(value = {
            "/login",
            "/signup",
            "/app",
            "/app/**",
            "/console",
            "/console/**"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
