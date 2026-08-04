package com.example.zcs_bridge;

import androidx.annotation.NonNull;

// Flutter imports
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

// Android imports
import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.TextView;

// Java utilities
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// ZCS SDK Core imports
import com.zcs.sdk.DriverManager;
import com.zcs.sdk.SdkResult;
import com.zcs.sdk.SdkData;
import com.zcs.sdk.Sys;
import com.zcs.sdk.ConnectTypeEnum;

// Card Reader imports
import com.zcs.sdk.card.CardInfoEntity;
import com.zcs.sdk.card.CardReaderManager;
import com.zcs.sdk.card.CardReaderTypeEnum;
import com.zcs.sdk.card.CardSlotNoEnum;
// import com.zcs.sdk.card.ICCard;
import com.zcs.sdk.card.MagCard;
import com.zcs.sdk.card.RfCard;
import com.zcs.sdk.card.SLE4428Card;
import com.zcs.sdk.card.SLE4442Card;
import com.zcs.sdk.card.NativeNfcCard;
import com.zcs.sdk.listener.OnSearchCardListener;
import com.zcs.sdk.listener.OnNativeNfcDetectedListener;

// EMV Transaction imports
import com.zcs.sdk.emv.EmvApp;
import com.zcs.sdk.emv.EmvCapk;
import com.zcs.sdk.emv.EmvData;
import com.zcs.sdk.emv.EmvHandler;
import com.zcs.sdk.emv.EmvResult;
import com.zcs.sdk.emv.EmvTermParam;
import com.zcs.sdk.emv.EmvTransParam;
import com.zcs.sdk.emv.OnEmvListener;

// PIN Pad imports
import com.zcs.sdk.pin.PinAlgorithmMode;
import com.zcs.sdk.pin.MagEncryptTypeEnum;
import com.zcs.sdk.pin.PinMacTypeEnum;
import com.zcs.sdk.pin.PinWorkKeyTypeEnum;
import com.zcs.sdk.pin.pinpad.PinPadManager;

// Printer imports
import com.zcs.sdk.Printer;
import com.zcs.sdk.print.PrnStrFormat;
import com.zcs.sdk.print.PrnTextFont;
import com.zcs.sdk.print.PrnTextStyle;

// Hardware Control imports
import com.zcs.sdk.Beeper;
import com.zcs.sdk.Led;
import com.zcs.sdk.LedLightModeEnum;
import com.zcs.sdk.HQrsanner;
import android.os.SystemClock;

import android.view.KeyEvent;
import android.widget.EditText;

// External Port imports
import com.zcs.sdk.exteranl.ExternalCardManager;
// import com.zcs.sdk.exteranl.ICCard;

// Bluetooth imports
import com.zcs.sdk.bluetooth.BluetoothListener;
import com.zcs.sdk.bluetooth.BluetoothManager;
import com.zcs.sdk.bluetooth.emv.CardDetectedEnum;
import com.zcs.sdk.bluetooth.emv.EmvStatusEnum;
import com.zcs.sdk.bluetooth.emv.OnBluetoothEmvListener;

// Utility imports
import com.zcs.sdk.util.StringUtils;
import com.zcs.sdk.util.LogUtils;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import com.google.zxing.BarcodeFormat;
import java.io.InputStream;
import java.util.List;

/**
 * ZCSPlugin - Flutter plugin for ZCS ZCS SDK integration
 * 
 * This plugin provides a bridge between Flutter and the native ZCS SDK
 * allowing Flutter apps to interact with POS hardware features like:
 * - Card reading (magnetic, IC, contactless)
 * - EMV transaction processing
 * - Receipt printing
 * - PIN pad operations
 * - Device management
 * - QR Code scanning
 */
public class ZcsBridge implements FlutterPlugin, MethodCallHandler {
    
    // Channel name for communication between Flutter and Android
    private static final String CHANNEL_NAME = "zcs_bridge";
    private static final String TAG = "ZCSPLUGIN";
    
    // Flutter method channel for communication
    private MethodChannel channel;
    private Context context;
    private HQrsanner mHQrsanner;
    
    // Background thread executor for SDK operations
    private ExecutorService executor;
    private Handler mainHandler;
    
    // SDK instance variables
    private DriverManager mDriverManager;
    private Printer mPrinter;
    private boolean isSupportCutter = false;
    
