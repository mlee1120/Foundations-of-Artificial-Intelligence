/*
 * This file illustrates lab1.java from Lab1.
 */

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * This program first reads a terrain image, path points text file, and elevations text file.
 * After that, it applies A* search to find the optimal path among every two provided points
 * according to the provided information. Finally, it outputs a new terrain image with the
 * optimal path drawn on it and also a text file showing the total distance of the optimal path.
 *
 * @author Michael Lee, ml3406@rit.edu
 */
public class lab1 {
    /**
     * width and height in pixel
     */
    int x, y;

    /**
     * real-world pixel size
     */
    double dx, dy;

    /**
     * input image
     */
    BufferedImage img;

    /**
     * a list to store all points to visit
     */
    List<int[]> points;

    /**
     * a 2D list to store elevations info
     */
    List<List<Double>> elevations;

    /**
     * a 2D list to store all colors of the input image (convenient to calculate RGB)
     */
    List<List<Color>> colors;

    /**
     * a list to store the optimal path (pixel by pixel)
     */
    List<String> path;

    /**
     * a 2D list to store all pixels' heuristic values
     */
    List<List<Double>> h;

    /**
     * a map to store pixels' g(n)
     */
    Map<String, Double> g;

    /**
     * a map to store all types of terrain speed
     */
    Map<String, Double> terrainSpeed;

    /**
     * a map to store pixels' predecessors (for backtracking the optimal path)
     */
    Map<String, String> predecessors;

    /**
     * a set to store visited pixel during A* search
     */
    Set<String> visited;

    /**
     * a priority queue used during A* search
     */
    PriorityQueue<Object[]> pq;

    /**
     * The constructor initializes some important fields.
     *
     * @param terrain_img the file name of the input image
     */
    public lab1(String terrain_img) {
        // hard-coded width and height
        x = 395;
        y = 500;

        // hard-coded real-world pixel size
        dx = 10.29;
        dy = 7.75;

        elevations = new ArrayList<>();
        points = new ArrayList<>();
        colors = new ArrayList<>();
        path = new ArrayList<>();
        h = new ArrayList<>();
        g = new HashMap<>();
        predecessors = new HashMap<>();
        visited = new HashSet<>();
        pq = new PriorityQueue<>(1, new SortByHeuristic());

        // read image
        try {
            img = ImageIO.read(new File(terrain_img));
        } catch (IOException e) {
            System.out.println("File not found or some other errors.");
        }

        for (int i = 0; i < y; i++) {
            elevations.add(new ArrayList<>());
            colors.add(new ArrayList<>());
            h.add(new ArrayList<>());
            for (int j = 0; j < x; j++) {
                colors.get(i).add(new Color(img.getRGB(j, i)));
                h.get(i).add(10000000.0);
            }
        }

        terrainSpeed = new HashMap<>();
        // hard-coded speeds (m/s) for every terrain
        terrainSpeed.put("248 148 18", 3.5);
        terrainSpeed.put("255 192 0", 2.7);
        terrainSpeed.put("255 255 255", 2.4);
        terrainSpeed.put("2 208 60", 2.0);
        terrainSpeed.put("2 136 40", 1.5);
        terrainSpeed.put("5 73 24", 0.0);
        terrainSpeed.put("0 0 255", 0.0);
        terrainSpeed.put("71 51 3", 3.5);
        terrainSpeed.put("0 0 0", 3.0);
        terrainSpeed.put("205 0 101", 0.0);
    }

    /**
     * This method deals with input tasks, including reading elevation and path information.
     *
     * @param elevation_file a text file with elevation information
     * @param path_file      a text file with path (points) information
     */
    public void input(String elevation_file, String path_file) {
        // read and store elevations
        try (BufferedReader input = new BufferedReader(new FileReader(elevation_file))) {
            String line;
            String[] splitLine;
            int lines = 0;
            while ((line = input.readLine()) != null) {
                line = line.trim();
                splitLine = line.split("\\s+");
                for (int i = 0; i < x; i++) {
                    elevations.get(lines).add(Double.parseDouble(splitLine[i]));
                }
                lines++;
            }
        } catch (IOException e) {
            System.out.println("File not found or some other errors.");
        }

        // read and store path (sequence of points)
        try (BufferedReader input = new BufferedReader(new FileReader(path_file))) {
            String line;
            String[] splitLine;
            while ((line = input.readLine()) != null) {
                splitLine = line.split(" ");
                points.add(new int[]{Integer.parseInt(splitLine[0]), Integer.parseInt(splitLine[1])});
            }
        } catch (IOException e) {
            System.out.println("File not found or some other errors.");
        }
    }

