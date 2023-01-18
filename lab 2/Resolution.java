/*
 * This file illustrates Resolution.java from Lab2.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * This program first reads one or multiple files encoding knowledge
 * bases and then determines if the knowledge bases are satisfiable.
 *
 * @author Michael Lee, ml3406@rit.edu
 */
public class Resolution {
    /** a set to store all predicates used in the KB */
    public Set<String> predicates;

    /** a set to store all variables used in the KB */
    public Set<String> variables;

    /** a set to store all constants used in the KB */
    public Set<String> constants;

    /** a set to store all functions used in the KB */
    public Set<String> functions;

    /** a list to store all clauses and resolvent */
    public List<List<String>> clauses;

    /** a list to temporarily store all new resolvent (resolved from existing clauses) */
    public List<List<String>> clausesToAdd;

    /** an auxiliary map to store variables to be unified (key: variable; value: constant/function) */
    public Map<String, String> toUnify;

    /** if the provided KB is satisfiable */
    public boolean satisfiable;

    /**
     * The constructor initializes all fields.
     */
    public Resolution() {
        predicates = new HashSet<>();
        variables = new HashSet<>();
        constants = new HashSet<>();
        functions = new HashSet<>();
        clauses = new ArrayList<>();
        clausesToAdd = new ArrayList<>();
        toUnify = new HashMap<>();
        satisfiable = true;
    }

    /**
     * This method deals with input tasks, including reading all predicates,
     * variables, constants, functions, and all clauses in the KB, and storing
     * them into corresponding data structures.
     *
     * @param filename the input filename
     */
    public void input(String filename) {
        try (BufferedReader input = new BufferedReader(new FileReader(filename))) {
            String line;
            String[] splitLine;

            // time to read clauses or not
            boolean clause = false;
            while ((line = input.readLine()) != null) {
                splitLine = line.split("\\s+");
                if (!clause && splitLine.length > 1) {
                    switch (splitLine[0]) {
                        case "Predicates:" -> predicates.addAll(Arrays.asList(splitLine).subList(1, splitLine.length));
                        case "Variables:" -> variables.addAll(Arrays.asList(splitLine).subList(1, splitLine.length));
                        case "Constants:" -> constants.addAll(Arrays.asList(splitLine).subList(1, splitLine.length));
                        case "Functions:" -> functions.addAll(Arrays.asList(splitLine).subList(1, splitLine.length));
                    }
                }
                if (clause) {
                    /* a clause is composed of one or several literals =>
                       every clause is an arraylist of arraylists (literals) */
                    clauses.add(new ArrayList<>());
                    for (String s : splitLine) {
                        clauses.get(clauses.size() - 1).add(s);
                    }
                    // sort every clause's literals (for checking duplicated clauses)
                    clauses.get(clauses.size() - 1).sort(new SortLiteral());
                }
                if (!clause && splitLine[0].equals("Clauses:")) clause = true;

            }
        } catch (IOException e) {
            System.out.println("File not found or some IO errors.");
            System.exit(-1);
        }
    }

    /**
     * This method performs the main algorithm of resolution by pairing every two clauses
     * to see if they can be resolved and produce a new clause (resolvent). After pairing
     * every clause, it then checks if every newly produced clause is a duplicate of the
     * existing clause. If not, add it to the clauses lists and then keep on pairing the
     * new clauses with the existing clauses to see if there will be undiscovered clauses.
     */
    public void resolve() {
        // to keep on pairing or not
        boolean carryOn;

        // an auxiliary variable to prevent wasting time on pairing clauses that are already been paired
        int index = 0;
        do {
            clausesToAdd.clear();
            carryOn = false;

            // resolve every two clauses if possible (pairing)
            for (int i = 0; i < clauses.size() - 1; i++) {
                for (int j = index + 1; j < clauses.size(); j++) {
                    pairClauses(i, j);
                }
            }

            // remember where to start pairing
            index = clauses.size() - 1;
            if (!satisfiable) break;

            // if there are newly produced clauses
            if (clausesToAdd.size() > 0) {
                // check duplicates and keep on pairing if there are new clauses
                if (checkDuplicateAndAdd()) carryOn = true;
            }
        } while (carryOn);
    }


    /**
     * This method compares two clauses and calls its helper function
     * to check if two literals form different clauses are resolvable.
     * If two clauses are resolvable, add the newly produced clause to
     * an auxiliary list.
     *
     * @param index1 index of one clause
     * @param index2 index of the other clause
     */
    public void pairClauses(int index1, int index2) {
        // if two clauses both have only one literal, check if they can be resolved into an empty clause
        if (clauses.get(index1).size() == 1 && clauses.get(index2).size() == 1) {
            if (resolvable(clauses.get(index1).get(0), clauses.get(index2).get(0))) {
                satisfiable = false;
            }
        } else {
            // check every pair of literals from different clauses
            for (int i = 0; i < clauses.get(index1).size(); i++) {
                for (int j = 0; j < clauses.get(index2).size(); j++) {
                    toUnify.clear();
                    if (resolvable(clauses.get(index1).get(i), clauses.get(index2).get(j))) {
                        List<String> temp = new ArrayList<>();
                        for (int k = 0; k < clauses.get(index1).size(); k++) {
                            if (k != i) {
                                String sTemp = clauses.get(index1).get(k);
                                // swapping variables with corresponding constants/functions
                                for (String s : toUnify.keySet()) {
                                    sTemp = sTemp.replace(s, toUnify.get(s));
                                }
                                temp.add(sTemp);
                            }
                        }
                        for (int l = 0; l < clauses.get(index2).size(); l++) {

                            if (l != j) {
                                String sTemp = clauses.get(index2).get(l);
                                for (String s : toUnify.keySet()) {
                                    sTemp = sTemp.replace(s, toUnify.get(s));
                                }
                                temp.add(sTemp);
                            }
                        }
                        temp.sort(new SortLiteral());
                        clausesToAdd.add(temp);
                    }
                }
            }
        }
    }

