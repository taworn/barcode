package diy.barcode;

import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.IOException;
import java.util.List;

/**
 * A camera preview fragment.
 */
public class CameraFragment extends Fragment implements SurfaceHolder.Callback {

    static {
        System.loadLibrary("iconv");
    }

    public interface Callback {
        void getData(@Nullable String data);
    }

    private static final String TAG = CameraFragment.class.getSimpleName();

    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private int cameraId;
    private Camera camera;
    private Camera.Parameters defaultParameters;
    private Camera.Parameters parameters;

    private Handler handler = new Handler();
    private Runnable doAutoFocus = new Runnable() {
        public void run() {
            camera.autoFocus(autoFocusCallback);
        }
    };
    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            handler.postDelayed(doAutoFocus, 1000);
        }
    };

    private ImageScanner scanner;
    private Callback callback;
    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();

            Image barcode = new Image(size.width, size.height, "Y800");
            barcode.setData(data);

            int result = scanner.scanImage(barcode);
            if (result != 0 && callback != null) {
                SymbolSet syms = scanner.getResults();
                for (Symbol sym : syms) {
                    callback.getData(sym.getData());
                }
            }
        }
    };

    public CameraFragment() {
        // Required empty public constructor
    }

    public static CameraFragment newInstance() {
        CameraFragment fragment = new CameraFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, String.format("onCreate(): savedInstanceState = %h", savedInstanceState));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);
        Log.d(TAG, String.format("onCreateView(): container = %h, savedInstanceState = %h", container, savedInstanceState));

        surfaceView = (SurfaceView) view.findViewById(R.id.surface_view);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        cameraId = -1;
        camera = null;
        defaultParameters = null;
        parameters = null;

        scanner = new ImageScanner();
        scanner.setConfig(0, Config.X_DENSITY, 3);
        scanner.setConfig(0, Config.Y_DENSITY, 3);

        callback = null;
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, String.format("onActivityCreated(): savedInstanceState = %h", savedInstanceState));
        if (savedInstanceState != null) {
            cameraId = savedInstanceState.getInt("cameraId", -1);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        Log.d(TAG, String.format("onSaveInstanceState(): savedInstanceState = %h", savedInstanceState));
        savedInstanceState.putInt("cameraId", cameraId);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause()");
        release();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()");
        acquire();
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d(TAG, String.format("surfaceCreated(): surfaceHolder == %h", surfaceHolder));
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int w, int h) {
        Log.d(TAG, String.format("surfaceChanged(): surfaceHolder == %h, format = %d, w = %d, h = %d", surfaceHolder, format, w, h));
        if (camera != null) {
            try {
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                camera.setPreviewCallback(previewCallback);
                camera.autoFocus(autoFocusCallback);
            }
            catch (Exception e) {
                Log.d(TAG, "Camera cannot starting preview! error: " + e.getMessage());
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        Log.d(TAG, String.format("surfaceDestroyed(): surfaceHolder == %h", surfaceHolder));
        if (camera != null) {
            handler.removeCallbacks(doAutoFocus);
            camera.setPreviewCallback(null);
            camera.stopPreview();
        }
    }

    public int getCameraId() {
        return cameraId;
    }

    public void setCameraId(int value) {
        Log.d(TAG, String.format("setCameraId(): cameraId = %d, value = %d", cameraId, value));
        if (cameraId != value) {
            release();
            cameraId = value;
            acquire();
        }
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(@Nullable Callback value) {
        callback = value;
    }

    private void acquire() {
        if (cameraId >= 0) {
            if (camera == null) {
                Camera c = Camera.open(cameraId);
                if (c != null) {
                    camera = c;
                    setupParameters();
                    try {
                        camera.setPreviewDisplay(surfaceHolder);
                        camera.startPreview();
                        camera.setPreviewCallback(previewCallback);
                        camera.autoFocus(autoFocusCallback);
                    }
                    catch (IOException e) {
                        Log.d(TAG, "camera.setPreviewDisplay(surfaceHolder) error: " + e.getMessage());
                    }
                    Log.d(TAG, "acquire(): camera acquire OK");
                }
                else
                    Log.d(TAG, "acquire(): camera acquire NOT OK!");
            }
            else
                Log.d(TAG, "acquire(): camera already acquire");
        }
    }

    private void release() {
        if (camera != null) {
            Log.d(TAG, "release(): camera != null");
            try {
                handler.removeCallbacks(doAutoFocus);
                camera.setPreviewCallback(null);
                camera.stopPreview();
                camera.setPreviewDisplay(null);
            }
            catch (IOException e) {
                Log.d(TAG, "camera.setPreviewDisplay(null) error: " + e.getMessage());
            }
            if (defaultParameters != null)
                camera.setParameters(defaultParameters);
            camera.release();
            camera = null;
        }
        else
            Log.d(TAG, "release(): camera == null");
    }

    private void setupParameters() {
        // retrieves system information
        int rotation = getActivity().getWindowManager().getDefaultDisplay().getRotation();
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
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int finalDegrees;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            finalDegrees = (info.orientation + degrees) % 360;
            finalDegrees = (360 - finalDegrees) % 360;  // compensate the mirror
        }
        else {  // back-facing
            finalDegrees = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(finalDegrees);

        // gets camera parameters
        defaultParameters = camera.getParameters();
        parameters = camera.getParameters();

        // sets auto focus mode, if available
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO))
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        // sets flash light, if available
        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes.contains(Camera.Parameters.FLASH_MODE_AUTO))
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);

        // sets camera parameters
        camera.setParameters(parameters);
    }

}
