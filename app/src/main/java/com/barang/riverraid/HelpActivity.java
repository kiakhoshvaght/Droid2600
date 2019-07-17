package com.barang.riverraid;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.Objects;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ui.FullscreenActivity;

public class HelpActivity extends Activity {

    @BindView(R.id.start_btn)
    protected Button startBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
        ButterKnife.bind(this);

        Objects.requireNonNull(getActionBar()).hide();
    }

    @OnClick({R.id.start_btn})
    public void onStartButtonClick(View view){
        startActivity(new Intent(this, FullscreenActivity.class));
    }
}
