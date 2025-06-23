package org.whu.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/hello")
public class HelloWorldController {
    @GetMapping("world")
    public String hello() {
        return "Hello World!";
    }
}
