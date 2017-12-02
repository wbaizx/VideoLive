package com.videolive;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.library.vd.VDEncoder;

public class SendReady extends AppCompatActivity {
    private EditText url;
    private EditText port;
    private EditText framerate;
    private EditText bitrate;
    private EditText pu_width;
    private EditText pu_height;
    private EditText pr_width;
    private EditText pr_height;
    private EditText c_width;
    private EditText c_height;
    private EditText bitrate_vc;
    private RadioGroup videoCode;
    private RadioGroup preview;
    private RadioGroup rotate;
    private Button begin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_ready);

        url = findViewById(R.id.url);
        port = findViewById(R.id.port);
        framerate = findViewById(R.id.framerate);
        bitrate = findViewById(R.id.bitrate);
        pr_width = findViewById(R.id.pr_width);
        c_width = findViewById(R.id.c_width);
        pr_height = findViewById(R.id.pr_height);
        c_height = findViewById(R.id.c_height);
        bitrate_vc = findViewById(R.id.bitrate_vc);
        pu_width = findViewById(R.id.pu_width);
        pu_height = findViewById(R.id.pu_height);
        videoCode = findViewById(R.id.svideoCode);
        preview = findViewById(R.id.preview);
        rotate = findViewById(R.id.rotate);
        begin = findViewById(R.id.begin);

        begin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                star();
            }
        });
    }

    private void star() {
        Intent intent = new Intent(this, Send.class);
        Bundle bundle = new Bundle();
        bundle.putString("url", url.getText().toString());
        bundle.putInt("port", Integer.parseInt(port.getText().toString()));
        bundle.putInt("framerate", Integer.parseInt(framerate.getText().toString()));
        bundle.putInt("bitrate", Integer.parseInt(bitrate.getText().toString()) * 1024);
        bundle.putInt("bitrate_vc", Integer.parseInt(bitrate_vc.getText().toString()) * 1024);
        bundle.putInt("pu_width", Integer.parseInt(pu_width.getText().toString()));
        bundle.putInt("pu_height", Integer.parseInt(pu_height.getText().toString()));
        bundle.putInt("pr_width", Integer.parseInt(pr_width.getText().toString()));
        bundle.putInt("pr_height", Integer.parseInt(pr_height.getText().toString()));
        bundle.putInt("c_height", Integer.parseInt(c_height.getText().toString()));
        bundle.putInt("c_width", Integer.parseInt(c_width.getText().toString()));

        if (videoCode.getCheckedRadioButtonId() == R.id.sh264) {
            bundle.putString("videoCode", VDEncoder.H264);
        } else {
            bundle.putString("videoCode", VDEncoder.H265);
        }
        if (preview.getCheckedRadioButtonId() == R.id.haspreview) {
            bundle.putBoolean("ispreview", true);
        } else {
            bundle.putBoolean("ispreview", false);
        }
        if (rotate.getCheckedRadioButtonId() == R.id.front) {
            bundle.putBoolean("rotate", true);
        } else {
            bundle.putBoolean("rotate", false);
        }

        intent.putExtras(bundle);
        startActivity(intent);
    }
}
