package com.djl.camera;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.djl.camera.util.DJLLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * Created by DJl on 2016/8/17.
 * email:1554068430@qq.com
 * 封装的相机操作接口
 */

public interface ICameraOperate {

    /**
     * 添加相机cameraId显示预览的view
     */
    void addPreviewView(String cameraId, PreviewConfig config);

    /**
     * 清除一个预览显示
     */
    void removePreviewView(String cameraId, TextureView previewView);

    /**
     * 打开相机
     */
    void openCamera(String cameraId);

    /**
     * 打开相机
     */
    void closeCamera(String cameraId);

    /**
     * 保存一张照片
     */
    void takePhoto(String cameraId, PhotoConfig config);

    /**
     * 开始录像
     */
    void startVideoRecord(String cameraId, VideoConfig config);

    /**
     * 停止录像
     */
    void stopVideoRecord(String cameraId, TextureView previewView);


    class PreviewConfig {
        TextureView previewView;
        // TODO
    }

    class PhotoConfig {
        String SavePath;
        // TODO
    }

    class VideoConfig {
        String SavePath;
        // TODO
    }


    class CameraOperate implements ICameraOperate {
        Context mContext;
        private CameraManager mCameraManager;
        HashMap<String, CameraConfig> mCameras = new HashMap<>();
        public String[] mCameraIdList;

