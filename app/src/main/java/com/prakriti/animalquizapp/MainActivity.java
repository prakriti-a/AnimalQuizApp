package com.prakriti.animalquizapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.util.Set;

public class MainActivity extends AppCompatActivity {
// launch mode as single top in manifests file

    public static final String NUM_OF_GUESSES_KEY = "settings_numberOfGuesses";
    public static final String ANIMAL_TYPES_KEY = "settings_animalTypes";
    public static final String BG_COLOR_KEY = "settings_chooseBackgroundColor";
    public static final String FONT_KEY = "settings_chooseFont";

    private boolean isSettingsChanged = false;

    public static Typeface holdOn, murberry, qabilFreeTrial;

    //  ref to Main Fragment
    MainFragment mainQuizFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        holdOn = Typeface.createFromAsset(getAssets(), "fonts/Hold On.ttf");
        murberry = Typeface.createFromAsset(getAssets(), "fonts/Murberry.ttf");
        qabilFreeTrial = Typeface.createFromAsset(getAssets(), "fonts/Qabil Free Trial.ttf");

        // specify default values for when app is running for the first time
        PreferenceManager.setDefaultValues(this, R.xml.quiz_preferences, false);
            // readAgain - false, app shouldn't read default values after installing & having changed preferences
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(settingsChangedListener);
            // get preferences & create a listener to be called each time settings are changed

        mainQuizFragment = (MainFragment) getSupportFragmentManager().findFragmentById(R.id.animalQuizFragment); // ref to fragment
            // pass id of fragment inside content_main.xml

        // also get any previously saved changes upon starting up of app
        mainQuizFragment.changeQuizGuessRows(PreferenceManager.getDefaultSharedPreferences(this));
        mainQuizFragment.changeAnimalTypesInQuiz(PreferenceManager.getDefaultSharedPreferences(this));
        mainQuizFragment.changeQuizBackgroundColor(PreferenceManager.getDefaultSharedPreferences(this));
        mainQuizFragment.changeQuizFont(PreferenceManager.getDefaultSharedPreferences(this));
        mainQuizFragment.resetAnimalQuiz(); // reset with changes
        isSettingsChanged = false; // now settings are not changed
    }

    private SharedPreferences.OnSharedPreferenceChangeListener settingsChangedListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override // called whenever user makes changes to settings
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            isSettingsChanged = true; // when settings are changed
            switch (key) { // key passed here as arg

                case NUM_OF_GUESSES_KEY:
                    mainQuizFragment.changeQuizGuessRows(sharedPreferences); // pass received param
                    mainQuizFragment.resetAnimalQuiz(); // reset quiz with changed guess options
                    Toast.makeText(MainActivity.this, R.string.changes_applied, Toast.LENGTH_SHORT).show();
                    break;

                case ANIMAL_TYPES_KEY: // check for what value was selected, coz multiple options
                    Set<String> animalTypes = sharedPreferences.getStringSet(ANIMAL_TYPES_KEY, null);
                        // string set of default values can be specified in above line --------
                    if(animalTypes != null && animalTypes.size()>0) {
                        mainQuizFragment.changeAnimalTypesInQuiz(sharedPreferences);
                        mainQuizFragment.resetAnimalQuiz();
                        Toast.makeText(MainActivity.this, R.string.changes_applied, Toast.LENGTH_SHORT).show();
                    }
                    else { // both checkboxes are unchecked, set is empty
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        animalTypes.add(getString(R.string.default_animal_type)); // put default value into the empty set
                            // check default value already specified in xml preferences file
                        editor.putStringSet(ANIMAL_TYPES_KEY, animalTypes); // pass & save set to preferences
                        editor.apply();
                        Toast.makeText(MainActivity.this, R.string.animal_type_error, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case BG_COLOR_KEY:
                    mainQuizFragment.changeQuizBackgroundColor(sharedPreferences);
                    // mainQuizFragment.resetAnimalQuiz(); // not necessary?
                    Toast.makeText(MainActivity.this, R.string.changes_applied, Toast.LENGTH_SHORT).show();
                    break;

                case FONT_KEY:
                    mainQuizFragment.changeQuizFont(sharedPreferences);
                    // mainQuizFragment.resetAnimalQuiz(); // not necessary ?
                    Toast.makeText(MainActivity.this, R.string.changes_applied, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}