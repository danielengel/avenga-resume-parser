package com.resume.parser.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import gate.*;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.ToXMLContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Iterator;

import static gate.Utils.stringFor;

@Service
public class ParserService {


    public JsonObject parseResume(MultipartFile file) throws Exception {
        ContentHandler handler = new ToXMLContentHandler();
        new AutoDetectParser().parse(new ByteArrayInputStream(file.getBytes()), handler, new Metadata());
        String tikkaConvertedFileToHtml = handler.toString();

        System.setProperty("gate.site.config", System.getProperty("user.dir") + "/GATEFiles/gate.xml");
        if (Gate.getGateHome() == null)
            Gate.setGateHome(new File(System.getProperty("user.dir") + "/GATEFiles"));
        if (Gate.getPluginsHome() == null)
            Gate.setPluginsHome(new File(System.getProperty("user.dir") + "/GATEFiles/plugins"));
        Gate.init();

        Annie annie = new Annie();
        annie.initAnnie();

        Corpus corpus = Factory.newCorpus("Annie corpus");
        FeatureMap params = Factory.newFeatureMap();
        params.put("preserveOriginalContent", Boolean.TRUE);
        params.put("collectRepositioningInfo", Boolean.TRUE);
        Document resume = Factory.newDocument(tikkaConvertedFileToHtml);
        corpus.add(resume);

        annie.setCorpus(corpus);
        annie.execute();

        Iterator<Document> iter = corpus.iterator();
        JsonObject parsedJSON = new JsonObject();
        if (iter.hasNext()) {
            JsonObject profileJSON = new JsonObject();
            Document doc = iter.next();
            AnnotationSet defaultAnnotSet = doc.getAnnotations();

            AnnotationSet curAnnSet;
            Iterator<Annotation> it;
            Annotation currAnnot;

            // Name
            curAnnSet = defaultAnnotSet.get("NameFinder");
            if (curAnnSet.iterator().hasNext()) {
                currAnnot = curAnnSet.iterator().next();
                String gender = (String) currAnnot.getFeatures().get("gender");
                if (gender != null && gender.length() > 0) {
                    profileJSON.addProperty("gender", gender);
                }

                // Needed name Features
                JsonObject nameJson = new JsonObject();
                String[] nameFeatures = new String[]{"firstName", "middleName", "surname"};
                for (String feature : nameFeatures) {
                    String s = (String) currAnnot.getFeatures().get(feature);
                    if (s != null && s.length() > 0) {
                        nameJson.addProperty(feature, s);
                    }
                }
                profileJSON.add("name", nameJson);
            }

            curAnnSet = defaultAnnotSet.get("TitleFinder");
            if (curAnnSet.iterator().hasNext()) {
                currAnnot = curAnnSet.iterator().next();
                String title = stringFor(doc, currAnnot);
                if (title != null && title.length() > 0) {
                    profileJSON.addProperty("title", title);
                }
            }

            String[] annSections = new String[]{"EmailFinder", "AddressFinder", "PhoneFinder", "URLFinder"};
            String[] annKeys = new String[]{"email", "address", "phone", "url"};
            for (short i = 0; i < annSections.length; i++) {
                String annSection = annSections[i];
                curAnnSet = defaultAnnotSet.get(annSection);
                it = curAnnSet.iterator();
                JsonArray sectionArray = new JsonArray();
                while (it.hasNext()) {
                    currAnnot = it.next();
                    String s = stringFor(doc, currAnnot);
                    if (s != null && s.length() > 0) {
                        sectionArray.add(s);
                    }
                }
                if (sectionArray.size() > 0) {
                    profileJSON.add(annKeys[i], sectionArray);
                }
            }
            if (!profileJSON.isJsonNull()) {
                parsedJSON.add("basics", profileJSON);
            }

            String[] otherSections = new String[]{"summary", "education_and_training", "skills", "accomplishments",
                    "awards", "credibility", "extracurricular", "misc"};
            for (String otherSection : otherSections) {
                curAnnSet = defaultAnnotSet.get(otherSection);
                it = curAnnSet.iterator();
                JsonArray subSections = new JsonArray();
                while (it.hasNext()) {
                    JsonObject subSection = new JsonObject();
                    currAnnot = it.next();
                    String key = (String) currAnnot.getFeatures().get("sectionHeading");
                    String value = stringFor(doc, currAnnot);
                    if (!StringUtils.isBlank(key) && !StringUtils.isBlank(value)) {
                        subSection.addProperty(key, value);
                    }
                    if (!subSection.isJsonNull()) {
                        subSections.add(subSection);
                    }
                }
                if (!subSections.isJsonNull()) {
                    parsedJSON.add(otherSection, subSections);
                }
            }

            curAnnSet = defaultAnnotSet.get("work_experience");
            it = curAnnSet.iterator();
            JsonArray workExperiences = new JsonArray();
            while (it.hasNext()) {
                JsonObject workExperience = new JsonObject();
                currAnnot = it.next();
                String key = (String) currAnnot.getFeatures().get("sectionHeading");
                if (key.equals("work_experience_marker")) {
                    String[] annotations = new String[]{"date_start", "date_end", "jobtitle", "organization"};
                    for (String annotation : annotations) {
                        String v = (String) currAnnot.getFeatures().get(annotation);
                        if (!StringUtils.isBlank(v)) {
                            workExperience.addProperty(annotation, v);
                        }
                    }
                    key = "text";

                }
                String value = stringFor(doc, currAnnot);
                if (!StringUtils.isBlank(key) && !StringUtils.isBlank(value)) {
                    workExperience.addProperty(key, value);
                }
                if (!workExperience.isJsonNull()) {
                    workExperiences.add(workExperience);
                }

            }
            if (!workExperiences.isJsonNull()) {
                parsedJSON.add("work_experience", workExperiences);
            }

        }
        return parsedJSON;
    }
}
