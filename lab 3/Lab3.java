/*
 * This file illustrates Lab3.java from lab3.
 */

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This program is able to perform decision tree learning algorithm and AdaBoosted decision
 * tree learning algorithm for training models that determines if a given sentence (15 words)
 * is in English or Dutch. After training, it exports the model as a file (serialized object).
 * It can also read in trained model and perform predictions according to the trained model.
 *
 * @author Michael Lee, ml3406@rit.edu
 */
public class Lab3 implements Serializable {
    /**
     * serialVersionUID is used to determine if a deserialized object has been
     * serialized with the same version of the class
     */
    @Serial
    private static final long serialVersionUID = 630L;

    /**
     * a list of examples
     * for training: the training set
     * for predicting: the testing set
     */
    public transient List<List<String>> examples;

    /**
     * a list of weights for all examples
     */
    public transient List<Double> weightsEx;

    /**
     * a list of weights for all hypotheses
     */
    public List<Double> weightsH;

    /**
     * a list of hypotheses
     */
    public List<MyTree> hypotheses;

    /**
     * depth limit of a decision tree
     */
    public int maxDepth;

    /**
     * The constructor initializes all data structures.
     */
    public Lab3() {
        examples = new ArrayList<>();
        weightsEx = new ArrayList<>();
        weightsH = new ArrayList<>();
        hypotheses = new ArrayList<>();
        maxDepth = 9;
    }

    /**
     * This method reads in the training set while training
     * a model or reads in testing set while predicting.
     *
     * @param task     - train/predict
     * @param filename - the input filename
     */
    void input(String task, String filename) {
        // train
        if (task.equalsIgnoreCase("train")) {
            try (BufferedReader input = new BufferedReader(new FileReader(filename))) {
                String example;
                String[] split;
                while ((example = input.readLine()) != null) {
                    // split every sentence with | and white space
                    split = example.split("[| ]");
                    examples.add(new ArrayList<>());
                    for (int i = 0; i < 16; i++) {
                        examples.get(examples.size() - 1).add(split[i]);
                    }
                }
            } catch (IOException e) {
                System.out.println("File not found or input error!");
                System.exit(-1);
            }
        }

        // predict
        else {
            examples = new ArrayList<>();
            try (BufferedReader input = new BufferedReader(new FileReader(filename))) {
                String example;
                String[] split;
                while ((example = input.readLine()) != null) {
                    // add "?|" at the beginning of every sentence for convenience
                    example = "?|" + example;
                    split = example.split("[| ]");
                    examples.add(new ArrayList<>());
                    for (int i = 0; i < 16; i++) {
                        examples.get(examples.size() - 1).add(split[i]);
                    }
                }
            } catch (IOException e) {
                System.out.println("File not found or input error!");
                System.exit(-1);
            }

        }
    }

    /**
     * This method implements AdaBoost algorithm.
     *
     * @param k - the number of hypotheses
     */
    public void adaBoost(int k) {
        // initialize example weights (1/N)
        for (int i = 0; i < examples.size(); i++) {
            weightsEx.add(1.0 / examples.size());
        }

        double error;
        // generate k weighted hypotheses
        for (int i = 0; i < k; i++) {
            hypotheses.add(new MyTree(new Node()));
            decisionTree(examples, weightsEx, hypotheses.get(i).root);

            // reset error
            error = 0.0;

            // if prediction is not correct, accumulate the error
            for (int j = 0; j < examples.size(); j++) {
                if (!examples.get(j).get(0).equals(predict(hypotheses.get(hypotheses.size() - 1).root, examples.get(j)))) {
                    error = error + weightsEx.get(j);
                }
            }

            // print out training error of every hypothesis
            System.out.println("Error rate #" + (i + 1) + ": " + error);

            // if prediction is correct (change the weight of the example)
            for (int j = 0; j < examples.size(); j++) {
                if (examples.get(j).get(0).equals(predict(hypotheses.get(hypotheses.size() - 1).root, examples.get(j)))) {
                    if (error != 1.0) weightsEx.set(j, weightsEx.get(j) * (error / (1.0 - error)));
                }
            }

            // normalize examples' weights
            normalize();

            // calculate the weight of the hypothesis
            if (error != 0.0) weightsH.add(Math.log((1.0 - error) / error));
            else weightsH.add(100.0);
        }
    }

