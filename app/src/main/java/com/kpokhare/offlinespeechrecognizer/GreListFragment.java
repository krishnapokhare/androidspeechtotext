package com.kpokhare.offlinespeechrecognizer;


import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import static com.kpokhare.offlinespeechrecognizer.VoiceRecognitionActivity.DEVICE_ID;


/**
 * A simple {@link Fragment} subclass.
 */
public class GreListFragment extends Fragment {
    ArrayList<GreWord> GreWords = new ArrayList<GreWord>();
    RecyclerView greWordsRecyclerView;
    DatabaseReference GreWordListDB;
    static final String LOG_TAG = "GreListFragmentActivity";
    GreWordsAdapter greWordsAdapter;

    public GreListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GreWord word = new GreWord("aggravate");
        word.addSynonyms("make worse");
        word.addSynonyms("irritate");
//        GreWords.add(word);
//        word = new GreWord("entice");
//        word.addSynonyms("attract");
//        word.addSynonyms("lure");
//        GreWords.add(word);
//        word = new GreWord("endeavor");
//        word.addSynonyms("to make an effort");
//        word.addSynonyms("to try very hard");
//        GreWords.add(word);
//        word = new GreWord("affinity");
//        word.addSynonyms("close connection");
//        word.addSynonyms("relationship");
//        GreWords.add(word);
        GreWordListDB = FirebaseDatabase.getInstance().getReference("GRE_Words_List");
        UUID uniqueID = UUID.randomUUID();
        GreWordListDB.child(DEVICE_ID).child(uniqueID.toString()).setValue(word);
        InitializeList();
        getActivity().setTitle("GRE Words List");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        greWordsAdapter = new GreWordsAdapter(GreWords);
        View view = inflater.inflate(R.layout.fragment_gre_list, container, false);
        greWordsRecyclerView = (RecyclerView) view.findViewById(R.id.cardView);
        greWordsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager MyLayoutManager = new LinearLayoutManager(getActivity());
        MyLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (greWordsRecyclerView != null) {
            greWordsRecyclerView.setAdapter(greWordsAdapter);
        }
        greWordsRecyclerView.setLayoutManager(MyLayoutManager);

        return view;
    }

    public class GreWordsAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private ArrayList<GreWord> list;

        public GreWordsAdapter(ArrayList<GreWord> Data) {
            list = Data;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycle_items, parent, false);
            MyViewHolder holder = new MyViewHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
//            Log.i("Name", list.get(position).getName());
            GreWord word = list.get(position);
            holder.titleTextView.setText(word.getName());
            holder.synonymsTextView.setText(TextUtils.join(", ", word.getSynonyms()));
//            Log.i("TextViewValue", holder.titleTextView.getText().toString());
//            holder.coverImageView.setImageResource(list.get(position).getImageResourceId());
//            holder.coverImageView.setTag(list.get(position).getImageResourceId());
//            holder.likeImageView.setTag(R.drawable.ic_like);

        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        public TextView titleTextView;
        public TextView synonymsTextView;

        public MyViewHolder(View v) {
            super(v);
            titleTextView = (TextView) v.findViewById(R.id.titleTextView);
            titleTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getContext(), titleTextView.getText().toString(), Toast.LENGTH_SHORT).show();
                }
            });

            synonymsTextView = (TextView) v.findViewById(R.id.synonymsTextView);

//            coverImageView = (ImageView) v.findViewById(R.id.coverImageView);
//            likeImageView = (ImageView) v.findViewById(R.id.likeImageView);
//            shareImageView = (ImageView) v.findViewById(R.id.shareImageView);
//            likeImageView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//
//                    int id = (int)likeImageView.getTag();
//                    if( id == R.drawable.ic_like){
//
//                        likeImageView.setTag(R.drawable.ic_liked);
//                        likeImageView.setImageResource(R.drawable.ic_liked);
//
//                        Toast.makeText(getActivity(),titleTextView.getText()+" added to favourites",Toast.LENGTH_SHORT).show();
//
//                    }else{
//
//                        likeImageView.setTag(R.drawable.ic_like);
//                        likeImageView.setImageResource(R.drawable.ic_like);
//                        Toast.makeText(getActivity(),titleTextView.getText()+" removed from favourites",Toast.LENGTH_SHORT).show();
//
//
//                    }
//
//                }
//            });


//            shareImageView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                    Uri imageUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE +
//                            "://" + getResources().getResourcePackageName(coverImageView.getId())
//                            + '/' + "drawable" + '/' + getResources().getResourceEntryName((int)coverImageView.getTag()));
//
//
//                    Intent shareIntent = new Intent();
//                    shareIntent.setAction(Intent.ACTION_SEND);
//                    shareIntent.putExtra(Intent.EXTRA_STREAM,imageUri);
//                    shareIntent.setType("image/jpeg");
//                    startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));
//
//
//
//                }
//            });


        }
    }

    public void InitializeList() {

        GreWordListDB.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.i(LOG_TAG, "Data changed in Database");
                Log.i(LOG_TAG, DEVICE_ID);
                Log.i(LOG_TAG, String.valueOf(dataSnapshot.getChildrenCount()));
                Log.i(LOG_TAG, String.valueOf(dataSnapshot.child(DEVICE_ID).getChildrenCount()));
                GreWords.clear();
                for (DataSnapshot ds : dataSnapshot.child(DEVICE_ID).getChildren()) {
                    GreWord greWord = ds.getValue(GreWord.class);
                    Log.i(LOG_TAG, greWord.getName());
                    GreWords.add(greWord);
                }

                greWordsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(LOG_TAG, "loadPost:onCancelled", databaseError.toException());
            }
        });
    }

}
