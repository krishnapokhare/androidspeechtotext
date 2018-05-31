package com.kpokhare.offlinespeechrecognizer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends BaseActivity implements RecognitionListener {
    private static final String MINIMUM_SPEECH_INTERVAL = "15";
    private static final String SILENCE_LENGTH = "5";
    private static final int MENU_PLAY = 1000;
    boolean isRecording = false;
    FloatingActionButton fab;
    private SpeechRecognizer speech;
    private Intent recognizerIntent;
    //    private SharedPreferences preferences;
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
    //    private ActionMode.Callback mActionModeCallback;
//    private Object mActionMode;
    private String keyword;
//    private File recordingFile;
//    private boolean isPlaying;
//    private MediaRecorder myAudioRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        setSupportActionBar((Toolbar)findViewById(R.id.main_toolbar));
        speechTextView = findViewById(R.id.speechTextView);
        recordingLangHeadingTextView = findViewById(R.id.recordingLanguageHeading);
        speakingLangHeadingTextView = findViewById(R.id.speakingLanguageHeading);
        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRecordingButtonClick();
            }
        });


        languages = getResources().getStringArray(R.array.languages);
        languageValues = getResources().getStringArray(R.array.languages_values);
//        Log.d(LOG_TAG_DEBUG, "onCreate: languages" + languages.length);
//        Log.d(LOG_TAG_DEBUG, "onCreate: languageValues" + languageValues.length);

        PreferenceManager.setDefaultValues(this, R.xml.pref_general, false);
        new LoadSupportedLanguages(this).execute("test");
//
//        File path = new File(
//                Environment.getExternalStorageDirectory().getAbsolutePath()
//                        + "/Android/data/com.kpokhare.offlinespeechrecognizer/files/");
//        Log.i(LOG_TAG_DEBUG,path.getAbsolutePath());
//        path.mkdirs();
//        try {
//            recordingFile = File.createTempFile("recording", ".3gp", path);
//        } catch (IOException e) {
//            throw new RuntimeException("Couldn't create file on SD card", e);
//        }

//        mActionModeCallback = new ActionMode.Callback() {
//
//            // Called when the action mode is created; startActionMode() was called
//            @Override
//            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//                // Inflate a menu resource providing context menu items
//                MenuInflater inflater = mode.getMenuInflater();
//                inflater.inflate(R.menu.context_menu, menu);
//                return true;
//            }
//
//            // Called each time the action mode is shown. Always called after onCreateActionMode, but
//            // may be called multiple times if the mode is invalidated.
//            @Override
//            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//                return false; // Return false if nothing is done
//            }
//
//            // Called when the user selects a contextual menu item
//            @Override
//            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.action_play:
//                        Log.i(LOG_TAG_DEBUG,"Play clicked");
//                        mode.finish(); // Action picked, so close the CAB
//                        return true;
//                    default:
//                        return false;
//                }
//            }
//
//            // Called when the user exits the action mode
//            @Override
//            public void onDestroyActionMode(ActionMode mode) {
//                mActionMode = null;
//            }
//        };
//
//        speechTextView.setOnLongClickListener(new View.OnLongClickListener() {
//            // Called when the user long-clicks on someView
//            public boolean onLongClick(View view) {
//                Log.d(LOG_TAG_DEBUG,"Textview Long Clicked");
//                if (mActionMode != null) {
//                    return false;
//                }
//
//                // Start the CAB using the ActionMode.Callback defined above
//                mActionMode = view.startActionMode(mActionModeCallback);
//                view.setSelected(true);
//                return true;
//            }
//        });

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
                    Log.d(LOG_TAG_DEBUG, "selected text is " + selectedText);
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
        new SaveCurrentRecording().execute(speechTextView.getText().toString(), recordingLangCode, recordingLangName);

    }

    private void InitializeSpeechSettings() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        Log.d(LOG_TAG_DEBUG, "Method: InitializeSpeechSettings");
