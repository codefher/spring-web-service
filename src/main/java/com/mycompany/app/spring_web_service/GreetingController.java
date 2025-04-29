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
        resp.put("message", "¡Hola desde Spring Boot, desde el main!");
        resp.put("timestamp", LocalDateTime.now().toString());
        return resp;
    }

    // —––––––––– Code Smell: duplicación de lógica —–––––––––
    @GetMapping("/greetingDuplicado")
    public Map<String,Object> greetingDuplicado() {
        // exactamente igual a greeting()
        Map<String,Object> resp = new HashMap<>();
        resp.put("message", "¡Hola desde Spring Boot, pruebita deploy!");
        resp.put("timestamp", LocalDateTime.now().toString());
        return resp;
    }

    // —––––––––– Bug intencional: comparación de Strings con == —–––––––––
    @GetMapping("/bugExample")
    public boolean bugExample() {
        String a = "test", b = new String("test");
        return (a == b);  // Sonar marcará esto como bug: comparar Strings con '=='
    }
}
