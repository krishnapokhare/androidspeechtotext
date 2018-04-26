package com.kpokhare.offlinespeechrecognizer;

import android.app.ActionBar;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.util.SortedList;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static com.kpokhare.offlinespeechrecognizer.VoiceRecognitionActivity.DEVICE_ID;

public class ConversationsActivity extends AppCompatActivity {
    private List<Conversation> conversationList;
    private RecyclerView convRecyclerView;
    private ConversationsAdapter convAdapter;
    private RecyclerView.LayoutManager convLayoutManager;
    static final String LOG_TAG = "ConversationsActivity";
    DatabaseReference conversationDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations);
        conversationDB = FirebaseDatabase.getInstance().getReference("Conversations");
        Log.i(LOG_TAG, "Database connected");
        convRecyclerView = (RecyclerView) findViewById(R.id.convRecyclerView);

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        convRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        convLayoutManager = new LinearLayoutManager(this);
        convRecyclerView.setLayoutManager(convLayoutManager);
        convRecyclerView.setItemAnimator(new DefaultItemAnimator());
        conversationList = new ArrayList<>();
        convAdapter = new ConversationsAdapter(conversationList, new ConversationsAdapter.ConversationsAdapterListener() {
            @Override
            public void deleteOnClick(View v, int position) {
                String id = conversationList.get(position).ID;
                conversationDB.child(DEVICE_ID).child(id).removeValue();
            }
        });
        convRecyclerView.setAdapter(convAdapter);
        Log.i(LOG_TAG, "Setup completed");
        //PrepareData();


        conversationDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(LOG_TAG, "Data changed in Database");
                Log.i(LOG_TAG, DEVICE_ID);
                conversationList.clear();
                for (DataSnapshot ds : dataSnapshot.child(DEVICE_ID).getChildren()) {
                    Conversation conversation = ds.getValue(Conversation.class);
                    Log.i(LOG_TAG, conversation.Content);
                    conversationList.add(conversation);
                }
                convAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(LOG_TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });

        ActionBar actionbar = getActionBar();
        if (actionbar != null) {
            //Setting up Action bar color using # color code.
            actionbar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ff2d9a59")));
        }
    }
}

