package com.as.anagramsolver;

import java.text.Normalizer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.uberspot.storageutils.StorageUtils;

import de.cketti.library.changelog.ChangeLog;

public class StartPage extends SherlockActivity {

	private DictionaryDBCreator dbCreator;
	private String languageSelected;
	private StorageUtils storage;
	private DBSearchTask dbSearchTask;

	/* Views */
	private ListView listView;
	private TextView output;
	private EditText input;
	private Spinner spinner;
	private CheckBox searchSubstrings;
	private Button searchButton;
	private boolean searching;
	private String searchString;
	private ProgressDialog mProgressDialog;

	/** Pattern used for removing diacritical marks like accents as to normalize
	 *  given words to a simpler form */
	private Pattern pattern;

	/** An AsyncTask that:
	 * - Displays a progress dialog
	 * - Initializes the sqlite database with getReadableDatabase()
	 * - Updates it with the latest enabled languages and creates
	 *   the appropriate tables for them
	 * - Dismisses the progress dialog
	 */
	class DBLoaderTask extends AsyncTask<String, String, String> {
        @Override
        protected void onPreExecute() {
        	mProgressDialog = ProgressDialog.show(StartPage.this,
        							getString(R.string.please_wait),
        							getString(R.string.populating_db),
        							true, false);
        }

        @Override
		protected String doInBackground(String... strings) {
        	if(!searching) {
        		SQLiteDatabase db = dbCreator.getReadableDatabase();

        		//For each available language
        		for (String dict : DictionaryDBCreator.DICTIONARIES) {
        			publishProgress(dict);
        			dbCreator.createTable(db, dict);
        		}
        	}
            return "";
        }

        @Override
		protected void onPostExecute(String result) {
        	dismissProgressDialog();
        }

        @Override
	    public void onProgressUpdate(String... args) {
        	mProgressDialog.setMessage(getString(R.string.populating_dbs, args[0]));
        }
    }

	/** An AsyncTask that searches for anagrams and updates the UI when they're found.
	 */
	class DBSearchTask extends AsyncTask<String, Void, String> {
		private String[] words;

        @Override protected void onPreExecute() {
        	searching = true;
        	Toast.makeText(getApplicationContext(),
        			getString(R.string.searching_please_wait),
        			Toast.LENGTH_SHORT)
        			.show();
        }

        @Override
		protected String doInBackground(String... strings) {
        	String inLetters = searchString;

        	if(inLetters.contains("*")) {

        		inLetters = inLetters.replace('*', '%');
        		Set<String> starWords = new HashSet<String>();
        		starWords.addAll( dbCreator.getStarMatches(languageSelected, inLetters) );
				words = starWords.toArray(new String[starWords.size()]);
				publishProgress();

        	} else {
        		// Normalize input
        		String nfdNormalizedString = Normalizer.normalize((inLetters), Normalizer.Form.NFD);
        		inLetters = pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase(Locale.getDefault());

	    		if( searchSubstrings.isChecked() ) {
	    			searchAllMatchingAnagrams(languageSelected, inLetters);
	    		} else {
	    			Set<String> matchingWords = new HashSet<String>();
	    			matchingWords.addAll( dbCreator.getMatchingAnagrams(languageSelected, inLetters) );
					words = matchingWords.toArray(new String[matchingWords.size()]);
					publishProgress();
	    		}
    		}
            return "";
        }

        @Override
		protected void onPostExecute(String result) {
        	onCancelled();
        }

        @Override
		protected void onProgressUpdate(Void... values) {
        	output.setText(getString(R.string.matches, words.length ));

	    	//UpdateListview
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(
											getApplicationContext(),
											R.layout.word_layout, words);

			if( SettingsPage.sortOptionSelected.equals("2") ) {
				adapter.sort(lengthAscComp);
			} else if( SettingsPage.sortOptionSelected.equals("3") ) {
				adapter.sort(lengthDescComp);
			} else {
				adapter.sort(alphabeticComp);
			}

			listView.setAdapter(adapter);
        }

