package com.example.autodeliveryapp.list_managers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.autodeliveryapp.R;
import com.example.autodeliveryapp.data.HistoryItem;

import java.util.List;

public class HistoryManager extends RecyclerView.Adapter<HistoryManager.ViewHolder> {

    private final Context       context;
    private final List<HistoryItem> list;

    public HistoryManager(Context context, List<HistoryItem> list) {
        this.context = context;
        this.list    = list;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = list.get(position);

        holder.tvOrderId.setText(item.getOrderId());
        holder.tvPickup.setText(item.getPickup());
        holder.tvDropoff.setText(item.getDropoff());
        holder.tvDate.setText(item.getDate());
        holder.tvPrice.setText(item.getPrice());
        holder.tvStatus.setText(item.getStatus());

        // Đổi màu badge theo trạng thái
        switch (item.getStatus()) {
            case "Hoàn thành":
                holder.tvStatus.setTextColor(context.getColor(R.color.status_completed));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_tag_green);
                break;
            case "Đang giao":
                holder.tvStatus.setTextColor(context.getColor(R.color.status_processing));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_tag_orange);
                break;
            case "Đã hủy":
                holder.tvStatus.setTextColor(context.getColor(R.color.status_cancelled));
                holder.tvStatus.setBackgroundResource(R.drawable.bg_tag_red);
                break;
        }
    }

    @Override
    public int getItemCount() { return list.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvPickup, tvDropoff, tvStatus, tvDate, tvPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tvOrderId);
            tvPickup  = itemView.findViewById(R.id.tvPickup);
            tvDropoff = itemView.findViewById(R.id.tvDropoff);
            tvStatus  = itemView.findViewById(R.id.tvStatus);
            tvDate    = itemView.findViewById(R.id.tvDate);
            tvPrice   = itemView.findViewById(R.id.tvPrice);
        }
    }
}