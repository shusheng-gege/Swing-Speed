package com.opposport.badminton.vibrationapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {
    private DatabaseHelper dbHelper;
    private ListView listView;
    private TextView emptyTextView;
    private HistoryAdapter adapter;
    private List<TrainingRecord> records;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        dbHelper = new DatabaseHelper(this);

        listView = findViewById(R.id.historyListView);
        emptyTextView = findViewById(R.id.emptyTextView);
        Button backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadHistory();
    }

    private void loadHistory() {
        records = dbHelper.getAllRecords();

        if (records.isEmpty()) {
            emptyTextView.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            return;
        }

        emptyTextView.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);

        adapter = new HistoryAdapter();
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            TrainingRecord rec = records.get(position);
            dbHelper.deleteRecord(rec.getId());
            loadHistory();
            return true;
        });
    }

    private class HistoryAdapter extends BaseAdapter {
        private final SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

        @Override
        public int getCount() {
            return records.size();
        }

        @Override
        public Object getItem(int position) {
            return records.get(position);
        }

        @Override
        public long getItemId(int position) {
            return records.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history, parent, false);
                holder = new ViewHolder();
                holder.date = convertView.findViewById(R.id.historyDate);
                holder.swings = convertView.findViewById(R.id.historySwings);
                holder.avg = convertView.findViewById(R.id.historyAvg);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            TrainingRecord rec = records.get(position);
            holder.date.setText(sdf.format(new Date(rec.getTimestamp())));
            holder.swings.setText(String.valueOf(rec.getCount()));
            holder.avg.setText(String.format(Locale.getDefault(), "avg %.0f km/h", rec.getAvgSpeed()));

            return convertView;
        }
    }

    private static class ViewHolder {
        TextView date;
        TextView swings;
        TextView avg;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}