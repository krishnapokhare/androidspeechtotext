package com.kpokhare.offlinespeechrecognizer;

import android.media.MediaRecorder;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;

public class CallRecordingActivity extends BaseActivity {

    private static final String TAG = "CallRecordingActivity";
    private MediaRecorder recorder;
    private boolean isRecordStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_recording);
        final TextView recordingTextView = findViewById(R.id.recordingTextView);
        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FloatingActionButton f = ((FloatingActionButton) v);
                if (recordingTextView.getText() != "Recording Started") {
                    recordingTextView.setText("Recording Started");
//                    Snackbar.make(v, "Recording Started", Snackbar.LENGTH_SHORT)
//                            .setAction("Action", null).show();
                    f.setImageResource(R.drawable.ic_pause_black_24dp);
                    startMediaRecorder(getAudioSource("VOICE_CALL"));
                } else {
                    recordingTextView.setText("Recording Stopped");
//                    Snackbar.make(v, "Recording Stopped", Snackbar.LENGTH_SHORT)
//                            .setAction("Action", null).show();
                    f.setImageResource(R.drawable.ic_mic_black_24dp);
                    stopRecording();
                }
            }
        });
    }

    public File createDirIfNotExists(String path) {
        File folder = new File(Environment.getExternalStorageDirectory() + "/PhoneCallRecording");

        if (!folder.exists()) {
            if (!folder.mkdirs()) {
                Log.e("TravellerLog :: ", "folder is created");
            }
        }


        File file = new File(folder, path + ".3GPP");

        try {
            if (!file.exists()) {
                if (file.createNewFile()) {
                    Log.e("TravellerLog :: ", "file is created");
                }
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return file;


    }

    public void stopRecording() {
        recorder.stop();

        recorder.reset();

        recorder.release();

        recorder = null;
    }

    private boolean startMediaRecorder(final int audioSource) {
//        File dir = Environment.getExternalStorageDirectory();
//        final File audioFile;
//        try {
//            audioFile = File.createTempFile("sound", ".3gp", dir);
//        } catch (IOException e) {
//            Log.e(TAG, "external storage access error");
//            return false;
//        }
        final File audioFile = createDirIfNotExists(Calendar.getInstance().getTime().toString());

        recorder = new MediaRecorder();
        try {
            recorder.reset();
            recorder.setAudioSource(audioSource);
            recorder.setAudioSamplingRate(8000);
            recorder.setAudioEncodingBitRate(12200);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            String fileName = audioFile.getAbsolutePath();
            recorder.setOutputFile(fileName);

            MediaRecorder.OnErrorListener errorListener = new MediaRecorder.OnErrorListener() {
                public void onError(MediaRecorder arg0, int arg1, int arg2) {
                    Log.e(TAG, "OnErrorListener " + arg1 + "," + arg2);
                    audioFile.delete();
                }
            };
            recorder.setOnErrorListener(errorListener);

            MediaRecorder.OnInfoListener infoListener = new MediaRecorder.OnInfoListener() {
                public void onInfo(MediaRecorder arg0, int arg1, int arg2) {
                    Log.e(TAG, "OnInfoListener " + arg1 + "," + arg2);
                    audioFile.delete();
                }
            };
            recorder.setOnInfoListener(infoListener);


            recorder.prepare();
            // Sometimes prepare takes some time to complete
            Thread.sleep(2000);
            recorder.start();
            isRecordStarted = true;
            return true;
        } catch (Exception e) {
            e.getMessage();
            return false;
        }
    }

    public static int getAudioSource(String str) {
        if (str.equals("MIC")) {
            return MediaRecorder.AudioSource.MIC;
        } else if (str.equals("VOICE_COMMUNICATION")) {
            return MediaRecorder.AudioSource.VOICE_COMMUNICATION;
        } else if (str.equals("VOICE_CALL")) {
            return MediaRecorder.AudioSource.VOICE_CALL;
        } else if (str.equals("VOICE_DOWNLINK")) {
            return MediaRecorder.AudioSource.VOICE_DOWNLINK;
        } else if (str.equals("VOICE_UPLINK")) {
            return MediaRecorder.AudioSource.VOICE_UPLINK;
        } else if (str.equals("VOICE_RECOGNITION")) {
            return MediaRecorder.AudioSource.VOICE_RECOGNITION;
        } else if (str.equals("CAMCORDER")) {
            return MediaRecorder.AudioSource.CAMCORDER;
        } else {
            return MediaRecorder.AudioSource.DEFAULT;
        }
    }
}
