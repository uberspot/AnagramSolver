package com.as.anagramsolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;


public class DictionaryDBCreator extends SQLiteOpenHelper {

	/** The list of dictionaries available */
	public static final ArrayList<String> DICTIONARIES = new ArrayList<String>(
			Arrays.asList("English", "Greek", "Polish", "French", "German", "Spanish", "Italian")    );
	
	public static final String DEFAULT_DICTIONARY = DICTIONARIES.get(0);
	
	/** An array containing the corresponding raw dictionary file for each language of the enumeration. */
	private static final int[] dictIDs = { R.raw.en_us, R.raw.el_gr, R.raw.pl_pl, R.raw.fr_fr, R.raw.de_de, R.raw.es_es, R.raw.it_it},
						/** An array containing the corresponding raw sorted dictionary file. 
						 * Each word in that file is the same as the original but with its characters 
						 * sorted alphabetically, lowercased and normalized(no accents etc) */
					    sdictIDs = { R.raw.en_us_sorted, R.raw.el_gr_sorted, R.raw.pl_pl_sorted, R.raw.fr_fr_sorted, R.raw.de_de_sorted, R.raw.es_es_sorted, R.raw.it_it_sorted};
	
	/** A set containing all the DICTIONARYs that are enabled*/
	private Set<String> enabledDictionaries; 
	
	private static final int DATABASE_VERSION = 27;
	private static final String DATABASE_NAME = "Dictionaries";
	private Context context;
	
	/** Pattern used for removing diacritical marks like accents as to normalize given words to a simpler form */
	private Pattern pattern;
	
	public DictionaryDBCreator(Context context, Set<String> enabledDictionaries) {
	    this(context, DATABASE_NAME, null, DATABASE_VERSION, enabledDictionaries);
	}
	
	public DictionaryDBCreator(Context context) {
	    this(context, DATABASE_NAME, null, DATABASE_VERSION, 
	    		new HashSet<String>());
	}
	
	public DictionaryDBCreator(Context context, String name,
			CursorFactory factory, int version, Set<String> enabledDictionaries) {
		super(context, name, factory, version);
		this.context = context;
		this.setEnabledDictionaries(enabledDictionaries);
		pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		//For each available language
		for(String dict: DICTIONARIES) {
			createTable(db, dict);
		}
	}

	public void createTable(final SQLiteDatabase db, String dict) {
		//If it is enabled
		if(hasLoadedDictionary(dict)) {
			dict = dict.substring(0, 1).toUpperCase() + dict.substring(1).toLowerCase();
			int position = DICTIONARIES.indexOf(dict);
			
			//And if the table doesn't exist already
			if(!tableExists(db, dict)) {
				//Create and fill it
				db.execSQL("CREATE TABLE IF NOT EXISTS " + dict + "(word TEXT, aword INTEGER);");
				fillDictionary(db, dictIDs[position], sdictIDs[position], dict);
			}
		//Else Drop it from the database so it doesn't take up space
		} else {
			db.execSQL("DROP TABLE IF EXISTS " + dict + ";");
		}
		//Clean up after leftover pages in memory
		db.rawQuery("VACUUM", null);
	}

	public boolean hasLoadedDictionary(String d) {
		return enabledDictionaries.contains(d);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older tables if existed
		for(String dictionary : DICTIONARIES) {
			db.execSQL("DROP TABLE IF EXISTS " + dictionary);
		}
        // Create tables again
        onCreate(db);
	}
	
