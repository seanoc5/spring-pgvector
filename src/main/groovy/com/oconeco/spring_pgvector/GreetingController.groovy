package com.oconeco.spring_pgvector

//import org.springframework.ai.model.Model
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

@Controller
class GreetingController {

    @GetMapping("/greeting")
    public String hello(@RequestParam(name="name", defaultValue = "World", required = false) String name, Model model) {
        model.addAttribute("name", name);
        return "greeting";
    }

}
