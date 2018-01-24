package io.agora;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.agora.rtc.Constants;
import io.agora.rtc.IRtcEngineEventHandler;
import io.agora.rtc.RtcEngine;
import io.agora.rtc.mediaio.AgoraTextureCamera;
import io.agora.rtc.mediaio.AgoraTextureView;
import io.agora.rtc.mediaio.IVideoSink;

import static io.agora.rtc.mediaio.MediaIO.BufferType.BYTE_ARRAY;
import static io.agora.rtc.mediaio.MediaIO.BufferType.BYTE_BUFFER;
import static io.agora.rtc.mediaio.MediaIO.BufferType.TEXTURE;
import static io.agora.rtc.mediaio.MediaIO.PixelFormat.I420;
import static io.agora.rtc.mediaio.MediaIO.PixelFormat.TEXTURE_OES;


/**
 * Created by wyylling@gmail.com on 03/01/2018.
 */

public class AgoraActivity extends AppCompatActivity {
    private static final String TAG = AgoraActivity.class.getSimpleName();

    private RtcEngine mRtcEngine;
    private IRtcEngineEventHandler mRtcEventHandler;
    private AgoraTextureCamera mVideoSource;
    private IVideoSink mLocalVideoRender;
    private Map<Integer, Boolean> mUsers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_textureview_render);

        initRtcEngine();
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkSelfPermissions();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        RtcEngine.destroy();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        switch (requestCode) {
            case PERMISSION_REQ_ID_RECORD_AUDIO: {
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA);
                } else {
                    finish();
                }
                break;
            }
            case PERMISSION_REQ_ID_CAMERA: {
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                    checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
                    //((AGApplication) getApplication()).initWorkerThread();
                } else {
                    finish();
                }
                break;
            }
            case PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE: {
                if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    finish();
                }
                break;
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Standard Android full-screen functionality.
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void initRtcEngine() {
        try {
            mRtcEventHandler = new IRtcEngineEventHandler() {
                @Override
                public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
                    Log.e(TAG, "onJoinChannelSuccess");
                }

                @Override
                public void onFirstRemoteVideoDecoded(int uid, int width, int height, int elapsed) {
                    Log.e(TAG, "onFirstRemoteVideoDecoded");
                    addRemoteRender(uid, width, height);
                }

                @Override
                public void onUserOffline(int uid, int reason) {
                }

                @Override
                public void onUserJoined(int uid, int elapsed) {
                }

                @Override
                public void onError(int err) {
                }

                @Override
                public void onWarning(int warn) {
                }
            };

            mRtcEngine = RtcEngine.create(this, getString(R.string.private_broadcasting_app_id), mRtcEventHandler);
            mRtcEngine.setParameters("{\"rtc.log_filter\": 65535}");
            mRtcEngine.setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING);
            mRtcEngine.enableVideo();
            mRtcEngine.enableDualStreamMode(true);

            mRtcEngine.setVideoProfile(Constants.VIDEO_PROFILE_480P, false);
            mRtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER);

            createVideoSource();
            createVideoRender();

            mRtcEngine.setVideoSource(mVideoSource);
            mRtcEngine.setLocalVideoRenderer(mLocalVideoRender);

            mRtcEngine.startPreview();

            mRtcEngine.joinChannel(null, "hq", "hq with RtcEngine", 0);

        } catch (Exception ex) {
           // printLog(ex.toString());
        }
    }

    private void createVideoSource() {
        mVideoSource = new AgoraTextureCamera(this,640, 480);
    }

    private void createVideoRender() {
        mLocalVideoRender = (AgoraTextureView) findViewById(R.id.main_textureview);
        ((AgoraTextureView)mLocalVideoRender).init((mVideoSource.getEglContext()));
        ((AgoraTextureView)mLocalVideoRender).setBufferType(TEXTURE);
        ((AgoraTextureView)mLocalVideoRender).setPixelFormat(TEXTURE_OES);

    }

    private void addRemoteRender(int uid, int width, int height) {
        if (mUsers.containsKey(uid)) return;

        mUsers.put(uid, true);
        AgoraTextureView render = (AgoraTextureView)findViewById(R.id.remote_textureview);
        render.init(null);
        render.setBufferType(BYTE_BUFFER);
        render.setPixelFormat(I420);
        mRtcEngine.setRemoteVideoRenderer(uid, render);
    }

    private static final int BASE_VALUE_PERMISSION = 0X0001;
    private static final int PERMISSION_REQ_ID_RECORD_AUDIO = BASE_VALUE_PERMISSION + 1;
    private static final int PERMISSION_REQ_ID_CAMERA = BASE_VALUE_PERMISSION + 2;
    private static final int PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE = BASE_VALUE_PERMISSION + 3;

    private boolean checkSelfPermissions() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO) &&
                checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA) &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQ_ID_WRITE_EXTERNAL_STORAGE);
    }

    public boolean checkSelfPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
            return false;
        }

        return true;
    }
}