        @Override
        protected void onCancelled() {
        	searching = false;
	    	searchButton.setText(getString(R.string.search_words));
	    	Toast.makeText(getApplicationContext(),
	    			getString(R.string.search_ended),
	    			Toast.LENGTH_SHORT)
	    			.show();
        }

        /** Searches for anagrams with all the words that can be formed
         * from the given letters in value and from all the subsets of those letters
         * and updates the results view every time it finds a result.
    	 * @param dict The dictionary in which to search for matches
    	 * @param value The letters to search for anagrams
    	 * @return
    	 */
    	private void searchAllMatchingAnagrams(String dict, String value) {
    			int numOfSubsets = 1 << value.length();
    			Set<String> matchingWords = new HashSet<String>();

    			for (int i = 0; i < numOfSubsets; i++) {
    				int pos = value.length() - 1;
    				int bitmask = i;

    				StringBuilder str = new StringBuilder("");
    				while ( bitmask > 0 ) {
    					if ((bitmask & 1) == 1) {
							str.append(value.charAt(pos));
						}
    					bitmask >>= 1;
    					pos--;
    				}
    				if( str.length() > 3 ) {
    					Set<String> tempSet = dbCreator.getMatchingAnagrams(dict, str.toString());
    					if(!tempSet.isEmpty()) {
	    					matchingWords.addAll(tempSet);
	    					words = matchingWords.toArray(new String[matchingWords.size()]);
	    					publishProgress();
    					}
    				}
    				if( isCancelled() ) {
    					break;
    				}
    			}
    	}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.start_page);

        storage = new StorageUtils(getApplicationContext());

        boolean keepScreenOn = PreferenceManager.getDefaultSharedPreferences(this)
        								.getBoolean(SettingsPage.KEEP_SCREEN_ON_KEY, true);
        if(keepScreenOn) {
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}

        // Show changelog if it's the first time a new version loads
        ChangeLog cl = new ChangeLog(this);
        if ( cl.isFirstRun() ) {
        	cl.getLogDialog().show();
        }

        // Load previously selected language from preferences
        languageSelected = storage.getPreference(SettingsPage.LANG_SEL_KEY,
        								DictionaryDBCreator.DEFAULT_DICTIONARY);

  		searching = false;

