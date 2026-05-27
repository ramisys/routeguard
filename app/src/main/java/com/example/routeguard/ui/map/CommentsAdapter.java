package com.example.routeguard.ui.map;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routeguard.R;
import com.example.routeguard.data.model.Comment;

import java.util.ArrayList;
import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private final List<Comment> comments = new ArrayList<>();

    public void setComments(List<Comment> newComments) {
        comments.clear();
        comments.addAll(newComments);
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        comments.add(0, comment);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvTime;
        private final TextView tvText;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCommenterName);
            tvTime = itemView.findViewById(R.id.tvCommentTime);
            tvText = itemView.findViewById(R.id.tvCommentText);
        }

        public void bind(Comment comment) {
            tvName.setText(comment.getUserName());
            tvText.setText(comment.getText());
            
            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    comment.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            tvTime.setText(relativeTime);
        }
    }
}
