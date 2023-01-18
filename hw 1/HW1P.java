/*
 * This file illustrates HW1P.java from assignment HW1-P.
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This program first reads a dictionary of legal English words and stores all words in a HashSet.
 * After that, it will be given two words (strings), and it will change the first word one letter
 * at a time to the second word, provided that every result is a word in the dictionary.
 *
 * @author Michael Lee, ml3406@rit.edu
 */
public class HW1P {
    /** the HashSet to store all the words */
    Set<String> dict;
    /** a list of Strings to stores the order of word changes */
    List<String> order;
    /** if a path is found or not */
    boolean found;

    /**
     * The constructor initiates some important fields.
     */
    public HW1P() {
        dict = new HashSet<>();
        order = new ArrayList<>();
        found = false;
    }

    /**
     * This methods reads a dictionary of legal English words from a text file
     * and stores them in a HashSet.
     */
    public void loadDict() {
        try (BufferedReader input = new BufferedReader(new FileReader("exampleWords.txt"))) {
            String word;
            while ((word = input.readLine()) != null) {
                dict.add(word);
            }
        } catch (FileNotFoundException e1) {
            System.out.println("exampleWords.txt not found.");
        } catch (IOException e2) {
            System.out.println("Input error!");
        }
    }

    /**
     * This method calls the searching method and the result printing method.
     */
    public void begin() {
        order.clear();
        found = false;
        search("cold", "warm");
        print();
        order.clear();
        found = false;
        search("small", "short");
        print();
    }

    /**
     * This method executes the searching algorithm recursively. It scans the given two words from left to right.
     * Whenever it finds an unmatched letter, it changes the letter of the first word to the second one and sees
     * if the new word created exists in the dictionary. If yes, the method calls itself by passing the new word
     * and the second word to itself to find the next unmatched letter; If no, it keeps the unmatched letter and
     * looks for the next one. By doing this, it can find and record a path from the first word to the second one
     * by changing one letter at a time if there exists at least one.
     *
     * @param word1 the first given word to be changed to the second word
     * @param word2 the second given word
     */
    public void search(String word1, String word2) {
        if (word1.equals(word2)) found = true;
        // two words are identical -> don't scan
        if (!found) {
            // scan to find unmatched letter
            for (int i = 0; i < word1.length(); i++) {
                if (word1.charAt(i) != word2.charAt(i)) {
                    String modified = word1.substring(0, i) + word2.charAt(i) + word1.substring(i + 1);
                    // if the new word exists in the dictionary
                    if (dict.contains(modified)) {
                        search(modified, word2);
                        if (found) break;
                    }
                }
            }
        }
        // record the path found backwards
        if (found) order.add(word1);
    }

    /**
     * This method prints out the path from the first word to the second word.
     */
    public void print() {
        if (found) {
            System.out.print("A path was found: ");
            for (int i = order.size() - 1; i >= 0; i--) {
                System.out.print(order.get(i));
                if (i != 0) System.out.print(" -> ");
            }
            System.out.println();
        } else {
            System.out.println("No path was found.");
        }
    }

    /**
     * Main method.
     *
     * @param args command line arguments -- unused
     */
    public static void main(String[] args) {
        HW1P hw1p = new HW1P();
        hw1p.loadDict();
        hw1p.begin();
    }
}
