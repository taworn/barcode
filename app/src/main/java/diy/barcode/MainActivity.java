package diy.barcode;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements CameraFragment.Callback {

    private CameraFragment fragment;
    private TextView textQr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textQr = (TextView) findViewById(R.id.text_qr);
        fragment = (CameraFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_camera);
        fragment.setCallback(this);
        fragment.setCameraId(0);
        // use this code to pause/resume
        /*
        if (fragment.getCameraId() < 0)
            fragment.setCameraId(0);
        else
            fragment.setCameraId(-1);
        */

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        if (fab != null)
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setIcon(R.mipmap.ic_launcher)
                            .setTitle(R.string.about_dialog_title)
                            .setMessage(R.string.about_dialog_message)
                            .setPositiveButton(R.string.about_dialog_positive, null)
                            .show();
                }
            });
    }

    @Override
    public void getData(@Nullable String data) {
        if (data != null && !data.equals(textQr.getText().toString())) {
            textQr.setText(data);
        }
    }

}
