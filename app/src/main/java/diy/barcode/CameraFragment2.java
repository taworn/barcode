package diy.barcode;

import android.app.Fragment;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraFragment2 extends Fragment implements SurfaceHolder.Callback {

    private static final String TAG = "CameraFragment";

    private Camera camera = null;
    private Camera.Parameters parameters = null;
    private Camera.Parameters defaultParameters = null;

    private Handler handler = null;
    private Runnable runnable = new Runnable() {
        public void run() {
            if (camera != null)
                camera.autoFocus(callback);
        }
    };
    Camera.AutoFocusCallback callback = new Camera.AutoFocusCallback() {
        public void onAutoFocus(boolean success, Camera camera) {
            handler.postDelayed(runnable, 1000);
        }
    };

    private int cameraDegrees = 0;

    public CameraFragment2() {
        // Required empty public constructor
    }

    public static CameraFragment2 newInstance() {
        CameraFragment2 fragment = new CameraFragment2();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_camera, container, false);

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

        try {
            camera = Camera.open(); // attempt to get a Camera instance

            // retrieves camera information
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(0, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraDegrees = (cameraInfo.orientation + degrees) % 360;
                cameraDegrees = (360 - cameraDegrees) % 360;  // compensate the mirror
            }
            else {  // back-facing
                cameraDegrees = (cameraInfo.orientation - degrees + 360) % 360;
            }

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

            camera.release();
            camera = null;
        }
        catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.d(TAG, "Camera is not available! reason: " + e.getMessage());
        }

        // prepares SurfaceView to preview
        SurfaceView surfaceView = (SurfaceView) view.findViewById(R.id.surface_view);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        acquire();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        release();
    }

    private void acquire() {
        try {
            camera = Camera.open(); // attempt to get a Camera instance
            camera.setDisplayOrientation(cameraDegrees);
            camera.setParameters(parameters);
            camera.autoFocus(callback);
        }
        catch (Exception e) {
            // Camera is not available (in use or does not exist)
            Log.d(TAG, "Camera is not available! reason: " + e.getMessage());
        }
    }

    private void release() {
        if (camera != null) {
            parameters = camera.getParameters();
            camera.setParameters(defaultParameters);
            camera.release();
            camera = null;
        }
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        // stop preview before making changes
        try {
            if (camera == null) {
                try {
                    acquire();
                }
                catch (Exception e) {
                    // Camera is not available (in use or does not exist)
                    Log.d(TAG, "Camera is not available! reason: " + e.getMessage());
                }
            }
            camera.stopPreview();
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        }
        catch (Exception e) {
            // ignore: tried to stop a non-existent preview
            Log.d(TAG, "Camera not starting preview! reason: " + e.getMessage());
        }
    }

    public void save() {
        Camera.PictureCallback pictureCB = new Camera.PictureCallback() {
            public void onPictureTaken(byte[] data, Camera cam) {
                // restart previewing
                camera.startPreview();

                // format string for file
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                File file;
                try {
                    file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "IMG_" + timestamp + ".jpg");
                }
                catch (Exception e) {
                    Log.e(TAG, "Couldn't create media file! reason: " + e.getMessage());
                    return;
                }

                try {
                    FileOutputStream stream = new FileOutputStream(file);
                    stream.write(data);
                    stream.close();
                }
                catch (FileNotFoundException e) {
                    Log.e(TAG, "File not found! reason: " + e.getMessage());
                    e.getStackTrace();
                }
                catch (IOException e) {
                    Log.e(TAG, "I/O error writing file! reason: " + e.getMessage());
                    e.getStackTrace();
                }
            }
        };
        camera.takePicture(null, null, pictureCB);
    }

}
