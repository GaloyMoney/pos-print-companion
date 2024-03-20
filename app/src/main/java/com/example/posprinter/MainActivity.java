package com.example.posprinter;

import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.appcompat.app.AppCompatActivity;
import net.nyx.printerservice.print.IPrinterService;
import net.nyx.printerservice.print.PrintTextFormat;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
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
                String paymentHash = data.getQueryParameter("paymentHash");
                if (isPrinterServiceBound) {
                    PrintReceipt(username, amount, paymentHash);
                } else {
                    pendingPrintData = new String[]{username, amount, paymentHash};
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

    private void PrintReceipt(String username, String amount, String paymentHash) {
        singleThreadExecutor.submit(() -> {
            try {
                PrintTextFormat dashedFormat = new PrintTextFormat();

                dashedFormat.setStyle(0);
                dashedFormat.setTextSize(27);
                dashedFormat.setAli(1);
                dashedFormat.setStyle(1);


                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                String date = dateFormat.format(new Date());

                String dashedLine = new String(new char[32]).replace("\0", "-");

                Bitmap originalBitmap = BitmapFactory.decodeStream(getAssets().open("blink-logo.png"));
                int maxWidthPixels = 200;
                double aspectRatio = (double) originalBitmap.getWidth() / originalBitmap.getHeight();
                int newHeight = (int) (maxWidthPixels / aspectRatio);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, maxWidthPixels, newHeight, true);

                printerService.printBitmap(resizedBitmap, 1, 1);

                printerService.printText( dashedLine, dashedFormat);

                printDynamicKeyValue("Username:" ,"     ", username);
                printDynamicKeyValue("Amount:","         ", amount);
                printDynamicKeyValue("Time:","              ", date);

                printerService.printText( dashedLine , dashedFormat);

                PrintTextFormat formatTxid = new PrintTextFormat();
                formatTxid.setAli(1);
                formatTxid.setTextSize(23);
                formatTxid.setStyle(1);
                printerService.printText("Payment Hash", formatTxid);
                printerService.printText(paymentHash, formatTxid);
                printerService.printText("\n", formatTxid);
                paperOut();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void printDynamicKeyValue(String key, String space ,String value) throws RemoteException {
        PrintTextFormat textFormat = new PrintTextFormat();
        textFormat.setStyle(0);
        textFormat.setTextSize(23);
        textFormat.setStyle(1);
        printerService.printText(key + space + value , textFormat);
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