    /**
     * This method performs decision tree learning algorithm.
     *
     * @param ex      - a list of examples
     * @param weights - a list of weights for the examples
     * @param current - current node in the tree
     */
    public void decisionTree(List<List<String>> ex, List<Double> weights, Node current) {
        if (current.depth == maxDepth) current.answer = majority(ex, weights);
        else {
            // the less the remainder the more the information gain
            double remainder;
            double minRemainder = calculateE(ex, weights);

            // all examples are all the same classification (leaf node)
            if (minRemainder == 0.0) current.answer = majority(ex, weights);

            // the index of the feature which gains more information than any other feature does
            int minIndex = -1;
            for (int i = 0; i < 9; i++) {
                if (!current.tested.contains(i)) {
                    remainder = calculateR(i, ex, weights);
                    if (minRemainder > remainder) {
                        minRemainder = remainder;
                        minIndex = i;
                    }
                }
            }

            // all features are already tested or can't gain more information (leaf node)
            if (minIndex == -1) {
                current.answer = majority(ex, weights);
            }

            // keep on developing the tree
            else {
                // binary attribute => binary tree (2 children)
                List<List<String>> sub1 = new ArrayList<>();
                List<List<String>> sub2 = new ArrayList<>();
                List<Double> w1 = new ArrayList<>();
                List<Double> w2 = new ArrayList<>();
                boolean decision = true;
                for (int i = 0; i < ex.size(); i++) {
                    switch (minIndex) {
                        case 0 -> decision = feature1(ex.get(i));
                        case 1 -> decision = feature2(ex.get(i));
                        case 2 -> decision = feature3(ex.get(i));
                        case 3 -> decision = feature4(ex.get(i));
                        case 4 -> decision = feature5(ex.get(i));
                        case 5 -> decision = feature6(ex.get(i));
                        case 6 -> decision = feature7(ex.get(i));
                        case 7 -> decision = feature8(ex.get(i));
                        case 8 -> decision = feature9(ex.get(i));
                    }
                    if (decision) {
                        sub1.add(new ArrayList<>(ex.get(i)));
                        w1.add(weights.get(i));
                    } else {
                        sub2.add(new ArrayList<>(ex.get(i)));
                        w2.add(weights.get(i));
                    }
                }
                current.index = minIndex;
                current.children.add(new Node());
                current.children.add(new Node());

                // record what features are tested along the path to the root
                current.children.get(0).tested.addAll(current.tested);
                current.children.get(1).tested.addAll(current.tested);
                current.children.get(0).tested.add(minIndex);
                current.children.get(1).tested.add(minIndex);

                // depth correction
                current.children.get(0).depth = current.depth + 1;
                current.children.get(1).depth = current.depth + 1;
                decisionTree(sub1, w1, current.children.get(0));
                decisionTree(sub2, w2, current.children.get(1));
            }
        }
    }

    /**
     * This method calculates and returns the majority of the provided weighted examples.
     *
     * @param ex      - the provided examples
     * @param weights - the weights of the examples
     * @return the majority
     */
    public String majority(List<List<String>> ex, List<Double> weights) {
        double sum = 0.0;
        for (int i = 0; i < ex.size(); i++) {
            if (ex.get(i).get(0).equals("en")) sum += weights.get(i);
            else sum -= weights.get(i);
        }
        if (sum >= 0) return "en";
        else return "nl";
    }

    /**
     * This method calculates and returns the total entropy of a given list of examples.
     *
     * @param ex      - a given list of examples
     * @param weights - weights of the given examples
     * @return the total entropy
     */
    public double calculateE(List<List<String>> ex, List<Double> weights) {
        double en = 0, nl = 0;
        for (int i = 0; i < ex.size(); i++) {
            if (ex.get(i).get(0).equals("en")) en += weights.get(i);
            else nl += weights.get(i);
        }
        if (en == 0 || nl == 0) return 0.0;

        // probabilities
        double p1 = en / (en + nl);
        double p2 = nl / (en + nl);
        return -(p1 * Math.log(p1) / Math.log(2) + p2 * Math.log(p2) / Math.log(2));
    }

