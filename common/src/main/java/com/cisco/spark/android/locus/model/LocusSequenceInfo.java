package com.cisco.spark.android.locus.model;

import java.util.*;


/**
 * A representation of the information needed to determine the order of loci
 *
 * We use the following information to determine order
 *
 *   entries - array of Long ( unique values that define the locus )
 *   rangeStart - value of first entry that is not included in entries array
 *   rangeEnd - value of last entry that is not included in entries array
 *
 * If all entries are contained in the entries array, then rangeStart and rangeEnd will be 0
 *
 * An empty LocusSequenceInfo will be
 *
 * rangeStart = 0
 * rangeEnd = 0
 * entries = {}
 *
 */
public class LocusSequenceInfo {

    public static enum OverwriteWithResult {
        // the value passed in is newer than the current LocusSequenceInfo
        TRUE,
        // the value passed in is older than or equal to the current LocusSequenceInfo
        FALSE,
        // there are inconsistencies between the two versions - they both contain overlapping unique values
        DESYNC
    }

    public static enum CompareResult {
        GREATER_THAN,
        LESS_THAN,
        EQUAL,
        DESYNC
    }

    private final List<Long> entries;
    private final Long rangeStart;
    private final Long rangeEnd;

    public LocusSequenceInfo() {
        entries = Collections.emptyList();
        rangeStart = 0L;
        rangeEnd = 0L;
    }

