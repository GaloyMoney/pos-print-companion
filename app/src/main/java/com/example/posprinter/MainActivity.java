package com.example.posprinter;

import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import net.nyx.printerservice.print.IPrinterService;
import net.nyx.printerservice.print.PrintTextFormat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private IPrinterService printerService;
    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private boolean isPrinterServiceBound = false;
    private String[] pendingPrintData = null;

    private ServiceConnection connService = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            printerService = null;
            isPrinterServiceBound = false;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            printerService = IPrinterService.Stub.asInterface(service);
            isPrinterServiceBound = true;
            if (pendingPrintData != null) {
                PrintReceipt(pendingPrintData[0], pendingPrintData[1], pendingPrintData[2]);
                pendingPrintData = null;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        bindService();
        handleSendText(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSendText(intent);
    }

    private void handleSendText(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri data = intent.getData();
            if (data != null) {
                String username = data.getQueryParameter("username");
                String amount = data.getQueryParameter("amount");
                String sats = data.getQueryParameter("sats");
                if (isPrinterServiceBound) {
                    PrintReceipt(username, amount, sats);
                } else {
                    pendingPrintData = new String[]{username, amount, sats};
                }
                finish();
            }
        }
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
        printerService.printText(key, keyFormat);
        printerService.printText(value + "\n", valueFormat);
    }

    private void PrintReceipt(String username, String amount, String sats) {
        singleThreadExecutor.submit(() -> {
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
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isPrinterServiceBound) {
            unbindService(connService);
            isPrinterServiceBound = false;
        }
    }
}
