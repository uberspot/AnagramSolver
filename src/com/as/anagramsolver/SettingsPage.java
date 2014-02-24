package com.as.anagramsolver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.uberspot.storageutils.StorageUtils;

@SuppressLint("NewApi")
@SuppressWarnings("deprecation")
public class SettingsPage extends SherlockPreferenceActivity {
	
	public static final String LANG_SEL_KEY = "langSelected";
	public static final String SEARCH_SUBSTR_KEY = "searchSubstr";
	public static final String LANG_ENABLED_KEY = "langEnabled";
	public static final String KEEP_SCREEN_ON_KEY = "keepscreenon";
	public static final String SORT_OPTION_KEY = "sortpreference";
	private StorageUtils storage;
	public static String sortOptionSelected = "1";
	
	/** A set containing all the DICTIONARYs that are enabled*/
	private static Set<String> enabledDictionaries;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
				
		// If in android 3+ use a preference fragment which is the new recommended way
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getFragmentManager().beginTransaction()
					.replace(android.R.id.content, new PreferenceFragment() {
						@Override
						public void onCreate(final Bundle savedInstanceState) {
							super.onCreate(savedInstanceState);
							addPreferencesFromResource(R.xml.settings);
							findPreference(LANG_ENABLED_KEY)
								.setOnPreferenceClickListener(enableLanguageListener);
							setEnabledLangSumm(findPreference(LANG_ENABLED_KEY));
							setSortOptionSumm(findPreference(SORT_OPTION_KEY));
						}
					})
					.commit();
		} else {
			// Otherwise load the preferences.xml in the Activity like in previous android versions
			addPreferencesFromResource(R.xml.settings);
			findPreference(LANG_ENABLED_KEY).setOnPreferenceClickListener(enableLanguageListener);
			setEnabledLangSumm(findPreference(LANG_ENABLED_KEY));
			setSortOptionSumm(findPreference(SORT_OPTION_KEY));
		}
		
		storage = new StorageUtils(getApplicationContext());
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpTo(this, new Intent(this, StartPage.class));
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onDestroy() {
		// reload preferences on exit from settings screen
		Context context = getApplicationContext();
		loadSettings(context);
		super.onDestroy();
	}

	/** Loads user settings to app. Called when settings change and users exits from 
	 *  settings screen or when the app first starts. 
	 *  */
	public static void loadSettings(Context context) {
		StorageUtils storage = StorageUtils.getInstance(context);
		
		// Load enabled languages from preferences
  		Set<String> enabledLangs = storage.getPreferenceSet(
  							SettingsPage.LANG_ENABLED_KEY, 
  							new HashSet<String>(
  									Arrays.asList(new String[] { DictionaryDBCreator.DEFAULT_DICTIONARY })
  									)
  							);
  		
  		SettingsPage.setEnabledDictionaries(enabledLangs);
    	
    	sortOptionSelected = PreferenceManager.getDefaultSharedPreferences(context)
    							.getString(SettingsPage.SORT_OPTION_KEY, "1");
	}
	
	private Preference.OnPreferenceClickListener enableLanguageListener = 
			new Preference.OnPreferenceClickListener() {
					@Override
					public boolean onPreferenceClick(Preference pref) {
						showLanguageSelection(pref);
						return true;
					}
	};
	
	/** Called when the settings button is pressed. Displays an alert dialog
     * with a checkbox for each language that can be enabled. 
     * After the Ok button is pressed it saves the enabled languages in the preferences
     * and loads the appropriate databases again.
     */
    private void showLanguageSelection(final Preference pref) {
    	// Find which language checkboxes should be checked based on the languagesEnabled set
    	int dictionariesSize = DictionaryDBCreator.DICTIONARIES.size();
        boolean[] checkedLanguages = new boolean[dictionariesSize];
        String[] langs = new String[dictionariesSize];
        for(int i=0; i < checkedLanguages.length; i++) { 
        	langs[i] = DictionaryDBCreator.DICTIONARIES.get(i);
        	if( SettingsPage.hasLoadedDictionary(langs[i]) ){
        		checkedLanguages[i] = true;
        	} else {
        		checkedLanguages[i] = false;
        	}
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Set the dialog title
        builder.setTitle(R.string.choose_enabled_languages)
        // Specify the list array, the items to be selected by default (null for none),
        // and the listener through which to receive callbacks when items are selected
               .setMultiChoiceItems(langs, checkedLanguages, 
                          new DialogInterface.OnMultiChoiceClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                	   		   String checkedLang = DictionaryDBCreator.DICTIONARIES.get(which);
		                       if( isChecked ) {
		                           // If the user checked the item, add it to the selected items
		                    	   SettingsPage.addEnabledDictionary(checkedLang);
		                       } else if ( SettingsPage.hasLoadedDictionary(checkedLang) ) {
		                           // Else, if the item is already in the array, remove it 
		                    	   SettingsPage.removeEnabledDictionary(checkedLang);
		                       }
                   }
               })
               // Set the action buttons
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                	   		// Save selected language list 
                	   		storage.savePreferenceSet(LANG_ENABLED_KEY, SettingsPage.getEnabledDictionaries());
                	   		
                	   		pref.setSummary(getString(R.string.enabled_languages_list, 
            						SettingsPage.getEnabledDictionaries().toString()));
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) { }
               }).show();
    }
    
	private void setSortOptionSumm(final Preference pref) {
		pref.setOnPreferenceChangeListener(
		        new OnPreferenceChangeListener() {
		    public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		pref.setSummary(getString(R.string.sort_results_sum, 
						getResources()
							.getStringArray(R.array.sortOptionsArray)
									[Integer.parseInt((String) newValue) - 1] ));
					return true;
		    }
		});
		pref.setSummary(getString(R.string.sort_results_sum, 
					getResources()
						.getStringArray(R.array.sortOptionsArray)
								[Integer.parseInt(sortOptionSelected) - 1] ));
	}

	private void setEnabledLangSumm(final Preference pref) {
		pref.setOnPreferenceChangeListener(
		        new OnPreferenceChangeListener() {
		    public boolean onPreferenceChange(Preference preference, Object newValue) {
		    		pref.setSummary(getString(R.string.enabled_languages_list, 
						SettingsPage.getEnabledDictionaries().toString()));
					return true;
		    }
		});
		pref.setSummary(getString(R.string.enabled_languages_list, 
						SettingsPage.getEnabledDictionaries().toString()));
	}

	/** enabledDictionaries Methods START **/
    

	public static boolean hasLoadedDictionary(String d) {
		return enabledDictionaries.contains(d);
	}
	
	public static Set<String> getEnabledDictionaries() {
		return enabledDictionaries;
	}
	
	public static void addEnabledDictionary(String dict) {
		if(enabledDictionaries == null)
			enabledDictionaries = new HashSet<String>();
		enabledDictionaries.add(dict);
	}
	
	public static void removeEnabledDictionary(String dict) {
		if(enabledDictionaries != null)
			enabledDictionaries.remove(dict);
	}

	public static void setEnabledDictionaries(Set<String> enabledDictionaries) {
		if(SettingsPage.enabledDictionaries != null)
			SettingsPage.enabledDictionaries.clear();
		else
			SettingsPage.enabledDictionaries = new HashSet<String>();
		SettingsPage.enabledDictionaries.addAll(enabledDictionaries);
	}
	
    /** enabledDictionaries Methods END **/
}