    /*
     * Note : entries list must be sorted
     */
    public LocusSequenceInfo(Long rangeStart, Long rangeEnd, List<Long> entries) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.entries = new LinkedList<Long>(entries);
    }

    public List<Long> getEntries() {
        return entries;
    }

    public Long getRangeStart() {
        return rangeStart;
    }

    public Long getRangeEnd() {
        return rangeEnd;
    }

    public boolean isEmpty() {
        return (entries.isEmpty() && rangeStart == 0L && rangeEnd == 0L);
    }

    public String toString() {
        return "Sequence=" + Arrays.toString(entries.toArray()) + ",rangestart=" + rangeStart + ",rangeEnd=" + rangeEnd;
    }

    /*
     * Get the first value in the list
     */
    private Long getEntriesFirstValue() {
        if (entries.isEmpty()) {
            return 0L;
        } else {
            return entries.get(0);
        }
    }

    /*
     * Get the last value in the list
     */
    private Long getEntriesLastValue() {
        if (entries.isEmpty()) {
            return 0L;
        } else {
            return entries.get(entries.size() - 1);
        }
    }

    private Long getCompareFirstValue() {
        Long retVal = getRangeStart();

        if (Long.valueOf(0L).equals(retVal)) {
            retVal = getEntriesFirstValue();
        }

        return retVal;
    }

    private Long getCompareLastValue() {
        Long retVal = getEntriesLastValue();

        if (Long.valueOf(0L).equals(retVal)) {
            retVal = getRangeEnd();
        }

        return retVal;
    }

    public OverwriteWithResult overwriteWith(LocusSequenceInfo newLocus) {

        OverwriteWithResult retVal;

        // special case the empty sequence.  If you are currently empty then say update no matter what
        if (this.isEmpty() || newLocus.isEmpty()) {
            retVal = OverwriteWithResult.TRUE;
        } else {
            CompareResult compareResult = compareTo(this, newLocus);
            switch (compareResult) {
                case GREATER_THAN:
                    retVal = OverwriteWithResult.FALSE;
                    break;
                case LESS_THAN:
                    retVal = OverwriteWithResult.TRUE;
                    break;
                case EQUAL:
                    retVal = OverwriteWithResult.FALSE;
                    break;
                case DESYNC:
                    retVal = OverwriteWithResult.DESYNC;
                    break;
                default:
                    retVal = OverwriteWithResult.TRUE;
                    break;
            }
        }

        return retVal;
    }

    // compare the local sequence with remote base sequence,
    // return TRUE if local locus was more recent or equal the base sequence; otherwise, return DESYNC
    public OverwriteWithResult overwriteWithBaseSequence(LocusSequenceInfo baseLocus) {

        OverwriteWithResult retVal;

        // special case the empty sequence.  If you are currently empty then say update no matter what
        if (this.isEmpty() || baseLocus == null || baseLocus.isEmpty()) {
            retVal = OverwriteWithResult.TRUE;
        } else {
            CompareResult compareResult = compareTo(this, baseLocus);
            switch (compareResult) {
                case GREATER_THAN:
                case EQUAL:
                    retVal = OverwriteWithResult.TRUE;
                    break;
                case LESS_THAN:
                case DESYNC:
                    retVal = OverwriteWithResult.DESYNC;
                    break;
                default:
                    retVal = OverwriteWithResult.TRUE;
                    break;
            }
        }

        return retVal;
    }

    private boolean inRange(Long value) {
        return (value >= getRangeStart() && value <= getRangeEnd());
    }

    // calculate "only in a's" list and "only in b's" list
    private static void populateSets(LocusSequenceInfo a, LocusSequenceInfo b, List<Long> aOnly, List<Long> bOnly) {
        Iterator<Long> aIter = a.getEntries().iterator();
        Iterator<Long> bIter = b.getEntries().iterator();

        boolean atEnd = false;
        while (aIter.hasNext() && bIter.hasNext()) {
            Long aVal = aIter.next();
            Long bVal = bIter.next();

            while (!aVal.equals(bVal) && !atEnd) {
                while (aVal > bVal) {
                    if (!a.inRange(bVal)) {
                        bOnly.add(bVal);
                    }
                    if (bIter.hasNext()) {
                        bVal = bIter.next();
                    } else {
                        atEnd = true;
                        break;
                    }
                }

                while (bVal > aVal) {
                    if (!b.inRange(aVal)) {
                        aOnly.add(aVal);
                    }
                    if (aIter.hasNext()) {
                        aVal = aIter.next();
                    } else {
                        atEnd = true;
                        break;
                    }
                }
            }
        }

        while (aIter.hasNext()) {
            Long aVal = aIter.next();
            if (!b.inRange(aVal)) {
                aOnly.add(aVal);
            }
        }

        while (bIter.hasNext()) {
            Long bVal = bIter.next();
            if (!a.inRange(bVal)) {
                bOnly.add(bVal);
            }
        }
    }

    public static CompareResult compareTo(LocusSequenceInfo a, LocusSequenceInfo b) {
        List<Long> aOnly = new ArrayList<Long>();
        List<Long> bOnly = new ArrayList<Long>();

        // if all of a's values are less than b's, b is newer
        if (a.getCompareLastValue() < b.getCompareFirstValue()) {
            return CompareResult.LESS_THAN;
        }

        // if all of a's values are greater than b's, a is newer
        if (a.getCompareFirstValue() > b.getCompareLastValue()) {
            return CompareResult.GREATER_THAN;
        }

        // calculate "only in a's" list and "only in b's" list
        populateSets(a, b, aOnly, bOnly);

        if (aOnly.isEmpty() && bOnly.isEmpty()) {
            // both sets are completely empty, use range to figure out order
            if (a.getRangeEnd() > b.getRangeEnd()) {
                return CompareResult.GREATER_THAN;
            } else if (a.getRangeEnd() < b.getRangeEnd()) {
                return CompareResult.LESS_THAN;
            } else if (a.getRangeStart() < b.getRangeStart()) {
                return CompareResult.GREATER_THAN;
            } else if (a.getRangeStart() > b.getRangeStart()) {
                return CompareResult.LESS_THAN;
            } else {
                return CompareResult.EQUAL;
            }
        }

        // If b has nothing unique and a does, then a is newer
        if (!aOnly.isEmpty() && bOnly.isEmpty()) {
            return CompareResult.GREATER_THAN;
        }

        // if I have nothing unique but b does, then b is newer
        if (!bOnly.isEmpty() && aOnly.isEmpty()) {
            return CompareResult.LESS_THAN;
        }

        // both have unique entries...
        // if a unique value in one list is within the min and max value in the others unique list then we are desync'd
        for (Long i : aOnly) {
            if (i > bOnly.get(0) && i < bOnly.get(bOnly.size() - 1)) {
                return CompareResult.DESYNC;
            }
        }
        for (Long i : bOnly) {
            if (i > aOnly.get(0) && i < aOnly.get(aOnly.size() - 1)) {
                return CompareResult.DESYNC;
            }
        }

        // aOnly and bOnly are 2 non-overlapping sets.  compare first item in both
        if (aOnly.get(0) > bOnly.get(0)) {
            return CompareResult.GREATER_THAN;
        } else {
            return CompareResult.LESS_THAN;
        }
    }
}
