package com.blink.pos.companion;

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
                dispatchPrintRequest(pendingPrintData);
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
                String[] printData = extractPrintData(data);
                if (isPrinterServiceBound) {
                    dispatchPrintRequest(printData);
                } else {
                    pendingPrintData = printData;
                }
                finish();
            }
        }
    }

    private String[] extractPrintData(Uri data) {
        String app = data.getQueryParameter("app");
        if (app == null) {
            return extractPayData(data);
        }

        switch (app) {
            case "voucher":
                return extractVoucherData(data);
            default: //default is blink pos/pay
                return extractPayData(data);
        }
    }

    private String[] extractVoucherData(Uri data) {
        String lnurl = data.getQueryParameter("lnurl");
        String voucherPrice = data.getQueryParameter("voucherPrice");
        String voucherAmount = data.getQueryParameter("voucherAmount");
        String voucherSecret = data.getQueryParameter("voucherSecret");
        String commissionPercentage = data.getQueryParameter("commissionPercentage");
        String identifierCode = data.getQueryParameter("identifierCode");

        return new String[]{"voucher", lnurl, voucherPrice, voucherAmount, voucherSecret, commissionPercentage, identifierCode};
    }

    private String[] extractPayData(Uri data) {
        String username = data.getQueryParameter("username");
        String amount = data.getQueryParameter("amount");
        String paymentHash = data.getQueryParameter("paymentHash");
        String transactionId = data.getQueryParameter("id");
        String date = data.getQueryParameter("date");
        String time = data.getQueryParameter("time");
        return new String[]{"pay", username, amount, paymentHash, transactionId, date, time};
    }

    private void dispatchPrintRequest(String[] printData) {
        switch (printData[0]) {
            case "voucher":
                printVoucherReceipt(printData[1], printData[2], printData[3], printData[4], printData[5], printData[6]);
                break;
            default:
                printPayReceipt(printData[1], printData[2], printData[3], printData[4], printData[5], printData[6]);
                break;
        }
    }

    private void printVoucherReceipt(String lnurl, String voucherPrice, String voucherAmount, String voucherSecret, String commissionPercentage, String identifierCode) {
        singleThreadExecutor.submit(() -> {
            try {
                PrintTextFormat dashedFormat = new PrintTextFormat();
                dashedFormat.setStyle(0);
                dashedFormat.setTextSize(27);
                dashedFormat.setAli(1);
                dashedFormat.setStyle(1);

                //Blink Logo
                Bitmap originalBitmap = BitmapFactory.decodeStream(getAssets().open("blink-logo.png"));
                int maxWidthPixels = 200;
                double aspectRatio = (double) originalBitmap.getWidth() / originalBitmap.getHeight();
                int newHeight = (int) (maxWidthPixels / aspectRatio);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, maxWidthPixels, newHeight, true);
                printerService.printBitmap(resizedBitmap, 1, 1);

                //dashed line
                String dashedLine = new String(new char[32]).replace("\0", "-");
                printerService.printText( dashedLine, dashedFormat);

                //transaction data
                printDynamicKeyValue("Price:" ,"               ", voucherPrice);
                printDynamicKeyValue("Value:","               ", voucherAmount);
                printDynamicKeyValue("Identifier:","         ", identifierCode);
                printDynamicKeyValue("Commission:","   ", commissionPercentage +  "%");

                
                //dashed line
                printerService.printText( dashedLine , dashedFormat);

                //QR
                printerService.printQrCode(lnurl, 350, 350, 1);
                printerService.printText(dashedLine, dashedFormat);

                PrintTextFormat appLink = new PrintTextFormat();
                appLink.setAli(1);
                appLink.setTextSize(25);
                appLink.setStyle(1);
                printerService.printText("voucher secret", appLink);
                printerService.printText(formatVoucherSecret(voucherSecret), appLink);

                // dashed line
                printerService.printText( dashedLine, dashedFormat);
                
                printerService.printText("voucher.blink.sv", appLink);
                printerService.printText("\n", appLink);

                //stop printing
                paperOut();
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    public static String formatVoucherSecret(String secret) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < secret.length(); i += 4) {
            if (i > 0) {
                formatted.append(" ");
            }
            formatted.append(secret.substring(i, Math.min(i + 4, secret.length())));
        }
        return formatted.toString();
    }

    private void printPayReceipt(String username, String amount, String paymentHash, String transactionId, String date, String time) {
        singleThreadExecutor.submit(() -> {
            try {
                PrintTextFormat dashedFormat = new PrintTextFormat();
                dashedFormat.setStyle(0);
                dashedFormat.setTextSize(27);
                dashedFormat.setAli(1);
                dashedFormat.setStyle(1);


                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
                dateFormat.setTimeZone(TimeZone.getDefault());
                timeFormat.setTimeZone(TimeZone.getDefault());
                String currentDate = dateFormat.format(new Date());
                String currentTime = timeFormat.format(new Date());

                //Blink Logo
                Bitmap originalBitmap = BitmapFactory.decodeStream(getAssets().open("blink-logo.png"));
                int maxWidthPixels = 200;
                double aspectRatio = (double) originalBitmap.getWidth() / originalBitmap.getHeight();
                int newHeight = (int) (maxWidthPixels / aspectRatio);
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, maxWidthPixels, newHeight, true);
                printerService.printBitmap(resizedBitmap, 1, 1);


                //dashed line
                String dashedLine = new String(new char[32]).replace("\0", "-");
                printerService.printText( dashedLine, dashedFormat);


                //transaction data
                printDynamicKeyValue("Username:" ,"    ", username);
                printDynamicKeyValue("Amount:","        ", amount);
                if (date != null && !date.isEmpty()) {
                    printDynamicKeyValue("Date:","              ", date);
                } else {
                    printDynamicKeyValue("Date:","              ", currentDate);
                }
                if (time != null && !time.isEmpty()) {
                    printDynamicKeyValue("Time:","             ", time);
                }else{
                    printDynamicKeyValue("Time:","             ", currentTime);
                }


                //dashed line
                printerService.printText( dashedLine , dashedFormat);


                //transaction hash
                PrintTextFormat formatTxid = new PrintTextFormat();
                formatTxid.setAli(1);
                formatTxid.setTextSize(23);
                formatTxid.setStyle(1);

                if (transactionId != null && !transactionId.isEmpty()) {
                    printerService.printText("Blink Internal Id", formatTxid);
                    printerService.printText(transactionId, formatTxid);
                    printerService.printText("\n", formatTxid);
                }

                if (paymentHash != null && !paymentHash.isEmpty()) {
                    printerService.printText("Payment Hash", formatTxid);
                    printerService.printText(paymentHash, formatTxid);
                    printerService.printText("\n", formatTxid);
                }


                //stop printing
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


    //bind service-------------------------------------------------
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isPrinterServiceBound) {
            unbindService(connService);
            isPrinterServiceBound = false;
        }
    }
}
