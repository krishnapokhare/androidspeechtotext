package com.kpokhare.offlinespeechrecognizer;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

public class ConversationsAdapter extends RecyclerView.Adapter<ConversationsAdapter.ViewHolder> {
    private List<Conversation> ConversationList;
    public ConversationsAdapterListener itemClickListener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTextView, mLangNameTextView;
        public ImageButton mDeleteConversation;
        public View mView;

        public ViewHolder(View v) {
            super(v);
            mView = v;
            mTextView = v.findViewById(R.id.textView_convText);
            mLangNameTextView = v.findViewById(R.id.textView_convLangName);
            mDeleteConversation = v.findViewById(R.id.button_deleteConversation);

            mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickListener.textViewOnClick(v, getAdapterPosition());
                }
            });

            mDeleteConversation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    itemClickListener.deleteOnClick(v, getAdapterPosition());
                }
            });
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ConversationsAdapter(List<Conversation> conversationList) {
        ConversationList = conversationList;
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ConversationsAdapter(List<Conversation> conversationList, ConversationsAdapterListener listener) {
        ConversationList = conversationList;
        itemClickListener = listener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ConversationsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.conversations_content, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        Conversation conversation=ConversationList.get(position);
        holder.mTextView.setText(conversation.Content);
        holder.mLangNameTextView.setText(conversation.LanguageName);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return ConversationList.size();
    }

    public interface ConversationsAdapterListener {
        void deleteOnClick(View v, int position);

        void textViewOnClick(View v, int position);
    }
}

