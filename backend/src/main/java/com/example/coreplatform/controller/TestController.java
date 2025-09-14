package com.example.coreplatform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

  @GetMapping("/test")
  public Map<String, String> getTestMessage() {
    return Map.of("message", "Backend is working!");
  }
}