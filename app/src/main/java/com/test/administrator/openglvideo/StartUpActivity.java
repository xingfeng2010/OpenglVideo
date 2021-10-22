package com.test.administrator.openglvideo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.test.administrator.openglvideo.render.RenderListActivity;

import androidx.appcompat.app.AppCompatActivity;

public class StartUpActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{
    private ListView mListView;
    private LayoutInflater mLayoutInflator;
    private Class[] classes = new Class[] {
            RenderListActivity.class,
            DyanmicHeart.class,
            VideoEncodeActivity.class,
            VideoActivity.class,
            VideoCombineActivity.class
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        mListView = findViewById(R.id.list_view);

        mListView.setAdapter(new MyBaseAdapter());
        mListView.setOnItemClickListener(this);

        mLayoutInflator = LayoutInflater.from(this);

        checkPermission(this);
    }

    public static boolean checkPermission(
            Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, 1);

        }
        return false;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(StartUpActivity.this, classes[i]);
        StartUpActivity.this.startActivity(intent);
    }

    private class MyBaseAdapter extends BaseAdapter {
        private String[] classDescription = new String[] {
                "Render List",
                "DyanmicHeart",
                "VideoEncode",
                "MediaExtractor Play video",
                "VideoCombine"
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