    /**
     * This method executes A* search algorithm.
     */
    public void aStar() {
        int xCurrent, yCurrent, xVisit, yVisit;
        // perform A* search for every two points from path file
        for (int i = 0; i < points.size() - 1; i++) {
            xCurrent = points.get(i)[0];
            yCurrent = points.get(i)[1];
            xVisit = points.get(i + 1)[0];
            yVisit = points.get(i + 1)[1];

            // reset all heuristic values
            for (int j = 0; j < x; j++) {
                for (int k = 0; k < y; k++) {
                    h.get(k).set(j, 10000000.0);
                }
            }

            // h(n) of the visiting point is 0
            h.get(yVisit).set(xVisit, 0.0);

            // calculate heuristic values for every point (pixel)
            heuristic();

            // add starting point with it's f(n) value to the priority queue
            pq.add(new Object[]{xCurrent, yCurrent, h.get(yCurrent).get(xCurrent)});

            // g(n) of the starting point is 0
            g.put(xCurrent + " " + yCurrent, 0.0);

            /* although the starting point has no predecessor, adding
               itself as its predecessor is convenient for back tracking */
            predecessors.put(xCurrent + " " + yCurrent, xCurrent + " " + yCurrent);

            // some auxiliary variables for A* search
            Object[] temp;
            int xTemp, yTemp;
            double gParent, g1, g2, f;

            // keep searching until reaching the visiting point
            while (pq.size() != 0) {
                temp = pq.poll();
                xTemp = (int) temp[0];
                yTemp = (int) temp[1];
                visited.add(xTemp + " " + yTemp);

                // search east adjacent pixel
                if (xTemp + 1 < x && !visited.contains((xTemp + 1) + " " + yTemp)) {
                    gParent = g.get(xTemp + " " + yTemp);
                    g1 = gParent + cost(xTemp, yTemp, xTemp + 1, yTemp);
                    f = g1 + h.get(yTemp).get(xTemp + 1);

                    // already in the priority queue, but might be updated
                    if (predecessors.containsKey((xTemp + 1) + " " + yTemp)) {
                        g2 = g.get((xTemp + 1) + " " + yTemp);
                        if (g2 > g1) {
                            predecessors.put((xTemp + 1) + " " + yTemp, xTemp + " " + yTemp);
                            g.put((xTemp + 1) + " " + yTemp, g1);
                            pq.add(new Object[]{xTemp + 1, yTemp, f});
                        }
                    } else {
                        predecessors.put((xTemp + 1) + " " + yTemp, xTemp + " " + yTemp);
                        g.put((xTemp + 1) + " " + yTemp, g1);
                        pq.add(new Object[]{xTemp + 1, yTemp, f});
                    }
                }

                // search west adjacent pixel
                if (xTemp - 1 >= 0 && !visited.contains((xTemp - 1) + " " + yTemp)) {
                    gParent = g.get(xTemp + " " + yTemp);
                    g1 = gParent + cost(xTemp, yTemp, xTemp - 1, yTemp);
                    f = g1 + h.get(yTemp).get(xTemp - 1);
                    if (predecessors.containsKey((xTemp - 1) + " " + yTemp)) {
                        g2 = g.get((xTemp - 1) + " " + yTemp);
                        if (g2 > g1) {
                            predecessors.put((xTemp - 1) + " " + yTemp, xTemp + " " + yTemp);
                            g.put((xTemp - 1) + " " + yTemp, g1);
                            pq.add(new Object[]{xTemp - 1, yTemp, f});
                        }
                    } else {
                        predecessors.put((xTemp - 1) + " " + yTemp, xTemp + " " + yTemp);
                        g.put((xTemp - 1) + " " + yTemp, g1);
                        pq.add(new Object[]{xTemp - 1, yTemp, f});
                    }

                }

                // search south adjacent pixel
                if (yTemp + 1 < y && !visited.contains(xTemp + " " + (yTemp + 1))) {
                    gParent = g.get(xTemp + " " + yTemp);
                    g1 = gParent + cost(xTemp, yTemp, xTemp, yTemp + 1);
                    f = g1 + h.get(yTemp + 1).get(xTemp);
                    if (predecessors.containsKey(xTemp + " " + (yTemp + 1))) {
                        g2 = g.get(xTemp + " " + (yTemp + 1));
                        if (g2 > g1) {
                            predecessors.put(xTemp + " " + (yTemp + 1), xTemp + " " + yTemp);
                            g.put(xTemp + " " + (yTemp + 1), g1);
                            pq.add(new Object[]{xTemp, yTemp + 1, f});
                        }
                    } else {
                        predecessors.put(xTemp + " " + (yTemp + 1), xTemp + " " + yTemp);
                        g.put(xTemp + " " + (yTemp + 1), g1);
                        pq.add(new Object[]{xTemp, yTemp + 1, f});
                    }
                }

                // search north adjacent pixel
                if (yTemp - 1 >= 0 && !visited.contains(xTemp + " " + (yTemp - 1))) {
                    gParent = g.get(xTemp + " " + yTemp);
                    g1 = gParent + cost(xTemp, yTemp, xTemp, yTemp - 1);
                    f = g1 + h.get(yTemp - 1).get(xTemp);
                    if (predecessors.containsKey(xTemp + " " + (yTemp - 1))) {
                        g2 = g.get(xTemp + " " + (yTemp - 1));
                        if (g2 > g1) {
                            predecessors.put(xTemp + " " + (yTemp - 1), xTemp + " " + yTemp);
                            g.put(xTemp + " " + (yTemp - 1), g1);
                            pq.add(new Object[]{xTemp, yTemp - 1, f});
                        }
                    } else {
                        predecessors.put(xTemp + " " + (yTemp - 1), xTemp + " " + yTemp);
                        g.put(xTemp + " " + (yTemp - 1), g1);
                        pq.add(new Object[]{xTemp, yTemp - 1, f});
                    }
                }

                // reach the visiting point
                assert pq.peek() != null;
                if ((int) pq.peek()[0] == xVisit && (int) pq.peek()[1] == yVisit) {
                    predecessors.put(pq.peek()[0] + " " + pq.peek()[1], predecessors.get(xVisit + " " + yVisit));
                    visited.clear();
                    pq.clear();
                    g.clear();
                    break;
                }
            }
            // back track and record the optimal path
            String aString = xVisit + " " + yVisit;
            do {
                path.add(aString);
                aString = predecessors.get(aString);
            } while (!aString.equals(xCurrent + " " + yCurrent));
            path.add(aString);
            predecessors.clear();
        }
    }

