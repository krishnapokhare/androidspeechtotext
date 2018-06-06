package com.kpokhare.offlinespeechrecognizer;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
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

public class MainActivity extends BaseActivity implements RecognitionListener {
    private static final int REQUEST_RECORD_PERMISSION = 100;
    private static final String MINIMUM_SPEECH_INTERVAL = "15";
    private static final String SILENCE_LENGTH = "5";
    private static final int MENU_PLAY = 1000;
    ImageView fab;
    private int WordCountInterval = 5;
    boolean isRecording = false;
    private int WordCountIntervalIncrementor = WordCountInterval;
    private SpeechRecognizer speech;
    private Intent recognizerIntent;
    private String recordingLangCode;
    private TextView speechTextView;
    private String finalResult;
    private boolean stopRecording;
    private String recordingLangName = "English-United States";
    String[] languages;
    String[] languageValues;
    private TextView recordingLangHeadingTextView;
    private TextView speakingLangHeadingTextView;
    private TextToSpeech textToSpeech;
    private boolean readyToSpeak;
    private String keyword;
    private Date intervalSpeechStopDate;
    private Date intervalSpeechStartDate;
    private long totalSpeechTime;
    private TextView avgWordCountTextView;
    private int minimum_words_vibration;
    private TextView errorTextView;
    private int startSpeechMessageCount;
    private TextView keywordValueTextView;
    private TextView keywordTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        setSupportActionBar((Toolbar)findViewById(R.id.main_toolbar));
        speechTextView = findViewById(R.id.speechTextView);
        recordingLangHeadingTextView = findViewById(R.id.recordingLanguageHeading);
        speakingLangHeadingTextView = findViewById(R.id.speakingLanguageHeading);
        avgWordCountTextView = findViewById(R.id.avgWordCountTextView);
        keywordValueTextView = findViewById(R.id.keywordValueTextView);
        errorTextView = findViewById(R.id.errorTextView);
        keywordTextView = findViewById(R.id.keywordTextView);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecordingButtonClick();
            }
        });

        languages = getResources().getStringArray(R.array.languages);
        languageValues = getResources().getStringArray(R.array.languages_values);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        LoadSupportedLanguages(this);
        InitializeRecognizerIntent();
        speechTextView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.add(Menu.NONE, MENU_PLAY, 1, "Pronounce word").setIcon(R.drawable.ic_play_sound);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                // Remove the "select all" option
                menu.removeItem(android.R.id.selectAll);
                // Remove the "cut" option
                menu.removeItem(android.R.id.cut);
                // Remove the "copy all" option
                menu.removeItem(android.R.id.copy);
                return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == MENU_PLAY) {
                    int min = 0;
                    int max = speechTextView.getText().length();
                    if (speechTextView.isFocused()) {
                        final int selStart = speechTextView.getSelectionStart();
                        final int selEnd = speechTextView.getSelectionEnd();

                        min = Math.max(0, Math.min(selStart, selEnd));
                        max = Math.max(0, Math.max(selStart, selEnd));
                    }
                    // Perform your definition lookup with the selected text
                    final CharSequence selectedText = speechTextView.getText().subSequence(min, max);
                    PlayTextToSpeech(selectedText.toString());
                    // Finish and close the ActionMode
                    mode.finish();
                    return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {

            }
        });
    }

    private boolean checkRecordingPermission() {
        Log.d(LOG_TAG_DEBUG, "Method: CheckRecordingPermission");
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions
                    (MainActivity.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            REQUEST_RECORD_PERMISSION);
            return false;
        } else {
            return true;
        }
    }

    private void onRecordingButtonClick() {
        if (!isRecording) {
            if (checkRecordingPermission()) {
                onStartButtonClick();
            }
        } else {
            onStopButtonClick();
        }
    }

    private void onStartButtonClick() {
        fab.setImageResource(R.drawable.ic_stop_black_24dp);
        isRecording = true;
        stopRecording = false;
        InitializeSpeechSettings();
        //InitializeRecording();
        startListening();
        Snackbar.make(findViewById(R.id.mainCoordinatorLayout), "Recording in progress", Snackbar.LENGTH_INDEFINITE).show();
//        new RecordAudio().execute();
    }

    private void onStopButtonClick() {
        stopRecording = true;
        stopListening();
        Snackbar.make(findViewById(R.id.mainCoordinatorLayout), "Recording Stopped", Snackbar.LENGTH_SHORT).show();
        isRecording = false;
        fab.setImageResource(R.drawable.ic_mic_black_24dp);
        SaveCurrentRecording(speechTextView.getText().toString(), recordingLangCode, recordingLangName);
    }

    private void InitializeSpeechSettings() {
        Log.d(LOG_TAG_DEBUG, "Method: InitializeSpeechSettings");
        InitializeSpeechRecognizer();
        WordCountIntervalIncrementor = WordCountInterval;
        finalResult = "";
        totalSpeechTime = 0;
        speechTextView.setText(finalResult);
        startSpeechMessageCount = 0;
    }

    private void InitializeSpeechRecognizer() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(this);
    }

    private void InitializeRecognizerIntent() {
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_LENGTH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_LENGTH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, recordingLangCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, recordingLangCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, recordingLangCode);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, MINIMUM_SPEECH_INTERVAL);
    }

    private void stopListening() {
        if (speech != null) {
            speech.stopListening();
            speech.cancel();
            speech.destroy();
            speech = null;
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
        if (startSpeechMessageCount == 0) {
            Toast.makeText(this, "Listening now...", Toast.LENGTH_SHORT).show();
            startSpeechMessageCount++;
        }
    }

    @Override
    public void onBeginningOfSpeech() {
        Log.d(LOG_TAG_DEBUG, "Method:onBeginningOfSpeech");
        intervalSpeechStartDate = Calendar.getInstance().getTime();
    }

    @Override
    public void onRmsChanged(float rmsdB) {
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
        Log.d(LOG_TAG_DEBUG, "Method:onError:" + getErrorText(error));
        intervalSpeechStopDate = Calendar.getInstance().getTime();
        if (error == SpeechRecognizer.ERROR_NO_MATCH) {
            startListening();
        } else {//if(error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
            onStopButtonClick();
            Toast.makeText(this, "Error: " + getErrorText(error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(LOG_TAG_DEBUG, "Method:onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
//            new ProcessResultTask().execute(matches.get(0));
            ProcessResult(matches.get(0));
            startListening();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(LOG_TAG_DEBUG, "Method:onPartialResults");
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
            intervalSpeechStopDate = Calendar.getInstance().getTime();
            Log.d(LOG_TAG_DEBUG, "PartialResults:" + matches.get(0));
//            new ProcessPartialResultTask().execute(matches.get(0));
            ProcessPartialResult(matches.get(0));
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

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        recordingLangCode = preferences.getString("languages", "en-US");
        recordingLangName = getRecordingLangName(recordingLangCode);
        recordingLangHeadingTextView.setText(recordingLangName);
        String speakingLanguage = preferences.getString("speakinglanguages", "en-US");
        String speakingLanguageName = Locale.forLanguageTag(speakingLanguage).getDisplayName();
        speakingLangHeadingTextView.setText(speakingLanguageName);
        keyword = preferences.getString("keyword", "Not set");
        keywordValueTextView.setText(keyword);
        WordCountInterval = Integer.parseInt(preferences.getString("word_count_interval", "5"));
        minimum_words_vibration = Integer.parseInt(preferences.getString("minimum_words_vibration", getString(R.string.minimum_words_vibration)));
        //PrepareTextToSpeech(this);
        new PrepareTextToSpeechTask(this).execute();
    }

    private void LoadSupportedLanguages(final Context context) {
        final TextToSpeech[] textToSpeech1 = new TextToSpeech[1];
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                if (!preferences.contains("langNames") || !preferences.contains("langCodes")) {
                    final List<String> langCodes = new LinkedList<String>();
                    final List<String> langNames = new LinkedList<String>();
                    textToSpeech1[0] = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                        @Override
                        public void onInit(int status) {
                            Log.d(LOG_TAG_DEBUG, "Method: ASYNC onInit");
                            if (status == TextToSpeech.SUCCESS) {
                                Set<Locale> languages = textToSpeech1[0].getAvailableLanguages();
                                String speakingLang = "";
                                List<Locale> sortedLanguages = new ArrayList<Locale>(languages);
                                Log.i(LOG_TAG_DEBUG, String.valueOf(sortedLanguages.size()));
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
                                SharedPreferences sharedPreferences = context.getApplicationContext().getSharedPreferences("SPEECH_RECOGNIZER", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                Log.i(LOG_TAG_DEBUG, String.valueOf(langNames.size()));
                                Log.i(LOG_TAG_DEBUG, String.valueOf(langCodes.size()));
                                editor.putString("langNames", TextUtils.join(",", langNames));
                                editor.putString("langCodes", TextUtils.join(",", langCodes));
                                editor.apply();
                            }
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

//    static class LoadSupportedLanguagesTask extends AsyncTask<String, Integer, String> {
//
//        private TextToSpeech textToSpeech1;
//        WeakReference<MainActivity> activityRef;
//
//        public LoadSupportedLanguagesTask(MainActivity activity) {
//            this.activityRef = new WeakReference<MainActivity>(activity);
//        }
//
//        //Log.d(LOG_TAG_DEBUG,"LoadSupportedLanguagesTask");
//        protected String doInBackground(String... test) {
//            Log.d(LOG_TAG_DEBUG, "Method: ASYNC LoadSupportedLanguagesTask");
//            try {
//                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activityRef.get());
//                if (!preferences.contains("langNames") || !preferences.contains("langCodes")) {
//                    final List<String> langCodes = new LinkedList<String>();
//                    final List<String> langNames = new LinkedList<String>();
//                    textToSpeech1 = new TextToSpeech(activityRef.get(), new TextToSpeech.OnInitListener() {
//                        @Override
//                        public void onInit(int status) {
//                            Log.d(LOG_TAG_DEBUG, "Method: ASYNC onInit");
//                            if (status == TextToSpeech.SUCCESS) {
//                                Set<Locale> languages = textToSpeech1.getAvailableLanguages();
//                                String speakingLang = "";
//                                List<Locale> sortedLanguages = new ArrayList<Locale>(languages);
//                                Log.i(LOG_TAG_DEBUG, String.valueOf(sortedLanguages.size()));
//                                Collections.sort(sortedLanguages, new Comparator<Locale>() {
//                                    @Override
//                                    public int compare(Locale o1, Locale o2) {
//                                        return o1.getDisplayName().compareTo(o2.getDisplayName());
//                                    }
//                                });
//
//                                for (Locale lang : sortedLanguages) {
////                                    Log.d(LOG_TAG_DEBUG, lang.toString());
//                                    langCodes.add(lang.toLanguageTag());
//                                    langNames.add(lang.getDisplayName());
//                                }
//                                SharedPreferences sharedPreferences = activityRef.get().getApplicationContext().getSharedPreferences("SPEECH_RECOGNIZER", MODE_PRIVATE);
//                                SharedPreferences.Editor editor = sharedPreferences.edit();
//                                Log.i(LOG_TAG_DEBUG, String.valueOf(langNames.size()));
//                                Log.i(LOG_TAG_DEBUG, String.valueOf(langCodes.size()));
//                                editor.putString("langNames", TextUtils.join(",", langNames));
//                                editor.putString("langCodes", TextUtils.join(",", langCodes));
//                                editor.apply();
//                            }
//                        }
//                    });
//                    return "Loaded supported Languages";
//                } else {
//                    return "Supported Languages already loaded";
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//                return e.getMessage();
//            }
//        }
//
//        @Override
//        protected void onPostExecute(String s) {
//            super.onPostExecute(s);
//            Log.d(LOG_TAG_DEBUG, s);
//        }
//    }

    private void SaveCurrentRecording(final String recordingText, final String LangCode, final String LangName) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!recordingText.isEmpty()) {
                    Conversation conversation = new Conversation(recordingText, LangCode, LangName);
                    DatabaseReference conversationDB = FirebaseDatabase.getInstance().getReference("Conversations");
                    conversationDB.child(DEVICE_ID);
                    conversationDB.child(DEVICE_ID).child(conversation.ID).setValue(conversation);
                    Log.d(LOG_TAG_DEBUG, "Saved in database");
                } else {
                    Log.d(LOG_TAG_DEBUG, "Recording Text is empty.");
                }
            }
        };
        new Thread(runnable).start();
    }

//    static class SaveCurrentRecordingTask extends AsyncTask<String, Integer, String> {
//
//        @Override
//        protected String doInBackground(String... strings) {
//            Log.d(LOG_TAG_DEBUG, "Method: ASYNC SaveCurrentRecordingTask");
//            try {
//                String recordingText = strings[0];
//                String LangCode = strings[1];
//                String LangName = strings[2];
//                if (!recordingText.isEmpty()) {
//                    Conversation conversation = new Conversation(recordingText, LangCode, LangName);
//                    DatabaseReference conversationDB = FirebaseDatabase.getInstance().getReference("Conversations");
//                    conversationDB.child(DEVICE_ID);
//                    conversationDB.child(DEVICE_ID).child(conversation.ID).setValue(conversation);
//                    Log.d(LOG_TAG_DEBUG, "Saved in database");
//                } else {
//                    Log.d(LOG_TAG_DEBUG, "Recording Text is empty.");
//                }
//            } catch (Exception e) {
//                Log.e(LOG_TAG_DEBUG, e.getMessage());
//                e.printStackTrace();
//            }
//            return "Completed saving recording in Database";
//        }
//    }

//    private void CalculateAvgWordCount(final String partialFinalResult) {
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                long intervalTime = intervalSpeechStopDate.getTime() - intervalSpeechStartDate.getTime();
//                long temporaryTotalSpeechTime = totalSpeechTime + intervalTime / 1000;
//                Log.d(LOG_TAG_DEBUG, "Temporary Total Speech Time: " + temporaryTotalSpeechTime);
//                if (temporaryTotalSpeechTime >= WordCountIntervalIncrementor) {
////                CalculateAvgWordCount(temporaryTotalSpeechTime, partialFinalResult);
//                    Log.d(LOG_TAG_DEBUG, "Method: CalculateAvgWordCount");
//                    int wordCount = Global.countWordsUsingSplit(partialFinalResult);
//                    Log.d(LOG_TAG_DEBUG, "Word Count:" + Integer.toString(wordCount));
//                    final long avgWordCount = wordCount / (temporaryTotalSpeechTime / WordCountInterval);
//                    Log.d(LOG_TAG_DEBUG, "Avg Word Count:" + Long.toString(avgWordCount));
//                    WordCountIntervalIncrementor = WordCountIntervalIncrementor + WordCountInterval;
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (avgWordCount > minimum_words_vibration) {
//                                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
//                                Objects.requireNonNull(v, "Vibrator service is returning as null.").vibrate(500);
//                            }
//                            avgWordCountTextView.setText(Long.toString(avgWordCount) + " words per " + Integer.toString(WordCountInterval) + " seconds.");
//                        }
//                    });
//
//                }
//            }
//        };
//        new Thread(runnable).start();
//    }

    private class CalculateAvgWordCountTask extends AsyncTask<String, Integer, Long> {

        @Override
        protected Long doInBackground(String... strings) {
            String partialFinalResult = strings[0];
            long intervalTime = intervalSpeechStopDate.getTime() - intervalSpeechStartDate.getTime();
            long temporaryTotalSpeechTime = totalSpeechTime + intervalTime / 1000;
            Log.d(LOG_TAG_DEBUG, "Temporary Total Speech Time: " + temporaryTotalSpeechTime);
            if (temporaryTotalSpeechTime >= WordCountIntervalIncrementor) {
//                CalculateAvgWordCount(temporaryTotalSpeechTime, partialFinalResult);
                Log.d(LOG_TAG_DEBUG, "Method: CalculateAvgWordCount");
                int wordCount = Global.countWordsUsingSplit(partialFinalResult);
                Log.d(LOG_TAG_DEBUG, "Word Count:" + Integer.toString(wordCount));
                long avgWordCount = wordCount / (temporaryTotalSpeechTime / WordCountInterval);
                Log.d(LOG_TAG_DEBUG, "Avg Word Count:" + Long.toString(avgWordCount));
                WordCountIntervalIncrementor = WordCountIntervalIncrementor + WordCountInterval;
                return avgWordCount;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Long avgWordCount) {
            super.onPostExecute(avgWordCount);
            if (avgWordCount != null) {
                if (avgWordCount > minimum_words_vibration) {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    Objects.requireNonNull(v, "Vibrator service is returning as null.").vibrate(500);
                }
                avgWordCountTextView.setText(Long.toString(avgWordCount) + " words per " + Integer.toString(WordCountInterval) + " seconds.");
            }
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

    private void ProcessPartialResult(final String partialResult) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG_DEBUG, "ProcessPartialResult:" + partialResult);
                final String partialFinalResult = finalResult + " " + partialResult;
//                CalculateAvgWordCount(partialFinalResult);
                new CalculateAvgWordCountTask().execute(partialFinalResult);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speechTextView.setText(partialFinalResult);
                    }
                });
            }
        };
        new Thread(runnable).start();
    }

    private void ProcessResult(final String result) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(LOG_TAG_DEBUG, "Method: ProcessResult");
//                String finalResultParam = result;
                Log.d(LOG_TAG_DEBUG, finalResult);
                finalResult = finalResult + " " + result;
                long intervalTime = intervalSpeechStopDate.getTime() - intervalSpeechStartDate.getTime();
                totalSpeechTime = totalSpeechTime + intervalTime / 1000;
//                CalculateKeywordCount(finalResult);
                new CalculateKeywordCountTask().execute(finalResult);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        speechTextView.setText(finalResult);
                    }
                });
                if (totalSpeechTime < WordCountInterval) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            errorTextView.setText(R.string.Not_Enough_Time_Avg_ErrorMsg);
                        }
                    });
                }
            }
        };
        new Thread(runnable).start();
    }

