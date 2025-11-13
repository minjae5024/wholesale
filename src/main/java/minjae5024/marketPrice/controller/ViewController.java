package minjae5024.marketPrice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/community")
    public String communityPage() {
        return "community";
    }

    @GetMapping("/posts/new")
    public String postWritePage() {
        return "post-write";
    }

    @GetMapping("/posts/{id}")
    public String postDetailPage() {
        return "post-detail";
    }
}
