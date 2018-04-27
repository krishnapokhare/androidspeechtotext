package com.kpokhare.offlinespeechrecognizer;


import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class GreListFragment extends Fragment {
    ArrayList<GreWord> GreWords = new ArrayList<GreWord>();
    RecyclerView greWordsRecyclerView;


    public GreListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GreWords.add(new GreWord("Similar"));
        GreWords.add(new GreWord("very"));
        GreWords.add(new GreWord("true"));
        GreWords.add(new GreWord("Opportunity"));
        getActivity().setTitle("GRE Words List");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gre_list, container, false);
        greWordsRecyclerView = (RecyclerView) view.findViewById(R.id.cardView);
        greWordsRecyclerView.setHasFixedSize(true);
        LinearLayoutManager MyLayoutManager = new LinearLayoutManager(getActivity());
        MyLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        if (GreWords.size() > 0 & greWordsRecyclerView != null) {
            greWordsRecyclerView.setAdapter(new MyAdapter(GreWords));
        }
        greWordsRecyclerView.setLayoutManager(MyLayoutManager);

        return view;
    }

    public class MyAdapter extends RecyclerView.Adapter<MyViewHolder> {
        private ArrayList<GreWord> list;

        public MyAdapter(ArrayList<GreWord> Data) {
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
            Log.i("Name", list.get(position).getName());
            holder.titleTextView.setText(list.get(position).getName());
            Log.i("TextViewValue", holder.titleTextView.getText().toString());
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
        public ImageView coverImageView;
        public ImageView likeImageView;
        public ImageView shareImageView;

        public MyViewHolder(View v) {
            super(v);
            titleTextView = (TextView) v.findViewById(R.id.titleTextView);
            titleTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(getContext(), titleTextView.getText().toString(), Toast.LENGTH_SHORT).show();
                }
            });
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

}
