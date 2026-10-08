package com.deadlineiq.controller;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/faculty")
public class FacultyController {

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }
        if (currentUser.getRole() != Role.FACULTY) {
            return "redirect:/login";
        }

        model.addAttribute("user", currentUser);
        model.addAttribute("totalAssignments", 12);
        model.addAttribute("activeDeadlines", 5);
        model.addAttribute("completedSubmissions", 142);
        model.addAttribute("pendingSubmissions", 38);

        return "faculty/dashboard";
    }
}
