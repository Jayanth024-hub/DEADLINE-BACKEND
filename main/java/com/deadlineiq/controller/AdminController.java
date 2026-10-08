package com.deadlineiq.controller;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() != Role.ADMIN) {
            return "redirect:/login";
        }

        List<User> allUsers = userService.getAllUsers();

        model.addAttribute("user", currentUser);
        model.addAttribute("allUsers", allUsers);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("totalStudents", userService.countByRole(Role.STUDENT));
        model.addAttribute("totalFaculty", userService.countByRole(Role.FACULTY));
        model.addAttribute("totalCoordinators", userService.countByRole(Role.COORDINATOR));
        model.addAttribute("totalDeans", userService.countByRole(Role.DEAN));
        model.addAttribute("totalDeadlines", 36);

        return "admin/dashboard";
    }
}
