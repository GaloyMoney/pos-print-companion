package com.example.posprinter;

import android.content.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import net.nyx.printerservice.print.IPrinterService;
import net.nyx.printerservice.print.PrintTextFormat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private IPrinterService printerService;
    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler();
    private ServiceConnection connService = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            printerService = null;
            handler.postDelayed(MainActivity.this::bindService, 5000);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            printerService = IPrinterService.Stub.asInterface(service);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPaper = findViewById(R.id.btn_paper);
        btnPaper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                paperOut();
            }
        });

        Button btnPrintText = findViewById(R.id.btn1);
        btnPrintText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                printText("sid" , "12 USD" , "20 SATS");
            }
        });

        bindService();
    }

    private void bindService() {
        Intent intent = new Intent();
        intent.setPackage("net.nyx.printerservice");
        intent.setAction("net.nyx.printerservice.IPrinterService");
        bindService(intent, connService, Context.BIND_AUTO_CREATE);
    }

    private void paperOut() {
        singleThreadExecutor.submit(() -> {
            try {
                printerService.paperOut(80);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }


    private void printKeyValueBelow(String key, String value) throws RemoteException {
        PrintTextFormat keyFormat = new PrintTextFormat();
        keyFormat.setStyle(1);
        keyFormat.setUnderline(true);
        keyFormat.setTextSize(24);
        PrintTextFormat valueFormat = new PrintTextFormat();
        valueFormat.setStyle(0);
        valueFormat.setTextSize(32);
        printerService.printText(key , keyFormat);
        printerService.printText(value + "\n", valueFormat);
    }

    private void printText( String username, String amount, String sats) {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    PrintTextFormat headingFormat = new PrintTextFormat();
                    headingFormat.setStyle(1);
                    headingFormat.setUnderline(true);
                    headingFormat.setAli(1);
                    headingFormat.setTextSize(32);
                    printerService.printText("BLINK POS\n\n", headingFormat);

                    printKeyValueBelow("Username:", username);
                    printKeyValueBelow("Amount:", amount);
                    printKeyValueBelow("Sats:", sats);
                    paperOut();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connService);
    }
}
