package com.deadlineiq.controller;

import com.deadlineiq.model.Role;
import com.deadlineiq.model.User;
import com.deadlineiq.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

/**
 * Controller handling user authentication, session creation, registration, and logout.
 */
@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(value = "registered", required = false) String registered,
                                @RequestParam(value = "logout", required = false) String logout,
                                @RequestParam(value = "redirect", required = false) String redirectUrl,
                                Model model,
                                HttpSession session) {
        // If already logged in, redirect straight to requested page or their dashboard
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            if (redirectUrl != null && !redirectUrl.isBlank() && !redirectUrl.equals("/login") && !redirectUrl.equals("/register")) {
                return "redirect:" + redirectUrl;
            }
            return redirectByRole(currentUser.getRole());
        }

        if (registered != null) {
            model.addAttribute("success", "Account created successfully! Please sign in.");
        }
        if (logout != null) {
            model.addAttribute("info", "You have been logged out securely.");
        }
        if (redirectUrl != null && !redirectUrl.isBlank()) {
            model.addAttribute("redirect", redirectUrl);
        }
        return "login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam("email") String email,
                               @RequestParam("password") String password,
                               @RequestParam(value = "redirect", required = false) String redirectUrl,
                               HttpSession session,
                               Model model) {
        Optional<User> authUser = userService.authenticate(email, password);

        if (authUser.isPresent()) {
            User user = authUser.get();
            session.setAttribute("currentUser", user);
            if (redirectUrl != null && !redirectUrl.isBlank() && !redirectUrl.equals("/login") && !redirectUrl.equals("/register")) {
                return "redirect:" + redirectUrl;
            }
            return redirectByRole(user.getRole());
        } else {
            model.addAttribute("error", "Invalid email or password. Please try again.");
            model.addAttribute("enteredEmail", email);
            if (redirectUrl != null && !redirectUrl.isBlank()) {
                model.addAttribute("redirect", redirectUrl);
            }
            return "login";
        }
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser != null) {
            return redirectByRole(currentUser.getRole());
        }
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/register")
    public String processRegister(@ModelAttribute("user") User user, Model model) {
        if (user.getRole() == null) {
            user.setRole(Role.STUDENT); // Default to Student
        }

        boolean success = userService.register(user);
        if (success) {
            return "redirect:/login?registered=true";
        } else {
            model.addAttribute("error", "An account with this email already exists!");
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/?logout=true";
    }

    private String redirectByRole(Role role) {
        switch (role) {
            case STUDENT:
                return "redirect:/student/dashboard";
            case FACULTY:
                return "redirect:/faculty/dashboard";
            case COORDINATOR:
                return "redirect:/coordinator/dashboard";
            case DEAN:
                return "redirect:/dean/dashboard";
            case ADMIN:
                return "redirect:/admin/dashboard";
            default:
                return "redirect:/";
        }
    }
}
