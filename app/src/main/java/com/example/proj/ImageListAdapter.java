package com.example.proj;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class ImageListAdapter extends ArrayAdapter<ImageItem> {
    public ImageListAdapter(@NonNull Context context, int resource, @NonNull List<ImageItem> objects) {
        super(context, resource, objects);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item, parent, false);
        }

        ImageItem currentItem = getItem(position);

        ImageView image = convertView.findViewById(R.id.listImageView);
        TextView tags = convertView.findViewById(R.id.listTagsView);
        TextView stamp = convertView.findViewById(R.id.listStampView);

        image.setImageBitmap(currentItem.image);
        tags.setText(currentItem.tags);
        stamp.setText(currentItem.stamp);

        return convertView;
    }
}
