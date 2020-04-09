package com.resume.parser.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.tika.exception.TikaException;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;


import gate.util.GateException;

@Service
public class ParserService {

    @Autowired
    private ResumeParserProgram resumeParserProgram;

    public JSONObject parseResume(MultipartFile file) {
        String uploadedFolder = System.getProperty("user.dir");
        if (uploadedFolder != null && !uploadedFolder.isEmpty()) {
            uploadedFolder += "/Resumes/";
        } else
            throw new RuntimeException("User Directory not found");
        File tikkaConvertedFile = null;
        byte[] bytes = null;
        try {
            bytes = file.getBytes();
        } catch (IOException exception) {
            throw new RuntimeException(exception.getMessage());
        }
        Path path = null;
        try {
            path = Paths.get(uploadedFolder + file.getOriginalFilename());
            if (!Files.exists(path.getParent()))
                Files.createDirectories(path.getParent());
            path = Files.write(path, bytes);
        } catch (IOException exception) {
            throw new RuntimeException(exception.getMessage());

        }
        try {
            tikkaConvertedFile = resumeParserProgram.parseToHTMLUsingApacheTikka(path.toAbsolutePath().toString());
        } catch (IOException | SAXException | TikaException exception) {
            throw new RuntimeException(exception.getMessage());

        }
        JSONObject parsedJSON = null;
        if (tikkaConvertedFile != null) {
            try {
                parsedJSON = resumeParserProgram.loadGateAndAnnie(tikkaConvertedFile);
            } catch (GateException | IOException exception) {
                throw new RuntimeException(exception.getMessage());

            }
        }
        return parsedJSON;
    }

}
