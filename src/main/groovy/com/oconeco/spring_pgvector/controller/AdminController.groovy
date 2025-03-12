package com.oconeco.spring_pgvector.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/admin")
class AdminController {

    @GetMapping(["", "/dashboard"])
    String dashboard() {
        return "admin/dashboard"
    }

//    @GetMapping("/embeddings")
//    String embeddings() {
//        // You can add model attributes here to populate the page with data
//        return "admin/embeddings"
//    }

    @GetMapping("/settings")
    String settings() {
        return "admin/settings"
    }

    @GetMapping("/users")
    String users() {
        return "admin/users"
    }

    @GetMapping("/reports/usage")
    String usageReports() {
        return "admin/reports/usage"
    }

    @GetMapping("/reports/performance")
    String performanceReports() {
        return "admin/reports/performance"
    }
}
