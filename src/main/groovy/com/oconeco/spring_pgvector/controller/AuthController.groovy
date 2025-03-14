package com.oconeco.spring_pgvector.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/auth")
class AuthController {

    @GetMapping("/login")
    String loginPage() {
        return "auth/login"
    }
}