    /**
     * This method calculates the time cost while traveling from one pixel to one of its adjacent
     * pixels according to terrains and elevations
     *
     * @param xStart x-coordinate the starting pixel
     * @param yStart y-coordinate the starting pixel
     * @param xVisit x-coordinate the visiting pixel
     * @param yVisit y-coordinate the visiting pixel
     * @return the time cost
     */
    public double cost(int xStart, int yStart, int xVisit, int yVisit) {
        // time cost and up/downhill factor
        double t, hill;
        double altitude_difference = elevations.get(yVisit).get(xVisit) - elevations.get(yStart).get(xStart);

        // determine terrain speed according to r, g, and b values
        int red = colors.get(yStart).get(xStart).getRed();
        int green = colors.get(yStart).get(xStart).getGreen();
        int blue = colors.get(yStart).get(xStart).getBlue();
        double speed = terrainSpeed.get(red + " " + green + " " + blue);

        // impassible terrain or too high (too cold) for human (given them a high cost so that A* won't pick them)
        if (speed == 0.0 || elevations.get(yVisit).get(xVisit) >= 3000.0) t = 1000000.0;
        else if (xStart == xVisit && yStart == yVisit) t = 0.0;
        else {
            if (xStart == xVisit) {

                // 30-degree or more downhill => 20% speed up; 30-degree or more uphill => 20% speed loss
                if (altitude_difference >= dy / Math.sqrt(3)) hill = -0.2;
                else if (altitude_difference <= -dy / Math.sqrt(3)) hill = 0.2;
                else hill = 0.0;
                if (altitude_difference >= dy / Math.sqrt(2)) t = 1000000.0;
                else t = dy / (speed * (1.0 + hill));
            } else {
                if (altitude_difference >= dx / Math.sqrt(3)) hill = -0.2;
                else if (altitude_difference <= -dx / Math.sqrt(3)) hill = 0.2;
                else hill = 0.0;
                if (altitude_difference >= dx / Math.sqrt(2)) t = 1000000.0;
                else t = dx / (speed * (1.0 + hill));
            }
        }
        return t;
    }

