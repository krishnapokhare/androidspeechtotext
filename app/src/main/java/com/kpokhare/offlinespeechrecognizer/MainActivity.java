package com.kpokhare.offlinespeechrecognizer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

import static com.kpokhare.offlinespeechrecognizer.BaseActivity.LOG_TAG_DEBUG;

public class MainActivity extends AppCompatActivity implements RecognitionListener {
    private static final String MINIMUM_SPEECH_INTERVAL = "15";
    private static final String SILENCE_LENGTH = "5";
    boolean isRecording = false;
    FloatingActionButton fab;
    private SpeechRecognizer speech;
    private Intent recognizerIntent;
    private SharedPreferences preferences;
    private String recordingLangCode;
    private TextView speechTextView;
    private String finalResult;
    private boolean stopRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar)findViewById(R.id.main_toolbar));
        speechTextView = findViewById(R.id.speechTextView);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecordingButtonClick();
            }
        });
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    private void onRecordingButtonClick() {
        if (!isRecording) {
            onStartButtonClick();
        } else {
            onStopButtonClick();
        }
    }

    private void onStartButtonClick() {
        fab.setImageResource(R.drawable.ic_pause_black_24dp);
        isRecording = true;
        stopRecording=false;
        InitializeSpeechSettings();
        startListening();
        Snackbar.make(findViewById(R.id.mainCoordinatorLayout), "Recording in progress", Snackbar.LENGTH_INDEFINITE).show();
    }

    private void onStopButtonClick(){
        stopRecording=true;
        stopListening();
        Snackbar.make(findViewById(R.id.mainCoordinatorLayout), "Recording Stopped", Snackbar.LENGTH_SHORT).show();
        isRecording = false;
        fab.setImageResource(R.drawable.ic_mic_black_24dp);
    }

    private void InitializeSpeechSettings() {
        Log.d(LOG_TAG_DEBUG, "Method: InitializeSpeechSettings");
//        totalSpeechTime = 0;
//        timerInSeconds = 0;
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        Log.d(LOG_TAG_DEBUG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_LENGTH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_LENGTH);

        recordingLangCode = preferences.getString("languages", "en-US");

        Log.d(LOG_TAG_DEBUG, "Language" + recordingLangCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recordingLangCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, recordingLangCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, recordingLangCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, MINIMUM_SPEECH_INTERVAL);
//        String j = preferences.getString("word_count_interval", "5");
//        WordCountInterval = Integer.parseInt(j);
//        WordCountIntervalIncrementor = WordCountInterval;
//
//        returnedText.setText("");
//        wordCountTextView.setText(getString(R.string.average_word_count_text));
//        keywordTextView.setText(getString(R.string.keyword_count_text));
        finalResult = "";
        speechTextView.setText(finalResult);
    }

    private void stopListening() {
        if (speech != null) {
            speech.stopListening();
        }

    }

    private void startListening() {
        if (speech != null && !stopRecording) { //Checking if Stop Recording button is clicked in UI
            speech.startListening(recognizerIntent);
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(LOG_TAG_DEBUG, "Method:onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(LOG_TAG_DEBUG, "Method:onBeginningOfSpeech");
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        Log.d(LOG_TAG_DEBUG,"Method:onRmsChanged");
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d(LOG_TAG_DEBUG, "Method:onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(LOG_TAG_DEBUG, "Method:onEndOfSpeech");
    }

    @Override
    public void onError(int error) {
        Log.d(LOG_TAG_DEBUG, "Method:onError"+getErrorText(error));
        if(error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT){
            startListening();
        }else if(error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
            if(speech != null) {
                speech.cancel();
                speech.destroy();
                speech = null;
            }
        }
        else{
            Snackbar.make(findViewById(R.id.mainCoordinatorLayout), "Error:" + getErrorText(error), Snackbar.LENGTH_SHORT).show();
            onStopButtonClick();
        }
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(LOG_TAG_DEBUG, "Method:onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
            new ProcessResult().execute(matches.get(0));
            startListening();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(LOG_TAG_DEBUG, "Method:onPartialResults");
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
            new ProcessPartialResult().execute(matches.get(0));
        }
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d(LOG_TAG_DEBUG, "Method:onEvent");
    }

    private static String getErrorText(int errorCode) {
        //Log.d(LOG_TAG_DEBUG,"IntializeTextToSpeech");
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
                message = "Timed out due to no speech";
                break;
            default:
                message = "Didn't understand, please try again.";
                break;
        }
        return message;
    }

    private class ProcessPartialResult extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            String partialResult = strings[0];
            Log.d(LOG_TAG_DEBUG + "ASYNC", partialResult);
            String partialFinalResult = finalResult + " " + partialResult;
            return partialFinalResult;
        }

        @Override
        protected void onPostExecute(String result) {
            final String returnedValue = result;
            super.onPostExecute(result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    speechTextView.setText(returnedValue);
                }
            });

        }
    }

    private class ProcessResult extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            String finalResultParam = strings[0];
            Log.d(LOG_TAG_DEBUG, finalResult);
            finalResult = finalResult + " " + finalResultParam;
            return finalResult;
        }

        @Override
        protected void onPostExecute(String result) {
            final String returnedValue = result;
            super.onPostExecute(result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    speechTextView.setText(returnedValue);
                }
            });

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (speech != null) {
            speech.stopListening();
            speech.cancel();
            speech.destroy();
            speech = null;
        }
    }
}
