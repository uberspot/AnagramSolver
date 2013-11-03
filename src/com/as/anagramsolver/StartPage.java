package com.as.anagramsolver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import de.cketti.library.changelog.ChangeLog;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidStorageUtils.StorageUtils;

public class StartPage extends SherlockActivity {

	private static final String LANG_SEL_KEY = "langSelected";
	private static final String SEARCH_SUBSTR_KEY = "searchSubstr";
	private static final String LANG_ENABLED_KEY = "langEnabled";
	private DictionaryDBCreator dbCreator;
	private String languageSelected;
	private StorageUtils storage;
	DBSearchTask dbSearchTask;
	
	/* Views */
	private ListView listView;
	private TextView output;
	private EditText input;
	private Spinner spinner;
	private CheckBox searchSubstrings;
	private Button searchButton;
	private boolean searching;
	
	/** An AsyncTask that:
	 * - Displays a progress dialog
	 * - Initializes the sqlite database with getReadableDatabase()
	 * - Updates it with the latest enabled languages and creates 
	 *   the appropriate tables for them
	 * - Dismisses the progress dialog
	 */
	class DBLoaderTask extends AsyncTask<String, String, String> {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
        	mProgressDialog = ProgressDialog.show(StartPage.this, 
        			getString(R.string.please_wait), getString(R.string.populating_dbs) , true, false);
        }

        protected String doInBackground(String... strings) {
        		SQLiteDatabase db = dbCreator.getReadableDatabase();
        		
        		//For each available language
        		for(String dict: dbCreator.getEnabledDictionaries()) {
        			publishProgress(dict);
        			dbCreator.createTable(db, dict);
        		}
            return "";
        }

        protected void onPostExecute(String result) {
        	if(mProgressDialog!=null && mProgressDialog.isShowing()){
				try {
					mProgressDialog.dismiss();
				} catch(IllegalArgumentException e) { return; }
			} else {
				return;
			}
        }
        
