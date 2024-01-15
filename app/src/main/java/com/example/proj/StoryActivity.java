package com.example.proj;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class StoryActivity extends AppCompatActivity {
    private EditText findEditText;
    private TextView selectedText, storyText;
    private CheckBox drawingCheck, checkItem1, checkItem2, checkItem3;
    private ListView lv;
    private SQLiteDatabase db;
    private ArrayList<ImageItem> ls = new ArrayList<>();
    private ImageListAdapter adapter;
    private final String API_KEY = "";  // API key omitted.
    private final String URL = "https://api.textcortex.com/v1/texts/social-media-posts";
    private String selectedTags = "";
    private TextToSpeech tts;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story);

        findEditText = findViewById(R.id.findText);
        selectedText = findViewById(R.id.selectedText);
        storyText = findViewById(R.id.storyText);

        drawingCheck = findViewById(R.id.drawingCheck);
        checkItem1 = findViewById(R.id.checkItem1);
        checkItem2 = findViewById(R.id.checkItem2);
        checkItem3 = findViewById(R.id.checkItem3);

        lv = findViewById(R.id.listView);

        db = this.openOrCreateDatabase("Proj_Data", Context.MODE_PRIVATE, null);

        adapter = new ImageListAdapter(this, R.layout.list_item, ls);
        lv.setAdapter(adapter);

        performQuery(new View(this));
    }

    public void performQuery(View view) {
        Cursor c;
        ls.clear();
        adapter.notifyDataSetChanged();
        String findText = findEditText.getText().toString();

        if (drawingCheck.isChecked()) {
            c = db.rawQuery(
                "SELECT * " +
                    "FROM Image " +
                    "WHERE tags like ? " +
                    "UNION " +
                    "SELECT * " +
                    "FROM Drawing " +
                    "WHERE tags like ? " +
                    "ORDER BY stamp DESC;",
            new String[]{"%" + findText + "%", "%" + findText + "%"});

        } else {
            c = db.rawQuery(
                "SELECT * " +
                    "FROM Image " +
                    "WHERE tags like ? " +
                    "ORDER BY stamp DESC;",
            new String[]{"%" + findText + "%"});
        }

        if (c.getCount() == 0) return;

        c.moveToFirst();

        for (int i = 0; i < c.getCount(); i++) {
            if (i == 3) break;
            ls.add(new ImageItem(
                    BitmapFactory.decodeByteArray(c.getBlob(1), 0, c.getBlob(1).length),
                    c.getString(3),
                    c.getString(2)
            ));
            adapter.notifyDataSetChanged();
            c.moveToNext();
        }

        updateSelectedText(new View(this));
    }

    public void updateSelectedText(View view) {
        selectedTags = "";

        if (checkItem1.isChecked() && ls.size() >= 1) {
            selectedTags += ls.get(0).tags;
        }

        if (checkItem2.isChecked() && ls.size() >= 2) {
            if (checkItem1.isChecked()) selectedTags += ", ";
            selectedTags += ls.get(1).tags;
        }

        if (checkItem3.isChecked() && ls.size() >= 3) {
            if (checkItem1.isChecked() || checkItem2.isChecked()) selectedTags += ", ";
            selectedTags += ls.get(2).tags;
        }

        selectedText.setText("You selected: " + selectedTags);
    }

    public void generateStory(View view) {
        if (!(checkItem1.isChecked() || checkItem2.isChecked() || checkItem3.isChecked()) || selectedTags.equals("")) {
            Toast.makeText(this, "You must select tags!", Toast.LENGTH_SHORT).show();
            return;
        }

        updateSelectedText(new View(this));
        MediaPlayer.create(this, R.raw.fairy).start();

        try {
            makeHttpRequest();
        } catch (JSONException e) {
            Log.e("error", e.toString());
        }
    }

    private void makeHttpRequest() throws JSONException {
        JSONObject data = new JSONObject();
        data.put("context", "story");
        data.put("max_tokens", "90");
        data.put("mode", "twitter");
        data.put("model", "chat-sophos-1");
        data.put("keywords", new JSONArray(selectedTags.split(", ")));

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, URL, data, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                try {
                    storyText.setText(response.getJSONObject("data").getJSONArray("outputs").getJSONObject(0).get("text").toString());
                    Log.i("INFO", response.toString());
                    speak();
                } catch (JSONException e) {
                    Log.e("error", e.toString());
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error", new String(error.networkResponse.data));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + API_KEY);
                return headers;
            }
        };

        RequestQueue q = Volley.newRequestQueue(this);
        q.add(request);
    }

    public void speak() {
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                    tts.speak(storyText.getText().toString(), TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });
    }

    public void onClickBack(View view) {
        MediaPlayer.create(this, R.raw.suspense).start();
        startActivity(new Intent(this, MainActivity.class));
    }
}
