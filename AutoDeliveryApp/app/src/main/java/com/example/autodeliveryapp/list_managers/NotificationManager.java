package com.example.autodeliveryapp.list_managers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autodeliveryapp.R;
import com.example.autodeliveryapp.data.NotificationItem;

import java.util.List;

public class NotificationManager extends RecyclerView.Adapter<NotificationManager.ViewHolder> {

    private final Context              context;
    private final List<NotificationItem> list;

    public NotificationManager(Context context, List<NotificationItem> list) {
        this.context = context;
        this.list    = list;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = list.get(position);
        holder.tvIcon.setText(item.getIcon());
        holder.tvTitle.setText(item.getTitle());
        holder.tvMessage.setText(item.getMessage());
        holder.tvTime.setText(item.getTime());
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvIcon, tvTitle, tvMessage, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon    = itemView.findViewById(R.id.tvIcon);
            tvTitle   = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime    = itemView.findViewById(R.id.tvTime);
        }
    }
}