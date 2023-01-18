/*
 * This file illustrates DTAda.java from HW3.
 */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This program performs decision tree learning algorithm and prints out some all
 * answers to HW3.
 *
 * @author Michael Lee, ml3406@rit.edu
 */
public class DTAda {
    /**
     * number of attributes of an example
     */
    int n;
    /**
     * decision tree
     */
    MyTree dt;


    /**
     * The constructor initializes the tree.
     */
    public DTAda() {
        dt = new MyTree();
    }

    /**
     * This method reads all examples and stores them in a list.
     *
     * @param filename - the input filename
     */
    public List<List<Boolean>> input(String filename) {
        List<List<Boolean>> examples = new ArrayList<>();
        try (BufferedReader input = new BufferedReader(new FileReader(filename))) {
            String example;
            String[] split;
            while ((example = input.readLine()) != null) {
                split = example.split("\\s+");
                n = split.length - 1;
                examples.add(new ArrayList<>());
                for (int i = 0; i < n; i++) {
                    if (split[i].equals("True")) examples.get(examples.size() - 1).add(true);
                    else examples.get(examples.size() - 1).add(false);
                }

                // class A => true; class B => false
                if (split[n].equals("A")) examples.get(examples.size() - 1).add(true);
                else examples.get(examples.size() - 1).add(false);
            }
        } catch (IOException e) {
            System.out.println("File not found or input error!");
        }
        return examples;
    }

    /**
     * This method performs decision tree learning algorithm.
     *
     * @param examples - a list of examples
     * @param current  - current node in the decision tree to be handled
     */
    public void decisionTree(List<List<Boolean>> examples, Node current) {
        if (dt.size == 0) {
            dt.root = current;
            dt.size++;
        }

        // check if all examples are all the same class
        boolean same = true;
        boolean first = true;
        for (int i = 0; i < examples.size(); i++) {
            if (i == 0) first = examples.get(i).get(n);
            else {
                if (first != examples.get(i).get(n)) {
                    same = false;
                    break;
                }
            }
        }

        // if all the same, answer that value
        if (same) {
            if (first) current.answer = "A";
            else current.answer = "B";
        } else {
            // noise
            if (current.tested.size() == n) {
                if (majority(examples)) current.answer = "A";
                else current.answer = "B";
            } else {
                if (current.level == 1) {
                    if (majority(examples)) current.answer = "A";
                    else current.answer = "B";
                } else {
                    double remainder;
                    double minRemainder = calculateE(examples);
                    int minIndex = -1;
                    for (int i = 0; i < n; i++) {
                        if (!current.tested.contains(i)) {
                            remainder = calculateR(i, examples);
                            if (minRemainder > remainder) {
                                minRemainder = remainder;
                                minIndex = i;
                            }
                        }
                    }
                    // can't gain more information
                    if (minIndex == -1) {
                        if (majority(examples)) current.answer = "A";
                        else current.answer = "B";
                    } else {
                        List<List<Boolean>> sub1 = new ArrayList<>();
                        List<List<Boolean>> sub2 = new ArrayList<>();
                        for (List<Boolean> l : examples) {
                            if (l.get(minIndex)) sub1.add(new ArrayList<>(l));
                            else sub2.add(new ArrayList<>(l));
                        }
                        current.index = minIndex;
                        current.children.add(new Node(current.level + 1));
                        dt.size++;
                        current.children.add(new Node(current.level + 1));
                        dt.size++;
                        current.children.get(0).tested.add(minIndex);
                        current.children.get(1).tested.add(minIndex);
                        decisionTree(sub1, current.children.get(0));
                        decisionTree(sub2, current.children.get(1));
                    }
                }
            }
        }
    }

    /**
     * This method calculates and returns the majority of the provided examples.
     *
     * @param examples - the provided examples
     * @return the majority
     */
    public boolean majority(List<List<Boolean>> examples) {
        int a = 0, b = 0;
        for (List<Boolean> l : examples) {
            if (l.get(n)) a++;
            else b++;
        }
        return a >= b;
    }

