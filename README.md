#AnagramSolver

AnagramSolver is an open source Android app that finds the words that can be created with some given letters.
It supports English, Greek, German, Italian, Spanich, Polish, Turkish and French. By default only english is enabled.
You can enable the other languages from the settings button on top.
TIP: If you long click on a search result it will search for the words definition online. That's why it needs the Internet permission.

It works by normalizing both the letters it's searching and the words in the dictionary so that it can compare them fast in a database. Basically it uses an original dictionary where each line is a different word and a sorted version of that dictionary where each line is the same corresponding word but sorted alphabetically, lowercased and stripped from accents.

For example if you search an anagram for the letters "nsake" it will convert it to "aekns" and compare it to the sorted words in the English table. There it will find 2 instances of aekns. One for the word "snake" and one for the word "sneak" and it will return them.

This of course results in a bit bigger database (depending on the dictionaries size). Alternatively radix tries could be used to load each dictionary and its normalized version but when i tested some implementations of radix tries my device would eventually run out of memory for the app. So a database was a 'lighter' choice. I also tested a solution where i copied the databases on the first run instead of creating them from scratch but that took double the storage space in the device and the speed improvement on the first run wasn't that significant.

All the dictionaries used in the app were taken from publicly available OpenOffice spelling extensions. Hope that's ok. :)

##Screenshots

![Screenshot1](https://raw.github.com/uberspot/AnagramSolver/master/screenshot1.png)
![Screenshot2](https://raw.github.com/uberspot/AnagramSolver/master/screenshot2.png)

##License

    AnagramSolver is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

##Download

* (Play Store)[https://play.google.com/store/apps/details?id=com.as.anagramsolver]
* (F-Droid)[https://f-droid.org/repository/browse/?fdid=com.as.anagramsolver]
