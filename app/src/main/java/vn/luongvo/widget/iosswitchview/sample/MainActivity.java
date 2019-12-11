package vn.luongvo.widget.iosswitchview.sample;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import vn.luongvo.widget.iosswitchview.SwitchView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final SwitchView switchView = findViewById(R.id.switchview);
        switchView.setOnCheckedChangeListener(new SwitchView.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(SwitchView switchView, boolean isChecked) {
                Toast.makeText(MainActivity.this, "onCheckedChanged: " + isChecked,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