    /**
     * This method calculates and returns the total entropy of a given examples.
     *
     * @param examples - a given list of examples
     * @return the total entropy
     */
    public double calculateE(List<List<Boolean>> examples) {
        double positive = 0, negative = 0;
        for (List<Boolean> l : examples) {
            if (l.get(n)) positive++;
            else negative++;
        }
        if (positive == 0 || negative == 0) return 0.0;
        // probabilities
        double p1 = positive / (positive + negative);
        double p2 = negative / (positive + negative);
        return -(p1 * Math.log(p1) / Math.log(2) + p2 * Math.log(p2) / Math.log(2));
    }

    /**
     * This method calculates remainder for finding the most informative attribute.
     *
     * @param index    - index of the attribute
     * @param examples - a list of examples
     * @return the remainder to test this attribute
     */
    public double calculateR(int index, List<List<Boolean>> examples) {
        List<List<Boolean>> sub1 = new ArrayList<>();
        List<List<Boolean>> sub2 = new ArrayList<>();
        for (List<Boolean> l : examples) {
            if (l.get(index)) sub1.add(new ArrayList<>(l));
            else sub2.add(new ArrayList<>(l));
        }
        return (calculateE(sub1) * sub1.size() + calculateE(sub2) * sub2.size()) / examples.size();
    }


    /**
     * This method prints out the answers from hw3.
     *
     * @param examples - a list of all examples
     */
    public void output(List<List<Boolean>> examples) {
        // print the decision tree
        System.out.println(dt.root.index);
        System.out.print(dt.root.children.get(0).answer + " ");
        System.out.println(dt.root.children.get(1).answer);


        // answer question 2
        int test = dt.root.index;

        // A => true; B => false
        boolean class1 = true;
        boolean class2 = false;
        double correct = 0.0, wrong = 0.0;
        double errorRate;
        for (List<Boolean> l : examples) {
            if (l.get(test)) {
                if (l.get(n) == class1) correct++;
                else wrong++;
            } else {
                if (l.get(n) == class2) correct++;
                else wrong++;
            }
        }
        // 2-a.
        errorRate = wrong / examples.size();
        System.out.println("error rate = " + errorRate);

        // 2-b.
        double w1 = Math.log((1.0 - errorRate) / errorRate) / Math.log(2);
        System.out.println("hypothesis weight #1 = " + w1);

        // 2-c.
        double exWeight = 1.0 / examples.size();
        System.out.println("initial weights of each example: " + exWeight);

        // 2-d.
        double exWeightCorrect = exWeight * errorRate / (1.0 - errorRate);
        // normalize
        double temp1 = exWeightCorrect / (exWeight * wrong + exWeightCorrect * correct);
        double temp2 = exWeight / (exWeight * wrong + exWeightCorrect * correct);
        System.out.println("new weights of each example that was correctly classified: " + temp1);
        System.out.println("new weights of each example that was incorrectly classified: " + temp2);


    }

    /**
     * main method
     *
     * @param args - command line arguments (input filename)
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: DTAda.java input_filename");
            System.exit(0);
        }
        DTAda hw3 = new DTAda();
        List<List<Boolean>> examples = hw3.input(args[0]);
        hw3.decisionTree(examples, new Node(0));
        hw3.output(examples);
    }
}

/**
 * an auxiliary class representing nodes in a decision tree
 */
class Node {
    /**
     * the index of testing attribute
     */
    public int index;
    /**
     * a list of children
     */
    public List<Node> children;
    /**
     * a set of indices of the tested attributes along the path to the root
     */
    public Set<Integer> tested;
    /**
     * answer of this node if this is a leaf node
     */
    public String answer;

    /**
     * the level this node at in the decision tree
     */
    public int level;

    /**
     * The constructor initializes all fields.
     */
    public Node(int level) {
        index = -1;
        children = new ArrayList<>();
        tested = new HashSet<>();
        answer = "";
        this.level = level;
    }
}

/**
 * an auxiliary class representing a decision tree
 */
class MyTree {
    /**
     * root node of this tree
     */
    Node root;
    /**
     * size of this tree
     */
    int size;

    /**
     * The constructor initializes the size of this tree.
     */
    public MyTree() {
        size = 0;
    }
}