    /**
     * This method calculates heuristic values of all points before every A* search.
     * It keeps scanning for checking if there a better heuristic value for every
     * point from their adjacent points (4 directions) until there is no change in
     * a single scan.
     */
    public void heuristic() {
        boolean d = true, n = true, e = true, s = true, w = true;
        while (d) {
            d = false;
            for (int i = 0; i < x; i++) {
                for (int j = 0; j < y; j++) {
                    // from east
                    if (i + 1 < x) {
                        if (h.get(j).get(i + 1) + cost(i, j, i + 1, j) < h.get(j).get(i)) {
                            h.get(j).set(i, h.get(j).get(i + 1) + cost(i, j, i + 1, j));
                            e = true;
                        } else e = false;
                    }

                    // from west
                    if (i - 1 >= 0) {
                        if (h.get(j).get(i - 1) + cost(i, j, i - 1, j) < h.get(j).get(i)) {
                            h.get(j).set(i, h.get(j).get(i - 1) + cost(i, j, i - 1, j));
                            w = true;
                        } else w = false;
                    }

                    // from south
                    if (j + 1 < y) {
                        if (h.get(j + 1).get(i) + cost(i, j, i, j + 1) < h.get(j).get(i)) {
                            h.get(j).set(i, h.get(j + 1).get(i) + cost(i, j, i, j + 1));
                            s = true;
                        } else s = false;
                    }

                    // from north
                    if (j - 1 >= 0) {
                        if (h.get(j - 1).get(i) + cost(i, j, i, j - 1) < h.get(j).get(i)) {
                            h.get(j).set(i, h.get(j - 1).get(i) + cost(i, j, i, j - 1));
                            n = true;
                        } else n = false;
                    }
                }
            }
            if (!e && !w && !s && !n) break;
        }
    }

    public void output(String output_img_name, String path_file) {
        // some auxiliary variables
        String[] aStringArray;
        String aString = path_file.substring(0, path_file.indexOf("."));
        int xTemp, yTemp;
        // determine the path color
        int r = 153, g = 48, b = 255, rgb = new Color(r, g, b).getRGB();

        // give all points a bigger mark
        for (int[] a : points) {
            xTemp = a[0];
            yTemp = a[1];
            for (int i = xTemp - 2; i <= xTemp + 2; i++) {
                for (int j = yTemp - 2; j <= yTemp + 2; j++) {
                    if (i >= 0 && i < x && j >= 0 & j < y) {
                        img.setRGB(i, j, rgb);
                    }
                }
            }
        }

        // draw path and accumulate the total distance
        double total_path_length = 0.0;
        int xLast = -1, yLast = -1;
        for (String s : path) {
            aStringArray = s.split(" ");
            xTemp = Integer.parseInt(aStringArray[0]);
            yTemp = Integer.parseInt(aStringArray[1]);
            img.setRGB(xTemp, yTemp, rgb);
            if (xLast != -1) {
                if (xTemp == xLast && Math.abs(yLast - yTemp) == 1) total_path_length += dy;
                else if (yTemp == yLast && Math.abs(xLast - xTemp) == 1) total_path_length += dx;
            }
            xLast = xTemp;
            yLast = yTemp;
        }

        // output image (.png) and distance (.txt and standard output)
        try (BufferedWriter output = new BufferedWriter(new FileWriter("Distance.txt"))) {
            System.out.println("The total distance of " + aString + " path is: " + total_path_length + " m.");
            output.write("The total distance of " + aString + " path is: " + total_path_length + " m.");
            ImageIO.write(img, "png", new File(output_img_name + ".png"));
        } catch (IOException e) {
            System.out.println("IO errors.");
        }
    }

    /**
     * Main method.
     *
     * @param args command line arguments -- terrain-image elevation-file path-file output-image-filename
     */
    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("Usage: lab1.java terrain-image elevation-file path-file output-image-filename");
            System.exit(0);
        }
        lab1 l1 = new lab1(args[0]);
        l1.input(args[1], args[2]);
        l1.aStar();
        l1.output(args[3], args[2]);
    }
}

/**
 * an auxiliary class used as comparator for the priority queue (sorted by f(n))
 */
class SortByHeuristic implements Comparator<Object[]> {
    @Override
    public int compare(Object[] o1, Object[] o2) {
        double result = (double) o1[2] - (double) o2[2];
        return Double.compare(result, 0.0);
    }
}