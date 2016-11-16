package pku.ss.luoxi.app;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

import pku.ss.luoxi.myweather.MainActivity;

/**
 * Created by admin on 2016/11/16.
 */
public class MyService extends Service {
    private Timer timer;
    private int count;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.d("startService:","onBind");
        return null;
    }
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d("startService:","onCreate");
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("startService:","onStartCommand");
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Intent intent = new Intent();
                intent.setAction(MainActivity.ACTION_SERVICE_UPDATE);
                //intent.putExtra( "count", ++count );
                sendBroadcast( intent );
            }
        },0,60*60*1000);

        return super.onStartCommand(intent, flags, startId);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (timer != null){
            timer.cancel();
        }
        Log.d("startService:","onDestroy");
    }


}
