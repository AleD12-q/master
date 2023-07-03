package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import searchengine.config.ServerConfig;

@Controller
public class DefaultController {
    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf.
     */
    private final ServerConfig serverConfig;

    public DefaultController(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    @RequestMapping("/")
    public String defaultPage(Model model) {
        return "index";
    }
}


