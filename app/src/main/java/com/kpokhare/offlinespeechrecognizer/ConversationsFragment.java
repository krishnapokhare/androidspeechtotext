package com.kpokhare.offlinespeechrecognizer;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.kpokhare.offlinespeechrecognizer.VoiceRecognitionActivity.DEVICE_ID;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConversationsFragment extends Fragment {
    RecyclerView conversationsRecyclerView;
    private List<Conversation> conversationList;
    DatabaseReference conversationDB;
    static final String LOG_TAG = "ConversationsActivity";
    private ConversationsAdapter convAdapter;

    public ConversationsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreate called");
        super.onCreate(savedInstanceState);
        InitializeList();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(LOG_TAG, "onCreateView called");
        View view = inflater.inflate(R.layout.fragment_conversations, container, false);
        conversationsRecyclerView = view.findViewById(R.id.cardView_conversation);
        if (conversationsRecyclerView == null) {
            Log.i(LOG_TAG, "RecyclerView is null");
        }
        conversationsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager MyLayoutManager = new LinearLayoutManager(getActivity());
        MyLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (conversationList.size() > 0 & conversationsRecyclerView != null) {
            conversationsRecyclerView.setAdapter(new ConversationsAdapter(conversationList));
        }
        conversationsRecyclerView.setLayoutManager(MyLayoutManager);

        convAdapter = new ConversationsAdapter(conversationList, new ConversationsAdapter.ConversationsAdapterListener() {
            @Override
            public void deleteOnClick(View v, int position) {
                String id = conversationList.get(position).ID;
                conversationDB.child(DEVICE_ID).child(id).removeValue();
            }

            @Override
            public void textViewOnClick(View v, int position) {
                String selectedItemContent = conversationList.get(position).Content;
                Intent ConversationDetailsActivity = new Intent(getActivity(), com.kpokhare.offlinespeechrecognizer.ConversationDetailsActivity.class);
                ConversationDetailsActivity.putExtra("Conv_Content", selectedItemContent);
                startActivity(ConversationDetailsActivity);
            }
        });
        conversationsRecyclerView.setAdapter(convAdapter);

        return view;
    }

    public void InitializeList() {
        conversationList = new ArrayList<>();
        conversationDB = FirebaseDatabase.getInstance().getReference("Conversations");
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
                Collections.sort(conversationList);
                convAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(LOG_TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

}
