package com.kpokhare.offlinespeechrecognizer;

import java.util.ArrayList;
import java.util.List;

public class GreWord {
    private String Name;
    private List<String> Synonyms;

    public GreWord() {
        this.Synonyms = new ArrayList<>();
    }

    public GreWord(String name) {
        this.Name = name;
        this.Synonyms = new ArrayList<String>();
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        this.Name = name;
    }

    public List<String> getSynonyms() {
        return Synonyms;
    }

    public void setSynonyms(List<String> synonyms) {
        this.Synonyms = synonyms;
    }

    public void addSynonyms(String synonym) {
        this.Synonyms.add(synonym);
    }
}
