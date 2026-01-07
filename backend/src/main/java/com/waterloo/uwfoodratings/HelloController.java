package com.waterloo.uwfoodratings;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String sayHello() {
        return "Welcome to the UW Food Ratings API! Go Warriors!";
    }
}