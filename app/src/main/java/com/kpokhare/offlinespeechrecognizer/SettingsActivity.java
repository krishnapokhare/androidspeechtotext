package com.kpokhare.offlinespeechrecognizer;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.firebase.auth.FirebaseAuth;

import java.util.List;
import java.util.Locale;


/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new GeneralPreferenceFragment())
                .commit();
        context=getApplicationContext();
    }

    @Override
    public void onBuildHeaders(List<Header> target) {
        super.onBuildHeaders(target);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        return android.preference.PreferenceFragment.class.getName().equals(fragmentName)
                || GeneralPreferenceFragment.class.getName().equals(fragmentName);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(BaseActivity.LOG_TAG_DEBUG, "Method: onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
//            case R.id.action_callrecording:
//                startActivity(new Intent(getApplicationContext(), CallRecordingActivity.class));
//                return true;
            case R.id.action_home:
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                return true;
            case R.id.action_settings:
                startActivity(new Intent(getApplicationContext(), SettingsActivity.class));
                return true;
            case R.id.action_conversations:
                startActivity(new Intent(getApplicationContext(), ConversationsActivity.class));
                return true;
//            case R.id.action_grewords:
//                startActivity(new Intent(getApplicationContext(), GreWordListActivity.class));
//                return true;
            case R.id.action_logout:
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(getApplicationContext(),LoginActivity.class));
                return true;
            default:
                return false;
        }
    }

    public static Context getAppContext(){
        return context;
    }

    public static class GeneralPreferenceFragment extends PreferenceFragment
    {

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.pref_general);

            ListPreference listPreferenceCategory = (ListPreference) findPreference("speakinglanguages");
            if (listPreferenceCategory != null) {
                Log.i("SETTINGSACTIVITY", "Speaking Languages present");
                SharedPreferences sharedPreferences = getActivity().getApplicationContext().getSharedPreferences("SPEECH_RECOGNIZER", MODE_PRIVATE);
                String langNamesArray = sharedPreferences.getString("langNames", null);
                String langValuesArray = sharedPreferences.getString("langCodes", null);
                String myEntries[] = new String[]{"English(United States)"};
                String myEntryValues[] = new String[]{"en-US"};
                if (langNamesArray != null) {
                    myEntries = langNamesArray.split(",");
                }
                if (langValuesArray != null) {
                    myEntryValues = langValuesArray.split(",");
                }
                listPreferenceCategory.setEntries(myEntries);
                listPreferenceCategory.setEntryValues(myEntryValues);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String speakingLanguage = preferences.getString("speakinglanguages", "en-US");
                listPreferenceCategory.setValue(speakingLanguage);
                String speakingLanguageDisplayName = Locale.forLanguageTag(speakingLanguage).getDisplayName();
                //listPreferenceCategory.setSummary(speakingLanguageDisplayName);
            }
        }
    }
}