	@Override 
	public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		onUpgrade(db, oldVersion, newVersion);
	}
	
	/** Reads the given input files and inserts each word and its sorted equivalent to the database
	 * @param db the database to update
	 * @param rawResourceId the file containing each word of the dictionary
	 * @param rawSortedResourceId the file containing each equivalent word of the dictionary sorted and normalized
	 * @param dict the dictionary for which to update the table
	 */
	private void fillDictionary(SQLiteDatabase db, int rawResourceId, int rawSortedResourceId, String dict) {
    	ContentValues v = new ContentValues();
    	db.execSQL("PRAGMA read_uncommitted = true;");
		db.beginTransaction(); 
		try {
				BufferedReader in1 = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(rawResourceId), "UTF-8"));
				BufferedReader in2 = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(rawSortedResourceId), "UTF-8"));
				
				String line1 = in1.readLine();
				String line2 = in2.readLine();
				
				while(line1!=null && line2!=null) {
						v.put("word", line1);
						v.put("aword",  Integer.parseInt(line2));
						
						db.insert(dict.toString(), null, v);
						
						line1 = in1.readLine();
						line2 = in2.readLine();
						v.clear();
				}
				db.setTransactionSuccessful();
		} catch (NotFoundException e) {
				System.out.println("File not found: " + e.getMessage());
		} catch (UnsupportedEncodingException e) {
				System.out.println("UnsupportedEncoding: " + e.getMessage());
		} catch (IOException e) {
				System.out.println("IO Error: " + e.getMessage());
		} 
		db.endTransaction();
	}
	
	/** Returns a Set<String> with all the words that can be formed from the given letters in value
	 * @param dict The dictionary in which to search for matches
	 * @param value The letters to search for anagrams
	 * @return
	 */
	public Set<String> getMatchingAnagrams(String dict, String value) {
		char[] l = value.toCharArray();
		java.util.Arrays.sort(l);
		
		String nfdNormalizedString = Normalizer.normalize((new String(l)), Normalizer.Form.NFD); 
	    String deaccented = pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase();
		
	    // Select "all matches" Query
	    String selectQuery = "SELECT word FROM " + dict + " WHERE aword=" +  deaccented.hashCode();
	    
		return rawQueryResults(selectQuery);
	}
	
	/** Returns a Set<String> with all the words that are like the regex word* or *word or wo*rd. 
	 * @param dict The dictionary in which to search for matches
	 * @param value The word to search for. Looks like word* or *word or wo*rd 
	 * @return
	 */
	public Set<String> getStarMatches(String dict, String word) {
		if(word == null || word.isEmpty())
			return new HashSet<String>();
	    // Select "all matches" Query
	    String selectQuery = "SELECT word FROM " + dict + " WHERE word LIKE \'" +  word + "\'";
		return rawQueryResults(selectQuery);
	}

	public Set<String> rawQueryResults(String selectQuery) {
		Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
	    
	    Set<String> matchingWords = new HashSet<String>();
	    
		// looping through all results and adding to list
		if (cursor!=null && cursor.moveToFirst()) {
			do {
				matchingWords.add(cursor.getString(0));
			} while (cursor.moveToNext());
		}
		
		cursor.close();
		return matchingWords;
	}
	
	/** Returns true if the table with the given name exists in the database, false otherwise
	 * @param tableName
	 * @return
	 */
	public boolean tableExists(SQLiteDatabase db, String tableName) {
		Cursor c = db.rawQuery("SELECT * FROM sqlite_master WHERE UPPER(name) LIKE UPPER('" + tableName + "') and type='table' ", null);
		boolean exists = c.moveToFirst();
		c.close();
	    return exists;
	}

	public Set<String> getEnabledDictionaries() {
		return enabledDictionaries;
	}
	
	public void addEnabledDictionary(String dict) {
		if(enabledDictionaries != null)
			enabledDictionaries.add(dict);
	}
	
	public void removeEnabledDictionary(String dict) {
		if(enabledDictionaries != null)
			enabledDictionaries.remove(dict);
	}

	public void setEnabledDictionaries(Set<String> enabledDictionaries) {
		if(this.enabledDictionaries != null)
			this.enabledDictionaries.clear();
		else
			this.enabledDictionaries = new HashSet<String>();
		this.enabledDictionaries.addAll(enabledDictionaries);
	}
}
