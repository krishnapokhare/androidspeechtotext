package com.kpokhare.offlinespeechrecognizer;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@IgnoreExtraProperties
public class Conversation implements Comparable<Conversation> {
    public String ID;
    public String Content;
    public String LanguageCode;
    public String LanguageName;
    private Date CreatedDate;

    public Conversation() {

    }

    public Conversation(String id, String content, String languageCode, String languageName) {
        ID = id;
        Content = content;
        LanguageCode = languageCode;
        LanguageName = languageName;
        CreatedDate = Calendar.getInstance().getTime();
    }

    public Date getCreatedDate() {
        return CreatedDate;
    }

    public void setCreatedDate(Date createdDate) {
        CreatedDate = createdDate;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", ID);
        result.put("content", Content);

        return result;
    }

    @Override
    public int compareTo(@NonNull Conversation o) {

        if (o == null || o.CreatedDate == null || this.CreatedDate == null) {
            return 0;
        }
        Log.i("compareToMethod", this.Content + "::::" + this.CreatedDate.toString() + "::::" + this.Content + "::::" + o.CreatedDate);
        if (this.CreatedDate.after(o.CreatedDate)) {
            Log.i("compareToMethod", "1");
            return 1;
        } else if (this.CreatedDate.before(o.CreatedDate)) {
            Log.i("compareToMethod", "0");
            return 0;
        } else {
            Log.i("compareToMethod", "1");
            return 1;
        }
    }
}
