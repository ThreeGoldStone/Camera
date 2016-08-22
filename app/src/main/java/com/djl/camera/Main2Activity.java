package com.djl.camera;

import android.graphics.SurfaceTexture;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;

public class Main2Activity extends AppCompatActivity {

    private TextureView mTextureView;
    private ICameraOperate.CameraOperate mCameraOperate;
    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener mSurfaceTextureListener
            = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            mCameraOperate.openCamera(mCameraOperate.mCameraIdList[0]);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
//            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        mTextureView = (TextureView) findViewById(R.id.textureView1);
        mCameraOperate = new ICameraOperate.CameraOperate(this);
        ICameraOperate.PreviewConfig config = new ICameraOperate.PreviewConfig();
        config.previewView = mTextureView;
        mCameraOperate.addPreviewView(mCameraOperate.mCameraIdList[0], config);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mTextureView.isAvailable()) {
            mCameraOperate.openCamera(mCameraOperate.mCameraIdList[0]);
        } else {
            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraOperate.closeCamera(mCameraOperate.mCameraIdList[0]);
    }
}
