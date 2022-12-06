package com.example.demo.Service;

import com.example.demo.Model.User;
import com.example.demo.Repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User findByAddress(String address) {
        return userRepository.findByAddress(address);
    }

    public String getUserAddress(User user) {
        return user.getAddress();
    }
}
