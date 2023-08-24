package com.example.demo.Controller;

import com.example.demo.Model.User;
import com.example.demo.Service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAll() {
        System.out.println("First commit");
        System.out.println("Release 2");
        List<User> employeeDTOList = userService.getAllUsers();
        return ResponseEntity.ok(employeeDTOList);
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllBy() {
        List<User> employeeDTOList = userService.getAllUsers();
        System.out.println("2");
        return ResponseEntity.ok(employeeDTOList);
    }
}
