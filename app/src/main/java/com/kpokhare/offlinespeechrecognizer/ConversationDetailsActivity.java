package com.kpokhare.offlinespeechrecognizer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class ConversationDetailsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation_details);
        TextView textView = findViewById(R.id.textView_conversationDetails);
        textView.setText(getIntent().getStringExtra("Conv_Content"));
        setTitle("Details");
    }
}
