package br.com.rizermarketplaces.core.marketplace.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// @Controller (diferente de @RestController) é usado quando queremos retornar views ou fazer redirects.
@Controller
public class DocsRedirectController {

    // Mapeia rotas amigáveis para a documentação e redireciona para o índice do Swagger UI.
    @GetMapping({"/docs", "/swagger-ui.html"})
    public String redirectToScalarDocs() {
        // 'redirect:' instrui o Spring MVC a enviar um redirect HTTP para o navegador.
        return "redirect:/docs/index.html";
    }
}