        @Override
	    public void onProgressUpdate(String... args){
        	mProgressDialog.setMessage(getString(R.string.populating_dbs, args[0]));
        }
    }
	
	/** An AsyncTask that searches for anagrams and updates the UI when they're found.
	 */
	class DBSearchTask extends AsyncTask<String, Void, String> {
		private String[] words;
		
        @Override protected void onPreExecute() { 
        	searching = true;
        	Toast.makeText(getApplicationContext(), getString(R.string.searching_please_wait), Toast.LENGTH_SHORT).show();
        }

        protected String doInBackground(String... strings) {
        	String inLetters = input.getText().toString().trim();
    		if(searchSubstrings.isChecked()) {
    			searchAllMatchingAnagrams(languageSelected, inLetters);
    		} else {
    			Set<String> matchingWords = new HashSet<String>();
    			matchingWords.addAll( dbCreator.getMatchingAnagrams(languageSelected, inLetters) );
				words = matchingWords.toArray(new String[matchingWords.size()]);
				publishProgress();
    		}
            return "";
        }

        protected void onPostExecute(String result) {
        	onCancelled();
        }
        
        protected void onProgressUpdate(Void... values) {
        	output.setText(getString(R.string.matches, words.length ));
	    	
	    	//UpdateListview
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(),
					R.layout.word_layout, words);
			listView.setAdapter(adapter);
        }
        
        @Override
        protected void onCancelled() {
        	searching = false;
	    	searchButton.setText(getString(R.string.search_words));
	    	Toast.makeText(getApplicationContext(), getString(R.string.search_ended), Toast.LENGTH_SHORT).show();
        }
        
        /** Searches for anagrams with all the words that can be formed from the given letters in value and from all the subsets of those letters
         *  and updates the results view every time it finds a result. 
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
    				while (bitmask > 0) {
    					if ((bitmask & 1) == 1)
    						str.append(value.charAt(pos));
    					bitmask >>= 1;
    					pos--;
    				}
    				if(str.length()>3) {
    					Set<String> tempSet = dbCreator.getMatchingAnagrams(dict, str.toString());
    					if(!tempSet.isEmpty()) {
	    					matchingWords.addAll(tempSet);
	    					words = matchingWords.toArray(new String[matchingWords.size()]);
	    					publishProgress();
    					}
    				}
    				if(isCancelled()) {
    					break;
    				}
    			}
    	}
    }
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.start_page);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        storage = new StorageUtils(getApplicationContext());
        
        // Show changelog if it's the first time a new version loads
        ChangeLog cl = new ChangeLog(this);
        if (cl.isFirstRun()) {
        	cl.getLogDialog().show();
        }
        
        // Load previously selected language from preferences
        languageSelected = storage.getPreference(LANG_SEL_KEY, DictionaryDBCreator.DEFAULT_DICTIONARY);
        
  		searching = false; 
  		
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
  		searchButton = (Button) findViewById(R.id.searchButton);
  		searchButton.setOnClickListener(new View.OnClickListener() {
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
	            			Uri.parse("http://google.com/search?q=define:" + ((TextView) v).getText().toString())));
				}
        		return true;
			}
        });
  		searchSubstrings = (CheckBox) findViewById(R.id.search_substrings);
        searchSubstrings.setChecked(PreferenceManager.getDefaultSharedPreferences(this)
				.getBoolean(SEARCH_SUBSTR_KEY, true));
  		searchSubstrings.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
		  	    editor.putBoolean(SEARCH_SUBSTR_KEY, isChecked);
		  	    editor.commit();
			}});
  		//Initialize the database
        dbCreator = new DictionaryDBCreator(getApplicationContext());
        // Load enabled languages from preferences
  		
  		dbCreator.setEnabledDictionaries(
  				storage.getPreferenceSet(LANG_ENABLED_KEY, 
  							new HashSet<String>(Arrays.asList(new String[] { DictionaryDBCreator.DEFAULT_DICTIONARY })))
  				);
        new DBLoaderTask().execute();
		
		setupSpinner(); 
    }
    
    /** Initializes the spinner view and fills it with the language choices 
     *  that are enabled via the settings. */
	private void setupSpinner() {
		String[] values = dbCreator.getEnabledDictionaries().toArray(new String[dbCreator.getEnabledDictionaries().size()]); 
		
		spinner.setAdapter(new ArrayAdapter<String>(this, 
        				android.R.layout.simple_list_item_1, values));
        
		spinner.setSelection(getSelectedLanguage(values));
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
		        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		        	languageSelected =  (String) parent.getItemAtPosition(pos);
		        	//Save change in preferences
		        	SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit();
			  	    editor.putString(LANG_SEL_KEY, languageSelected);
			  	    editor.commit();
		        } 
		        public void onNothingSelected(AdapterView<?> arg0) {}
        });
	}

	/** Returns the position of the selected language in the given array
	 * @param values
	 * @return an int which is the position of the language in the values or 0 if it is not found
	 */
	private int getSelectedLanguage(String[] values) {
		int i=0;
		for(; i<values.length; i++){
				if(values[i].equalsIgnoreCase(languageSelected))
					return i;
		}
		return 0;
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
    	getSupportMenuInflater().inflate(R.menu.start_page, menu);
        return true;
    }
    
    /** Called when the search button is pressed. */
    public void onSearchButtonClick() {
    	if (searching) {
    		dbSearchTask.cancel(true);
    		return;
    	}
    	if (dbCreator!=null && dbCreator.hasLoadedDictionary(languageSelected)) {
    		String inLetters = input.getText().toString().trim();
        	if(inLetters!=null && !"".equals(inLetters)) {
        		hideSoftKeyboard();
	    		searchButton.setText(getString(R.string.stop_search));
	    		dbSearchTask = new DBSearchTask();
	    		dbSearchTask.execute();
        	} else {
        		Toast.makeText(getApplicationContext(), getString(R.string.no_input_given), Toast.LENGTH_SHORT).show();
        	}
    	} else {
    		hideSoftKeyboard();
    		Toast.makeText(getApplicationContext(), getString(R.string.dict_not_loaded), Toast.LENGTH_SHORT).show();
    	}
    }
    
    private void hideSoftKeyboard(){
        if(getCurrentFocus()!=null && getCurrentFocus() instanceof EditText){
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings_button:
            	showLanguageSelection();
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    /** Called when the settings button is pressed. Displays an alert dialog
     * with a checkbox for each language that can be enabled. 
     * After the Ok button is pressed it saves the enabled languages in the preferences
     * and loads the appropriate databases again.
     */
    private void showLanguageSelection(){
    	// Find which language checkboxes should be checked based on the languagesEnabled set
    	int dictionariesSize = DictionaryDBCreator.DICTIONARIES.size();
        boolean[] checkedLanguages = new boolean[dictionariesSize];
        String[] langs = new String[dictionariesSize];
        for(int i=0; i < checkedLanguages.length; i++) { 
        	langs[i] = DictionaryDBCreator.DICTIONARIES.get(i);
        	if(dbCreator.hasLoadedDictionary(langs[i])){
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
		                       if (isChecked) {
		                           // If the user checked the item, add it to the selected items
		                    	   dbCreator.addEnabledDictionary(checkedLang);
		                       } else if (dbCreator.hasLoadedDictionary(checkedLang)) {
		                           // Else, if the item is already in the array, remove it 
		                    	   dbCreator.removeEnabledDictionary(checkedLang);
		                       }
                   }
               })
               // Set the action buttons
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                	   		// Save selected language list 
                	   		storage.savePreferenceSet(LANG_ENABLED_KEY, dbCreator.getEnabledDictionaries());
	                	    
		   			  	    // Update spinner language list
			   			  	setupSpinner();
			   			  	
			   			    //Update dictionary tables in database
			   			  	new DBLoaderTask().execute();
                   }
               })
               .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) { }
               }).show();
    }
   
}