    /**
     * This method calculates the remainder for finding the most informative feature.
     *
     * @param index   - index of the feature
     * @param ex      - a list of examples
     * @param weights - weights of the given examples
     * @return the remainder of testing this feature
     */
    public double calculateR(int index, List<List<String>> ex, List<Double> weights) {
        List<List<String>> sub1 = new ArrayList<>();
        List<List<String>> sub2 = new ArrayList<>();
        List<Double> w1 = new ArrayList<>();
        List<Double> w2 = new ArrayList<>();
        boolean decision = true;
        for (int i = 0; i < ex.size(); i++) {
            switch (index) {
                case 0 -> decision = feature1(ex.get(i));
                case 1 -> decision = feature2(ex.get(i));
                case 2 -> decision = feature3(ex.get(i));
                case 3 -> decision = feature4(ex.get(i));
                case 4 -> decision = feature5(ex.get(i));
                case 5 -> decision = feature6(ex.get(i));
                case 6 -> decision = feature7(ex.get(i));
                case 7 -> decision = feature8(ex.get(i));
                case 8 -> decision = feature9(ex.get(i));
            }
            if (decision) {
                sub1.add(new ArrayList<>(ex.get(i)));
                w1.add(weights.get(i));
            } else {
                sub2.add(new ArrayList<>(ex.get(i)));
                w2.add(weights.get(i));
            }
        }
        double wSum1 = 0.0, wSum2 = 0.0;
        for (Double d : w1) {
            wSum1 += d;
        }
        for (Double d : w2) {
            wSum2 += d;
        }
        return (calculateE(sub1, w1) * wSum1 + calculateE(sub2, w2) * wSum2) / (wSum1 + wSum2);
    }

    /**
     * This method predicts the result of an example according to the given hypothesis.
     *
     * @param current - current node in the tree (hypothesis)
     * @param example - the example
     * @return the prediction
     */
    public String predict(Node current, List<String> example) {
        switch (current.index) {
            case 0:
                if (feature1(example)) return predict(current.children.get(0), example);
                else return predict(current.children.get(1), example);
            case 1:
                if (feature2(example)) return predict(current.children.get(0), example);
                else return predict(current.children.get(1), example);
            case 2:
                if (feature3(example)) return predict(current.children.get(0), example);
                else return predict(current.children.get(1), example);
            case 3:
                if (feature4(example)) return predict(current.children.get(0), example);
                else return predict(current.children.get(1), example);
            case 4:
                if (feature5(example)) return predict(current.children.get(0), example);
                else return predict(current.children.get(1), example);
            case 5:
                if (feature6(example)) return predict(current.children.get(0), example);
                else return predict(current.children.get(1), example);
            case 6:
                if (feature7(example)) return predict(current.children.get(0), example);
                else return predict(current.children.get(1), example);
            case 7:
                if (feature8(example)) return predict(current.children.get(0), example);
                else return predict(current.children.get(1), example);
            case 8:
                if (feature9(example)) return predict(current.children.get(0), example);
                else return predict(current.children.get(1), example);
            default:
                return current.answer;
        }
    }

    /**
     * This method normalize the weights of all examples.
     */
    public void normalize() {
        double sum = 0.0;
        for (double d : weightsEx) {
            sum += d;
        }
        if (sum != 0.0) {
            for (int i = 0; i < weightsEx.size(); i++) {
                weightsEx.set(i, weightsEx.get(i) / sum);
            }
        }
    }

    /**
     * This method tests if the given sentence (example) has the word "de".
     *
     * @param example - the sentence
     * @return if the sentence has the word "de"
     */
    public boolean feature1(List<String> example) {
        for (int i = 1; i < example.size(); i++) {
            if (example.get(i).equalsIgnoreCase("de")) return true;
        }
        return false;
    }

    /**
     * This method tests if the given sentence (example) has the word "van".
     *
     * @param example - the sentence
     * @return if the sentence has the word "van"
     */
    public boolean feature2(List<String> example) {
        for (int i = 1; i < example.size(); i++) {
            if (example.get(i).equalsIgnoreCase("van")) return true;
        }
        return false;
    }

    /**
     * This method tests if the given sentence (example) has at least two words longer than 10 characters.
     *
     * @param example - the sentence
     * @return if the sentence has at least two words longer than 10 characters
     */
    public boolean feature3(List<String> example) {
        int counter = 0;
        for (int i = 1; i < example.size(); i++) {
            if (example.get(i).length() > 10) {
                counter++;
                if (counter == 2) return true;
            }
        }
        return false;
    }

    /**
     * This method tests if the sum of ASCII values of all characters
     * in the sentence (example) is greater than or equal to 8000
     *
     * @param example - the sentence
     * @return if the sum of ASCII values of all characters in the sentence >= 8000
     */
    public boolean feature4(List<String> example) {
        int sum = 0;
        for (int i = 1; i < example.size(); i++) {
            for (int j = 0; j < example.get(i).length(); j++) {
                sum += example.get(i).charAt(j);
            }
        }
        return sum >= 8000;
    }

