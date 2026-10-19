package com.hgnice.wingo;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenCaptureService extends Service {

    public static final String ACTION_OCR_UPDATE =
            "com.hgnice.wingo.OCR_UPDATE";
    public static final String EXTRA_OCR_TEXT = "ocr_text";

    private static final String CHANNEL_ID = "hgnice_capture";
    private static final int NOTIFICATION_ID = 17;

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private com.google.mlkit.vision.text.TextRecognizer recognizer;
    private final AtomicBoolean processing = new AtomicBoolean(false);

    private int screenWidth;
    private int screenHeight;
    private int density;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "HGNICE Screen Analysis",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm =
                    getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }

        recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification;
        if (Build.VERSION.SDK_INT >= 26) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("HGNICE WinGo Analyzer")
                    .setContentText("স্ক্রিনের লেখা শনাক্ত করা হচ্ছে")
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("HGNICE WinGo Analyzer")
                    .setContentText("স্ক্রিনের লেখা শনাক্ত করা হচ্ছে")
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .build();
        }

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo
                            .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            );
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }

        if (projection != null) return START_NOT_STICKY;

        int resultCode = intent.getIntExtra("resultCode", 0);

        Intent data;
        if (Build.VERSION.SDK_INT >= 33) {
            data = intent.getParcelableExtra("data", Intent.class);
        } else {
            data = intent.getParcelableExtra("data");
        }

        if (data == null) {
            sendText("স্ক্রিন ক্যাপচারের অনুমতি পাওয়া যায়নি।");
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(
                            MEDIA_PROJECTION_SERVICE);

            projection = manager.getMediaProjection(resultCode, data);

            if (projection == null) {
                sendText("স্ক্রিন ক্যাপচার শুরু করা যায়নি।");
                stopSelf();
                return START_NOT_STICKY;
            }

            projection.registerCallback(
                    new MediaProjection.Callback() {
                        @Override
                        public void onStop() {
                            stopCapture();
                            stopSelf();
                        }
                    },
                    null
            );

            startCapture();
        } catch (Exception e) {
            sendText("ক্যাপচার সমস্যা: " + e.getClass().getSimpleName());
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void startCapture() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)
                getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getRealMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        density = metrics.densityDpi;

        imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                PixelFormat.RGBA_8888, 2);

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;

            if (!processing.compareAndSet(false, true)) {
                image = reader.acquireLatestImage();
                if (image != null) image.close();
                return;
            }

            try {
                image = reader.acquireLatestImage();

                if (image == null) {
                    processing.set(false);
                    return;
                }

                Image.Plane plane = image.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();
                int pixelStride = plane.getPixelStride();
                int rowStride = plane.getRowStride();

                int paddedWidth = rowStride / pixelStride;

                Bitmap padded = Bitmap.createBitmap(
                        paddedWidth, screenHeight,
                        Bitmap.Config.ARGB_8888);

                padded.copyPixelsFromBuffer(buffer);

                Bitmap bitmap = Bitmap.createBitmap(
                        padded, 0, 0, screenWidth, screenHeight);

                padded.recycle();
                image.close();
                image = null;

                InputImage input = InputImage.fromBitmap(bitmap, 0);

                recognizer.process(input)
                        .addOnSuccessListener(result -> {
                            String text = result.getText();
                            if (text != null && !text.trim().isEmpty()) {
                                sendText(text);
                            }
                        })
                        .addOnFailureListener(e ->
                                sendText("স্ক্রিনের লেখা পড়া যায়নি।"))
                        .addOnCompleteListener(task -> {
                            bitmap.recycle();
                            processing.set(false);
                        });

            } catch (Exception e) {
                if (image != null) image.close();
                processing.set(false);
                sendText("স্ক্রিন পড়তে সমস্যা হয়েছে।");
            }
        }, null);

        virtualDisplay = projection.createVirtualDisplay(
                "HGNICE_Screen",
                screenWidth, screenHeight, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, null);

        sendText("স্ক্রিন ক্যাপচার চালু হয়েছে; লেখা শনাক্ত করা হচ্ছে।");
    }

    private void sendText(String text) {
        Intent update = new Intent(ACTION_OCR_UPDATE);
        update.setPackage(getPackageName());
        update.putExtra(EXTRA_OCR_TEXT, text);
        sendBroadcast(update);
    }

    private void stopCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (projection != null) {
            projection = null;
        }
    }

    @Override
    public void onDestroy() {
        stopCapture();

        if (recognizer != null) {
            recognizer.close();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
            }
