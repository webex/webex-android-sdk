package com.cisco.spark.android.locus.model;


public class LocusSequenceSyncDebug {

    private static final String UNDEFINED = "undef";
    private static final String DEFINE = "def";
    private static final String EMPTY = "empty";
    private static final String NO_VALUE = "na";
    private static final String EQUAL = "eq";
    private static final String GREATER_THAN = "gt";
    private static final String LESS_THAN = "lt";
    private static final String CONFLICT = "cf";

    private String base;
    private String working;
    private String target;
    private String comparisonBaseWorking;
    private String comparisonWorkingTarget;
    private String comparisonBaseTarget;


    public LocusSequenceSyncDebug() {
        base = working = target = UNDEFINED;
        comparisonBaseWorking = comparisonWorkingTarget = comparisonBaseTarget = NO_VALUE;
    }

    public void initializeWorkingToTarget(LocusSequenceInfo workingSequence, LocusSequenceInfo targetSequence, LocusSequenceInfo.CompareResult result) {
        working = getSequenceState(workingSequence);
        target = getSequenceState(targetSequence);
        comparisonWorkingTarget = getComparisonValue(result);
    }

    public void initializeBaseToWorkingAndTarget(LocusSequenceInfo baseSequence, LocusSequenceInfo workingSequence, LocusSequenceInfo targetSequence, LocusSequenceInfo.CompareResult result) {
        base = getSequenceState(baseSequence);
        comparisonBaseWorking = getAndFlipComparisonValue(result);
        comparisonBaseTarget = getComparisonValue(LocusSequenceInfo.compareTo(baseSequence, targetSequence));
    }

    private String getSequenceState(LocusSequenceInfo sequenceInfo) {
        if (sequenceInfo == null)
            return UNDEFINED;
        else if (sequenceInfo.isEmpty())
            return EMPTY;
        else
            return DEFINE;
    }

    private String getComparisonValue(LocusSequenceInfo.CompareResult result) {
        if (result == LocusSequenceInfo.CompareResult.GREATER_THAN)
            return GREATER_THAN;
        else if (result == LocusSequenceInfo.CompareResult.LESS_THAN)
            return LESS_THAN;
        else if (result == LocusSequenceInfo.CompareResult.EQUAL)
            return EQUAL;
        else if (result == LocusSequenceInfo.CompareResult.DESYNC)
            return CONFLICT;
        else
            return NO_VALUE;
    }

    private String getAndFlipComparisonValue(LocusSequenceInfo.CompareResult result) {
        String retVal = getComparisonValue(result);
        if (GREATER_THAN.equals(retVal))
            return LESS_THAN;
        else if (LESS_THAN.equals(retVal))
            return GREATER_THAN;
        else
            return retVal;
    }
    public String build() {
        return String.format("%s,%s,%s,%s,%s,%s", base, working, target, comparisonBaseWorking, comparisonWorkingTarget, comparisonBaseTarget);
    }
}
