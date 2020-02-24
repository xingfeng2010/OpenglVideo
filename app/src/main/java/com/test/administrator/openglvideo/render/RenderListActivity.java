package com.test.administrator.openglvideo.render;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.test.administrator.openglvideo.R;
import com.test.administrator.openglvideo.RenderActivity;

public class RenderListActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private ListView mListView;
    private LayoutInflater mLayoutInflator;
    private String[] classes = new String[] {
            "MapRender",
            "VideoRender",
            "CameraRender",
            "EasyVideoRender",
            "CustomRerder",
            "PicFilterRerder",
            "EmbossRerder",
            "SimpleRerder",
            "MyLearnRerder"
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_render_list);

        mListView = findViewById(R.id.list_view);

        mListView.setAdapter(new MyBaseAdapter());
        mListView.setOnItemClickListener(this);

        mLayoutInflator = LayoutInflater.from(this);
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(RenderListActivity.this, RenderActivity.class);
        intent.putExtra("rendername",classes[i]);
        RenderListActivity.this.startActivity(intent);
    }

    private class MyBaseAdapter extends BaseAdapter {
        private String[] classDescription = new String[] {
                "MapRender",
                "VideoRender",
                "CameraRender",
                "EasyVideoRender",
                "CustomRerder",
                "PicFilterRerder",
                "EmbossRerder",
                "SimpleRerder",
                "MyLearnRerder"
        };

        @Override
        public int getCount() {
            return classDescription.length;
        }

        @Override
        public String getItem(int i) {
            return classDescription[i];
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder viewHolder;
            if (view == null) {
                viewHolder = new ViewHolder();
                view = mLayoutInflator.inflate(R.layout.class_item,parent, false);
                viewHolder.textView = (TextView) view.findViewById(R.id.item_tv);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder)view.getTag();
            }

            viewHolder.textView.setText(classDescription[position]);

            return view;
        }
    }

    private static class ViewHolder {
        private TextView textView;
    }
}
