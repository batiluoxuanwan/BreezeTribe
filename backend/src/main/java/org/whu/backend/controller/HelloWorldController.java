package org.whu.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.whu.backend.common.Result;

@RestController
@RequestMapping("api/hello")
public class HelloWorldController {
    @GetMapping("world")
    public Result<?> hello() {
        return Result.success("hello world!");
    }
}
