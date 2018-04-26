package com.kpokhare.offlinespeechrecognizer;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@IgnoreExtraProperties
public class Conversation {
    public String ID;
    public String Content;
    public String LanguageCode;
    public String LanguageName;

    public Conversation() {

    }

    public Conversation(String id, String content, String languageCode, String languageName) {
        ID = id;
        Content = content;
        LanguageCode = languageCode;
        LanguageName = languageName;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", ID);
        result.put("content", Content);

        return result;
    }
}
