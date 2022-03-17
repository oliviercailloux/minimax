package io.github.oliviercailloux.minimax.utils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public class ScatteredPlot {

  public static void createScatteredPlot(String fileName, String csvPath)
      throws FileNotFoundException, IOException {
    String title = "Distribution of Questions";
    String xAxisName = "Questions";
    String yAxisName = "Runs";
    // Create dataset
    XYDataset dataset = readCSV(csvPath);

    // Create chart
    JFreeChart chart = ChartFactory.createScatterPlot(title, xAxisName, yAxisName, dataset);

    // // Changes background color -- No, white is a terrible idea
    // XYPlot plot = (XYPlot) chart.getPlot();
    // plot.setBackgroundPaint(Color.WHITE);

    // Create Panel
    // ChartPanel panel = new ChartPanel(chart);

    try (OutputStream out = new FileOutputStream(fileName)) {
      ChartUtilities.writeChartAsPNG(out, chart, 1600, 1000);
    }

  }

  /*
   * Note: CSV csvReader = new CSV(); CategoryDataset csvData = csvReader.readCategoryDataset(new
   * FileReader(csvPath)); cannot be used because it assumes only numbers in the csv
   */

  private static XYDataset readCSV(String csvPath) throws FileNotFoundException, IOException {
    try (BufferedReader csvReader = new BufferedReader(new FileReader(csvPath))) {
      XYSeriesCollection dataset = new XYSeriesCollection();
      XYSeries questionsVoters = new XYSeries("questionsVoters");
      XYSeries questionsCommittee = new XYSeries("questionsCommittee");

      int rows = 1;
      String row = csvReader.readLine();
      // 800 questions are too much, it's hard to see anything
      // in order to print the distribution of only the first 200 question
      // uncomment the following
      // while ((row = csvReader.readLine()) != null && rows<200) {
      while ((row = csvReader.readLine()) != null) {
        String[] data = row.split(",");
        int cols = 1;
        for (int col = 1; col < data.length; col += 2) {
          if (data[col].equals("1")) {
            questionsVoters.add(rows, cols++);

          } else {
            questionsCommittee.add(rows, cols++);
          }
        }
        rows++;
      }
      csvReader.close();

      dataset.addSeries(questionsVoters);
      dataset.addSeries(questionsCommittee);

      return dataset;
    }
  }

  public static void main(String[] args) {
    // the current csv file contains commas in the question fields, should be
    // changed
    String csvName =
        "/home/bea/Documents/workspace/minimax/experiments/Table 2/m10n20LimitedPessimistic_800_questions2.csv";
    String fileName = "/home/bea/Documents/workspace/minimax/experiments/test800.png";
    try {
      createScatteredPlot(fileName, csvName);
    } catch (FileNotFoundException e) {
      throw new IllegalStateException(e);
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
