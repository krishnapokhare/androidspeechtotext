package com.kpokhare.offlinespeechrecognizer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class VoiceRecognitionActivity extends BaseActivity implements RecognitionListener {

    private static final int REQUEST_RECORD_PERMISSION = 100;
    private Button recordingButton;
    private TextView wordCountTextView, errorTextView, keywordTextView, recordingLanguageTextView;
    private EditText returnedText;
    private ImageView SpeakButton;
    private ProgressBar progressBar;
    private SpeechRecognizer speech = null;
    private TextToSpeech textToSpeech = null;
    private Intent recognizerIntent;
    private static final String LOG_TAG_DEBUG = "DebugActivity";
    private final String SILENCE_LENGTH = "5";
    private final String MINIMUM_SPEECH_INTERVAL = "15";
    private boolean stopListening = false;
    private int count = 0;
    private String finalResult = "";
    private Date startTime;
    private int WordCountInterval = 5;
    private int WordCountIntervalIncrementor = WordCountInterval;
    private int wordCount;
    private long avgWordCount;
    private SharedPreferences preferences;
    public static SharedPreferences sharedPreferences;
    private boolean isRecordingInProgress = false;
    private long totalSpeechTime = 0;
    private Date intervalSpeechStartDate, intervalSpeechStopDate;
    //    Handler mHandler;
    private Runnable mRunnable;
    private int timerInSeconds = 0;
    private String[] languages;
    private String[] languageValues;
    private boolean readyToSpeak = false;
    private static DatabaseReference conversationDB;
    private String recordingLangCode, recordingLangName, speakingLangName;
//    static String DEVICE_ID = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG_DEBUG, "Method: onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_recognition);

        recordingLanguageTextView = findViewById(R.id.recordingLanguage);
        //speakingLanguageTextView = findViewById(R.id.speakingLanguage);
        returnedText = findViewById(R.id.resultsTextView);
        progressBar = findViewById(R.id.progressBar1);
        wordCountTextView = findViewById(R.id.wordCountTextView);
        errorTextView = findViewById(R.id.errorTextView);
        recordingButton = findViewById(R.id.recordingButton);
//        timerTextView = findViewById(R.id.timerTextView);
        keywordTextView = findViewById(R.id.keywordTextView);
        SpeakButton = findViewById(R.id.textToSpeechImageView);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences = this.getPreferences(MODE_PRIVATE);
        progressBar.setVisibility(View.INVISIBLE);
        languages = getResources().getStringArray(R.array.languages);
        languageValues = getResources().getStringArray(R.array.languages_values);
        //SaveSupportedLanguagesInSharedPreference();
        new LoadSupportedLanguages(this, preferences).execute("test");
        //InitializeSpeechSettings();

        conversationDB = FirebaseDatabase.getInstance().getReference("Conversations");

        Log.i(LOG_TAG_DEBUG, "DEVICEID:" + DEVICE_ID);
        //PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
    }

    private void InitializeSpeechSettings() {
        Log.d(LOG_TAG_DEBUG, "Method: InitializeSpeechSettings");
        totalSpeechTime = 0;
        timerInSeconds = 0;
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
        String j = preferences.getString("word_count_interval", "5");
        WordCountInterval = Integer.parseInt(j);
        WordCountIntervalIncrementor = WordCountInterval;

        returnedText.setText("");
        wordCountTextView.setText(getString(R.string.average_word_count_text));
        keywordTextView.setText(getString(R.string.keyword_count_text));
        finalResult = "";
    }

    private void InitializeTextToSpeech() {
        Log.d(LOG_TAG_DEBUG, "Method: InitializeTextToSpeech");
        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                switch (status) {
                    case TextToSpeech.SUCCESS:
                        String speechLang = preferences.getString("speakinglanguages", "en-US");
                        Log.d(LOG_TAG_DEBUG, "Speech Language " + speechLang);
                        int result = textToSpeech.setLanguage(Locale.forLanguageTag(speechLang));
                        if (result == TextToSpeech.LANG_MISSING_DATA
                                || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            Toast.makeText(getApplicationContext(), "This language is not supported", Toast.LENGTH_SHORT).show();
                        } else {
                            readyToSpeak = true;
                        }
                        break;
                    case TextToSpeech.ERROR:
                        Log.d(LOG_TAG_DEBUG, "TTS Error:" + status);
                        Toast.makeText(getApplicationContext(), "TTS Initialization failed", Toast.LENGTH_SHORT).show();
                        readyToSpeak = false;
                        break;
                    default:
                        Log.d(LOG_TAG_DEBUG, "Status of text to speech:" + status);
                        break;
                }
            }
        });
    }

    public void onRecordingBtnClick(View view) {
        Log.d(LOG_TAG_DEBUG, "Method: onRecordingBtnClick");
        if (!isRecordingInProgress) {
            CheckRecordingPermission();
            if (ContextCompat.checkSelfPermission(VoiceRecognitionActivity.this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startTime = Calendar.getInstance().getTime();
                Log.d(LOG_TAG_DEBUG, "Start Button Clicked");
                errorTextView.setText("");

                InitializeSpeechSettings();
                StartListeningSpeech();
                recordingButton.setText(R.string.recording_stop_displaytext);
                isRecordingInProgress = true;
                stopListening = false;
                //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (textToSpeech.isSpeaking()) {
                    textToSpeech.stop();
//                    textToSpeech.shutdown();
                }
            } else {
                Toast.makeText(getApplicationContext(), "RECORD AUDIO PERMISSION DENIED", Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.d(LOG_TAG_DEBUG, "Stop Button Clicked");
            stopListening = true;
            StopListeningSpeech();
            recordingButton.setText(R.string.recording_start_displaytext);
            isRecordingInProgress = false;
            //getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//            mHandler.removeCallbacksAndMessages(mRunnable);

            new SaveCurrentRecording().execute(returnedText.getText().toString(), recordingLangCode, recordingLangName);
        }
    }

    private void saveCurrentRecording(String recordingText) {
        Log.d(LOG_TAG_DEBUG, "Method: saveCurrentRecording");
        if (!recordingText.isEmpty()) {
            Conversation conversation = new Conversation(recordingText, recordingLangCode, recordingLangName);
            conversationDB.child(DEVICE_ID);
            conversationDB.child(DEVICE_ID).child(conversation.ID).setValue(conversation);

            Log.d(LOG_TAG_DEBUG, "Saved in database");
        } else {
            Log.d(LOG_TAG_DEBUG, "Recording Text is empty.");
        }
    }

    private void CheckRecordingPermission() {
        Log.d(LOG_TAG_DEBUG, "Method: CheckRecordingPermission");
        if (ContextCompat.checkSelfPermission(VoiceRecognitionActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions
                    (VoiceRecognitionActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_PERMISSION);
        }
    }

    public void onTextToSpeechClick(View view) {
        Log.d(LOG_TAG_DEBUG, "Method: onTextToSpeechClick");
        if (textToSpeech == null) {
            InitializeTextToSpeech();
        }
        Log.d(LOG_TAG_DEBUG, "Speak button clicked");
//        Log.d(LOG_TAG_DEBUG, Boolean.toString(readyToSpeak));
        if (isRecordingInProgress) {
            Toast.makeText(getApplicationContext(), "Recording in Progress. Please click on Stop Recording before clicking on Listen button", Toast.LENGTH_SHORT).show();
            return;
        }
        if (readyToSpeak && !Objects.equals(returnedText.getText().toString(), "")) {
            Log.d(LOG_TAG_DEBUG, "Ready to speak");
            String toSpeak = returnedText.getText().toString();
            //Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
            Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            try {
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, bundle, UUID.randomUUID().toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No text present for speech.", Toast.LENGTH_SHORT).show();
        }
    }

    private void StartListeningSpeech() {
        Log.d(LOG_TAG_DEBUG, "Method: StartListeningSpeech");
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setIndeterminate(true);
//        ActivityCompat.requestPermissions
//                (VoiceRecognitionActivity.this,
//                        new String[]{Manifest.permission.RECORD_AUDIO},
//                        REQUEST_RECORD_PERMISSION);
        if (speech != null) {
            speech.stopListening();
        }
        if (speech != null) {
            speech.startListening(recognizerIntent);
        }
        errorTextView.setText("");
    }

    private void StopListeningSpeech() {
        Log.d(LOG_TAG_DEBUG, "Method: StopListeningSpeech");
        progressBar.setIndeterminate(false);
        progressBar.setVisibility(View.INVISIBLE);
        if (speech != null) {
            speech.stopListening();
        }
    }


    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(LOG_TAG_DEBUG, "Method: onReadyForSpeech");
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(LOG_TAG_DEBUG, "Method: onBeginningOfSpeech");
        intervalSpeechStartDate = Calendar.getInstance().getTime();
        progressBar.setIndeterminate(false);
        progressBar.setMax(10);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
//        Log.d(LOG_TAG_DEBUG, "Method: onRmsChanged");
        progressBar.setProgress((int) rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        Log.d(LOG_TAG_DEBUG, "Method: onBufferReceived");
        Log.d(LOG_TAG_DEBUG, "onBufferReceived: " + Arrays.toString(buffer));
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(LOG_TAG_DEBUG, "Method: onEndOfSpeech");
        intervalSpeechStopDate = Calendar.getInstance().getTime();
        progressBar.setIndeterminate(true);
        StopListeningSpeech();
    }

    @Override
    public void onError(int errorCode) {
        Log.d(LOG_TAG_DEBUG, "Method: onError");
        intervalSpeechStopDate = Calendar.getInstance().getTime();
        String errorMessage = getErrorText(errorCode);
        Log.e(LOG_TAG_DEBUG, "Error: " + errorMessage);
        if (errorCode != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            errorTextView.setText(errorMessage);
        }

        if (!stopListening && errorCode == SpeechRecognizer.ERROR_NO_MATCH) {
            StartListeningSpeech();
        } else {
            //StopListeningSpeech();
//            if(speech != null){
//                speech.stopListening();
//            }
            //speech.cancel();
            isRecordingInProgress = false;
            CalculateKeywordCount();
            progressBar.setIndeterminate(false);
            progressBar.setVisibility(View.INVISIBLE);
            recordingButton.setText(R.string.recording_start_displaytext);
            if (totalSpeechTime < WordCountInterval) {
                errorTextView.setText(R.string.Not_Enough_Time_ErrorMsg);
            }
        }
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(LOG_TAG_DEBUG, "Method: onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        Log.d(LOG_TAG_DEBUG, "finalResult: " + finalResult);
        if (matches != null && matches.size() > 0) {
            Log.d(LOG_TAG_DEBUG, "Matches: " + matches.toString());
            finalResult = finalResult + matches.get(0) + ". ";
            long intervalTime = intervalSpeechStopDate.getTime() - intervalSpeechStartDate.getTime();
            totalSpeechTime = totalSpeechTime + intervalTime / 1000;
            Log.d(LOG_TAG_DEBUG, "Total Speech Time: " + totalSpeechTime);
            //CalculateAvgWordCount(totalSpeechTime, finalResult);
        }
        returnedText.setText(finalResult);

        if (!stopListening) {
            StartListeningSpeech();
        } else {
            CalculateKeywordCount();
            if (totalSpeechTime < WordCountInterval) {
                errorTextView.setText(R.string.Not_Enough_Time_Avg_ErrorMsg);
            }
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(LOG_TAG_DEBUG, "Method: onPartialResults");
//        long timeElapsedInMS = Calendar.getInstance().getTimeInMillis() - startTime.getTime();
//        long timeElapsedInS = timeElapsedInMS / 1000;
        //Log.d(LOG_TAG_DEBUG, "Time Elapsed in sec:" + Long.toString(timeElapsedInS));
        // Calling Async method to show Partial results in the TextBox
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
            intervalSpeechStopDate = Calendar.getInstance().getTime();
            ProcessPartialResults(matches);
        }

    }

    private void ProcessPartialResults(ArrayList<String> matches) {
        long intervalTime = intervalSpeechStopDate.getTime() - intervalSpeechStartDate.getTime();
        long temporaryTotalSpeechTime = totalSpeechTime + intervalTime / 1000;
        Log.d(LOG_TAG_DEBUG, "Temporary Total Speech Time: " + temporaryTotalSpeechTime);
        String partialFinalResults = finalResult + matches.get(0);
        returnedText.setText(partialFinalResults);
        if (temporaryTotalSpeechTime >= WordCountIntervalIncrementor) {
            CalculateAvgWordCount(temporaryTotalSpeechTime, partialFinalResults);
            int minimumWordsBeforeVibration = Integer.parseInt(preferences.getString("minimum_words_vibration", getString(R.string.minimum_words_vibration)));
            if (avgWordCount > minimumWordsBeforeVibration) {
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                Objects.requireNonNull(v, "Vibrator service is returning as null.").vibrate(500);
            }
        }
    }

    private void CalculateKeywordCount() {
        Log.d(LOG_TAG_DEBUG, "Method: CalculateKeywordCount");
        String keyword = preferences.getString("keyword", null);
        Log.d(LOG_TAG_DEBUG, "keyword: " + keyword);
        Log.d(LOG_TAG_DEBUG, "Final Result: " + finalResult);
        if (keyword != null) {
            keywordTextView.setText(getString(R.string.keyword_count_text) + CountOfSubstringInString(finalResult, keyword));
        }


    }

    private void CalculateAvgWordCount(long timeTaken, String words) {
        Log.d(LOG_TAG_DEBUG, "Method: CalculateAvgWordCount");
        wordCount = VoiceRecognitionActivity.countWordsUsingSplit(words);
        Log.d(LOG_TAG_DEBUG, "Word Count:" + Integer.toString(wordCount));
        avgWordCount = wordCount / (timeTaken / WordCountInterval);
        Log.d(LOG_TAG_DEBUG, "Avg Word Count:" + Long.toString(avgWordCount));
        WordCountIntervalIncrementor = WordCountIntervalIncrementor + WordCountInterval;
        wordCountTextView.setText("Status:" + Long.toString(avgWordCount) + " words per " + Integer.toString(WordCountInterval) + " seconds.");
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        Log.d(LOG_TAG_DEBUG, "Method: onEvent");
        Log.d(LOG_TAG_DEBUG + "onEvent", "onEvent");
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG_DEBUG, "Method: onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG_DEBUG, "Inside OnRequestPermissionsResult");
                    if (speech != null) {
                        try {
                            Log.d(LOG_TAG_DEBUG, "Permission Granted");
                        } catch (Exception e) {
                            Log.e(LOG_TAG_DEBUG, e.getMessage());
                            e.printStackTrace();
                        }
                    }
                } else {
                    Toast.makeText(VoiceRecognitionActivity.this, "Permission Denied!", Toast
                            .LENGTH_SHORT).show();
                }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(LOG_TAG_DEBUG, "Method: onResume");
        recordingLangCode = preferences.getString("languages", "en-US");
        recordingLangName = getRecordingLangName(recordingLangCode);
        String speakingLanguage = preferences.getString("speakinglanguages", "en-US");
        String speakingLanguageName = Locale.forLanguageTag(speakingLanguage).getDisplayName();
        //speakingLanguageTextView.setText("Speaking in :" +Locale.forLanguageTag(speakingLanguage).getDisplayName());
        recordingLanguageTextView.setText("Recording in :" + recordingLangName + "\n" + "Speaking in :" + speakingLanguageName);
        recordingLanguageTextView.setTextColor(Color.BLACK);
        InitializeTextToSpeech();

    }

    private String getRecordingLangName(String langCode) {
        Log.d(LOG_TAG_DEBUG, "Method: getRecordingLangName");
        int langValueIndex = Arrays.asList(languageValues).indexOf(langCode);
        return languages[langValueIndex];
    }

    @Override
    protected void onPause() {
        Log.d(LOG_TAG_DEBUG, "Method: onPause");
        super.onPause();
//        if (speech != null) {
//            speech.stopListening();
//            speech.cancel();
//            speech.destroy();
//            Log.d(LOG_TAG_DEBUG, "destroy");
//        }
    }

    @Override
    protected void onStop() {
        Log.d(LOG_TAG_DEBUG, "Method: onStop");
        super.onStop();
        if (speech != null) {
            speech.stopListening();
            speech.cancel();
            speech.destroy();
            speech = null;
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
            textToSpeech = null;
        }
        Log.d(LOG_TAG_DEBUG, "Method: onStop completed");
    }


    private static int countWordsUsingSplit(String input) {
        //Log.d(LOG_TAG_DEBUG,"countWordsUsingSplit");
        if (input == null || input.isEmpty()) {
            return 0;
        }
        String[] words = input.split("\\s+");
        return words.length;
    }

    private static int CountOfSubstringInString(String string, String substring) {
        //Log.d(LOG_TAG_DEBUG,"CountOfSubstringInString");
        int count = 0;
        int idx = 0;
        while ((idx = string.indexOf(substring, idx)) != -1) {
            idx++;
            count++;
        }
        return count;
    }


    static class LoadSupportedLanguages extends AsyncTask<String, Integer, String> {

        private TextToSpeech textToSpeech1;
        SharedPreferences preferences;
        WeakReference<VoiceRecognitionActivity> activityRef;

        public LoadSupportedLanguages(VoiceRecognitionActivity activity, SharedPreferences preferences) {
            this.activityRef = new WeakReference<VoiceRecognitionActivity>(activity);
            this.preferences = preferences;
        }

        //Log.d(LOG_TAG_DEBUG,"LoadSupportedLanguagesTask");
        protected String doInBackground(String... test) {
            Log.d(LOG_TAG_DEBUG, "Method: doInBackground");
            try {
                if (!preferences.contains("langNames") || !preferences.contains("langCodes")) {
                    final List<String> langCodes = new LinkedList<String>();
                    final List<String> langNames = new LinkedList<String>();
                    textToSpeech1 = new TextToSpeech(activityRef.get(), new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            Log.d(LOG_TAG_DEBUG, "Method: ASYNC onInit");
                            if (status == TextToSpeech.SUCCESS) {
                                Set<Locale> languages = textToSpeech1.getAvailableLanguages();
                                String speakingLang = "";
                                List<Locale> sortedLanguages = new ArrayList<Locale>(languages);
                                Collections.sort(sortedLanguages, new Comparator<Locale>() {
                                    @Override
                                    public int compare(Locale o1, Locale o2) {
                                        return o1.getDisplayName().compareTo(o2.getDisplayName());
                                    }
                                });

                                for (Locale lang : sortedLanguages) {
//                                    Log.d(LOG_TAG_DEBUG, lang.toString());
                                    langCodes.add(lang.toLanguageTag());
                                    langNames.add(lang.getDisplayName());
                                }
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("langNames", TextUtils.join(",", langNames));
                                editor.putString("langCodes", TextUtils.join(",", langCodes));
                                editor.apply();
                            }
                        }
                    });
                    return "Loaded supported Languages";
                } else {
                    return "Supported Languages already loaded";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return e.getMessage();
            }
        }
    }

    static class SaveCurrentRecording extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            try {
                Log.d(LOG_TAG_DEBUG, "Method: saveCurrentRecording");
                String recordingText = strings[0];
                String LangCode = strings[1];
                String LangName = strings[2];
                if (!recordingText.isEmpty()) {
                    Conversation conversation = new Conversation(recordingText, LangCode, LangName);
                    conversationDB.child(DEVICE_ID);
                    conversationDB.child(DEVICE_ID).child(conversation.ID).setValue(conversation);
                    Log.d(LOG_TAG_DEBUG, "Saved in database");
                } else {
                    Log.d(LOG_TAG_DEBUG, "Recording Text is empty.");
                }
            } catch (Exception e) {
                Log.e(LOG_TAG_DEBUG, e.getMessage());
                e.printStackTrace();
            }
            return "Completed saving recording in Database";
        }
    }
}
