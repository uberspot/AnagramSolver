package com.as.anagramsolver;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.as.anagramsolver.DictionaryDBCreator.DICTIONARY;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class StartPage extends Activity {

	private DictionaryDBCreator dbCreator;
	private final static String preferencesName = "AnagramPrefs";
	private DICTIONARY languageSelected;
	private Set<String> languagesEnabled;
	
	/* Views */
	private ListView listView;
	private TextView output;
	private EditText input;
	private Spinner spinner;
	
	/** An AsyncTask that:
	 * - Displays a progress dialog
	 * - Initializes the sqlite database with getReadableDatabase()
	 * - Updates it with the latest enabled languages and creates 
	 *   the appropriate tables for them
	 * - Dismisses the progress dialog
	 */
	class DBLoaderTask extends AsyncTask<String, Void, String> {
        private ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
        	mProgressDialog = ProgressDialog.show(StartPage.this, 
        			"Please wait", "Populating sqlite databases. \n" + 
        			"This might take 3-10 minutes depending on the enabled languages...", true, false);
        }

        protected String doInBackground(String... strings) {
        		SQLiteDatabase db = dbCreator.getReadableDatabase();
        		
        		Set<DICTIONARY> languages = new HashSet<DICTIONARY>();
        		for(String lang: languagesEnabled) languages.add(DICTIONARY.valueOf(lang));
        		dbCreator.setEnabledDictionaries(languages);
        		dbCreator.createTables(db);
            return "";
        }

        protected void onPostExecute(String result) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.start_page);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); 
        
        // Load previously selected language from preferences
        languageSelected = DICTIONARY.valueOf(getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        								.getString("languageSelected", "ENGLISH"));
        
        // Load enabled languages from preferences
  		languagesEnabled = getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
  				.getStringSet("languagesEnabled", new HashSet<String>(Arrays.asList(new String[] { "ENGLISH" })));
  		
  		// Find the Views once in OnCreate to save time and not use findViewById later.
  		spinner = (Spinner) findViewById(R.id.langSpinner);
  		input = (EditText) findViewById(R.id.inputText);
  		output = (TextView) findViewById(R.id.results);
  		listView = (ListView) findViewById(R.id.wordList);
  		listView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> lView, View v,
					int pos, long id) {
				if (v!=null) {
					TextView textClicked = (TextView) v.findViewById(R.id.wordTextView);
	            	startActivity(new Intent(Intent.ACTION_VIEW, 
	            			Uri.parse("http://google.com/search?q=define:" + textClicked.getText().toString())));
				}
        		return true;
			}
        });
  		
  		
  		//Initialize the database
        dbCreator = new DictionaryDBCreator(getApplicationContext());
        new DBLoaderTask().execute();
		
		setupSpinner(); 
    }
    
    /** Initializes the spinner view and fills it with the language choices 
     *  that are enabled via the settings. */
	private void setupSpinner() {		
		String[] values = languagesEnabled.toArray(new String[languagesEnabled.size()]); 
		
		spinner.setAdapter(new ArrayAdapter<String>(this, 
        				android.R.layout.simple_spinner_dropdown_item, values));
        
		spinner.setSelection(getSelectedLanguage(values));
		spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
		        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
		        	languageSelected =  DICTIONARY.valueOf((String) parent.getItemAtPosition(pos));
		        	//Save change in preferences
		        	SharedPreferences.Editor editor = getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit();
			  	    editor.putString("languageSelected", languageSelected.toString());
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
				if(values[i].equalsIgnoreCase(languageSelected.toString()))
					return i;
		}
		return 0;
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start_page, menu);
        return true;
    }
    
    @Override
	public void onDestroy() {
		super.onDestroy();
		this.finish();
	}
    
    /** Called when the search button is pressed.
     * @param view
     */
    public void onSearchButtonClick(View view) {
    	String inLetters = input.getText().toString().trim();
    	
    	if (dbCreator!=null) {
    		Set<String> w = dbCreator.getAllMatchingAnagrams(languageSelected, inLetters);
    		String[] words = w.toArray(new String[w.size()]);
    		
	    	output.setText("Matches (" + words.length + "):\n");
	    	
	    	//UpdateListview
	    	ListAdapter adapter = new ListAdapter(getApplicationContext(), words);
	    	listView.setAdapter(adapter);
    	}
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
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
        boolean[] checkedLanguages = new boolean[DICTIONARY.values().length];
        for(int i=0; i<checkedLanguages.length; i++) { 
        	if(languagesEnabled.contains(DICTIONARY.values()[i].toString())){
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
               .setMultiChoiceItems(R.array.languages, checkedLanguages, //CHANGE the preselected ones
                          new DialogInterface.OnMultiChoiceClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which,
                           boolean isChecked) {
                	   		   String checkedLang = DICTIONARY.values()[which].toString();
		                       if (isChecked) {
		                           // If the user checked the item, add it to the selected items
		                    	   languagesEnabled.add(checkedLang);
		                       } else if (languagesEnabled.contains(checkedLang)) {
		                           // Else, if the item is already in the array, remove it 
		                    	   languagesEnabled.remove(checkedLang);
		                       }
                   }
               })
               // Set the action buttons
               .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int id) {
                	   		// Save selected language list 
	                	    SharedPreferences.Editor editor = getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit();
		   			  	    editor.putStringSet("languagesEnabled", languagesEnabled);
		   			  	    editor.commit();
		   			  	    
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
