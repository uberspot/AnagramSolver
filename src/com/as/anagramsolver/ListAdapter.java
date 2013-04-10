package com.as.anagramsolver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ListAdapter extends BaseAdapter {

	private LayoutInflater layoutInflater;
	
    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    private String[] items;

    public ListAdapter(Context context, String[] items) {
        this.items = items;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
	public boolean hasStableIds() {
        return true;
    }

	public int getCount() {
		return items.length;
	}

	public Object getItem(int itemPosition) {
		return items[(itemPosition<items.length)?itemPosition:items.length-1 ];
	}

	public long getItemId(int itemPosition) {
		return items[(itemPosition<items.length)?itemPosition:items.length-1 ].hashCode();
	}

	public View getView(int itemPosition, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		
        if (convertView == null) {
        	holder = new ViewHolder();
            convertView = layoutInflater.inflate(R.layout.word_layout, null);
	        holder.textView = (TextView) convertView.findViewById(R.id.wordTextView);
	        
	        convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.textView.setText(items[itemPosition]);
        return convertView;
	}
	
	public static class ViewHolder {
        public TextView textView;
    }
}