//    private class ProcessPartialResultTask extends AsyncTask<String, Integer, String> {
//
//        public ProcessPartialResultTask(){
//            Log.d(LOG_TAG_DEBUG, "Method:ProcessPartialResultTask");
//        }
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            Log.d(LOG_TAG_DEBUG, "onPreExecute:ProcessPartialResultTask");
//        }
//
//        @Override
//        protected String doInBackground(String... strings) {
//            Log.d(LOG_TAG_DEBUG, "doInBackground");
//            String partialResult = strings[0];
//
//            String partialFinalResult = finalResult + " " + partialResult;
//            return partialFinalResult;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            final String returnedValue = result;
//            super.onPostExecute(result);
//            new CalculateAvgWordCountTask().execute(result);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    speechTextView.setText(returnedValue);
//                }
//            });
//        }
//    }

    private String getRecordingLangName(String langCode) {
        Log.d(LOG_TAG_DEBUG, "Method: getRecordingLangName:" + langCode);
        Log.d(LOG_TAG_DEBUG, languageValues.toString());
        int langValueIndex = Arrays.asList(languageValues).indexOf(langCode);
        return languages[langValueIndex];
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem menuPlay = menu.findItem(MENU_PLAY);
        if (menuPlay == null) {
            menuPlay = menu.add(Menu.NONE, MENU_PLAY, 111, R.string.playRecording);
            menuPlay.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menuPlay.setIcon(R.drawable.ic_play_sound);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean returnValue = super.onOptionsItemSelected(item);
        if (item.getItemId() == MENU_PLAY) {
//            new PlayAudio().execute();
            PlayTextToSpeech(speechTextView.getText().toString());
        }
        return returnValue;
    }

//    private class ProcessResultTask extends AsyncTask<String, Integer, String> {
//        @Override
//        protected String doInBackground(String... strings) {
//            Log.d(LOG_TAG_DEBUG, "Method: ASYNC ProcessResultTask");
//            String finalResultParam = strings[0];
//            Log.d(LOG_TAG_DEBUG, finalResult);
//            finalResult = finalResult + " " + finalResultParam;
//            long intervalTime = intervalSpeechStopDate.getTime() - intervalSpeechStartDate.getTime();
//            totalSpeechTime = totalSpeechTime + intervalTime / 1000;
//            return finalResult;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            final String returnedValue = result;
//            super.onPostExecute(result);
//            new CalculateKeywordCountTask().execute(result);
//            runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    speechTextView.setText(returnedValue);
//                }
//            });
//            if (totalSpeechTime < WordCountInterval) {
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        errorTextView.setText(R.string.Not_Enough_Time_Avg_ErrorMsg);
//                    }
//                });
//            }
//        }
//    }

//    private void CalculateKeywordCount(final String recordingText) {
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                Log.d(LOG_TAG_DEBUG, "Method: CalculateKeywordCount");
//                if (keyword != null) {
//                    final int result = Global.CountOfSubstringInString(recordingText, keyword);
//
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            keywordTextView.setText(String.valueOf(result));
//                        }
//                    });
//
//                }
//            }
//        };
//        new Thread(runnable).start();
//    }

    private class CalculateKeywordCountTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected Integer doInBackground(String... strings) {
            Log.d(LOG_TAG_DEBUG, "Method: ASYNC CalculateKeywordCountTask");
            String recordingText = strings[0];
            Log.d(LOG_TAG_DEBUG, "Method: CalculateKeywordCount");
            Log.d(LOG_TAG_DEBUG, "keyword: " + keyword);
            Log.d(LOG_TAG_DEBUG, "Final Result: " + recordingText);
            if (keyword != null) {
                return Global.CountOfSubstringInString(recordingText, keyword);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if (result != null) {
                TextView keywordTextView = findViewById(R.id.keywordTextView);
                keywordTextView.setText(String.valueOf(result));
            }
        }
    }

//    private void PrepareTextToSpeech(final Context context){
//        Runnable runnable=new Runnable() {
//            @Override
//            public void run() {
//                Log.d(LOG_TAG_DEBUG, "Method: ASYNC PrepareTextToSpeechTask");
//                textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
//                    @Override
//                    public void onInit(int status) {
//                        switch (status) {
//                            case TextToSpeech.SUCCESS:
//                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
//                                String speechLang = preferences.getString("speakinglanguages", "en-US");
//                                Log.d(LOG_TAG_DEBUG, "Speech Language: " + speechLang);
//                                int result = textToSpeech.setLanguage(Locale.forLanguageTag(speechLang));
//                                if (result == TextToSpeech.LANG_MISSING_DATA
//                                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
////                                Toast.makeText(getApplicationContext(), "This language is not supported", Toast.LENGTH_SHORT).show();
//                                    Log.w(LOG_TAG_DEBUG, "This language is not supported");
//                                } else {
//                                    readyToSpeak = true;
//                                }
//                                break;
//                            case TextToSpeech.ERROR:
//                                Log.w(LOG_TAG_DEBUG, "TTS Error:" + status);
////                            Toast.makeText(getApplicationContext(), "TTS Initialization failed", Toast.LENGTH_SHORT).show();
//                                readyToSpeak = false;
//                                break;
//                            default:
//                                Log.w(LOG_TAG_DEBUG, "Status of text to speech:" + status);
//                                break;
//                        }
//                    }
//                });
//            }
//        };
//        new Thread(runnable).start();
//    }

    private class PrepareTextToSpeechTask extends AsyncTask<Void, Integer, Void> {

        WeakReference<MainActivity> activityRef;

        public PrepareTextToSpeechTask(MainActivity activity) {
            this.activityRef = new WeakReference<MainActivity>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Log.d(LOG_TAG_DEBUG, "Method: ASYNC PrepareTextToSpeechTask");
            textToSpeech = new TextToSpeech(activityRef.get(), new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    switch (status) {
                        case TextToSpeech.SUCCESS:
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activityRef.get());
                            String speechLang = preferences.getString("speakinglanguages", "en-US");
                            Log.d(LOG_TAG_DEBUG, "Speech Language: " + speechLang);
                            int result = textToSpeech.setLanguage(Locale.forLanguageTag(speechLang));
                            if (result == TextToSpeech.LANG_MISSING_DATA
                                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
//                                Toast.makeText(getApplicationContext(), "This language is not supported", Toast.LENGTH_SHORT).show();
                                Log.w(LOG_TAG_DEBUG, "This language is not supported");
                            } else {
                                readyToSpeak = true;
                            }
                            break;
                        case TextToSpeech.ERROR:
                            Log.w(LOG_TAG_DEBUG, "TTS Error:" + status);
//                            Toast.makeText(getApplicationContext(), "TTS Initialization failed", Toast.LENGTH_SHORT).show();
                            readyToSpeak = false;
                            break;
                        default:
                            Log.w(LOG_TAG_DEBUG, "Status of text to speech:" + status);
                            break;
                    }
                }
            });
            return null;
        }
    }

    private void PlayTextToSpeech(String returnedText) {
        if (isRecording) {
            Toast.makeText(getApplicationContext(), "Recording in Progress. Please click on Stop Recording before clicking on Listen button", Toast.LENGTH_SHORT).show();
        }
        if (readyToSpeak && !Objects.equals(returnedText, "")) {
            Log.d(LOG_TAG_DEBUG, "Ready to speak");
            String toSpeak = returnedText;
            Toast.makeText(getApplicationContext(), toSpeak, Toast.LENGTH_SHORT).show();
            Bundle bundle = new Bundle();
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
            try {
                textToSpeech.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, bundle, UUID.randomUUID().toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "No text present for speech.", Toast.LENGTH_SHORT).show();
            Log.w(LOG_TAG_DEBUG, "No text present for speech");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(LOG_TAG_DEBUG, "Method: onRequestPermissionsResult");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_RECORD_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(LOG_TAG_DEBUG, "Permission Granted");
                    onStartButtonClick();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied! You won't be able to use Recording function of this app.", Toast
                            .LENGTH_SHORT).show();
                }
        }
    }
}