  		pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  		// Find the Views once in OnCreate to save time and not use findViewById later.
  		spinner = (Spinner) findViewById(R.id.langSpinner);
  		input = (EditText) findViewById(R.id.inputText);
  		input.setOnEditorActionListener(new OnEditorActionListener() {
  		    @Override
  		    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
  		        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
  		        	onSearchButtonClick();
  		            return true;
  		        }
  		        return false;
  		    }
  		});
  		ImageButton clearButton = (ImageButton) findViewById(R.id.clearButton);
  		clearButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if(input != null) {
					input.setText("");
					input.requestFocus();
				}
			}
		});
  		searchButton = (Button) findViewById(R.id.searchButton);
  		searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
			public void onClick(View v) {
            	onSearchButtonClick();
            }
        });
  		output = (TextView) findViewById(R.id.results);
  		listView = (ListView) findViewById(R.id.wordList);
  		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> lView, View v,
					int pos, long id) {
				if (v!=null) {
	            	startActivity(new Intent(Intent.ACTION_VIEW,
	            					Uri.parse("http://google.com/search?q=define:"
	            								+ ((TextView) v).getText().toString())
	            					));
				}
        		return true;
			}
        });
  		searchSubstrings = (CheckBox) findViewById(R.id.search_substrings);
        searchSubstrings.setChecked(PreferenceManager.getDefaultSharedPreferences(this)
									.getBoolean(SettingsPage.SEARCH_SUBSTR_KEY, true));
  		searchSubstrings.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SharedPreferences.Editor editor =
						PreferenceManager.getDefaultSharedPreferences(
								getApplicationContext()
								)
								.edit();
		  	    editor.putBoolean(SettingsPage.SEARCH_SUBSTR_KEY, isChecked);
		  	    editor.commit();
			}});
  		// Initialize the database
        dbCreator = new DictionaryDBCreator(getApplicationContext());
    }

    @Override
    public void onResume() {
    	super.onResume();

    	if( !searching ) {
	    	// Load settings each time the activity loads to ensure latest changes are applied
	    	SettingsPage.loadSettings(getApplicationContext());

	    	new DBLoaderTask().execute();

			setupSpinner();
    	}
    }

    @Override
    public void onPause() {
    	super.onPause();

    	dismissProgressDialog();
    }

    /** Initializes the spinner view and fills it with the language choices
     *  that are enabled via the settings. */
	private void setupSpinner() {
		String[] values = SettingsPage.getEnabledDictionaries()
								.toArray(
										new String[SettingsPage.getEnabledDictionaries().size()]
								 );

		spinner.setAdapter(new ArrayAdapter<String>(this,
        				android.R.layout.simple_list_item_1, values));

		spinner.setSelection(getSelectedLanguage(values));
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
		        @Override
				public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

		        	languageSelected =  (String) parent.getItemAtPosition(pos);
		        	//Save change in preferences
		        	SharedPreferences.Editor editor = PreferenceManager
		        				.getDefaultSharedPreferences(getApplicationContext())
		        				.edit();
			  	    editor.putString(SettingsPage.LANG_SEL_KEY, languageSelected);
			  	    editor.commit();
		        }
		        @Override
				public void onNothingSelected(AdapterView<?> arg0) {}
        });
	}

	/** Returns the position of the selected language in the given array
	 * @param values
	 * @return an int which is the position of the language in the values or 0 if it is not found
	 */
	private int getSelectedLanguage(String[] values) {
		for(int i=0; i < values.length; i++){
				if(values[i].equalsIgnoreCase(languageSelected)) {
					return i;
				}
		}
		return 0;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
    	getSupportMenuInflater().inflate(R.menu.start_page_menu, menu);
        return true;
    }

    /** Called when the search button is pressed. */
    public void onSearchButtonClick() {
    	if (searching) {
    		dbSearchTask.cancel(true);
    		return;
    	}
    	if (dbCreator!=null && SettingsPage.hasLoadedDictionary(languageSelected)) {
    		searchString = input.getText().toString();
        	if(searchString != null
        			&& !(searchString = searchString.trim()).isEmpty()
        			&& searchString.matches("[^!@#$%`~;&\"\\(\\)\\[\\]{}.,<>]+")) {
        		hideSoftKeyboard();
	    		searchButton.setText(getString(R.string.stop_search));
	    		dbSearchTask = new DBSearchTask();
	    		dbSearchTask.execute();
        	} else {
        		Toast.makeText(getApplicationContext(),
        				getString(R.string.no_input_given),
        				Toast.LENGTH_SHORT)
        				.show();
        	}
    	} else {
    		hideSoftKeyboard();
    		Toast.makeText(getApplicationContext(),
    				getString(R.string.dict_not_loaded),
    				Toast.LENGTH_SHORT)
    				.show();
    	}
    }

    private void hideSoftKeyboard(){
        if(getCurrentFocus()!=null && getCurrentFocus() instanceof EditText) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_button:
            	startActivity(new Intent(this, SettingsPage.class));
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	private void dismissProgressDialog() {
		if(mProgressDialog != null){
			try {
				mProgressDialog.dismiss();
			} catch(IllegalArgumentException e) { return; }
		}
	}

	private static final Comparator<String> alphabeticComp = new Comparator<String>() {
		@Override
		public int compare(String str1, String str2) {
			return str1.compareTo(str2);
		}
	};

	/** Compares strings based on string length. If they have the same length
	 * then they are compared alphabetically
	 */
	private static final Comparator<String> lengthAscComp = new Comparator<String>() {
		@Override
		public int compare(String str1, String str2) {
			if(str1.length() == str2.length()) {
				return str1.compareTo(str2);
			}
			return Integer.valueOf( str1.length() ).compareTo( str2.length() );
		}
	};

	/** Compares strings based on string length. If they have the same length
	 * then they are compared alphabetically
	 */
	private static final Comparator<String> lengthDescComp = new Comparator<String>() {
		@Override
		public int compare(String str1, String str2) {
			if(str1.length() == str2.length()) {
				return str1.compareTo(str2);
			}
			return Integer.valueOf( str2.length() ).compareTo( str1.length() );
		}
	};

}
