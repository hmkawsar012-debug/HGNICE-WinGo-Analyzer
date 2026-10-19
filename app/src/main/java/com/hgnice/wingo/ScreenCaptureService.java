package com.hgnice.wingo;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {

    private static final String CHANNEL_ID = "hgnice_screen_capture";
    public static final String OCR_ACTION =
            "com.hgnice.wingo.OCR_TEXT";
    public static final String OCR_EXTRA = "recognized_text";

    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private TextRecognizer recognizer;
    private Handler handler;
    private boolean processing = false;
    private String lastText = "";

    private final MediaProjection.Callback projectionCallback =
            new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    releaseCapture();
                    stopSelf();
                }
            };

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());
        recognizer = TextRecognition.getClient(
                TextRecognizerOptions.DEFAULT_OPTIONS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "HGNICE Screen Capture",
                    NotificationManager.IMPORTANCE_LOW);

            NotificationManager manager =
                    getSystemService(NotificationManager.class);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("HGNICE WinGo Analyzer")
                    .setContentText("স্ক্রিনের লেখা শনাক্ত করার চেষ্টা চলছে")
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .build();
        } else {
            notification = new Notification.Builder(this)
                    .setContentTitle("HGNICE WinGo Analyzer")
                    .setContentText("স্ক্রিন বিশ্লেষণ চলছে")
                    .setSmallIcon(android.R.drawable.ic_menu_view)
                    .build();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    1,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        } else {
            startForeground(1, notification);
        }

        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        int resultCode = intent.getIntExtra("resultCode", 0);
        Intent data;

        if (Build.VERSION.SDK_INT >= 33) {
            data = intent.getParcelableExtra("data", Intent.class);
        } else {
            data = intent.getParcelableExtra("data");
        }

        if (data == null || resultCode != Activity.RESULT_OK) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(
                            Context.MEDIA_PROJECTION_SERVICE);

            projection = manager.getMediaProjection(resultCode, data);

            if (projection == null) {
                stopSelf();
                return START_NOT_STICKY;
            }

            DisplayMetrics metrics = new DisplayMetrics();
            WindowManager wm =
                    (WindowManager) getSystemService(WINDOW_SERVICE);
            wm.getDefaultDisplay().getRealMetrics(metrics);

            int width = metrics.widthPixels;
            int height = metrics.heightPixels;
            int density = metrics.densityDpi;

            imageReader = ImageReader.newInstance(
                    width, height, PixelFormat.RGBA_8888, 2);

            projection.registerCallback(projectionCallback, handler);

            virtualDisplay = projection.createVirtualDisplay(
                    "HGNICE-Screen",
                    width,
                    height,
                    density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.getSurface(),
                    null,
                    handler);

            imageReader.setOnImageAvailableListener(
                    this::processLatestImage, handler);

        } catch (Exception e) {
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void processLatestImage(ImageReader reader) {
        if (processing) {
            return;
        }

        Image image = null;
        Bitmap bitmap = null;

        try {
            image = reader.acquireLatestImage();

            if (image == null) {
                return;
            }

            Image.Plane plane = image.getPlanes()[0];
            ByteBuffer buffer = plane.getBuffer();
            int pixelStride = plane.getPixelStride();
            int rowStride = plane.getRowStride();
            int width = image.getWidth();
            int height = image.getHeight();
            int paddedWidth = rowStride / pixelStride;

            Bitmap padded = Bitmap.createBitmap(
                    paddedWidth, height, Bitmap.Config.ARGB_8888);

            padded.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(padded, 0, 0, width, height);
            padded.recycle();

        } catch (Exception e) {
            if (bitmap != null) {
                bitmap.recycle();
            }
        } finally {
            if (image != null) {
                image.close();
            }
        }

        if (bitmap == null) {
            return;
        }

        processing = true;
        Bitmap captured = bitmap;

        recognizer.process(InputImage.fromBitmap(captured, 0))
                .addOnSuccessListener(result -> {
                    String text = result.getText().trim();

                    if (!text.isEmpty() && !text.equals(lastText)) {
                        lastText = text;

                        Intent update = new Intent(OCR_ACTION);
                        update.setPackage(getPackageName());
                        update.putExtra(OCR_EXTRA, text);
                        sendBroadcast(update);
                    }
                })
                .addOnCompleteListener(task -> {
                    captured.recycle();
                    processing = false;
                });
    }

    private void releaseCapture() {
        if (imageReader != null) {
            imageReader.setOnImageAvailableListener(null, null);
            imageReader.close();
            imageReader = null;
        }

        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }
    }

    @Override
    public void onDestroy() {
        releaseCapture();

        if (projection != null) {
            projection.unregisterCallback(projectionCallback);
            projection.stop();
            projection = null;
        }

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
