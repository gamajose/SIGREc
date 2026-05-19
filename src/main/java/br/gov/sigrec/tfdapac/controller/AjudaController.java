package br.gov.sigrec.tfdapac.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AjudaController {

    @GetMapping("/ajuda/manual")
    public String manual() {
        return "ajuda/manual";
    }

    @GetMapping("/ajuda/guia-preenchimento")
    public String guiaPreenchimento() {
        return "ajuda/guia-preenchimento";
    }

    @GetMapping("/ajuda/faq")
    public String faq() {
        return "ajuda/faq";
    }

    @GetMapping("/ajuda/sobre")
    public String sobre() {
        return "ajuda/sobre";
    }
}