        public CameraOperate(Context mContext) {
            this.mContext = mContext;
            mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            try {
                mCameraIdList = mCameraManager.getCameraIdList();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void addPreviewView(String cameraId, PreviewConfig config) {
            if (mCameras.get(cameraId) == null) {
                mCameras.put(cameraId, new CameraConfig(cameraId));
            }
            mCameras.get(cameraId).previewConfigs.add(config);
        }

        @Override
        public void removePreviewView(String cameraId, TextureView previewView) {
            CameraConfig cameraConfig = mCameras.get(cameraId);
            if (cameraConfig != null) {
                PreviewConfig previewConfigToRemove = null;
                for (PreviewConfig previewConfig : cameraConfig.previewConfigs) {
                    if (previewConfigToRemove.previewView.equals(previewView)) {
                        previewConfigToRemove = previewConfig;
                    }
                }
                cameraConfig.previewConfigs.remove(previewConfigToRemove);
            }
        }

        @Override
        public void openCamera(String cameraId) {
            if (mCameras.get(cameraId) != null) {
                mCameras.get(cameraId).startBackgroundThread();
                mCameras.get(cameraId).openCamera();
            } else {
                DJLLog.e("Camera ： " + cameraId + " not init open failed");
            }
        }

        @Override
        public void closeCamera(String cameraId) {
            if (mCameras.get(cameraId) != null) {
                mCameras.get(cameraId).closeCamera();
                mCameras.get(cameraId).stopBackgroundThread();
            } else {
                DJLLog.e("Camera ： " + cameraId + " not init open failed");
            }
        }

        @Override
        public void takePhoto(String cameraId, PhotoConfig config) {

        }

        @Override
        public void startVideoRecord(String cameraId, VideoConfig config) {

        }

        @Override
        public void stopVideoRecord(String cameraId, TextureView previewView) {

        }

        class CameraConfig {
            String CameraId;
            ArrayList<PreviewConfig> previewConfigs = new ArrayList<>();
            HandlerThread backgroundThread;
            Handler mBackgroundHandler;
            private Semaphore mCameraOpenCloseLock = new Semaphore(1);
            CameraDevice mCameraDevice;
            private CameraCaptureSession mPreviewSession;
            private CaptureRequest.Builder mPreviewBuilder;
            /**
             * MediaRecorder
             */
            private MediaRecorder mMediaRecorder;

            public CameraConfig(String cameraId) {
                CameraId = cameraId;
            }


            void startBackgroundThread() {
                backgroundThread = new HandlerThread("camera:" + CameraId + ">backgroundThread");
                backgroundThread.start();
                mBackgroundHandler = new Handler(backgroundThread.getLooper());
            }

            void stopBackgroundThread() {
                backgroundThread.quitSafely();
                try {
                    backgroundThread.join();
                    backgroundThread = null;
                    mBackgroundHandler = null;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            void openCamera() {
                try {
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    if (!mCameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                        throw new RuntimeException("Time out waiting to lock camera opening.");
                    }
                    CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(CameraId);
                    StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    map.getOutputSizes(TextureView.class);
                            mCameraManager.openCamera(CameraId, mStateCallback, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera opening.");
                }
            }

            void closeCamera() {
                try {
                    mCameraOpenCloseLock.acquire();
                    if (null != mCameraDevice) {
                        mCameraDevice.close();
                        mCameraDevice = null;
                    }
                    if (null != mMediaRecorder) {
                        mMediaRecorder.release();
                        mMediaRecorder = null;
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException("Interrupted while trying to lock camera closing.");
                } finally {
                    mCameraOpenCloseLock.release();
                }
            }

            private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice cameraDevice) {
                    DJLLog.i("CameraDevice:" + CameraId + " - onOpened");
                    mCameraDevice = cameraDevice;
                    startPreview();
                    mCameraOpenCloseLock.release();
                }

                @Override
                public void onDisconnected(CameraDevice cameraDevice) {
                    DJLLog.i("CameraDevice:" + CameraId + " - onDisconnected");
                    mCameraOpenCloseLock.release();
                }

                @Override
                public void onError(CameraDevice cameraDevice, int i) {
                    DJLLog.i("CameraDevice:" + CameraId + " - onError");
                    mCameraOpenCloseLock.release();
                }
            };

            /**
             * Update the camera preview. {@link #startPreview()} needs to be called in advance.
             */

            private void updatePreview() {
                if (null == mCameraDevice) {
                    return;
                }
                try {
                    mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
                    mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, mBackgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            private void startPreview() {
                try {
                    mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    ArrayList<Surface> surfaces = new ArrayList<>();
                    for (PreviewConfig previewConfig : previewConfigs) {
                        SurfaceTexture surfaceTexture = previewConfig.previewView.getSurfaceTexture();
                        Surface surface = new Surface(surfaceTexture);
                        surfaces.add(surface);
                        mPreviewBuilder.addTarget(surface);
                    }
                    mCameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession cameraCaptureSession) {
                            DJLLog.i("CameraDevice:" + CameraId + " - createCaptureSession - onConfigured");
                            mPreviewSession = cameraCaptureSession;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            DJLLog.i("CameraDevice:" + CameraId + " - createCaptureSession - onConfigureFailed");
                        }
                    }, mBackgroundHandler);

                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

        }

        /**
         * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
         * width and height are at least as large as the respective requested values, and whose aspect
         * ratio matches with the specified value.
         *
         * @param choices     The list of sizes that the camera supports for the intended output class
         * @param width       The minimum desired width
         * @param height      The minimum desired height
         * @param aspectRatio The aspect ratio
         * @return The optimal {@code Size}, or an arbitrary one if none were big enough
         */
        private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
            // Collect the supported resolutions that are at least as big as the preview Surface
            List<Size> bigEnough = new ArrayList<Size>();
            int w = aspectRatio.getWidth();
            int h = aspectRatio.getHeight();
            for (Size option : choices) {
                if (option.getHeight() == option.getWidth() * h / w &&
                        option.getWidth() >= width && option.getHeight() >= height) {
                    bigEnough.add(option);
                }
            }

            // Pick the smallest of those, assuming we found any
            if (bigEnough.size() > 0) {
                return Collections.min(bigEnough, new CompareSizesByArea());
            } else {
                DJLLog.e("Couldn't find any suitable preview size");
                return choices[0];
            }
        }

        /**
         * Compares two {@code Size}s based on their areas.
         */
        static class CompareSizesByArea implements Comparator<Size> {

            @Override
            public int compare(Size lhs, Size rhs) {
                // We cast here to ensure the multiplications won't overflow
                return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                        (long) rhs.getWidth() * rhs.getHeight());
            }

        }

        public static class ErrorDialog extends DialogFragment {

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Activity activity = getActivity();
                return new AlertDialog.Builder(activity)
                        .setMessage("This device doesn't support Camera2 API.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                activity.finish();
                            }
                        })
                        .create();
            }

        }
    }
}
