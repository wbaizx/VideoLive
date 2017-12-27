package com.videolive.video;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.library.live.vd.VDDecoder;
import com.videolive.R;

public class ReciveReady extends AppCompatActivity {
    private EditText port;
    private RadioGroup videoCode;
    private Button begin;
    private EditText multiple;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recive_ready);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        multiple = findViewById(R.id.multiple);
        port = findViewById(R.id.port);
        videoCode = findViewById(R.id.rvideoCode);
        begin = findViewById(R.id.begin);

        begin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                start();
            }
        });
    }

    private void start() {
        Intent intent = new Intent(this, Recive.class);
        Bundle bundle = new Bundle();
        bundle.putInt("multiple", Integer.parseInt(multiple.getText().toString()));
        if (videoCode.getCheckedRadioButtonId() == R.id.rh264) {
            bundle.putString("videoCode", VDDecoder.H264);
        } else {
            bundle.putString("videoCode", VDDecoder.H265);
        }
        bundle.putInt("port", Integer.parseInt(port.getText().toString()));
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