    /**
     * This method tests if the given sentence (example) has at least 1 word starts with 'z'.
     *
     * @param example - the sentence
     * @return if the sentence has at least 1 word starts with 'z'
     */
    public boolean feature5(List<String> example) {
        for (int i = 1; i < example.size(); i++) {
            if (example.get(i).charAt(0) == 'z' || example.get(i).charAt(0) == 'Z') return true;
        }
        return false;
    }

    /**
     * This method tests if the average ASCII value of all words' first character
     * of the given sentence (example) is greater than or equal to 100.
     *
     * @param example - the sentence
     * @return if the average ASCII value of all words' first character >= 100
     */
    public boolean feature6(List<String> example) {
        double sum = 0.0;
        for (int i = 1; i < example.size(); i++) {
            sum += example.get(i).charAt(0);
        }
        return sum / (example.size() - 1) >= 100;
    }

    /**
     * This method tests if the given sentence (example) has the word "are".
     *
     * @param example - the sentence
     * @return if the sentence has the word "are"
     */
    public boolean feature7(List<String> example) {
        for (int i = 1; i < example.size(); i++) {
            if (example.get(i).equalsIgnoreCase("are")) return true;
        }
        return false;
    }

    /**
     * This method tests if the given sentence (example) contains "zi".
     *
     * @param example - the sentence
     * @return if the sentence contains "zi"
     */
    public boolean feature8(List<String> example) {
        for (int i = 1; i < example.size(); i++) {
            if (example.get(i).contains("zi") || example.get(i).contains("Zi")) return true;
        }
        return false;
    }

    /**
     * This method tests if the average word length of the given sentence (example) is greater than 7.
     *
     * @param example - the sentence
     * @return if the average word length > 7
     */
    public boolean feature9(List<String> example) {
        double sum = 0.0;
        for (int i = 1; i < example.size(); i++) {
            sum += example.get(i).length();
        }
        return sum / (example.size() - 1) > 7;
    }

    /**
     * This method outputs the trained model while performing training or
     * prints out the predictions of all given sentences while predicting.
     *
     * @param task - train (with output filename)/predict
     */
    public void output(String[] task) {
        if (task[0].equalsIgnoreCase("train")) {
            try (ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(task[1]))) {
                output.writeObject(this);
            } catch (IOException e) {
                System.out.println("IO errors.");
                System.exit(-1);
            }
        } else {
            double result;
            for (List<String> l : examples) {
                result = 0.0;
                for (int i = 0; i < hypotheses.size(); i++) {
                    if (predict(hypotheses.get(i).root, l).equals("en")) result += weightsH.get(i);
                    else result -= weightsH.get(i);
                }
                if (result >= 0.0) System.out.println("en");
                else System.out.println("nl");
            }
        }
    }

    /**
     * main method
     *
     * @param args - command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 4 && args.length != 3) {
            System.out.println("Usage 1: Lab3.java train training_set_filename trained_model_filename dt/ada");
            System.out.println("Usage 2: Lab3.java predict trained_model testing_set_filename");
            System.exit(-1);
        }

        Lab3 lab3 = null;

        // train model
        if (args[0].equals("train")) {
            lab3 = new Lab3();
            lab3.input(args[0], args[1]);

            // start training
            // decision tree only (adaboost with K = 1)
            if (args[3].equals("dt")) {
                lab3.adaBoost(1);
            }
            // ada boost applied
            else {
                lab3.adaBoost(20);
            }

            // output the trained model
            lab3.output(new String[]{args[0], args[2]});
        }

        // predict
        else {
            // read trained model
            try (ObjectInputStream input = new ObjectInputStream(new FileInputStream(args[1]))) {
                lab3 = (Lab3) input.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Class not found or some IO errors.");
                System.exit(-1);
            }

            // read testing set
            lab3.input(args[0], args[2]);

            // print predictions
            lab3.output(new String[]{args[0]});
        }
    }
}

/**
 * an auxiliary class representing nodes in a decision tree
 */
class Node implements Serializable {
    /**
     * the index of feature (testing attribute)
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
     * depth of this node in a tree
     */
    public int depth;

    /**
     * The constructor initializes all fields.
     */
    public Node() {
        index = -1;
        children = new ArrayList<>();
        tested = new HashSet<>();
        answer = "";
        depth = 0;
    }
}

/**
 * an auxiliary class representing a decision tree
 */
class MyTree implements Serializable {
    /**
     * root node of this tree
     */
    Node root;

    /**
     * The constructor initializes the size of this tree.
     */
    public MyTree(Node root) {
        this.root = root;
    }
}