package com.example.posprinter;

import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import net.nyx.printerclient.aop.SingleClick;
import net.nyx.printerservice.print.IPrinterService;
import net.nyx.printerservice.print.PrintTextFormat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.example.posprinter.Result.msg;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    protected Button btnVer;
    protected Button btnPaper;
    protected Button btn1;
    protected Button btn2;
    protected Button btn3;
    protected Button btnScan;
    protected TextView tvLog;

    private static final int RC_SCAN = 0x99;
    public static String PRN_TEXT;
    protected Button btn4;
    protected Button btnLbl;
    protected Button btnLblLearning;

    private ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler();
    String[] version = new String[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_main);
        initView();
        bindService();
//        registerQscScanReceiver();
        PRN_TEXT = getString(R.string.print_text);
    }


    private IPrinterService printerService;
    private ServiceConnection connService = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            showLog("printer service disconnected, try reconnect");
            printerService = null;
            // 尝试重新bind
            handler.postDelayed(() -> bindService(), 5000);
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            printerService = IPrinterService.Stub.asInterface(service);
        }
    };

    private void bindService() {
        Intent intent = new Intent();
        intent.setPackage("net.nyx.printerservice");
        intent.setAction("net.nyx.printerservice.IPrinterService");
        bindService(intent, connService, Context.BIND_AUTO_CREATE);
    }

    private void unbindService() {
        unbindService(connService);
    }

    @SingleClick
    @Override
    public void onClick(View view) {
       if (view.getId() == R.id.btn_paper) {
            paperOut();
        } else if (view.getId() == R.id.btn1) {
            printText();
        } else if (view.getId() == R.id.btn2) {
            printBarcode();
        } else if (view.getId() == R.id.btn3) {
            printQrCode();
        } else if (view.getId() == R.id.btn4) {
            printBitmap();
        }
    }

    private void paperOut() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    printerService.paperOut(80);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printText() {
        printText(PRN_TEXT);
    }

    private void printBarcode() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = printerService.printBarcode("123456789", 300, 160, 1, 1);
                    showLog("Print text: " + msg(ret));
                    if (ret == 0) {
                        paperOut();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printQrCode() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = printerService.printQrCode("123456789", 300, 300, 1);
                    showLog("Print barcode: " + msg(ret));
                    if (ret == 0) {
                        paperOut();
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printBitmap() {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    int ret = printerService.printBitmap(BitmapFactory.decodeStream(getAssets().open("bmp.png")), 1, 1);
                    showLog("Print bitmap: " + msg(ret));
                    if (ret == 0) {
                        paperOut();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printKeyValueRightAligned(String key, String value, PrintTextFormat format) throws RemoteException {
        final int totalWidth = 32; // Adjust based on your printer's capacity
        int spacesNeededForKey = totalWidth - key.length() - value.length(); // Calculate space between key and value
        String paddingForValue = ""; // Initialize the padding string
        if (spacesNeededForKey > 0) {
            paddingForValue = new String(new char[spacesNeededForKey]).replace('\0', ' '); // Create padding spaces
        }
        String formattedText = key + paddingForValue + value;
        printerService.printText(formattedText + "\n", format); // Print the formatted string
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

    private void printText(String customText) {
        singleThreadExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    // Assuming setting bold and underline for heading "BLINK POS"
                    PrintTextFormat headingFormat = new PrintTextFormat();
                    headingFormat.setStyle(1); // Bold
                    headingFormat.setUnderline(true);
                    headingFormat.setAli(1); // Centered
                    headingFormat.setTextSize(32);
                    printerService.printText("BLINK POS\n\n", headingFormat); // Print heading

                    // Print each key-value pair with the value below the key
                    printKeyValueBelow("Username:", "Siddharth");
                    printKeyValueBelow("Amount:", "$12 USD");
                    printKeyValueBelow("Sats:", "12 Sats");
                    printKeyValueBelow("Time:", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));

                    // Here, insert any additional logic needed for printing `customText` or handling the paper out
                    paperOut();
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SCAN && resultCode == RESULT_OK && data != null) {
            String result = data.getStringExtra("SCAN_RESULT");
            showLog("Scanner result: " + result);
        }
    }

    boolean existApp(String pkg) {
        try {
            return getPackageManager().getPackageInfo(pkg, 0) != null;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService();
//        unregisterQscReceiver();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_item_clear) {
            clearLog();
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        btnPaper = (Button) findViewById(R.id.btn_paper);
        btnPaper.setOnClickListener(MainActivity.this);
        btn1 = (Button) findViewById(R.id.btn1);
        btn1.setOnClickListener(MainActivity.this);
        btn2 = (Button) findViewById(R.id.btn2);
        btn2.setOnClickListener(MainActivity.this);
        btn3 = (Button) findViewById(R.id.btn3);
        btn3.setOnClickListener(MainActivity.this);
        tvLog = (TextView) findViewById(R.id.tv_log);
        btn4 = (Button) findViewById(R.id.btn4);
        btn4.setOnClickListener(MainActivity.this);
        btnLbl = (Button) findViewById(R.id.btn_lbl);
        btnLbl.setOnClickListener(MainActivity.this);
        btnLblLearning = (Button) findViewById(R.id.btn_lbl_learning);
        btnLblLearning.setOnClickListener(MainActivity.this);
    }

    void showLog(String log, Object... args) {
        if (args != null && args.length > 0) {
            log = String.format(log, args);
        }
        String res = log;
        Log.e(TAG, res);
        DateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (tvLog.getLineCount() > 100) {
                    tvLog.setText("");
                }
                tvLog.append((dateFormat.format(new Date()) + ":" + res + "\n"));
                tvLog.post(new Runnable() {
                    @Override
                    public void run() {
                        ((ScrollView) tvLog.getParent()).fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    void clearLog() {
        tvLog.setText("");
    }

}
