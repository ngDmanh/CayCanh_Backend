package com.caycanh.caycanh_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CayCanhController {
    @GetMapping("/hello")
    String sayHello(){
        return "Hello spring boot";
    }
}
