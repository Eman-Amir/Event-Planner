package com.example.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.R;
import com.example.databinding.ItemEventBinding;
import com.example.model.Event;
import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<Event> originalList = new ArrayList<>();
    private final List<Event> displayedList = new ArrayList<>();
    private final OnEventActionListener actionListener;

    public interface OnEventActionListener {
        void onEdit(Event event);
        void onDelete(Event event);
    }

    public EventAdapter(OnEventActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setEvents(List<Event> events) {
        this.originalList.clear();
        if (events != null) {
            this.originalList.addAll(events);
        }
        filter(""); // Reset filtration
    }

    public void filter(String query) {
        displayedList.clear();
        if (query == null || query.trim().isEmpty()) {
            displayedList.addAll(originalList);
        } else {
            String q = query.toLowerCase().trim();
            for (Event e : originalList) {
                if (e.getTitle().toLowerCase().contains(q) ||
                    e.getDescription().toLowerCase().contains(q) ||
                    e.getLocation().toLowerCase().contains(q) ||
                    e.getCategory().toLowerCase().contains(q)) {
                    displayedList.add(e);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEventBinding binding = ItemEventBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EventViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        holder.bind(displayedList.get(position));
    }

    @Override
    public int getItemCount() {
        return displayedList.size();
    }

    class EventViewHolder extends RecyclerView.ViewHolder {
        private final ItemEventBinding binding;

        public EventViewHolder(@NonNull ItemEventBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Event event) {
            binding.tvTitle.setText(event.getTitle());
            binding.tvDescription.setText(event.getDescription());
            binding.tvDate.setText(event.getDate());
            binding.tvTime.setText(event.getTime());
            binding.tvLocation.setText(event.getLocation());
            binding.tvCategory.setText(event.getCategory());

            // Bind Category Themes programmatically to the Views
            Context context = itemView.getContext();
            int colorRes;
            String cat = event.getCategory() == null ? "" : event.getCategory().toLowerCase();
            switch (cat) {
                case "conference":
                    colorRes = R.color.cat_conference;
                    break;
                case "social":
                    colorRes = R.color.cat_social;
                    break;
                case "workshop":
                    colorRes = R.color.cat_workshop;
                    break;
                case "meeting":
                default:
                    colorRes = R.color.cat_meeting;
                    break;
            }

            int colorValue = ContextCompat.getColor(context, colorRes);
            ColorStateList csl = ColorStateList.valueOf(colorValue);

            // Dynamically tint the category pillar badge background
            binding.tvCategory.setBackgroundTintList(csl);

            // Dynamically tint the left side indicator view stripe
            binding.categoryIndicator.setBackgroundColor(colorValue);

            // Set up Click Listeners
            binding.btnEdit.setOnClickListener(v -> actionListener.onEdit(event));
            binding.btnDelete.setOnClickListener(v -> actionListener.onDelete(event));
        }
    }
}
