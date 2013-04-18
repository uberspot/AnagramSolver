package com.as.anagramsolver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
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
	public enum DICTIONARY {ENGLISH, GREEK, POLISH, FRENCH}
	
	/** An array containing the corresponding raw dictionary file for each language of the enumeration. */
	private final int[] dictIDs={R.raw.en_us, R.raw.el_gr, R.raw.pl_pl, R.raw.fr_fr},
						/** An array containing the corresponding raw sorted dictionary file. 
						 * Each word in that file is the same as the original but with its characters 
						 * sorted alphabetically, lowercased and normalized(no accents etc) */
					    sdictIDs={R.raw.en_us_sorted, R.raw.el_gr_sorted, R.raw.pl_pl_sorted, R.raw.fr_fr_sorted};
	
	/** A set containing all the DICTIONARYs that are enabled*/
	private Set<DICTIONARY> enabledDictionaries; 
	
	private static final int DATABASE_VERSION = 22;
	private static final String DATABASE_NAME = "Dictionaries";
	private Context context;
	
	/** Pattern used for removing diacritical marks like accents as to normalize given words to a simpler form */
	private Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
	
	public DictionaryDBCreator(Context context, Set<DICTIONARY> enabledDictionaries) {
	    this(context, DATABASE_NAME, null, DATABASE_VERSION, enabledDictionaries);
	}
	
	public DictionaryDBCreator(Context context) {
	    this(context, DATABASE_NAME, null, DATABASE_VERSION, 
	    		new HashSet<DICTIONARY>());
	}
	
	public DictionaryDBCreator(Context context, String name,
			CursorFactory factory, int version, Set<DICTIONARY> enabledDictionaries) {
		super(context, name, factory, version);
		this.context = context;
		this.setEnabledDictionaries(enabledDictionaries);
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {
		createTables(db);
	}

	/** Based on the enabledDictionaries list it fills the tables with words.
	 * @param db
	 */
	public void createTables(final SQLiteDatabase db) {
		//For each available language
		DICTIONARY[] d = DICTIONARY.values();
		for(int i=0; i<d.length; i++) {
			//If it is enabled
			if(hasLoadedDictionary(d[i])) {
				//And if the table doesn't exist already
				if(!tableExists(d[i].toString())) {
					//Create and fill it
					db.execSQL("CREATE TABLE IF NOT EXISTS " + d[i].toString() + "(word TEXT, aword TEXT);");
					fillDictionary(db, dictIDs[i], sdictIDs[i], d[i]);
				}
			//Else Drop it from the database so it doesn't take up space
			} else {
				db.execSQL("DROP TABLE IF EXISTS " + d[i].toString() + ";");
			}
			//Clean up after leftover pages in memory
			db.rawQuery("VACUUM", null);
		}
	}

	public boolean hasLoadedDictionary(DICTIONARY d) {
		return enabledDictionaries.contains(d);
	}
	
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// Drop older tables if existed
		for(DICTIONARY dictionary : DICTIONARY.values()) {
			db.execSQL("DROP TABLE IF EXISTS " + dictionary.toString());
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
	private void fillDictionary(SQLiteDatabase db, int rawResourceId, int rawSortedResourceId, DICTIONARY dict) {
    	ContentValues v = new ContentValues();
		db.beginTransaction(); 
		try {
				BufferedReader in1 = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(rawResourceId), "UTF-8"));
				BufferedReader in2 = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(rawSortedResourceId), "UTF-8"));
				
				String line1 = in1.readLine();
				String line2 = in2.readLine();
				
				while(line1!=null && line2!=null) {
						v.put("word", line1);
						v.put("aword",  line2); 
						
						db.insert(dict.toString(), null, v);
						
						line1 = in1.readLine();
						line2 = in2.readLine();
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
	public Set<String> getMatchingAnagrams(DICTIONARY dict, String value) {
		char[] l = value.toCharArray();
		java.util.Arrays.sort(l);
		
		String nfdNormalizedString = Normalizer.normalize((new String(l)), Normalizer.Form.NFD); 
	    String deaccented = pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase();
		
	    // Select "all matches" Query
	    String selectQuery = "SELECT  * FROM " + dict.toString() + " WHERE aword=\"" +  deaccented +"\"";
	    Cursor cursor = getReadableDatabase().rawQuery(selectQuery, null);
	    
	    Set<String> matchingWords = new HashSet<String>();
	    
		// looping through all results and adding to list
		if (cursor.moveToFirst()) {
			do {
				matchingWords.add(cursor.getString(0));
			} while (cursor.moveToNext());
		}

		return matchingWords;
	}
	
	/** Returns true if the table with the given name exists in the database, false otherwise
	 * @param tableName
	 * @return
	 */
	public boolean tableExists(String tableName) {
	    return getReadableDatabase()
	    		.rawQuery("SELECT * FROM sqlite_master WHERE name ='" + tableName + "' and type='table' ", null)
	    		.moveToFirst();
	}

	public Set<DICTIONARY> getEnabledDictionaries() {
		return enabledDictionaries;
	}

	public void setEnabledDictionaries(Set<DICTIONARY> enabledDictionaries) {
		this.enabledDictionaries = new HashSet<DICTIONARY>();
		for(DICTIONARY dict: enabledDictionaries) {
			this.enabledDictionaries.add(dict);
		}
	}
}
