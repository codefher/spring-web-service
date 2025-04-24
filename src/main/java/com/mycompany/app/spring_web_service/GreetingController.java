package com.mycompany.app.spring_web_service;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GreetingController {

    @GetMapping("/greeting")
    public Map<String, Object> greeting() {
        Map<String,Object> resp = new HashMap<>();
        resp.put("message", "¡Hola desde Spring Boot!");
        resp.put("timestamp", LocalDateTime.now().toString());
        return resp;
    }
}