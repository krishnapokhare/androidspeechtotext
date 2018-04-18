package com.kpokhare.offlinespeechrecognizer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.sql.Array;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class VoiceRecognitionActivity extends AppCompatActivity implements RecognitionListener {

    private static final int REQUEST_RECORD_PERMISSION = 100;
    private Button recordingButton;
    private TextView timerTextView, wordCountTextView, errorTextView, keywordTextView,titleTextView;
    private EditText returnedText;
    private ImageView SpeakButton;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private TextToSpeech textToSpeech = null;
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
    private long totalSpeechTime = 0;
    private Date intervalSpeechStartDate, intervalSpeechStopDate;
    Handler mHandler;
    private Runnable mRunnable;
    int timerInSeconds=0;
    String[] languages;
    String[] languageValues;
    boolean readyToSpeak=false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_recognition);

        titleTextView = findViewById(R.id.title);
        returnedText = findViewById(R.id.resultsTextView);
        progressBar = findViewById(R.id.progressBar1);
        wordCountTextView = findViewById(R.id.wordCountTextView);
        errorTextView = findViewById(R.id.errorTextView);
        recordingButton = findViewById(R.id.recordingButton);
        timerTextView = findViewById(R.id.timerTextView);
        keywordTextView = findViewById(R.id.keywordTextView);
        SpeakButton =findViewById(R.id.textToSpeechImageView);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        progressBar.setVisibility(View.INVISIBLE);
        languages = getResources().getStringArray(R.array.languages);
        languageValues = getResources().getStringArray(R.array.languages_values);

        //InitializeSpeechSettings();
    }

    private void InitializeSpeechSettings() {
        totalSpeechTime = 0;
        timerInSeconds=0;
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        Log.i(LOG_TAG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));
        speech.setRecognitionListener(this);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        String i = preferences.getString("edit_text_Silence", getString(R.string.word_count_interval_default));
        Log.i(LOG_TAG, "Silence Settings Value " + i);
        int silenceSeconds = Integer.parseInt(i) * 1000;
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silenceSeconds);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, silenceSeconds);

        String language=preferences.getString("languages","en-US");
        Log.i(LOG_TAG, "Language" + language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,language);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,language);

        i = preferences.getString("minimum_speech_interval", getString(R.string.minimum_speech_interval_default));
        Log.i(LOG_TAG, "Minimum Speech Interval " + i);
        int minimumInterval = Integer.parseInt(i) * 1000;
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, minimumInterval);
        String j = preferences.getString("word_count_interval", "5");
        WordCountInterval = Integer.parseInt(j);
        WordCountIntervalIncrementor = WordCountInterval;
        returnedText.setText("");
        wordCountTextView.setText("Average Word Count: ");
        keywordTextView.setText("Keyword Count: ");
        finalResult = "";
        mHandler = new Handler();
        mHandler.post(mRunnable);
        new Runnable() {
            @Override
            public void run() {
                timerTextView.setText(String.valueOf(timerInSeconds++));
                mHandler.postDelayed(this, 1000);
            }
        }.run();

    }

    public void onRecordingBtnClick(View view) {
        if (!isRecordingInProgress) {
            if (ContextCompat.checkSelfPermission(VoiceRecognitionActivity.this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startTime = Calendar.getInstance().getTime();
                Log.d(LOG_TAG, "Start Button Clicked");
                errorTextView.setText("");

                InitializeSpeechSettings();
                StartListeningSpeech();
                recordingButton.setText(R.string.recording_stop_displaytext);
                isRecordingInProgress = true;
                stopListening = false;
                //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (textToSpeech != null) {
                    textToSpeech.stop();
                    textToSpeech.shutdown();
                }
            }
            else {
                Toast.makeText(getApplicationContext(), "RECORD AUDIO PERMISSION DENIED", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOG_TAG, "Stop Button Clicked");
            stopListening = true;
            StopListeningSpeech();
            recordingButton.setText(R.string.recording_start_displaytext);
            isRecordingInProgress = false;
            //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mHandler.removeCallbacksAndMessages(mRunnable);
            textToSpeech=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status != TextToSpeech.ERROR){
                        textToSpeech.setLanguage(Locale.ENGLISH);
                        readyToSpeak=true;
                    }else{
                        Log.i(LOG_TAG,"TTS Error:"+status);
                        readyToSpeak=false;
                    }
                }
            });
        }
    }

    public void onTextToSpeechClick(View view) {
        Log.i(LOG_TAG, "Speak button clicked");
        Log.i(LOG_TAG, Boolean.toString(readyToSpeak));
        if (readyToSpeak && returnedText.getText().toString() != "") {
            String toSpeak = returnedText.getText().toString();
            //Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
            Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            try {
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, bundle, UUID.randomUUID().toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
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
        Log.d(LOG_TAG, "Unchecked");
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
        intervalSpeechStartDate = Calendar.getInstance().getTime();
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
        intervalSpeechStopDate = Calendar.getInstance().getTime();
        Log.i(LOG_TAG, "onEndOfSpeech");
        progressBar.setIndeterminate(true);
        StopListeningSpeech();
    }

    @Override
    public void onError(int errorCode) {
        intervalSpeechStopDate = Calendar.getInstance().getTime();
        String errorMessage = getErrorText(errorCode);
        Log.e(LOG_TAG, "FAILED " + errorMessage);
        if(errorCode != SpeechRecognizer.ERROR_SPEECH_TIMEOUT)       {
            errorTextView.setText(errorMessage);
        }

        if (!stopListening && errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
            StartListeningSpeech();
        } else {
            CalculateKeywordCount();
            recordingButton.setText(R.string.recording_start_displaytext);
            if(totalSpeechTime < WordCountInterval){
                errorTextView.setText("Not enough time for counting average");
            }
        }
    }

    @Override
    public void onResults(Bundle results) {
        Log.i(LOG_TAG, "onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Log.d(LOG_TAG, "finalResult: " + finalResult);
        Log.d(LOG_TAG, "Matches: " + matches.toString());
        if (matches != null && matches.size() > 0) {
            finalResult = finalResult + matches.get(0) + ". ";
            long intervalTime = intervalSpeechStopDate.getTime() - intervalSpeechStartDate.getTime();
            totalSpeechTime = totalSpeechTime + intervalTime / 1000;
            Log.d(LOG_TAG, "Total Speech Time: " + totalSpeechTime);
            //CalculateAvgWordCount(totalSpeechTime, finalResult);
        }
        returnedText.setText(finalResult);

        if (!stopListening) {
            StartListeningSpeech();
        }else{
            CalculateKeywordCount();
            if(totalSpeechTime < WordCountInterval){
                errorTextView.setText("Not enough time for counting average");
            }
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.i(LOG_TAG, "onPartialResults");
//        long timeElapsedInMS = Calendar.getInstance().getTimeInMillis() - startTime.getTime();
//        long timeElapsedInS = timeElapsedInMS / 1000;
        //Log.i(LOG_TAG, "Time Elapsed in sec:" + Long.toString(timeElapsedInS));
        ArrayList<String> matches = partialResults
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
            intervalSpeechStopDate = Calendar.getInstance().getTime();
            long intervalTime = intervalSpeechStopDate.getTime() - intervalSpeechStartDate.getTime();
            long temporaryTotalSpeechTime = totalSpeechTime + intervalTime / 1000;
            Log.d(LOG_TAG, "Temporary Total Speech Time: " + temporaryTotalSpeechTime);
            String partialFinalResults = finalResult + matches.get(0);
            returnedText.setText(partialFinalResults);

            if (temporaryTotalSpeechTime >= WordCountIntervalIncrementor) {
                CalculateAvgWordCount(temporaryTotalSpeechTime, partialFinalResults);
                int minimumWordsBeforeVibration=Integer.parseInt(preferences.getString("minimum_words_vibration", getString(R.string.minimum_words_vibration)));
                if(avgWordCount > minimumWordsBeforeVibration){
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(500);
                }
            }
        }
    }

    private void CalculateKeywordCount(){
        String keyword=preferences.getString("keyword",null);
        Log.d(LOG_TAG,"keyword: "+keyword);
        Log.d(LOG_TAG,"Final Result: "+finalResult);
        if(keyword != null) {
            keywordTextView.setText("Keyword Count: "+CountOfSubstringInString(finalResult,keyword));
        }


    }

    private void CalculateAvgWordCount(long timeTaken, String words) {
        wordCount = VoiceRecognitionActivity.countWordsUsingSplit(words);
        Log.d(LOG_TAG, "Word Count:" + Integer.toString(wordCount));
        avgWordCount = wordCount / (timeTaken / WordCountInterval);
        Log.d(LOG_TAG, "Avg Word Count:" + Long.toString(avgWordCount));
        WordCountIntervalIncrementor = WordCountIntervalIncrementor + WordCountInterval;
        wordCountTextView.setText("Status:" + Long.toString(avgWordCount) + " words per " + Integer.toString(WordCountInterval) + " seconds.");
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
                message = "Timed out due to no speech";
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
                    Log.i(LOG_TAG,"Inside OnRequestPermissionsResult");
                    speech.startListening(recognizerIntent);
                } else {
                    Toast.makeText(VoiceRecognitionActivity.this, "Permission Denied!", Toast
                            .LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public void onResume() {
        String selectedLanguage=preferences.getString("languages","en-US");
        int langValueIndex = Arrays.asList(languageValues).indexOf(selectedLanguage);
        String selectedLangName=languages[langValueIndex];
        titleTextView.setText(selectedLangName);
        Log.i(LOG_TAG,"Language Changed: "+selectedLanguage);
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
//        if (speech != null) {
//            speech.stopListening();
//            speech.cancel();
//            speech.destroy();
//            Log.i(LOG_TAG, "destroy");
//        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (speech != null) {
            speech.stopListening();
            speech.cancel();
            speech.destroy();
            Log.i(LOG_TAG, "destroy");
        }
        if(textToSpeech != null){
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }

    private static int countWordsUsingSplit(String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }
        String[] words = input.split("\\s+");
        return words.length;
    }

    public static int CountOfSubstringInString(String string, String substring) {
        int count = 0;
        int idx = 0;
        while ((idx = string.indexOf(substring, idx)) != -1) {
            idx++;
            count++;
        }
        return count;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            case R.id.action_settings:
            startActivity(new Intent(VoiceRecognitionActivity.this, SettingsActivity.class));
            return true;
            default:
                return false;
        }


    }
}
