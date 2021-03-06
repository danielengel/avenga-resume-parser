package com.resume.parser.controller;

import com.google.gson.JsonObject;
import com.resume.parser.service.ParserService;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class WelcomeController {

    private final ParserService parserService;

    public WelcomeController(ParserService parserService) {
        this.parserService = parserService;
    }

    @GetMapping("/")
    public String main(Model model) {
        return "welcome";
    }

    @PostMapping("/")
    public String mainWithParam(@RequestParam MultipartFile resume, Model model) throws Exception {

        JsonObject parsedResume = parserService.parseResume(resume);

        model.addAttribute("parsedResume", parsedResume);

        return "welcome"; //view
    }

}