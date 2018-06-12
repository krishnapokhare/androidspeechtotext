package com.kpokhare.offlinespeechrecognizer.viewmodels;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.kpokhare.offlinespeechrecognizer.Conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConversationsViewModel extends ViewModel {
    DatabaseReference conversationDB = FirebaseDatabase.getInstance().getReference("Conversations");
    private String DEVICE_ID;
    //    private final FirebaseQueryLiveData liveData = new FirebaseQueryLiveData(HOT_STOCK_REF);
    private MutableLiveData<List<Conversation>> conversations;

    public ConversationsViewModel(String DEVICE_ID) {
        this.DEVICE_ID = DEVICE_ID;
    }

    @NonNull
    public LiveData<List<Conversation>> getConversations() {
        if (conversations == null) {
            conversations = new MutableLiveData<List<Conversation>>();
            loadConversations();
        }
        return conversations;
    }

    private void loadConversations() {
        conversationDB.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<Conversation> conversationList = new ArrayList<Conversation>();
                for (DataSnapshot ds : dataSnapshot.child(DEVICE_ID).getChildren()) {
                    Conversation conversation = ds.getValue(Conversation.class);
                    conversationList.add(conversation);
                }
                Collections.sort(conversationList);
                conversations.setValue(conversationList);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ViewModelError", databaseError.getDetails());
                conversations.setValue(null);
            }
        });
    }
}
