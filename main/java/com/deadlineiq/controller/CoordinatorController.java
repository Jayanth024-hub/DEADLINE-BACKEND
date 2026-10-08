package com.deadlineiq.controller;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/coordinator")
public class CoordinatorController {

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.getRole() != Role.COORDINATOR) {
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("totalOpportunities", 14);
        model.addAttribute("activeDrives", 6);
        model.addAttribute("upcomingEvents", 4);
        model.addAttribute("totalRegistrations", 240);

        return "coordinator/dashboard";
    }
}
