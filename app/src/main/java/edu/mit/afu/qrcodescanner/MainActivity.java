package edu.mit.afu.qrcodescanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.google.zxing.Result;


import me.dm7.barcodescanner.zxing.ZXingScannerView;


public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;
    public String TAG;
    public String tag;
    public final static String EXTRA_MESSAGE = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mScannerView = new ZXingScannerView(this);   // Programmatically initialize the scanner view
        setContentView(mScannerView);
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
        // Stop camera on pause
    }
3
    @Override
    public void handleResult(Result rawResult) {
        Log.e("handler", rawResult.getText()); // Prints scan results
        Log.e("handler", rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode)
        tag = rawResult.getText();

        //parse the raw result to get subscription topic
        int idIndex = tag.toLowerCase().indexOf("id=");
        int stIndex = tag.toLowerCase().indexOf("&st=");
        int osIndex = tag.toLowerCase().indexOf("&os=");
        String id = tag.substring(idIndex+3, stIndex);
        String st = tag.substring(stIndex+4, osIndex);
        tag = id + st + "ind=" + id.length();
        Log.d(TAG, tag);

        // switch to ServerConnect activity
        Intent intent = new Intent(this, ServerConnect.class);
        intent.putExtra(EXTRA_MESSAGE, tag);
        startActivity(intent);

        // If you would like to resume scanning, call this method below:
        // mScannerView.resumeCameraPreview(this);
    }
}