package com.example.routeguard.ui.settings;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.routeguard.R;
import com.example.routeguard.data.model.ErrorLog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ErrorLogAdapter extends RecyclerView.Adapter<ErrorLogAdapter.ViewHolder> {

    private List<ErrorLog> errorLogs = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public void setLogs(List<ErrorLog> logs) {
        this.errorLogs = logs;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_error_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ErrorLog log = errorLogs.get(position);
        holder.tvTag.setText(log.getTag());
        holder.tvTime.setText(dateFormat.format(new Date(log.getTimestamp())));
        holder.tvMessage.setText(log.getMessage());
        holder.tvStackTrace.setText(log.getStackTrace());
    }

    @Override
    public int getItemCount() {
        return errorLogs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTag, tvTime, tvMessage, tvStackTrace;

        ViewHolder(View itemView) {
            super(itemView);
            tvTag = itemView.findViewById(R.id.tvErrorTag);
            tvTime = itemView.findViewById(R.id.tvErrorTime);
            tvMessage = itemView.findViewById(R.id.tvErrorMessage);
            tvStackTrace = itemView.findViewById(R.id.tvStackTrace);
        }
    }
}