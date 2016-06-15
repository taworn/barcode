package diy.barcode;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PreviewCallback;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * A basic camera previewer class
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = CameraPreview.class.getSimpleName();

    private SurfaceHolder holder = null;

    private int cameraId = -1;
    private Camera camera = null;
    private PreviewCallback previewCallback = null;
    private AutoFocusCallback autoFocusCallback = null;
    private List<Camera.Size> supportedPreviewSizes = null;
    private Camera.Size previewSize = null;
    private int degrees = 0;

    private Paint paint = new Paint();

    public CameraPreview(@NonNull Context context) {
        super(context);
    }

    public CameraPreview(@NonNull Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CameraPreview(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public CameraPreview(@NonNull Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void open(int cameraId, @Nullable PreviewCallback previewCb, @Nullable AutoFocusCallback autoFocusCb) throws Exception {
        this.cameraId = cameraId;
        this.camera = Camera.open(cameraId);
        if (this.camera != null) {
            previewCallback = previewCb;
            autoFocusCallback = autoFocusCb;
            supportedPreviewSizes = null;
            if (camera != null) {
                Camera.Parameters parameters = this.camera.getParameters();
                if (parameters != null)
                    supportedPreviewSizes = parameters.getSupportedPreviewSizes();
            }
            previewSize = null;
            degrees = 0;

            // installs a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            holder = getHolder();
            holder.addCallback(this);
        }
    }

    public void close() {
        if (holder != null) {
            holder.removeCallback(this);
            holder = null;
        }
        degrees = 0;
        previewSize = null;
        supportedPreviewSizes = null;
        autoFocusCallback = null;
        previewCallback = null;
        if (camera != null) {
            camera.release();
            camera = null;
        }
        cameraId = -1;
    }

    public int getCameraDisplayOrientation() {
        return degrees;
    }

    public void setCameraDisplayOrientation(Activity activity) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }
        else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        this.degrees = result;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int width = resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        final int height = resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);
        if (supportedPreviewSizes != null) {
            previewSize = getOptimalPreviewSize(supportedPreviewSizes, width, height);
            setMeasuredDimension(width, height);
        }
        else
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        if (sizes == null)
            return null;

        double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) h / w;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.height / size.width;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - h) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - h);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - h) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - h);
                }
            }
        }

        return optimalSize;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        if (camera != null) {
            try {
                camera.setPreviewDisplay(holder);
            }
            catch (IOException e) {
                Log.d(TAG, "surfaceCreated error: " + e.getMessage());
            }
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            try {
                camera.stopPreview();
                camera.setPreviewDisplay(null);
            }
            catch (IOException e) {
                Log.d(TAG, "surfaceDestroyed error: " + e.getMessage());
            }
        }
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        /*
         * If your preview can change or rotate, take care of those events here.
         * Make sure to stop the preview before resizing or reformatting it.
         */
        if (this.holder.getSurface() == null) {
            // preview surface does not exist
            return;
        }

        // stop preview before making changes
        try {
            if (camera != null)
                camera.stopPreview();
        }
        catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }

        try {
            if (camera != null) {
                Camera.Parameters parameters = camera.getParameters();
                if (parameters != null) {
                    parameters.setPreviewSize(previewSize.width, previewSize.height);
                    //camera.setDisplayOrientation(90);
                    camera.setPreviewDisplay(holder);
                    camera.setPreviewCallback(previewCallback);
                    camera.startPreview();
                    camera.autoFocus(autoFocusCallback);
                }
            }
        }
        catch (Exception e) {
            Log.d(TAG, "surfaceChanged error: " + e.getMessage());
        }
    }

}
