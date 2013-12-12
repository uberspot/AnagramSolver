package sortDictionaries;

import java.io.*;
import java.text.*;
import java.util.regex.Pattern;

/**
 * @author uberspot
 */
public class sortDictionaries {

    private static final String[] langs = {"de_de", "el_gr", "en_us", "fr_fr", "pl_pl", "es_es", "it_it"};
    
    public static void main(String[] args) {
        for(String lang: langs)
            transformDictionary(lang);
    }
    
    public static void transformDictionary(String fileName) {
         try {
                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
                PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(fileName + "_sorted")));
                String line = in.readLine();
                Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
                while(line!=null) {
                        char[] l = line.toCharArray();
                        java.util.Arrays.sort(l);

                        String nfdNormalizedString = Normalizer.normalize((new String(l)), Normalizer.Form.NFD); 
			            String deaccented = pattern.matcher(nfdNormalizedString).replaceAll("").toLowerCase();

                        out.println(deaccented.hashCode());
                        line = in.readLine();
                }
                in.close();
                out.flush();
                out.close();
        } catch (UnsupportedEncodingException e) {
                        System.out.println("UnsupportedEncoding: " + e.getMessage());
        } catch (IOException e) {
                        System.out.println("IO Error: " + e.getMessage());
        } 
    }
}
