package com.egguncle.launchanywhere;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class MyAccountService extends Service {

    private MyAuthenticator myAuthenticator;

    public MyAccountService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        myAuthenticator = new MyAuthenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return myAuthenticator.getIBinder();
    }
}