    // Device state tracking
    private boolean isDeviceInitialized = false;
    private boolean isDeviceOpened = false;
    private boolean isScannerActive = false;

private String lastScannedData = "";
private boolean isWaitingForScan = false;
private Result pendingScanResult = null;
private EditText scanResultEditText;


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL_NAME);
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
        
        // Initialize background executor and main handler
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "ZCS Plugin attached to engine");
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
        
        // Cleanup resources
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
        
        // Stop scanner if active
        if (isScannerActive && mHQrsanner != null) {
            try {
                mHQrsanner.QRScanerPowerCtrl((byte) 0);
                isScannerActive = false;
            } catch (Exception e) {
                Log.w(TAG, "Failed to stop scanner during cleanup", e);
            }
        }
        
        Log.d(TAG, "ZCS Plugin detached from engine");
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        switch (call.method) {
            case "initializeDevice":
                initializeDevice(result);
                break;
            case "openDevice":
                openDevice(result);
                break;
            case "closeDevice":
                closeDevice(result);
                break;
            case "getDeviceInfo":
                getDeviceInfo(result);
                break;
            case "getDeviceStatus":
                getDeviceStatus(result);
                break;
            case "printDynamic":
                Map<String, Object> args = call.argument("args");
                Boolean bothCopies = call.argument("bothCopies");
                Integer pauseBetweenCopies = call.argument("pauseBetweenCopies");
                printDynamic(args, bothCopies != null ? bothCopies : false, 
                           pauseBetweenCopies != null ? pauseBetweenCopies : 5, result);
                break;
            case "cutPaper":
                cutPaper(result);
                break;
            case "openCashBox":
                openCashBox(result);
                break;
            case "getPrinterStatus":
                getPrinterStatus(result);
                break;
            case "getSerialNumber":
                getSerialNumber(result);
                break;
            case "printRawText":
                String text = call.argument("text");
                printRawText(text, result);
                break;
            case "printImage":
                byte[] imageData = call.argument("imageData");
                String imageAlign = call.argument("align");
                Integer imageWidth = call.argument("width");
                printImage(imageData, imageAlign != null ? imageAlign : "center", imageWidth, result);
                break;
             
        case "stopQRScan":
            stopQRScan(result);
            break;
        case "scanQRCode":
            scanQRCodeOnce(result);
            break;

            default:
                result.notImplemented();
                break;
        }
    }

    private void initializeDevice(Result result) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Initializing ZCS ZCS SDK...");
                
                // Initialize the ZCS SDK
                mDriverManager = DriverManager.getInstance();
                if (mDriverManager == null) {
                    throw new Exception("Failed to get DriverManager instance");
                }

                // Initialize the SDK hardware layer. This is REQUIRED before the
                // printer / cash-drawer will respond — without it getPrinterStatus()
                // returns SDK_ERROR (-1001) and openDevice()/printing/openCashBox fail.
                Sys sys = mDriverManager.getBaseSysDevice();
                if (sys == null) {
                    throw new Exception("Failed to get Sys device instance");
                }
                int initStatus = sys.sdkInit();
                if (initStatus != SdkResult.SDK_OK) {
                    // Some units need an explicit power-on before init succeeds
                    sys.sysPowerOn();
                    initStatus = sys.sdkInit();
                }
                if (initStatus != SdkResult.SDK_OK) {
                    throw new Exception("SDK hardware init failed, code: " + initStatus);
                }

                // Get printer instance
                mPrinter = mDriverManager.getPrinter();
                if (mPrinter == null) {
                    throw new Exception("Failed to get Printer instance");
                }
                
                // Check if device supports paper cutter
                isSupportCutter = mPrinter.isSuppoerCutter();

                // Initialize QR scanner
                mHQrsanner = mDriverManager.getHQrsannerDriver();
                if (mHQrsanner == null) {

                    Log.w(TAG, "QR Scanner not available on this device");
                    // Don't throw exception, just log warning as some devices may not have scanner
                }
           
                isDeviceInitialized = true;
                
                // Return result on main thread
                mainHandler.post(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "ZCS SDK initialized successfully");
                    response.put("supportsCutter", isSupportCutter);
                    response.put("hasQRScanner", mHQrsanner != null);
                    result.success(response);
                });
                
                Log.d(TAG, "SDK initialization completed successfully");
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to initialize SDK", e);
                mainHandler.post(() -> {
                    result.error("INIT_ERROR", "Failed to initialize SDK: " + e.getMessage(), null);
                });
            }
        });
    }

    private void openDevice(Result result) {
        if (!isDeviceInitialized) {
            result.error("DEVICE_NOT_INITIALIZED", "Device must be initialized first", null);
            return;
        }
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "Opening printer device...");
                
                // Check printer status first
                int status = mPrinter.getPrinterStatus();

                if (status == SdkResult.SDK_OK) {
                    isDeviceOpened = true;
                    
                    mainHandler.post(() -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Printer opened successfully");
                        response.put("status", "ready");
                        result.success(response);
                    });
                } else {
                    String statusMessage = getPrinterStatusMessage(status);
                    throw new Exception("Printer not ready: " + statusMessage);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to open device", e);
                mainHandler.post(() -> {
                    result.error("OPEN_ERROR", "Failed to open device: " + e.getMessage(), null);
                });
            }
        });
    }

    private void closeDevice(Result result) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Closing printer device...");
                
                // Stop scanner if active
                if (isScannerActive && mHQrsanner != null) {
                    try {
                        mHQrsanner.QRScanerPowerCtrl((byte) 0);
                        isScannerActive = false;
                        Log.d(TAG, "QR Scanner stopped during device close");
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to stop scanner during close", e);
                    }
                }
                
                // ZCS SDK doesn't require explicit close for printer
                // Just update the state
                isDeviceOpened = false;
                
                mainHandler.post(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Device closed successfully");
                    result.success(response);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to close device", e);
                mainHandler.post(() -> {
                    result.error("CLOSE_ERROR", "Failed to close device: " + e.getMessage(), null);
                });
            }
        });
    }

    private void getDeviceInfo(Result result) {
        executor.execute(() -> {
            try {
                Log.d(TAG, "Getting device information...");
                
                if (!isDeviceInitialized) {
                    throw new Exception("Device not initialized");
                }
                
                Map<String, Object> deviceInfo = new HashMap<>();
                deviceInfo.put("model", "ZCS ZCS");
                deviceInfo.put("serialNumber", "ZCS_" + System.currentTimeMillis());
                deviceInfo.put("sdkVersion", "1.8.1+");
                deviceInfo.put("supportsCutter", isSupportCutter);
                deviceInfo.put("hasQRScanner", mHQrsanner != null);
                deviceInfo.put("printerStatus", getPrinterStatusMessage(mPrinter.getPrinterStatus()));
                deviceInfo.put("is80MMPrinter", mPrinter.is80MMPrinter());
                
                mainHandler.post(() -> result.success(deviceInfo));
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to get device info", e);
                mainHandler.post(() -> {
                    result.error("INFO_ERROR", "Failed to get device info: " + e.getMessage(), null);
                });
            }
        });
    }

    private void getDeviceStatus(Result result) {
        Map<String, Object> status = new HashMap<>();
        status.put("initialized", isDeviceInitialized);
        status.put("opened", isDeviceOpened);
        status.put("ready", isDeviceInitialized && isDeviceOpened);
        status.put("supportsCutter", isSupportCutter);
        status.put("hasQRScanner", mHQrsanner != null);
        status.put("scannerActive", isScannerActive);
        result.success(status);
    }

    /**
     * Universal dynamic printing method that handles any type of document
     * @param args Map containing document data (businessName, header, fields, items, totals, etc.)
     * @param bothCopies If true, print both customer and merchant copies with pause between
     * @param pauseBetweenCopies Number of seconds to pause between copies (default: 5)
     * @param result Flutter result callback
     */
    private void printDynamic(Map<String, Object> args, boolean bothCopies, int pauseBetweenCopies, Result result) {
        if (!checkDeviceReady(result)) return;
        
        if (args == null) {
            result.error("INVALID_INPUT", "Arguments map cannot be null", null);
            return;
        }
        
        executor.execute(() -> {
            try {
                int printStatus = mPrinter.getPrinterStatus();
                if (printStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
                    throw new Exception("Out of paper");
                }
                
                // Get layout style (default: detailed)
                String layoutStyle = getStringValue(args, "layoutStyle", "detailed");
                
                // Print customer copy
                printDocumentCopy(args, "CUSTOMER COPY", layoutStyle);
                
                // Start printing the first copy
                int result_code = mPrinter.setPrintStart();
                
                if (result_code != SdkResult.SDK_OK) {
                    throw new Exception("Print failed with code: " + result_code);
                }
                
                // Wait for first copy to complete printing
                Thread.sleep(2000);
                
                // Print merchant copy if bothCopies is true
                if (bothCopies) {
                    Log.d(TAG, "First copy printed. Waiting " + pauseBetweenCopies + " seconds for user to cut paper...");
                    
                    // Pause to allow user to cut the first copy
                    // Convert seconds to milliseconds
                    Thread.sleep(pauseBetweenCopies * 1000L);
                    
                    Log.d(TAG, "Printing second copy...");
                    
                    // Print merchant copy
                    printDocumentCopy(args, "MERCHANT COPY", layoutStyle);
                    
                    // Start printing the second copy
                    result_code = mPrinter.setPrintStart();
                    
                    if (result_code != SdkResult.SDK_OK) {
                        throw new Exception("Second copy print failed with code: " + result_code);
                    }
                    
                    // Wait for second copy to complete
                    Thread.sleep(2000);
                }
                
                mainHandler.post(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", bothCopies ? 
                        "Both copies printed successfully with pause between them" : 
                        "Document printed successfully");
                    response.put("copies", bothCopies ? 2 : 1);
                    result.success(response);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to print dynamic document", e);
                mainHandler.post(() -> {
                    result.error("PRINT_ERROR", "Failed to print: " + e.getMessage(), null);
                });
            }
        });
    }

    /**
     * Print a single copy of the document with specified copy label
     */
    private void printDocumentCopy(Map<String, Object> args, String copyLabel, String layoutStyle) throws Exception {
        // Determine font sizes based on layout style
        int headerSize = layoutStyle.equals("compact") ? 35 : layoutStyle.equals("simple") ? 40 : 45;
        int subHeaderSize = layoutStyle.equals("compact") ? 25 : layoutStyle.equals("simple") ? 28 : 32;
        int normalSize = layoutStyle.equals("compact") ? 20 : layoutStyle.equals("simple") ? 22 : 24;
        int smallSize = layoutStyle.equals("compact") ? 18 : 20;
        
        // 0. Print logo image if provided (top of receipt, above business name)
        Object logo = args.get("logoBytes");
        if (logo instanceof byte[]) {
            try {
                Integer logoWidth = getIntValue(args, "logoWidth", null);
                String logoAlign = getStringValue(args, "logoAlign", "center");
                appendImage((byte[]) logo, logoAlign, logoWidth);
                // Add spacing after the logo
                PrnStrFormat logoSpacing = createFormat(normalSize, false, Layout.Alignment.ALIGN_CENTER);
                mPrinter.setPrintAppendString("", logoSpacing);
            } catch (Exception e) {
                // A bad logo should not abort the whole receipt
                Log.w(TAG, "Failed to print logo, skipping", e);
            }
        }

        // Print copy label if not customer copy (to avoid redundancy)
        if (!copyLabel.equals("CUSTOMER COPY")) {
            PrnStrFormat copyLabelFormat = createFormat(subHeaderSize, true, Layout.Alignment.ALIGN_CENTER);
            mPrinter.setPrintAppendString(copyLabel, copyLabelFormat);
            mPrinter.setPrintAppendString("", copyLabelFormat);
        }
        
        // 1. Print business name (centered, bold, large)
        String businessName = getStringValue(args, "businessName", null);
        if (businessName != null && !businessName.trim().isEmpty()) {
            PrnStrFormat format = createFormat(headerSize, true, Layout.Alignment.ALIGN_CENTER);
            mPrinter.setPrintAppendString(businessName, format);
        }
        
        // 2. Print header (centered, bold)
        String header = getStringValue(args, "header", null);
        if (header != null && !header.trim().isEmpty()) {
            PrnStrFormat format = createFormat(subHeaderSize, true, Layout.Alignment.ALIGN_CENTER);
            mPrinter.setPrintAppendString(header, format);
        }
        
        // 3. Print subHeader (centered)
        String subHeader = getStringValue(args, "subHeader", null);
        if (subHeader != null && !subHeader.trim().isEmpty()) {
            PrnStrFormat format = createFormat(normalSize, false, Layout.Alignment.ALIGN_CENTER);
            mPrinter.setPrintAppendString(subHeader, format);
        }
        
        // Add spacing after headers
        if (businessName != null || header != null || subHeader != null) {
            PrnStrFormat emptyFormat = createFormat(normalSize, false, Layout.Alignment.ALIGN_NORMAL);
            mPrinter.setPrintAppendString("", emptyFormat);
        }
        
        // 4. Print fields map (key-value pairs)
        @SuppressWarnings("unchecked")
        Map<String, Object> fields = (Map<String, Object>) args.get("fields");
        if (fields != null && !fields.isEmpty()) {
            printFields(fields, normalSize, smallSize);
        }
        
        // 5. Print items table if exists
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) args.get("items");
        if (items != null && !items.isEmpty()) {
            printItemsTable(items, normalSize, smallSize, layoutStyle);
        }
        
        // 6. Print totals if exists
        @SuppressWarnings("unchecked")
        Map<String, Object> totals = (Map<String, Object>) args.get("totals");
        if (totals != null && !totals.isEmpty()) {
            printTotals(totals, normalSize);
        }
        
        // 7. Print QR code if qrCodeField is provided
        String qrCodeField = getStringValue(args, "qrCodeField", null);
        if (qrCodeField != null && !qrCodeField.trim().isEmpty()) {
            String qrValue = getStringValue(args, qrCodeField, null);
            if (qrValue != null && !qrValue.trim().isEmpty()) {
                printQRCodeHelper(qrValue, 200, normalSize);
            }
        }
        
        // 8. Print footer (centered, small)
        String footer = getStringValue(args, "footer", null);
        if (footer != null && !footer.trim().isEmpty()) {
            PrnStrFormat format = createFormat(smallSize, false, Layout.Alignment.ALIGN_CENTER);
            mPrinter.setPrintAppendString("", format);
            mPrinter.setPrintAppendString(footer, format);
        }
        
        // Add extra spacing at the end
        PrnStrFormat emptyFormat = createFormat(smallSize, false, Layout.Alignment.ALIGN_NORMAL);
        mPrinter.setPrintAppendString("", emptyFormat);
        mPrinter.setPrintAppendString("", emptyFormat);
    }

    // ============== HELPER METHODS FOR DYNAMIC PRINTING ==============
    
    /**
     * Create a text format with specified parameters
     */
    private PrnStrFormat createFormat(int textSize, boolean isBold, Layout.Alignment alignment) {
        PrnStrFormat format = new PrnStrFormat();
        format.setTextSize(textSize);
        format.setStyle(isBold ? PrnTextStyle.BOLD : PrnTextStyle.NORMAL);
        format.setFont(PrnTextFont.MONOSPACE);
        format.setAli(alignment);
        return format;
    }
    
    /**
     * Get string value from map with default fallback
     */
    private String getStringValue(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        return value.toString();
    }

    /**
     * Get integer value from map with default fallback (handles Integer, Long, and numeric strings)
     */
    private Integer getIntValue(Map<String, Object> map, String key, Integer defaultValue) {
        Object value = map.get(key);
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    /**
     * Print key-value pairs from fields map
     */
    private void printFields(Map<String, Object> fields, int normalSize, int smallSize) throws Exception {
        PrnStrFormat keyFormat = createFormat(normalSize, false, Layout.Alignment.ALIGN_NORMAL);
        
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Skip null or empty values
            if (value == null || value.toString().trim().isEmpty()) {
                continue;
            }
            
            // Handle nested maps recursively
            if (value instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) value;
                mPrinter.setPrintAppendString(key + ":", keyFormat);
                printFields(nestedMap, normalSize - 2, smallSize - 2);
                continue;
            }
            
            String valueStr = value.toString();
            
            // Print key-value pair with proper alignment
            printKeyValue(key, valueStr, normalSize);
        }
        
        // Add spacing after fields
        PrnStrFormat emptyFormat = createFormat(normalSize, false, Layout.Alignment.ALIGN_NORMAL);
        mPrinter.setPrintAppendString("", emptyFormat);
    }
    
    /**
     * Print a key-value pair with left-aligned key and right-aligned value
     */
    private void printKeyValue(String key, String value, int textSize) throws Exception {
        PrnStrFormat format = createFormat(textSize, false, Layout.Alignment.ALIGN_NORMAL);
        
        // Calculate available width for 58mm paper (approximately 32 characters)
        int maxWidth = 32;
        int colonPos = key.length() + 2; // "Key: "
        
        // Truncate or wrap long values
        if (colonPos + value.length() > maxWidth) {
            // Try to fit on one line with truncation
            int availableSpace = maxWidth - colonPos - 3; // Reserve space for "..."
            if (availableSpace > 10) {
                value = value.substring(0, availableSpace) + "...";
            }
        }
        
        // Format the line with proper spacing
        String line = String.format("%-16s %s", key + ":", value);
        mPrinter.setPrintAppendString(line, format);
    }
    
    /**
     * Print items as a table
     */
    private void printItemsTable(List<Map<String, Object>> items, int normalSize, int smallSize, String layoutStyle) throws Exception {
        PrnStrFormat headerFormat = createFormat(normalSize, true, Layout.Alignment.ALIGN_NORMAL);
        PrnStrFormat normalFormat = createFormat(normalSize - 2, false, Layout.Alignment.ALIGN_NORMAL);
        PrnStrFormat separatorFormat = createFormat(smallSize, false, Layout.Alignment.ALIGN_NORMAL);
        
        // Print separator
        String separator = "--------------------------------";
        mPrinter.setPrintAppendString(separator, separatorFormat);
        
        // Print table header
        String headerLine = String.format("%-14s %3s %4s %6s", "Item", "Qty", "Unit", "Total");
        mPrinter.setPrintAppendString(headerLine, headerFormat);
        mPrinter.setPrintAppendString(separator, separatorFormat);
        
        // Print each item
        for (Map<String, Object> item : items) {
            String itemName = getStringValue(item, "Item", getStringValue(item, "item", ""));
            String qty = getStringValue(item, "Qty", getStringValue(item, "qty", getStringValue(item, "quantity", "")));
            String unit = getStringValue(item, "Unit", getStringValue(item, "unit", getStringValue(item, "price", "")));
            String total = getStringValue(item, "Total", getStringValue(item, "total", getStringValue(item, "amount", "")));
            
            // Skip empty items
            if (itemName.isEmpty() && qty.isEmpty() && unit.isEmpty() && total.isEmpty()) {
                continue;
            }
            
            // Truncate long item names
            if (itemName.length() > 14) {
                itemName = itemName.substring(0, 11) + "...";
            }
            
            // Format item line
            String itemLine = String.format("%-14s %3s %4s %6s", itemName, qty, unit, total);
            mPrinter.setPrintAppendString(itemLine, normalFormat);
        }
        
        // Print bottom separator
        mPrinter.setPrintAppendString(separator, separatorFormat);
    }
    
    /**
     * Print totals section
     */
    private void printTotals(Map<String, Object> totals, int normalSize) throws Exception {
        PrnStrFormat normalFormat = createFormat(normalSize, false, Layout.Alignment.ALIGN_NORMAL);
        PrnStrFormat boldFormat = createFormat(normalSize + 2, true, Layout.Alignment.ALIGN_NORMAL);
        
        // Print each total line
        for (Map.Entry<String, Object> entry : totals.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // Skip null or empty values
            if (value == null || value.toString().trim().isEmpty()) {
                continue;
            }
            
            String valueStr = value.toString();
            
            // Use bold format for "Total" line
            PrnStrFormat format = key.equalsIgnoreCase("Total") ? boldFormat : normalFormat;
            
            // Format with proper alignment (key left, value right)
            String line = String.format("%-20s %11s", key + ":", valueStr);
            mPrinter.setPrintAppendString(line, format);
        }
        
        // Add spacing after totals
        mPrinter.setPrintAppendString("", normalFormat);
    }
    
    /**
     * Print QR code helper method
     */
    private void printQRCodeHelper(String data, int size, int normalSize) throws Exception {
        PrnStrFormat format = createFormat(normalSize - 2, false, Layout.Alignment.ALIGN_CENTER);
        
        // Add spacing before QR code
        mPrinter.setPrintAppendString("", format);
        
        // Validate QR size (clamp between 100-400 for 58mm paper)
        int validSize = Math.max(100, Math.min(size, 400));
        
        // Print QR code
        mPrinter.setPrintAppendQRCode(data, validSize, validSize, Layout.Alignment.ALIGN_CENTER);
        
        // Add spacing after QR code
        mPrinter.setPrintAppendString("", format);
    }

    private void cutPaper(Result result) {
        if (!checkDeviceReady(result)) return;
        
        if (!isSupportCutter) {
            result.error("NOT_SUPPORTED", "Paper cutter not supported on this device", null);
            return;
        }
        
        executor.execute(() -> {
            try {
                int printStatus = mPrinter.getPrinterStatus();
                if (printStatus == SdkResult.SDK_OK) {
                    mPrinter.openPrnCutter((byte) 1);
                    
                    mainHandler.post(() -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("success", true);
                        response.put("message", "Paper cut successfully");
                        result.success(response);
                    });
                } else {
                    throw new Exception("Printer not ready for cutting");
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to cut paper", e);
                mainHandler.post(() -> {
                    result.error("CUT_ERROR", "Failed to cut paper: " + e.getMessage(), null);
                });
            }
        });
    }

    private void openCashBox(Result result) {
        if (!checkDeviceReady(result)) return;

        executor.execute(() -> {
            try {
                int ret = mPrinter.openBox();

                mainHandler.post(() -> {
                    Map<String, Object> response = new HashMap<>();
                    if (ret == SdkResult.SDK_OK) {
                        response.put("success", true);
                        response.put("message", "Cash drawer opened successfully");
                        result.success(response);
                    } else {
                        result.error("CASH_BOX_ERROR", "Failed to open cash drawer, code: " + ret, null);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Failed to open cash drawer", e);
                mainHandler.post(() -> {
                    result.error("CASH_BOX_ERROR", "Failed to open cash drawer: " + e.getMessage(), null);
                });
            }
        });
    }

    private void getPrinterStatus(Result result) {
        if (!isDeviceInitialized) {
            result.error("DEVICE_NOT_INITIALIZED", "Device must be initialized first", null);
            return;
        }
        
        executor.execute(() -> {
            try {
                int status = mPrinter.getPrinterStatus();
                String statusMessage = getPrinterStatusMessage(status);
                
                mainHandler.post(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("statusCode", status);
                    response.put("statusMessage", statusMessage);
                    response.put("isReady", status == SdkResult.SDK_OK);
                    response.put("isPaperOut", status == SdkResult.SDK_PRN_STATUS_PAPEROUT);
                    result.success(response);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to get printer status", e);
                mainHandler.post(() -> {
                    result.error("STATUS_ERROR", "Failed to get printer status: " + e.getMessage(), null);
                });
            }
        });
    }

    /**
     * Get device serial number
     * Retrieves the unique serial number of the POS device using ZCS SDK
     */
    private void getSerialNumber(Result result) {
        if (!isDeviceInitialized) {
            result.error("DEVICE_NOT_INITIALIZED", "Device must be initialized first", null);
            return;
        }
        
        executor.execute(() -> {
            try {
                Log.d(TAG, "Getting device serial number...");
                
                // Get Sys instance
                Sys sys = mDriverManager.getBaseSysDevice();
                if (sys == null) {
                    throw new Exception("Failed to get Sys device instance");
                }
                
                // Create string array to receive serial number
                String[] sn = new String[1];
                
                // Call getSN method
                int resultCode = sys.getSN(sn);
                
                if (resultCode == SdkResult.SDK_OK) {
                    String serialNumber = sn[0];
                    Log.d(TAG, "Serial number retrieved successfully: " + serialNumber);
                    
                    mainHandler.post(() -> {
                        result.success(serialNumber);
                    });
                } else {
                    // Handle error codes
                    String errorMessage = getSerialNumberErrorMessage(resultCode);
                    Log.e(TAG, "Failed to get serial number. Error code: " + resultCode + " - " + errorMessage);
                    
                    mainHandler.post(() -> {
                        result.error("SERIAL_NUMBER_ERROR", 
                            "Failed to get serial number: " + errorMessage + " (code: " + resultCode + ")", 
                            null);
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Exception while getting serial number", e);
                mainHandler.post(() -> {
                    result.error("SERIAL_NUMBER_ERROR", 
                        "Failed to get serial number: " + e.getMessage(), null);
                });
            }
        });
    }
    
    /**
     * Get error message for serial number error codes
     */
    private String getSerialNumberErrorMessage(int errorCode) {
        switch (errorCode) {
            case SdkResult.SDK_OK:
                return "Success";
            case SdkResult.SDK_ERROR:
                return "General error";
            case SdkResult.SDK_PARAMERR:
                return "Invalid parameter";
            case SdkResult.SDK_TIMEOUT:
                return "Operation timeout";
            default:
                return "Unknown error (code: " + errorCode + ")";
        }
    }

    /**
     * Print raw text string
     * Simple method to print plain text without any formatting structure
     * @param text The raw text string to print
     * @param result Flutter result callback
     */
    private void printRawText(String text, Result result) {
        if (!checkDeviceReady(result)) return;
        
        if (text == null || text.isEmpty()) {
            result.error("INVALID_INPUT", "Text cannot be null or empty", null);
            return;
        }
        
        executor.execute(() -> {
            try {
                // Check printer status
                int printStatus = mPrinter.getPrinterStatus();
                if (printStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
                    throw new Exception("Out of paper");
                }
                
                // Create default format for raw text
                PrnStrFormat format = createFormat(24, false, Layout.Alignment.ALIGN_NORMAL);
                
                // Split text by newlines and print each line
                String[] lines = text.split("\n");
                for (String line : lines) {
                    mPrinter.setPrintAppendString(line, format);
                }
                
                // Start printing
                int result_code = mPrinter.setPrintStart();
                
                if (result_code != SdkResult.SDK_OK) {
                    throw new Exception("Print failed with code: " + result_code);
                }
                
                // Wait for print to complete
                Thread.sleep(1000);
                
                mainHandler.post(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Raw text printed successfully");
                    result.success(response);
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to print raw text", e);
                mainHandler.post(() -> {
                    result.error("PRINT_ERROR", "Failed to print raw text: " + e.getMessage(), null);
                });
            }
        });
    }


    /**
     * Print an image/logo from raw image bytes (PNG, JPG, BMP, etc.)
     *
     * @param imageData Raw encoded image bytes (as received from Flutter Uint8List)
     * @param align     Alignment: "left", "center" (default) or "right"
     * @param width     Optional target width in printer dots; scaled to fit paper if null/too large
     * @param result    Flutter result callback
     */
    private void printImage(byte[] imageData, String align, Integer width, Result result) {
        if (!checkDeviceReady(result)) return;

        if (imageData == null || imageData.length == 0) {
            result.error("INVALID_INPUT", "Image data cannot be null or empty", null);
            return;
        }

        executor.execute(() -> {
            try {
                int printStatus = mPrinter.getPrinterStatus();
                if (printStatus == SdkResult.SDK_PRN_STATUS_PAPEROUT) {
                    throw new Exception("Out of paper");
                }

                // Decode, scale and append the image to the print buffer
                appendImage(imageData, align, width);

                // Start printing
                int result_code = mPrinter.setPrintStart();
                if (result_code != SdkResult.SDK_OK) {
                    throw new Exception("Print failed with code: " + result_code);
                }

                // Wait for print to complete
                Thread.sleep(1000);

                mainHandler.post(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Image printed successfully");
                    result.success(response);
                });

            } catch (Exception e) {
                Log.e(TAG, "Failed to print image", e);
                mainHandler.post(() -> {
                    result.error("PRINT_ERROR", "Failed to print image: " + e.getMessage(), null);
                });
            }
        });
    }

    /**
     * Decode raw image bytes into a Bitmap, scale it to fit the paper width and
     * append it to the current print buffer. Does NOT call setPrintStart(), so it
     * can be reused both standalone (printImage) and inline (printDynamic logo).
     */
    private void appendImage(byte[] imageData, String align, Integer width) throws Exception {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.length);
        if (bitmap == null) {
            throw new Exception("Failed to decode image data (unsupported or corrupt format)");
        }

        try {
            // Max printable width in dots (80mm paper ~576 dots, 58mm ~384 dots)
            int printerMaxWidth = mPrinter.is80MMPrinter() ? 576 : 384;

            int targetWidth = printerMaxWidth;
            if (width != null && width > 0) {
                targetWidth = Math.min(width, printerMaxWidth);
            }

            // Scale when an explicit width is requested, or when the image is wider
            // than the paper. Preserve aspect ratio; never upscale unless width given.
            if ((width != null && width > 0) || bitmap.getWidth() > targetWidth) {
                int targetHeight = (int) ((long) bitmap.getHeight() * targetWidth / bitmap.getWidth());
                if (targetHeight < 1) targetHeight = 1;
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                    bitmap = scaled;
                }
            }

            mPrinter.setPrintAppendBitmap(bitmap, getAlignment(align));
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    /**
     * Map a string alignment to the SDK's Layout.Alignment enum
     */
    private Layout.Alignment getAlignment(String align) {
        if (align == null) return Layout.Alignment.ALIGN_CENTER;
        switch (align.toLowerCase()) {
            case "left":
                return Layout.Alignment.ALIGN_NORMAL;
            case "right":
                return Layout.Alignment.ALIGN_OPPOSITE;
            case "center":
            default:
                return Layout.Alignment.ALIGN_CENTER;
        }
    }

    private boolean checkDeviceReady(Result result) {
        if (!isDeviceInitialized) {
            result.error("DEVICE_NOT_INITIALIZED", "Device must be initialized first", null);
            return false;
        }
        if (!isDeviceOpened) {
            result.error("DEVICE_NOT_OPENED", "Device must be opened first", null);
            return false;
        }
        return true;
    }
    

    private String getPrinterStatusMessage(int status) {
        switch (status) {
            case SdkResult.SDK_OK:
                return "Ready";
            case SdkResult.SDK_PRN_STATUS_PAPEROUT:
                return "Out of paper";
            default:
                return "Status code: " + status;
        }
    }

 
private void stopQRScan(Result result) {
    if (!checkDeviceReady(result)) return;
    
    executor.execute(() -> {
        try {
            // Power off scanner
            mHQrsanner.QRScanerPowerCtrl((byte)0);
            mHQrsanner.QRScanerCtrl((byte)0);
            
            mainHandler.post(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "QR scanner powered off");
                response.put("data", "");
                result.success(response);
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop QR scanner", e);
            mainHandler.post(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to stop QR scanner: " + e.getMessage());
                response.put("data", "");
                result.success(response);
            });
        }
    });
}

private void scanQRCodeOnce(Result result) {
    if (!checkDeviceReady(result)) return;
    
    if (mHQrsanner == null) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "QR Scanner not available on this device");
        response.put("data", "");
        result.success(response);
        return;
    }
    
    // Prevent multiple simultaneous scans
    if (isWaitingForScan) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Scanner is already in use");
        response.put("data", "");
        result.success(response);
        return;
    }
    
    executor.execute(() -> {
        try {
            Log.d(TAG, "Starting QR scan...");
            
            // Store the result callback
            pendingScanResult = result;
            isWaitingForScan = true;
            
            // Create EditText to capture scan results
            mainHandler.post(() -> {
                scanResultEditText = new EditText(context);
                scanResultEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
                    if (isWaitingForScan) {
                        String scannedData = textView.getText().toString().trim();
                        if (!scannedData.isEmpty()) {
                            handleScanResult(scannedData);
                        }
                    }
                    return false;
                });
                
                startScanningProcess();
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start QR scan", e);
            cleanupScan();
            mainHandler.post(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Failed to start QR scan: " + e.getMessage());
                response.put("data", "");
                result.success(response);
            });
        }
    });
}

private void startScanningProcess() {
    try {
        // Power on and activate scanner
        mHQrsanner.QRScanerCtrl((byte)1);
        mHQrsanner.QRScanerPowerCtrl((byte)0);
        SystemClock.sleep(10);
        mHQrsanner.QRScanerPowerCtrl((byte)1);
        SystemClock.sleep(100);
        
        // Request focus to capture scan input
        if (scanResultEditText != null) {
            scanResultEditText.requestFocus();
        }
        
        // Set timeout for scan operation
        mainHandler.postDelayed(() -> {
            if (isWaitingForScan) {
                handleScanTimeout();
            }
        }, 10000);
        
        Log.d(TAG, "QR scanner activated, waiting for scan...");
        
    } catch (Exception e) {
        Log.e(TAG, "Failed to start scanning process", e);
        handleScanError("Failed to activate scanner: " + e.getMessage());
    }
}

private void handleScanResult(String scannedData) {
    if (!isWaitingForScan || pendingScanResult == null) {
        return;
    }
    
    try {
        Log.d(TAG, "QR scan result received: " + scannedData);
        
        // Immediately close scanner after successful scan
        closeScanner();
        
        // Return result to Flutter
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "QR code scanned successfully");
        response.put("data", scannedData);
        
        pendingScanResult.success(response);
        cleanupScan();
        
    } catch (Exception e) {
        Log.e(TAG, "Error handling scan result", e);
        handleScanError("Error processing scan result: " + e.getMessage());
    }
}

private void handleScanTimeout() {
    Log.d(TAG, "QR scan timeout");
    closeScanner();
    
    if (pendingScanResult != null) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Scan timeout - no QR code detected");
        response.put("data", "");
        
        pendingScanResult.success(response);
    }
    cleanupScan();
}

