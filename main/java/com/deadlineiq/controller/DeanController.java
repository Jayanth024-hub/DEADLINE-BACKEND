package com.deadlineiq.controller;

import com.deadlineiq.model.Deadline;
import com.deadlineiq.model.DeadlineStatus;
import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.service.DeadlineService;
import com.deadlineiq.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/dean")
public class DeanController {

    private final DeadlineService deadlineService;
    private final UserService userService;

    public DeanController(DeadlineService deadlineService, UserService userService) {
        this.deadlineService = deadlineService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser == null || currentUser.getRole() != Role.DEAN) {
            return "redirect:/login";
        }

        List<Deadline> deadlines = deadlineService.getAllDeadlines();
        List<Deadline> upcomingDeadlines = deadlines.stream()
                .filter(deadline -> !deadline.isCompleted())
                .filter(deadline -> deadline.getDueDate() == null || !deadline.getDueDate().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(Deadline::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(5)
                .collect(Collectors.toList());
        long completedDeadlines = deadlines.stream().filter(Deadline::isCompleted).count();
        long overdueDeadlines = deadlines.stream()
                .filter(deadline -> deadline.getStatus() == DeadlineStatus.OVERDUE)
                .count();
        long courseCount = deadlines.stream()
                .map(Deadline::getTargetClass)
                .filter(targetClass -> targetClass != null && !targetClass.isBlank())
                .distinct()
                .count();

        model.addAttribute("user", currentUser);
        model.addAttribute("deadlines", deadlines);
        model.addAttribute("upcomingDeadlines", upcomingDeadlines);
        model.addAttribute("totalDeadlines", deadlines.size());
        model.addAttribute("completedDeadlines", completedDeadlines);
        model.addAttribute("overdueDeadlines", overdueDeadlines);
        model.addAttribute("courseCount", courseCount);
        model.addAttribute("totalStudents", userService.countByRole(Role.STUDENT));
        model.addAttribute("totalFaculty", userService.countByRole(Role.FACULTY));
        model.addAttribute("completionRate", deadlines.isEmpty() ? 0 : Math.round(completedDeadlines * 100.0 / deadlines.size()));

        return "dean/dashboard";
    }
}
