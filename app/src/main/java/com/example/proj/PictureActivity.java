package com.example.proj;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PictureActivity extends AppCompatActivity {
    private final String API_KEY = "";  // API key omitted.
    private File imageDir;
    private ImageView imageView;
    private SQLiteDatabase db;
    private EditText findEditText, tagsView;
    private ArrayList<ImageItem> ls = new ArrayList<>();
    private ImageListAdapter adapter;
    private ListView lv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picture);

        imageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        imageView = findViewById(R.id.topImageView);
        db = this.openOrCreateDatabase("Proj_Data", Context.MODE_PRIVATE, null);

        tagsView = findViewById(R.id.tagsText);
        findEditText = findViewById(R.id.findText);

        adapter = new ImageListAdapter(this, R.layout.list_item, ls);

        lv = findViewById(R.id.listView);
        lv.setAdapter(adapter);

        db.execSQL("CREATE TABLE IF NOT EXISTS Image (" +
                    "ID INTEGER PRIMARY KEY, " +
                    "picture TEXT, " +
                    "stamp TEXT, " +
                    "tags TEXT);"
        );

        Cursor c = db.rawQuery(
                "SELECT * " +
                    "FROM Image " +
                    "ORDER BY stamp DESC;",
                null);

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
    }

    public void startCamera(View view) {
        Intent camIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File f = new File(imageDir.getAbsolutePath(), "IMG_" + timeStamp + ".png");
        Uri u = FileProvider.getUriForFile(this, "com.example.fileproviderproj", f);
        camIntent.putExtra(MediaStore.EXTRA_OUTPUT, u);

        startActivityForResult(camIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String[] allFiles = imageDir.list();
            if (allFiles == null || allFiles.length == 0) return;
            Arrays.sort(allFiles, Collections.reverseOrder());

            String imagePath = new File(imageDir, allFiles[0]).getAbsolutePath();
            Bitmap image = BitmapFactory.decodeFile(imagePath);
            imageView.setImageBitmap(image);
        }
    }

    public void onClickSave(View view) {
        String[] allFiles = imageDir.list();
        if (allFiles == null || allFiles.length == 0) return;
        Arrays.sort(allFiles, Collections.reverseOrder());

        String imagePath = new File(imageDir, allFiles[0]).getAbsolutePath();
        Bitmap b = BitmapFactory.decodeFile(imagePath);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] ba = baos.toByteArray();

        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date());

        String tagsText = tagsView.getText().toString();

        ContentValues cv = new ContentValues();
        cv.put("picture", ba);
        cv.put("stamp", timestamp);
        cv.put("tags", tagsText);
        db.insert("Image", null, cv);

    }

    public void onClickFind(View view) {
        String findText = findEditText.getText().toString();
        ls.clear();
        adapter.notifyDataSetChanged();

        Cursor c = db.rawQuery(
                "SELECT * " +
                    "FROM Image " +
                    "WHERE tags like ? " +
                    "ORDER BY stamp DESC;",
                new String[]{"%" + findText + "%"}
        );

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
    }

    public void onClickBack(View view) {
        MediaPlayer.create(this, R.raw.suspense).start();
        startActivity(new Intent(this, MainActivity.class));
    }

    public void onClickTags(View view) {
        new Thread(() -> {
            try {
                vision();
                MediaPlayer.create(this, R.raw.fairy).start();
            } catch (IOException e) {
                Log.e("vision", e.toString());
            }
        }).start();
    }

    private void vision() throws IOException {
        //1. ENCODE image.
        String[] allFiles = imageDir.list();
        if (allFiles == null || allFiles.length == 0) return;
        Arrays.sort(allFiles, Collections.reverseOrder());
        String imagePath = new File(imageDir, allFiles[0]).getAbsolutePath();
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        Image myimage = new Image();
        myimage.encodeContent(bout.toByteArray());

        //2. PREPARE AnnotateImageRequest
        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
        annotateImageRequest.setImage(myimage);
        Feature f = new Feature();
        f.setType("LABEL_DETECTION");
        f.setMaxResults(10);
        List<Feature> lf = new ArrayList<Feature>();
        lf.add(f);
        annotateImageRequest.setFeatures(lf);

        //3.BUILD the Vision
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(new VisionRequestInitializer(API_KEY));
        Vision vision = builder.build();

        //4. CALL Vision.Images.Annotate
        BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
        List<AnnotateImageRequest> list = new ArrayList<AnnotateImageRequest>();
        list.add(annotateImageRequest);
        batchAnnotateImagesRequest.setRequests(list);
        Vision.Images.Annotate task = vision.images().annotate(batchAnnotateImagesRequest);
        BatchAnnotateImagesResponse response = task.execute();
        Log.v("tags", response.toPrettyString());

        double score = Double.parseDouble(response.getResponses().get(0).getLabelAnnotations().get(0).get("score").toString());

        if (score < 0.85) {
            runOnUiThread(() -> {
                tagsView.setText(response.getResponses().get(0).getLabelAnnotations().get(0).get("description").toString());
            });

        } else {
            int i = 0;
            String result = "";

            while (score >= 0.85) {
                if (i == 10) break;
                result += response.getResponses().get(0).getLabelAnnotations().get(i).get("description").toString();
                i++;
                score = Double.parseDouble(response.getResponses().get(0).getLabelAnnotations().get(i).get("score").toString());

                if (score >= 0.85) {
                    result += ", ";
                }
            }

            String finalResult = result;
            runOnUiThread(() -> {
                tagsView.setText(finalResult);
            });
        }

    }
}