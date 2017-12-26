package com.videolive.voice;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.videolive.R;

public class VoiceReady extends AppCompatActivity {
    private Button begin;
    private EditText url;
    private EditText port;
    private EditText collectionbitrate_vc;
    private EditText publishbitrate_vc;
    private EditText multiple;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_send_ready);

        url = findViewById(R.id.url);
        port = findViewById(R.id.port);
        begin = findViewById(R.id.begin);
        multiple = findViewById(R.id.multiple);
        publishbitrate_vc = findViewById(R.id.publishbitrate_vc);
        collectionbitrate_vc = findViewById(R.id.collectionbitrate_vc);

        begin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });
    }

    private void start() {
        Intent intent = new Intent(this, Voice.class);
        Bundle bundle = new Bundle();
        bundle.putString("url", url.getText().toString());
        bundle.putInt("port", Integer.parseInt(port.getText().toString()));
        bundle.putInt("multiple", Integer.parseInt(multiple.getText().toString()));
        bundle.putInt("collectionbitrate_vc", Integer.parseInt(collectionbitrate_vc.getText().toString()) * 1024);
        bundle.putInt("publishbitrate_vc", Integer.parseInt(publishbitrate_vc.getText().toString()) * 1024);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