    /**
     * This method checks if two literals are resolvable and also records the variables to be unified.
     *
     * @param s1 one literal from a clause
     * @param s2 the other literal from the other clause
     * @return if two literals are resolvable
     */
    public boolean resolvable(String s1, String s2) {
        // if two literals are resolvable, there must be one with negation sign and one without
        if ((s1.charAt(0) == '!' && s2.charAt(0) == '!') || (s1.charAt(0) != '!' && s2.charAt(0) != '!')) return false;

        // remove the negation sign for pairing their contents
        if (s1.charAt(0) == '!') s1 = s1.substring(1);
        else s2 = s2.substring(1);

        // remove parentheses and commas
        String[] a1 = s1.split("[(),]");
        String[] a2 = s2.split("[(),]");

        // indices of two literal string arrays (similar two sequence alignment)
        int index1 = 0, index2 = 0;
        do {
            if (a1[index1].equals(a2[index2])) {
                index1++;
                index2++;
            } else {
                // if there is an unmatched content, one or two must be a variable, or the literals are not resolvable
                if (!variables.contains(a1[index1]) && !variables.contains(a2[index2])) return false;

                // variables in both literals at current position (can be swapped to either one)
                if (variables.contains(a1[index1]) && variables.contains(a2[index2])) {
                    toUnify.put(a1[index1], a2[index2]);
                    index1++;
                    index2++;
                }
                // only one literal has a variable at current position
                else if (variables.contains(a1[index1])) {
                    // the other literal has a function
                    if (functions.contains(a2[index2])) {
                        toUnify.put(a1[index1], a2[index2] + "(" + a2[index2 + 1] + ")");
                        index1++;

                        /* This is the tricky part of dealing with functions: we have to deal with a function
                           according to the position of it.  e.g. loves(SKF0(x1),x2) VS person(SKF0(x1))
                           function in the middle VS function at the very end of a clause
                           If the function is in the middle, we have to deal with a null String.
                         */
                        index2 += 2;
                        // skip the null String
                        if (index2 < a2.length && a2[index2].equals("")) index2++;
                    }
                    // the other literal has a constant
                    else if (constants.contains(a2[index2])) {
                        toUnify.put(a1[index1], a2[index2]);
                        index1++;
                        index2++;
                    } else return false;
                } else {
                    if (functions.contains(a1[index1])) {
                        toUnify.put(a2[index2], a1[index1] + "(" + a1[index1 + 1] + ")");
                        index1 += 2;
                        if (index1 < a1.length && a1[index1].equals("")) index1++;
                        index2++;
                    } else if (constants.contains(a1[index1])) {
                        toUnify.put(a2[index2], a1[index1]);
                        index1++;
                        index2++;
                    } else return false;
                }
            }
        } while (index1 != a1.length && index2 != a2.length);

        // if there is no unmatched content after two literals are fully aligned
        return index1 == a1.length && index2 == a2.length;
    }

    /**
     * This method checks if every newly produced clause is a duplicate
     * of an existing one. If not, add it to the clauses.
     *
     * @return if there is at least one new clause that is not a duplicate of an existing one
     */
    public boolean checkDuplicateAndAdd() {
        boolean result = false;
        for (List<String> s1 : clausesToAdd) {
            // assume not a duplicate first
            boolean identical1 = false;

            // align with every existing clause
            for (List<String> s2 : clauses) {
                if (s1.size() == s2.size()) {
                    boolean identical2 = true;
                    // check every literal
                    for (int i = 0; i < s1.size(); i++) {
                        if (!s1.get(i).equals(s2.get(i))) {
                            identical2 = false;
                            break;
                        }
                    }
                    if (identical2) {
                        identical1 = true;
                        break;
                    }
                }
            }
            if (!identical1) {
                clauses.add(s1);
                result = true;
            }
        }
        return result;
    }

    /**
     * This method prints either yes or no to indicate if the provided KB is satisfiable.
     */
    public void output() {
        System.out.println(satisfiable ? "yes" : "no");
    }

    /**
     * Main method.
     *
     * @param args command line arguments -- input filename (one or multiple)
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: Resolution.java input-filename(one test case an argument)");
            System.exit(0);
        }

        for (String s : args) {
            Resolution lab2 = new Resolution();
            lab2.input(s);
            lab2.resolve();
            lab2.output();
        }
    }
}

/**
 * an auxiliary class used as comparator for the list of literal for every clause
 */
class SortLiteral implements Comparator<String> {
    /**
     * This method compares two String objects (literals) and decides their order.
     *
     * @param o1 one String (literal)
     * @param o2 the other String (literal)
     * @return their orders
     */
    @Override
    public int compare(String o1, String o2) {
        if (o1.charAt(0) == '!') o1 = o1.substring(1);
        if (o2.charAt(0) == '!') o2 = o2.substring(1);
        return o1.compareTo(o2);
    }
}