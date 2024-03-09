package org.framework;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileStatistics {

    public static void main(String[] args) {

        String filePath = "DC5A0outputs/D5A0wasted_resources";
        FileStatistics fileStatistics = new FileStatistics();
        System.out.println("D5A0 wasted_resources");
        fileStatistics.calculateStatistics(filePath);
        String filePath1 = "DC5A1outputs/D5A1wasted_resources";
        FileStatistics fileStatistics1 = new FileStatistics();
        System.out.println("D5A1 wasted_resources");
        fileStatistics1.calculateStatistics(filePath1);
        String filePath2 = "DC5A2outputs/D5A2wasted_resources";
        FileStatistics fileStatistics2 = new FileStatistics();
        System.out.println("D5A2 wasted_resources");
        fileStatistics2.calculateStatistics(filePath2);
        String filePath3 = "DC5A3outputs/D5A3wasted_resources";
        FileStatistics fileStatistics3 = new FileStatistics();
        System.out.println("D5A3 wasted_resources");
        fileStatistics3.calculateStatistics(filePath3);
    }

    public void calculateStatistics(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            double sum = 0;
            double minValue = Double.MAX_VALUE;
            double maxValue = Double.MIN_VALUE;
            int count = 0;

            while ((line = br.readLine()) != null) {
                try {
                    double value = Double.parseDouble(line.trim());
                    sum += value;
                    minValue = Math.min(minValue, value);
                    maxValue = Math.max(maxValue, value);
                    count++;
                } catch (NumberFormatException e) {
                    // Ignore lines that are not valid numbers
                    System.out.println("Skipping invalid number: " + line);
                }
            }

            if (count > 0) {
                double average = sum / count;
                System.out.println("Average: " + average);
                System.out.println("Maximum Value: " + maxValue);
                System.out.println("Minimum Value: " + minValue);

            } else {
                System.out.println("No valid numbers found in the file.");
            }

        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        }
    }
}