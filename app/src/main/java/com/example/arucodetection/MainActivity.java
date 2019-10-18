package com.example.arucodetection;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.annotation.TargetApi;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.Manifest.permission.CAMERA;


public class MainActivity extends AppCompatActivity{

    private static final int REQUEST_CAMERA_PERMISSION = 1234;

    static {
        System.loadLibrary("native-lib");
    }


    public native void ConvertRGBtoGray(int srcWidth, int srcHeight, ByteBuffer buffer, Surface dst);

    //public native void setCamera2ImageArUco(Image src, Surface dst);

    private TextureView mTextureView;

    private ImageReader mImageReader;

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener
            = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image nextImage = reader.acquireNextImage();
            Image.Plane[] planes = nextImage.getPlanes();
            Log.e("SSSSSSSSSSSSSSSSSSSSSSSSS", "planes " + planes.length);
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(nextImage.getWidth(), nextImage.getHeight());
            Surface dstSurface = new Surface(texture);
            ConvertRGBtoGray(nextImage.getWidth(), nextImage.getHeight(), planes[0].getBuffer(), dstSurface);
            nextImage.close();
        }
    };

    private CameraDevice mCamera;
    private Size mPreviewSize;
    private CameraCaptureSession mCameraSession;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private HandlerThread mBackgroundThread;
    private Handler mBackgroundHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 카메라 권한 체크
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            return;
        }
        mBackgroundThread = new HandlerThread("CameraBackground");
        mBackgroundThread.start();
        mBackgroundHandler = new Handler(mBackgroundThread.getLooper());
        Size largest = new Size(5312,2988);
        mImageReader = ImageReader.newInstance(largest.getWidth(), largest.getHeight(), ImageFormat.JPEG, /*maxImages*/2);

        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

        initTextureView();
    }

    private void initTextureView() {
        mTextureView = (TextureView) findViewById(R.id.texture);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
                Log.e("cklee", "MMM onSurfaceTextureAvailable");
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
                Log.e("cklee", "MMM onSurfaceTextureSizeChanged");
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                Log.e("cklee", "MMM onSurfaceTextureDestroyed");
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
                // 화면 갱신시마다 불림
//                Log.e("cklee", "MMM onSurfaceTextureUpdated");
            }
        });
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String[] cameraIdArray = manager.getCameraIdList();
            Log.e("cklee", "MMM cameraIds = " + Arrays.deepToString(cameraIdArray));

            // test 로 0 번 camera 를 사용
            String oneCameraId = cameraIdArray[0];

            CameraCharacteristics cameraCharacter = manager.getCameraCharacteristics(oneCameraId);
            Log.e("cklee", "MMM camraCharacter = " + cameraCharacter);

            StreamConfigurationMap map = cameraCharacter.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size[] sizesForStream = map.getOutputSizes(SurfaceTexture.class);
            Log.e("cklee", "MMM sizesForStream = " + Arrays.deepToString(sizesForStream));

            // 가장 큰 사이즈부터 들어있다
            mPreviewSize = sizesForStream[0];

            if(ActivityCompat.checkSelfPermission(this, CAMERA) != PackageManager.PERMISSION_GRANTED){
                return;
            }
            manager.openCamera(oneCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    mCamera = cameraDevice;
                    showCameraPreview();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    mCamera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int errorCode) {
                    Log.e("cklee", "MMM errorCode = " + errorCode);
                    mCamera.close();
                    mCamera = null;
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e("cklee", "MMM openCamera ", e);
        }
    }

    private void showCameraPreview() {
        try {
            SurfaceTexture texture = mTextureView.getSurfaceTexture();
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface textureViewSurface = new Surface(texture);

            mCaptureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            //mCaptureRequestBuilder.addTarget(textureViewSurface);
            // Caemara Image 경로 변경 2개의 surface 1개는 출력용 1개는 이미지 처리용, 이미지 처리용으로 받아서 opencv로 넘기도록 한다 -> imagereader를 통해
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

            List<Surface> outputSurfaces = new ArrayList<>();
            outputSurfaces.add(mImageReader.getSurface());

            mCamera.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mCameraSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Log.e("cklee", "MMM onConfigureFailed");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e("cklee", "MMM showCameraPreview ", e);
        }
    }

    private void updatePreview() {
        try {
            mCameraSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
        } catch (CameraAccessException e) {
            Log.e("cklee", "MMM updatePreview", e);
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            openCamera();
        }
    }
}