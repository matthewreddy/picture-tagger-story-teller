package com.example.proj;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onClickPicture(View view) {
        MediaPlayer.create(this, R.raw.camera).start();
        startActivity(new Intent(this, PictureActivity.class));
    }

    public void onClickDrawing(View view) {
        MediaPlayer.create(this, R.raw.pencil).start();
        startActivity(new Intent(this, DrawingActivity.class));
    }

    public void onClickStory(View view) {
        MediaPlayer.create(this, R.raw.page).start();
        startActivity(new Intent(this, StoryActivity.class));
    }
}