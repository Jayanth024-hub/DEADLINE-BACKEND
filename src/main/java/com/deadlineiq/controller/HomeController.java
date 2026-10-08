package com.deadlineiq.controller;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Controller handling public landing page and general site entry routes.
 */
@Controller
public class HomeController {

    @ModelAttribute
    public void addGlobalAttributes(Model model) {
        model.addAttribute("appName", "DeadlineIQ");
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        // Until user clicks sign out, redirect any authenticated user to their role dashboard
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            return redirectByRole(currentUser.getRole());
        }

        // Dynamic branding attributes passed to Thymeleaf
        model.addAttribute("appName", "DeadlineIQ");
        model.addAttribute("tagline", "One platform. Every deadline. Nothing missed.");
        
        // Highlight stats demonstrating dynamic rendering
        model.addAttribute("totalDeadlinesTracked", "1,250+");
        model.addAttribute("onTimeSubmissions", "99.4%");
        model.addAttribute("opportunitiesPosted", "48+");
        model.addAttribute("activeCampusUsers", "850+");

        return "index";
    }

    @GetMapping("/ai-chat")
    public String aiChat(Model model) {
        model.addAttribute("appName", "DeadlineIQ AI Assistant");
        return "ai-chat";
    }

    private String redirectByRole(Role role) {
        if (role == null) return "redirect:/student/dashboard";
        switch (role) {
            case STUDENT: return "redirect:/student/dashboard";
            case FACULTY: return "redirect:/faculty/dashboard";
            case COORDINATOR: return "redirect:/coordinator/dashboard";
            case DEAN: return "redirect:/dean/dashboard";
            case ADMIN: return "redirect:/admin/dashboard";
            default: return "redirect:/student/dashboard";
        }
    }
}
