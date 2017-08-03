package com.cisco.spark.android.locus.model;

import com.cisco.spark.android.locus.model.LocusSequenceInfo.OverwriteWithResult;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;

import static org.junit.Assert.assertTrue;

public class LocusDeltaTest {
    private static final String TEST_DATA_FILE_1 = "SeqCmp.json";
    private static final String TEST_DATA_FILE_2 = "BasicSeqCmp.json";

    private Gson gson;

    @Before
    public void setup() {
        gson = new Gson();
    }


    @Test
    public void testFile1Comparisons() {
        System.out.println("Executing: testFile1Comparisons");
        testFileComparisons(TEST_DATA_FILE_1);
    }

    @Test
    public void testFile2Comparisons() {
        System.out.println("Executing: testFile2Comparisons");
        testFileComparisons(TEST_DATA_FILE_2);
    }

    @Test
    public void testFile1Updates() {
        System.out.println("Executing: testFile1Updates");
        testFileUpdates(TEST_DATA_FILE_1);
    }


    // Helper methods

    private void testFileComparisons(String filename) {
        SequenceTestData testData = buildData(filename);

        int failedTests = 0;

        for (String key : testData.comparisons.dataList.keySet()) {
            ComparisonData comparison = testData.comparisons.dataList.get(key);

            LocusSequenceInfo before = testData.sequences.dataList.get(comparison.current);
            LocusSequenceInfo after = testData.sequences.dataList.get(comparison.update);
            LocusSequenceInfo.CompareResult result = LocusSequenceInfo.compareTo(before, after);

            boolean passed = result.name().equals(comparison.result);

            failedTests = reportTestResult(key, passed, failedTests, comparison.result, result.name(), null);
        }

        assertTrue(failedTests + " tests failed", failedTests == 0);
        System.out.println("\n");
    }

    private void testFileUpdates(String filename) {
        SequenceTestData testData = buildData(filename);

        int failedTests = 0;

        for (String key : testData.updates.dataList.keySet()) {
            UpdateData update = testData.updates.dataList.get(key);

            LocusSequenceInfo current = testData.sequences.dataList.get(update.current);
            LocusSequenceInfo newUpdate = testData.sequences.dataList.get(update.update);
            LocusSequenceInfo newBase = testData.sequences.dataList.get(update.newbase);

            OverwriteWithResult result;
            if (newBase != null)
                result = current.overwriteWith(newBase, newUpdate);
            else
                result = current.overwriteWith(newUpdate);

            boolean passed = update.resultEquals(result);

            failedTests = reportTestResult(key, passed, failedTests, update.result, result.name(), current);
        }

        assertTrue(failedTests + " test failed", failedTests == 0);
        System.out.println("");
    }

    private String readFile(String file) {
        System.out.println("Reading data file: " + file);

        StringBuilder sb = new StringBuilder();
        String strLine;
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            while ((strLine = reader.readLine()) != null) {
                sb.append(strLine.trim());
            }
        } catch (final IOException ex) {
            System.out.println("Error: " + ex.getMessage());
        }

        return sb.toString();
    }

    private SequenceTestData buildData(String filename) {
        SequenceTestData testData = new SequenceTestData();

        try {
            String json = readFile(filename);

            JSONObject dataFile = new JSONObject(json);

            System.out.println("Loading sequence data...");
            JSONObject sequences = dataFile.getJSONObject("sequences");
            if (sequences != null && sequences.length() > 0) {
                Iterator<String> keys = sequences.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject locusSequenceInfoObject = sequences.getJSONObject(key);
                    LocusSequenceInfo locusSequenceInfo = gson.fromJson(locusSequenceInfoObject.toString(), LocusSequenceInfo.class);
                    testData.sequences.addSequence(key, locusSequenceInfo);
                }
            }

            System.out.println("Loading comparison data...");
            JSONObject comparisons = dataFile.getJSONObject("comparisons");
            if (comparisons != null && comparisons.length() > 0) {
                Iterator<String> keys = comparisons.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject comparisonObject = comparisons.getJSONObject(key);
                    ComparisonData comparison = gson.fromJson(comparisonObject.toString(), ComparisonData.class);
                    testData.comparisons.addComparison(key, comparison);
                }
            }

            System.out.println("Loading update_action data...");
            JSONObject updates = dataFile.getJSONObject("update_actions");
            if (updates != null && updates.length() > 0) {
                Iterator<String> keys = updates.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject updateObject = updates.getJSONObject(key);
                    UpdateData update = gson.fromJson(updateObject.toString(), UpdateData.class);
                    testData.updates.addUpdate(key, update);
                }
            }

        } catch (JSONException ex) {
            System.out.println(ex.getMessage());
        }

        return testData;
    }

    private int reportTestResult(String testName, boolean passed, int currentFailureCount, String expectedResult, String actualResult, LocusSequenceInfo sequenceInfo) {
        System.out.printf("Test: %s %s", testName, passed ? "PASSED" : "FAILED");
        if (!passed) {
            currentFailureCount++;
            System.out.printf(" - Expected: '%s', actual: '%s'", expectedResult, actualResult);
        } else if (sequenceInfo != null) {
            System.out.printf("  (sync_debug=" + sequenceInfo.getSyncDebug() + ")");
        }
        System.out.printf("\n");
        return currentFailureCount;
    }


    // Classes to import and reflect JSON file data into.

    private class SequenceTestData {
        private Sequences sequences;
        private Comparisons comparisons;
        private Updates updates;

        public SequenceTestData() {
            sequences = new Sequences();
            comparisons = new Comparisons();
            updates = new Updates();
        }
    }

    private class Sequences {
        private HashMap<String, LocusSequenceInfo> dataList;
        public Sequences() {
            dataList = new HashMap<>();
        }
        public void addSequence(String name, LocusSequenceInfo item) {
            dataList.put(name, item);
        }
    }

    private class Comparisons {
        private HashMap<String, ComparisonData> dataList;
        public Comparisons() {
            dataList = new HashMap<>();
        }
        public void addComparison(String name, ComparisonData item) {
            dataList.put(name, item);
        }
    }

    private class Updates {
        private HashMap<String, UpdateData> dataList;
        public Updates() {
            dataList = new HashMap<>();
        }
        public void addUpdate(String name, UpdateData item) {
            dataList.put(name, item);
        }
    }

    private class ComparisonData {
        public String current;
        @SerializedName("new")
        public String update;
        public String result;
        public String description;

        public String getName() {
            return getClass().getName();
        }
    }

    private class UpdateData {
        public String current;
        public String newbase;
        @SerializedName("new")
        public String update;
        public String result;
        public String description;

        public boolean resultEquals(OverwriteWithResult overwriteWithResult) {
            switch (result) {
                case "ACCEPT_NEW":
                    return overwriteWithResult.equals(OverwriteWithResult.TRUE);
                case "KEEP_CURRENT":
                    return overwriteWithResult.equals(OverwriteWithResult.FALSE);
                case "DESYNC":
                    return overwriteWithResult.equals(OverwriteWithResult.DESYNC);
                default:
                    return false;
            }
        }
    }
}