private void handleScanError(String errorMessage) {
    Log.e(TAG, "QR scan error: " + errorMessage);
    closeScanner();
    
    if (pendingScanResult != null) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", errorMessage);
        response.put("data", "");
        
        pendingScanResult.success(response);
    }
    cleanupScan();
}

private void closeScanner() {
    try {
        if (mHQrsanner != null) {
            mHQrsanner.QRScanerCtrl((byte)0);
            mHQrsanner.QRScanerPowerCtrl((byte)0);
        }
        Log.d(TAG, "Scanner closed successfully");
    } catch (Exception e) {
        Log.e(TAG, "Error closing scanner", e);
    }
}

private void cleanupScan() {
    isWaitingForScan = false;
    pendingScanResult = null;
    scanResultEditText = null;
}
// // Add this method to handle successful scan results
// private void handleScanResult(String scannedData) {
//     if (!isWaitingForScan || pendingScanResult == null) {
//         return;
//     }
    
//     try {
//         Log.d(TAG, "QR scan result received: " + scannedData);
        
//         // Stop the scanner
//         mHQrsanner.QRScanerCtrl((byte)0);
//         mHQrsanner.QRScanerPowerCtrl((byte)0);
        
//         // Clean up
//         isWaitingForScan = false;
//         lastScannedData = scannedData;
        