//        totalSpeechTime = 0;
//        timerInSeconds = 0;
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        Log.d(LOG_TAG_DEBUG, "isRecognitionAvailable: " + SpeechRecognizer.isRecognitionAvailable(this));
        speech.setRecognitionListener(this);
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
//        String j = preferences.getString("word_count_interval", "5");
//        WordCountInterval = Integer.parseInt(j);
//        WordCountIntervalIncrementor = WordCountInterval;
//
//        returnedText.setText("");
//        wordCountTextView.setText(getString(R.string.average_word_count_text));
//        keywordTextView.setText(getString(R.string.keyword_count_text));
        keyword = preferences.getString("keyword", null);
        finalResult = "";
        speechTextView.setText(finalResult);
    }

    private void stopListening() {
        if (speech != null) {
            speech.stopListening();
            speech.cancel();
            speech.destroy();
            speech = null;
        }
        //StopRecording();
    }

    private void startListening() {
        if (speech != null && !stopRecording) { //Checking if Stop Recording button is clicked in UI
            speech.startListening(recognizerIntent);
            //new RecordAudio().execute();
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        Log.d(LOG_TAG_DEBUG, "Method:onReadyForSpeech");
        Toast.makeText(this, "You can speak now", Toast.LENGTH_SHORT).show();
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
        Log.d(LOG_TAG_DEBUG, "Method:onError:" + getErrorText(error));
        if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
            startListening();
        } else {//if(error == SpeechRecognizer.ERROR_CLIENT || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY){
            Toast.makeText(this, "Error: " + getErrorText(error), Toast.LENGTH_SHORT).show();
            onStopButtonClick();
        }
//        else{
//            Snackbar.make(findViewById(R.id.mainCoordinatorLayout), "Error:" + getErrorText(error), Snackbar.LENGTH_SHORT).show();
//            onStopButtonClick();
//        }
    }

    @Override
    public void onResults(Bundle results) {
        Log.d(LOG_TAG_DEBUG, "Method:onResults");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
            new ProcessResultTask().execute(matches.get(0));
            startListening();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        Log.d(LOG_TAG_DEBUG, "Method:onPartialResults");
        ArrayList<String> matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && matches.size() > 0) {
            new ProcessPartialResultTask().execute(matches.get(0));
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

    private class ProcessPartialResultTask extends AsyncTask<String, Integer, String> {

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

    private class ProcessResultTask extends AsyncTask<String, Integer, String> {
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
            new CalculateKeywordCountTask().execute(result);
        }
    }

    private class CalculateKeywordCountTask extends AsyncTask<String, Integer, String> {

        @Override
        protected String doInBackground(String... strings) {
            String recordingText = strings[0];
            Log.d(LOG_TAG_DEBUG, "Method: CalculateKeywordCount");
            Log.d(LOG_TAG_DEBUG, "keyword: " + keyword);
            Log.d(LOG_TAG_DEBUG, "Final Result: " + recordingText);
            if (keyword != null) {
                return getString(R.string.keyword_count_text) + Global.CountOfSubstringInString(recordingText, keyword);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                TextView keywordTextView = findViewById(R.id.keywordTextView);
                keywordTextView.setText(result);
            }
        }
    }

//    private void CalculateKeywordCount(String recordingText) {
//        Log.d(LOG_TAG_DEBUG, "Method: CalculateKeywordCount");
//        Log.d(LOG_TAG_DEBUG, "keyword: " + keyword);
//        Log.d(LOG_TAG_DEBUG, "Final Result: " + recordingText);
//        if (keyword != null) {
//            TextView keywordTextView = findViewById(R.id.keywordTextView);
//            keywordTextView.setText(getString(R.string.keyword_count_text) + Global.CountOfSubstringInString(recordingText, keyword));
//        }
//    }

//    private void CalculateAvgWordCount(long timeTaken, String words) {
//        Log.d(LOG_TAG_DEBUG, "Method: CalculateAvgWordCount");
//        int wordCount = Global.countWordsUsingSplit(words);
//        Log.d(LOG_TAG_DEBUG, "Word Count:" + Integer.toString(wordCount));
//        avgWordCount = wordCount / (timeTaken / WordCountInterval);
//        Log.d(LOG_TAG_DEBUG, "Avg Word Count:" + Long.toString(avgWordCount));
//        WordCountIntervalIncrementor = WordCountIntervalIncrementor + WordCountInterval;
//        wordCountTextView.setText("Status:" + Long.toString(avgWordCount) + " words per " + Integer.toString(WordCountInterval) + " seconds.");
//    }

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

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        recordingLangCode = preferences.getString("languages", "en-US");
        recordingLangName = getRecordingLangName(recordingLangCode);
        recordingLangHeadingTextView.setText(getString(R.string.recordingLanguageText) + " " + recordingLangName);
        String speakingLanguage = preferences.getString("speakinglanguages", "en-US");
        String speakingLanguageName = Locale.forLanguageTag(speakingLanguage).getDisplayName();
        speakingLangHeadingTextView.setText(getString(R.string.speakingLanguageHeading) + " " + speakingLanguageName);
        new PrepareTextToSpeechTask(this).execute();
    }

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

    static class LoadSupportedLanguages extends AsyncTask<String, Integer, String> {

        private TextToSpeech textToSpeech1;
        WeakReference<MainActivity> activityRef;

        public LoadSupportedLanguages(MainActivity activity) {
            this.activityRef = new WeakReference<MainActivity>(activity);
        }

        //Log.d(LOG_TAG_DEBUG,"LoadSupportedLanguages");
        protected String doInBackground(String... test) {
            Log.d(LOG_TAG_DEBUG, "Method: doInBackground");
            try {
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activityRef.get());
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
                                SharedPreferences sharedPreferences = activityRef.get().getApplicationContext().getSharedPreferences("SPEECH_RECOGNIZER", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                Log.i(LOG_TAG_DEBUG, String.valueOf(langNames.size()));
                                Log.i(LOG_TAG_DEBUG, String.valueOf(langCodes.size()));
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

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d(LOG_TAG_DEBUG, s);
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
                    DatabaseReference conversationDB = FirebaseDatabase.getInstance().getReference("Conversations");
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

    private class PrepareTextToSpeechTask extends AsyncTask<Void, Integer, Void> {

        WeakReference<MainActivity> activityRef;

        public PrepareTextToSpeechTask(MainActivity activity) {
            this.activityRef = new WeakReference<MainActivity>(activity);
        }

        @Override
        protected Void doInBackground(Void... voids) {
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
//
//    int frequency = 11025,channelConfiguration = AudioFormat.CHANNEL_IN_MONO;
//    int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
//
//    private class RecordAudio1 extends AsyncTask<Void, Integer, Void> {
//        @Override
//        protected Void doInBackground(Void... params) {
//            isRecording = true;
//            try {
//                DataOutputStream dos = new DataOutputStream(
//                        new BufferedOutputStream(new FileOutputStream(
//                                recordingFile)));
//                int bufferSize = AudioRecord.getMinBufferSize(frequency,
//                        channelConfiguration, audioEncoding);
//                AudioRecord audioRecord = new AudioRecord(
//                        MediaRecorder.AudioSource.MIC, frequency,
//                        channelConfiguration, audioEncoding, bufferSize);
//
//                short[] buffer = new short[bufferSize];
//                audioRecord.startRecording();
//                int r = 0;
//                while (isRecording) {
//                    int bufferReadResult = audioRecord.read(buffer, 0,
//                            bufferSize);
//                    for (int i = 0; i < bufferReadResult; i++) {
//                        dos.writeShort(buffer[i]);
//                    }
//                    publishProgress(new Integer(r));
//                    r++;
//                }
//                audioRecord.stop();
//                dos.close();
//            } catch (Throwable t) {
//                Log.e("AudioRecord", "Recording Failed");
//            }
//            return null;
//        }
//        protected void onProgressUpdate(Integer... progress) {
//            //statusText.setText(progress[0].toString());
//        }
//        protected void onPostExecute(Void result) {
//            //startRecordingButton.setEnabled(true);
//            //stopRecordingButton.setEnabled(false);
//            //startPlaybackButton.setEnabled(true);
//            Toast.makeText(MainActivity.this, "Recording completed", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void InitializeRecording(){
//        myAudioRecorder = new MediaRecorder();
//        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
//        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
//        myAudioRecorder.setOutputFile(recordingFile);
//    }
//
//    private void StopRecording(){
//        if(myAudioRecorder != null) {
//            myAudioRecorder.stop();
//            myAudioRecorder.release();
//            myAudioRecorder = null;
//        }
//    }
//
//    private class RecordAudio extends AsyncTask<Void, Integer, Void> {
//        @Override
//        protected Void doInBackground(Void... params) {
//            isRecording = true;
//            try {
//                myAudioRecorder.prepare();
//                myAudioRecorder.start();
//            } catch (IllegalStateException ise) {
//                ise.printStackTrace();
//            } catch (IOException ioe) {
//                ioe.printStackTrace();
//            }
//            return null;
//        }
//        protected void onProgressUpdate(Integer... progress) {
//            //statusText.setText(progress[0].toString());
//        }
//        protected void onPostExecute(Void result) {
//            //startRecordingButton.setEnabled(true);
//            //stopRecordingButton.setEnabled(false);
//            //startPlaybackButton.setEnabled(true);
//            Toast.makeText(MainActivity.this, "Recording completed", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private class PlayAudio extends AsyncTask<Void, Integer, Void> {
//        @Override
//        protected Void doInBackground(Void... params) {
//            isPlaying = true;
//
//            int bufferSize = AudioTrack.getMinBufferSize(frequency,channelConfiguration, audioEncoding);
//            short[] audiodata = new short[bufferSize / 4];
//
//            try {
//                Log.i(LOG_TAG_DEBUG,recordingFile.getAbsolutePath());
//                DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(recordingFile)));
//                AudioTrack audioTrack = new AudioTrack(
//                        AudioManager.STREAM_MUSIC, frequency,
//                        channelConfiguration, audioEncoding, bufferSize,
//                        AudioTrack.MODE_STREAM);
//
//                audioTrack.play();
//                while (isPlaying && dis.available() > 0) {
//                    int i = 0;
//                    while (dis.available() > 0 && i < audiodata.length) {
//                        audiodata[i] = dis.readShort();
//                        i++;
//                    }
//                    audioTrack.write(audiodata, 0, audiodata.length);
//                }
//                dis.close();
////                startPlaybackButton.setEnabled(false);
////                stopPlaybackButton.setEnabled(true);
//            } catch (Throwable t) {
//                Log.e("AudioTrack", "Playback Failed");
//            }
//            return null;
//        }
//    }
}
