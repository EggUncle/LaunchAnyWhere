package com.egguncle.launchanywhere;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SyncStateContract;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {
    final static String TAG = MainActivity.class.getSimpleName();


    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btn = findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent1 = new Intent();
                intent1.setComponent(new ComponentName(
                        "com.android.settings",
                        "com.android.settings.accounts.AddAccountSettings"));
                intent1.setAction(Intent.ACTION_RUN);
                intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                String authTypes[] = {SyncStateContract.Constants.ACCOUNT_TYPE};

                intent1.putExtra("account_types", authTypes);
                MainActivity.this.startActivity(intent1);
            }
        });
    }
}
