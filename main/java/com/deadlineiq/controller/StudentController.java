package com.deadlineiq.controller;

import com.deadlineiq.model.Deadline;
import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.service.DeadlineService;
import com.deadlineiq.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final DeadlineService deadlineService;
    private final UserService userService;

    public StudentController(DeadlineService deadlineService, UserService userService) {
        this.deadlineService = deadlineService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() != Role.STUDENT) {
            return "redirect:/login";
        }

        List<Deadline> studentDeadlines = deadlineService.getDeadlinesForStudent(currentUser.getSection());
        Map<String, Object> stats = deadlineService.getStudentStats(currentUser.getSection());

        model.addAttribute("user", currentUser);
        model.addAttribute("deadlines", studentDeadlines);
        model.addAttribute("totalDeadlines", stats.get("total"));
        model.addAttribute("pendingDeadlines", stats.get("pending"));
        model.addAttribute("completedDeadlines", stats.get("completed"));
        model.addAttribute("dueSoonDeadlines", stats.get("dueSoon"));
        model.addAttribute("overdueDeadlines", stats.get("overdue"));
        model.addAttribute("completionPercentage", stats.get("percentage"));

        return "student/dashboard";
    }

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        // Fetch fresh copy from service
        Optional<User> freshUser = userService.findById(currentUser.getId());
        model.addAttribute("user", freshUser.orElse(currentUser));
        return "student/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam("name") String name,
                                @RequestParam("department") String department,
                                @RequestParam(value = "year", required = false) String year,
                                @RequestParam(value = "section", required = false) String section,
                                @RequestParam(value = "password", required = false) String password,
                                HttpSession session,
                                Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/login";
        }

        boolean updated = userService.updateUserProfile(currentUser.getId(), name, department, year, section, password);
        if (updated) {
            User freshUser = userService.findById(currentUser.getId()).get();
            session.setAttribute("currentUser", freshUser);
            model.addAttribute("user", freshUser);
            model.addAttribute("success", "Profile updated successfully!");
        } else {
            model.addAttribute("user", currentUser);
            model.addAttribute("error", "Failed to update profile.");
        }

        return "student/profile";
    }

    @GetMapping("/deadlines/delete/{id}")
    public String deleteDeadline(@PathVariable("id") Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null && currentUser.getRole() == Role.STUDENT) {
            deadlineService.deleteDeadline(id);
        }
        return "redirect:/student/dashboard";
    }
}