//         // Return result to Flutter
//         Map<String, Object> response = new HashMap<>();
//         response.put("success", true);
//         response.put("message", "QR code scanned successfully");
//         response.put("data", scannedData);
        
//         pendingScanResult.success(response);
//         pendingScanResult = null;
        
//     } catch (Exception e) {
//         Log.e(TAG, "Error handling scan result", e);
//         handleScanError("Error processing scan result: " + e.getMessage());
//     }
// }

// Add this method to handle scan timeouts
// private void handleScanTimeout() {
//     if (!isWaitingForScan || pendingScanResult == null) {
//         return;
//     }
    
//     try {
//         Log.w(TAG, "QR scan timeout");
        
//         // Stop the scanner
//         mHQrsanner.QRScanerCtrl((byte)0);
//         mHQrsanner.QRScanerPowerCtrl((byte)0);
        
//         // Clean up
//         isWaitingForScan = false;
        
//         // Return timeout result
//         Map<String, Object> response = new HashMap<>();
//         response.put("success", false);
//         response.put("message", "Scan timeout - no QR code detected");
//         response.put("data", "");
        
//         pendingScanResult.success(response);
//         pendingScanResult = null;
        
//     } catch (Exception e) {
//         Log.e(TAG, "Error handling scan timeout", e);
//         handleScanError("Scan timeout error: " + e.getMessage());
//     }
// }

// // Add this method to handle scan errors
// private void handleScanError(String errorMessage) {
//     if (!isWaitingForScan || pendingScanResult == null) {
//         return;
//     }
    
//     try {
//         // Stop the scanner
//         if (mHQrsanner != null) {
//             mHQrsanner.QRScanerCtrl((byte)0);
//             mHQrsanner.QRScanerPowerCtrl((byte)0);
//         }
//     } catch (Exception e) {
//         Log.w(TAG, "Failed to stop scanner during error handling", e);
//     }
    
//     // Clean up
//     isWaitingForScan = false;
    
//     // Return error result
//     pendingScanResult.error("SCANNER_ERROR", errorMessage, null);
//     pendingScanResult = null;
// }

// // Add this method to get the last scanned data
// private void getLastScannedData(Result result) {
//     Map<String, Object> response = new HashMap<>();
//     response.put("success", true);
//     response.put("data", lastScannedData);
//     response.put("message", lastScannedData.isEmpty() ? "No data scanned yet" : "Last scanned data retrieved");
//     result.success(response);
// }


}

