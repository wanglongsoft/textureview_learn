# Android TextureView和Camera的基本用法
### 1. 简介
&ensp;&ensp;&ensp;&ensp;该工程采用TextureView和Camera实时显示手机前置相机的预览图像，通过该工程，可以学会TextureView和Camera的基本用法，如：  
1. TextureView常用函数有哪些，怎么与Camera关联起来
2. Camera接口的基本使用方式
3. TextureView和Camera的相关知识点  
 
### 2. TextureView基本用法
&ensp;&ensp;&ensp;&ensp;Android的TextureView可以将内容流直接投影到View中，可以用于实现LivePreview等功能，和SurfaceView不同，它不会在WMS中
单独创建窗口，而是作为View hierachy中的一个普通View，因此可以和其它普通View一样进行移动，旋转，缩放，动画等变化。
##### 关键代码
获取实例
```java
m_textureView = findViewById(R.id.texture_view);
```
实现SurfaceTextureListener
```java
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
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {//画面退出时调用（onDestroy）
        Log.d(TAG, "onSurfaceTextureDestroyed: ");
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        Log.d(TAG, "onSurfaceTextureUpdated: ");
    }
});
```
### 3 Camera基本用法(Camera2 API)
##### 关键代码
获取CameraManager实例，并实现AvailabilityCallback回调
```java
private CameraManager.AvailabilityCallback m_availabilityCallback = new CameraManager.AvailabilityCallback() {
    @Override
    public void onCameraAvailable(String cameraId) {//"0" 代表后置摄像头， "1" 代表前置摄像头
        Log.d(TAG, "onCameraAvailable cameraId : " + cameraId);
    }

    @Override
    public void onCameraUnavailable(String cameraId) {
        Log.d(TAG, "onCameraUnavailable cameraId : " + cameraId);
    }
};

m_handlerThread = new HandlerThread("CameraBackground");
m_handlerThread.start();
m_handler = new Handler(m_handlerThread.getLooper());

m_cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
m_cameraManager.registerAvailabilityCallback(m_availabilityCallback, m_handler);
```java
打开手机前置摄像头
```java
if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {//Camera权限验证
    return;
}
try {
    Log.d(TAG, "onClick: m_cameraID : " + m_cameraID);
    m_cameraManager.openCamera("0", m_stateCallback, m_handler);//强制设置为：后置摄像头

} catch (CameraAccessException e) {
    e.printStackTrace();
}
```java
设置预览请求
```java
private CameraDevice.StateCallback m_stateCallback = new CameraDevice.StateCallback() {
    @Override
    public void onOpened(CameraDevice cameraDevice) {//Camrea打开成功时回调
        Log.d(TAG, "onOpened: ");
        m_cameraDevice = cameraDevice;
        try {//设置预览请求
            m_captureRequestBuilder = m_cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            m_captureRequestBuilder.addTarget(m_surface);//与TextureView的Surface与Camera请求关联
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
```
开始预览
```java
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
```
### 4. 相关知识点
##### TextureView知识点
+ 必须开启硬件加速（这个默认就是开启的）
+ 可以像常规视图（View）那样使用它，包括进行平移、缩放等操作
+ 实现SurfaceTextureListener接口，监控SurfaceTexture 的各种状态
+ 调用TextureView的draw方法时，如果还没有初始化SurfaceTexture,那么就会初始化它,初始化好时，就会回调onSurfaceTextureAvailable,表示可以接收外界的绘制指令了,然后SurfaceTexture会以GL纹理信息更新到TextureView对应的HardwareLayer中
+ 接收外界的绘制指令的方式通常有两种，Surface提供dequeueBuffer/queueBuffer等硬件渲染接口和lockCanvas/unlockCanvasAndPost等软件渲染接口，使内容流的源可以往BufferQueue中填graphic buffer
+ 接收视频流的对象是SurfaceTexture，本质上是Surface，而不是textureView，textureView只是作为一个硬件加速层来展示
+ textureView的接口setSurfaceTexture可以设置SurfaceTexture,达到复用的目的
##### Camera知识点
[Camera实践指南](https://www.androidos.net.cn/doc/day/2018-02-18/15363.md)
