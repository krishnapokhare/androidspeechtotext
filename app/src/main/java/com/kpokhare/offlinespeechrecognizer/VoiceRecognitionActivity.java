package com.kpokhare.offlinespeechrecognizer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class VoiceRecognitionActivity extends AppCompatActivity implements RecognitionListener {

    private static final int REQUEST_RECORD_PERMISSION = 100;
    private Button recordingButton;
    private TextView returnedText;
    private TextView wordCountTextView;
    private TextView errorTextView;
    //    ToggleButton toggleButton;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    private String LOG_TAG = "VoiceRecognitionActivity";
    private boolean stopListening = false;
    private int count = 0;
    private String finalResult = "";
    private Date startTime;
    private int WordCountInterval = 5;
    private int WordCountIntervalIncrementor = WordCountInterval;
    private int wordCount;
    private long avgWordCount;
    private SharedPreferences preferences;
    private boolean isRecordingInProgress = false;
    private long totalSpeechTime=0;
    private Date partialSpeechStartDate,partialSpeechEndDate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_recognition);

        returnedText = findViewById(R.id.resultsTextView);
        progressBar = findViewById(R.id.progressBar1);
        wordCountTextView = findViewById(R.id.wordCountTextView);
        errorTextView = findViewById(R.id.errorTextView);
        recordingButton = findViewById(R.id.recordingButton);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);


        progressBar.setVisibility(View.INVISIBLE);
        InitializeSpeechSettings();
    }

    private void InitializeSpeechSettings() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                "en");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        String i = preferences.getString("edit_text_Silence", "5");
        Log.i(LOG_TAG, "Silence Settings Value " + i);
        int silenceSeconds = Integer.parseInt(i) * 1000;
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceSeconds);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceSeconds);

        i = preferences.getString("minimum_speech_interval", "15");
        Log.i(LOG_TAG, "Minimum Speech Interval " + i);
        int minimumInterval = Integer.parseInt(i) * 1000;
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, minimumInterval);
        String j = preferences.getString("word_count_interval", "5");
        WordCountInterval = Integer.parseInt(j);
        WordCountIntervalIncrementor = WordCountInterval;
        returnedText.setText("");
        finalResult = "";

    }

    public void onRecordingBtnClick(View view) {
        if (!isRecordingInProgress) {
            startTime = Calendar.getInstance().getTime();
            Log.i(LOG_TAG, "Start Button Clicked");
            errorTextView.setText("");
            InitializeSpeechSettings();
            StartListeningSpeech();
            recordingButton.setText("Stop Recording");
            isRecordingInProgress = true;
            stopListening = false;
        } else {
            Log.i(LOG_TAG, "Stop Button Clicked");
            stopListening = true;
            StopListeningSpeech();
            recordingButton.setText("Start Recording");
            isRecordingInProgress = false;
        }
    }

    private void StartListeningSpeech() {
        Log.i(LOG_TAG, "Checked");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
        ActivityCompat.requestPermissions
                (VoiceRecognitionActivity.this,
                        new String[]{Manifest.permission.RECORD_AUDIO},
                        REQUEST_RECORD_PERMISSION);
        errorTextView.setText("");
    }

    private void StopListeningSpeech() {
        Log.i(LOG_TAG, "Unchecked");
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.INVISIBLE);
        speech.stopListening();
    }


    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.i(LOG_TAG, "onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.i(LOG_TAG, "onBeginningOfSpeech");
        progressBar.setIndeterminate(false);
        progressBar.setMax(10);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        //Log.i(LOG_TAG, "onRmsChanged: " + rmsdB);
        progressBar.setProgress((int) rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.i(LOG_TAG, "onBufferReceived: " + buffer);
    }

    @Override
    public void onEndOfSpeech() {
        Log.i(LOG_TAG, "onEndOfSpeech");
        progressBar.setIndeterminate(true);
        StopListeningSpeech();
    }

    @Override
    public void onError(int errorCode) {
        String errorMessage = getErrorText(errorCode);
        Log.e(LOG_TAG, "FAILED " + errorMessage);
        errorTextView.setText(errorMessage);

        if (!stopListening && errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
            Log.i(LOG_TAG, Integer.toString(count));
            StartListeningSpeech();
            count++;
        } else {
            recordingButton.setText("Start Recording");
        }
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        String text = "";
        if (matches != null) {
            finalResult = finalResult + matches.get(0) + ". ";
        }
        returnedText.setText(finalResult);

        if (!stopListening) {
            Log.i(LOG_TAG, Integer.toString(count));
            StartListeningSpeech();
            count++;
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.i(LOG_TAG, "onPartialResults");
        long timeElapsedInMS = Calendar.getInstance().getTimeInMillis() - startTime.getTime();
        long timeElapsedInS = timeElapsedInMS / 1000;
        Log.i(LOG_TAG, "Time Elapsed in sec:" + Long.toString(timeElapsedInS));
        ArrayList<String> matches = partialResults
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            String partialFinalResults = finalResult + matches.get(0);
            returnedText.setText(partialFinalResults);

            if (timeElapsedInS >= WordCountIntervalIncrementor) {
                wordCount = VoiceRecognitionActivity.countWordsUsingSplit(partialFinalResults);
                Log.i(LOG_TAG, "Word Count:" + Integer.toString(wordCount));
                avgWordCount = wordCount / (timeElapsedInS / WordCountInterval);
                Log.i(LOG_TAG, "Avg Word Count:" + Long.toString(avgWordCount));
                WordCountIntervalIncrementor = WordCountIntervalIncrementor + WordCountInterval;
                wordCountTextView.setText("Status:" + Long.toString(avgWordCount) + " words per " + Integer.toString(WordCountInterval) + " seconds.");
            }
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.i(LOG_TAG + "onEvent", "onEvent");
    }

    private static String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO:
                message = "Audio recording error";
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                message = "Client side error";
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                message = "Insufficient permissions";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                message = "Network error";
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                message = "Network timeout";
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                message = "No match";
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                message = "RecognitionService busy";
                break;
            case SpeechRecognizer.ERROR_SERVER:
                message = "error from server";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                message = "No speech input";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    speech.startListening(recognizerIntent);
                } else {
                    Toast.makeText(VoiceRecognitionActivity.this, "Permission Denied!", Toast
                            .LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (speech != null) {
            speech.destroy();
            Log.i(LOG_TAG, "destroy");
        }
    }

    private static int countWordsUsingSplit(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        String[] words = input.split("\\s+");
        return words.length;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // launch settings activity
            startActivity(new Intent(VoiceRecognitionActivity.this, SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
