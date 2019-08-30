package soft.wl.textureviewcamera;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "TextureViewCamera";

    private Button m_play_button = null;

    private TextureView m_textureView = null;
    private Surface m_surface = null;
    private CameraManager m_cameraManager = null;
    private HandlerThread m_handlerThread = null;
    private Handler m_handler = null;
    private String m_cameraID = null;
    private List<Surface> m_surface_lsit = null;
    private CameraDevice m_cameraDevice = null;
    private CaptureRequest.Builder m_captureRequestBuilder = null;
    private CaptureRequest m_captureRequest = null;
    private CameraCaptureSession m_cameraCaptureSession = null;

    private CameraCaptureSession.StateCallback m_cameraCaptureStateCallback = new CameraCaptureSession.StateCallback() {
        @Override
        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
            Log.d(TAG, "onConfigured: ");
            m_cameraCaptureSession = cameraCaptureSession;
            try {
                // 设置连续自动对焦
                m_captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 设置关闭闪光灯
                m_captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.FLASH_MODE_OFF);
                // 生成一个预览的请求
                m_captureRequest = m_captureRequestBuilder.build();
                // 开始预览，即设置反复请求
                m_cameraCaptureSession.setRepeatingRequest(m_captureRequest, null, m_handler);

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
            Log.d(TAG, "onConfigureFailed: ");
        }
    };

    private CameraManager.AvailabilityCallback m_availabilityCallback = new CameraManager.AvailabilityCallback() {
        @Override
        public void onCameraAvailable(String cameraId) {
            Log.d(TAG, "onCameraAvailable cameraId : " + cameraId);
            m_cameraID = cameraId;
        }

        @Override
        public void onCameraUnavailable(String cameraId) {
            Log.d(TAG, "onCameraUnavailable cameraId : " + cameraId);
        }
    };

    private CameraDevice.StateCallback m_stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice cameraDevice) {
            Log.d(TAG, "onOpened: ");
            m_cameraDevice = cameraDevice;
            try {
                m_captureRequestBuilder = m_cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                m_captureRequestBuilder.addTarget(m_surface);
                m_cameraDevice.createCaptureSession(m_surface_lsit, m_cameraCaptureStateCallback, m_handler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(CameraDevice cameraDevice) {
            Log.d(TAG, "onDisconnected: ");
        }

        @Override
        public void onError(CameraDevice cameraDevice, int i) {
            Log.d(TAG, "onError: ");
        }

        @Override
        public void onClosed(CameraDevice camera) {
            super.onClosed(camera);
            Log.d(TAG, "onClosed: ");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ");
        setContentView(R.layout.activity_main);

        m_surface_lsit = new ArrayList<>();

        m_play_button = findViewById(R.id.play_button);

        if (null != m_play_button) {
            m_play_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "m_play_button onClick: ");
                    if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    try {
                        Log.d(TAG, "onClick: m_cameraID : " + m_cameraID);
                        m_cameraManager.openCamera(m_cameraID, m_stateCallback, m_handler);

                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        m_textureView = findViewById(R.id.texture_view);

        if(null != m_textureView) {
            m_textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                    Log.d(TAG, "onSurfaceTextureAvailable: ");
                    m_surface = new Surface(surfaceTexture);
                    m_surface_lsit.add(m_surface);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {
                    Log.d(TAG, "onSurfaceTextureSizeChanged: ");
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                    Log.d(TAG, "onSurfaceTextureDestroyed: ");
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                    Log.d(TAG, "onSurfaceTextureUpdated: ");
                }
            });
        }

        checkPermission(getApplicationContext(), Manifest.permission.CAMERA);

        m_handlerThread = new HandlerThread("CameraBackground");
        m_handlerThread.start();
        m_handler = new Handler(m_handlerThread.getLooper());

        m_cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        m_cameraManager.registerAvailabilityCallback(m_availabilityCallback, m_handler);
    }

    public void checkPermission(Context context, String permion) {
        int check_permission = ContextCompat.checkSelfPermission(context, permion);
        if(check_permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[] {permion}, 1);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: ");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: ");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        if(null != m_cameraManager) {
            m_cameraManager.unregisterAvailabilityCallback(m_availabilityCallback);
        }
        if(null != m_cameraDevice) {
            m_cameraDevice.close();
        }
    }
}
