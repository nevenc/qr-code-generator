package com.nevenc.qrcodegenerator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    @GetMapping("/")
    String home() {
        logger.debug("Accessing home page [/]. Redirecting to generator page [/generator].");
        return "redirect:/generator";
    }

    @GetMapping("/generator")
    String generator() {
        logger.debug("Accessing generator page [/generator].");
        return "generator";
    }

}
