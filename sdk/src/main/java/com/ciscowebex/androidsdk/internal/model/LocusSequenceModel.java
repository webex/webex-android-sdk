/*
 * Copyright 2016-2021 Cisco Systems Inc
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.ciscowebex.androidsdk.internal.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A representation of the information needed to determine the order of loci
 *
 * We use the following information to determine order
 *
 *   entries - array of long ( unique values that define the locus )
 *   rangeStart - value of first entry that is not included in entries array
 *   rangeEnd - value of last entry that is not included in entries array
 *   since - value of latest locus sequence timestamp
 *   sequenceHash - hash of all the locus sequence timestamps since locus went active in this session
 *   dataCenter - data center from/on which the locusSequeceInfo was received/set
 *
 * If all entries are contained in the entries array, then rangeStart and rangeEnd will be 0
 *
 * An empty LocusSequenceInfo will be
 *
 * rangeStart = 0
 * rangeEnd = 0
 * entries = {}
 * since = 0
 * sequenceHash = 0
 *
 */
public class LocusSequenceModel implements Cloneable {

    public enum OverwriteWithResult {
        // the value passed in is newer than the current LocusSequenceInfo
        TRUE,
        // the value passed in is older than or equal to the current LocusSequenceInfo
        FALSE,
        // there are inconsistencies between the two versions - they both contain overlapping unique values
        DESYNC
    }

    public enum CompareResult {
        GREATER_THAN,
        LESS_THAN,
        EQUAL,
        DESYNC
    }

    public static class LocusSequenceSyncDebugModel {

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


        public LocusSequenceSyncDebugModel() {
            base = working = target = UNDEFINED;
            comparisonBaseWorking = comparisonWorkingTarget = comparisonBaseTarget = NO_VALUE;
        }

        public void initializeWorkingToTarget(LocusSequenceModel workingSequence, LocusSequenceModel targetSequence) {
            working = getSequenceState(workingSequence);
            target = getSequenceState(targetSequence);
            comparisonWorkingTarget = getComparisonValue(LocusSequenceModel.compareTo(workingSequence, targetSequence));
        }

        public void initializeBaseToWorkingAndTarget(LocusSequenceModel baseSequence, LocusSequenceModel workingSequence, LocusSequenceModel targetSequence) {
            base = getSequenceState(baseSequence);
            working = getSequenceState(workingSequence);
            target = getSequenceState(targetSequence);
            comparisonBaseWorking = getComparisonValue(LocusSequenceModel.compareTo(baseSequence, workingSequence));
            comparisonBaseTarget = getComparisonValue(LocusSequenceModel.compareTo(baseSequence, targetSequence));
        }

        private String getSequenceState(LocusSequenceModel sequenceInfo) {
            if (sequenceInfo == null)
                return UNDEFINED;
            else if (sequenceInfo.isEmpty())
                return EMPTY;
            else
                return DEFINE;
        }

        private String getComparisonValue(LocusSequenceModel.CompareResult result) {
            if (result == LocusSequenceModel.CompareResult.GREATER_THAN)
                return GREATER_THAN;
            else if (result == LocusSequenceModel.CompareResult.LESS_THAN)
                return LESS_THAN;
            else if (result == LocusSequenceModel.CompareResult.EQUAL)
                return EQUAL;
            else if (result == LocusSequenceModel.CompareResult.DESYNC)
                return CONFLICT;
            else
                return NO_VALUE;
        }

        public String build() {
            return String.format("%s,%s,%s,%s,%s,%s", base, working, target, comparisonBaseWorking, comparisonWorkingTarget, comparisonBaseTarget);
        }
    }

    private final long[] entries;
    private final long rangeStart;
    private final long rangeEnd;
    private long since;
    private long sequenceHash;
    private String dataCenter;
    private int totalParticipants;
    private int dirtyParticipants;
    private LocusSequenceSyncDebugModel syncDebug = new LocusSequenceSyncDebugModel();

    public LocusSequenceModel() {
        this(0, 0);
    }
    public LocusSequenceModel(long rangeStart, long rangeEnd, long... entries) {
        this(rangeStart, rangeEnd, entries, 0, 0, null);
    }
    public LocusSequenceModel(long rangeStart, long rangeEnd, long[] entries, long since, long sequenceHash, String dataCenter) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.entries = entries == null ? new long[0] : entries;
        this.since = since;
        this.sequenceHash = sequenceHash;
        this.dataCenter = dataCenter;
    }

    // see: https://sqbu-github.cisco.com/WebExSquared/cloud-apps/wiki/Locus-Sequence-Comparison-Algorithm
    public static LocusSequenceModel.CompareResult compareTo(LocusSequenceModel a, LocusSequenceModel b) {
        // if all of a's values are less than b's, b is newer
        // if all of a's values are greater than b's, a is newer
        if (a.getMax() < b.getMin())
            return LocusSequenceModel.CompareResult.LESS_THAN;
        if (a.getMin() > b.getMax())
            return LocusSequenceModel.CompareResult.GREATER_THAN;

        List<Long> aOnly = new ArrayList<>();
        List<Long> bOnly = new ArrayList<>();
        populateUniqueSets(a, b, aOnly, bOnly);

        if (aOnly.isEmpty() && bOnly.isEmpty()) {
            if (a.rangeEnd - a.getMin() > b.rangeEnd - b.getMin()) {
                return LocusSequenceModel.CompareResult.GREATER_THAN;
            } else if (a.rangeEnd - a.getMin() < b.rangeEnd - b.getMin()) {
                return LocusSequenceModel.CompareResult.LESS_THAN;
            } else {
                return LocusSequenceModel.CompareResult.EQUAL;
            }
        } else if (!aOnly.isEmpty() && bOnly.isEmpty()) {
            // if a has nothing unique but b does, then b is newer
            return LocusSequenceModel.CompareResult.GREATER_THAN;
        } else if (!bOnly.isEmpty() && aOnly.isEmpty()) {
            // If b has nothing unique and a does, then a is newer
            return LocusSequenceModel.CompareResult.LESS_THAN;
        } else {
            // both have unique
            if ((a.rangeStart == 0 && b.rangeStart == 0) || areNumbersInRange(aOnly, b) || areNumbersInRange(bOnly, a)) {
                return LocusSequenceModel.CompareResult.DESYNC;
            } else if (aOnly.get(0) > bOnly.get(0)) {
                return LocusSequenceModel.CompareResult.GREATER_THAN;
            } else {
                return LocusSequenceModel.CompareResult.LESS_THAN;
            }
        }
    }

    private static boolean areNumbersInRange(List<Long> sequences, LocusSequenceModel sequenceInfo) {
        long max = sequenceInfo.getMax();
        long min = sequenceInfo.getMin();
        for (Long sequence : sequences) {
            if (sequence >= min && sequence <= max) {
                return true;
            }
        }
        return false;
    }

    // calculate "only in a's" list and "only in b's" list
    private static void populateUniqueSets(LocusSequenceModel a, LocusSequenceModel b, List<Long> ua, List<Long> ub) {
        int idxa = 0, idxb = 0, lena = a.entries.length, lenb = b.entries.length;

        while (idxa < lena && idxb < lenb) {
            long aval = a.entries[idxa], bval = b.entries[idxb];

            if (aval == bval) {
                idxa++; idxb++;
            } else if (aval < bval) {
                if (!b.inRange(aval))
                    //ua.set(idxa);
                    ua.add(a.entries[idxa]);
                idxa++;
            } else {
                if (!a.inRange(bval))
                    //ub.set(idxb);
                    ub.add(b.entries[idxb]);
                idxb++;
            }
        }

        while (idxa < lena && !b.inRange(a.entries[idxa])) {
            ua.add(a.entries[idxa]);
            idxa++;
        }
        while (idxb < lenb && !a.inRange(b.entries[idxb])) {
            ub.add(b.entries[idxb]);
            idxb++;
        }
    }

    public Long getSince() {
        return since == 0 ? null : since;
    }

    public Long getSequenceHash() {
        return sequenceHash == 0 ? null : sequenceHash;
    }

    public String getDataCenter() {
        return dataCenter;
    }

    public int getTotalParticipants() {
        return totalParticipants;
    }

    public int getDirtyParticipants() {
        return dirtyParticipants;
    }

    public String getSyncDebug() {
        return syncDebug.build();
    }

    public boolean isEmpty() {
        return (entries == null || entries.length == 0) && rangeStart == 0 && rangeEnd == 0;
    }

    // apply a new Locus
    public LocusSequenceModel.OverwriteWithResult overwriteWith(LocusSequenceModel newLocus) {
        LocusSequenceModel.OverwriteWithResult retVal;

        // special case the empty sequence.  If you are currently empty then say update no matter what
        if (this.isEmpty() || newLocus.isEmpty()) {
            retVal = LocusSequenceModel.OverwriteWithResult.TRUE;
        } else {
            LocusSequenceModel.CompareResult compareResult = compareTo(this, newLocus);
            switch (compareResult) {
                case GREATER_THAN:
                    retVal = LocusSequenceModel.OverwriteWithResult.FALSE;
                    break;
                case LESS_THAN:
                    retVal = LocusSequenceModel.OverwriteWithResult.TRUE;
                    break;
                case EQUAL:
                    retVal = LocusSequenceModel.OverwriteWithResult.FALSE;
                    break;
                case DESYNC:
                    retVal = LocusSequenceModel.OverwriteWithResult.DESYNC;
                    break;
                default:
                    retVal = LocusSequenceModel.OverwriteWithResult.TRUE;
                    break;
            }
            syncDebug.initializeWorkingToTarget(this, newLocus);
        }
        return retVal;
    }

    // apply a Locus delta event
    public LocusSequenceModel.OverwriteWithResult overwriteWith(LocusSequenceModel baseSequence, LocusSequenceModel targetSequence) {
        LocusSequenceModel.OverwriteWithResult ret = this.overwriteWith(targetSequence);
        if (ret == LocusSequenceModel.OverwriteWithResult.TRUE && baseSequence != null) {
            LocusSequenceModel.CompareResult baselineCompare = compareTo(this, baseSequence);
            switch (baselineCompare) {
                case GREATER_THAN:
                case EQUAL:
                    ret = LocusSequenceModel.OverwriteWithResult.TRUE;
                    break;
                case LESS_THAN:
                case DESYNC:
                    ret = LocusSequenceModel.OverwriteWithResult.DESYNC;
                    break;
            }
            syncDebug.initializeBaseToWorkingAndTarget(baseSequence, this, targetSequence);
        }
        return ret;
    }

    private boolean inRange(long value) {
        return value >= rangeStart && value <= rangeEnd;
    }

    private long getMin() {
        return rangeStart != 0 ? rangeStart : entries.length > 0 ? entries[0] : 0;
    }

    private long getMax() {
        return entries.length > 0 ? entries[entries.length - 1] : rangeEnd;
    }

    @Override
    public LocusSequenceModel clone() {
        try {
            return (LocusSequenceModel) super.clone();
        } catch (CloneNotSupportedException e) { }
        return null;
    }

    public boolean sequenceEqualsExact(LocusSequenceModel other) {
        return rangeStart == other.rangeStart && rangeEnd == other.rangeEnd && Arrays.equals(entries, other.entries);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LocusSequenceModel that = (LocusSequenceModel) o;

        if (rangeStart != that.rangeStart) return false;
        if (rangeEnd != that.rangeEnd) return false;
        if (since != that.since) return false;
        if (sequenceHash != that.sequenceHash) return false;
        if (totalParticipants != that.totalParticipants) return false;
        if (dirtyParticipants != that.dirtyParticipants) return false;
        if (!Arrays.equals(entries, that.entries)) return false;
        if (dataCenter != null ? !dataCenter.equals(that.dataCenter) : that.dataCenter != null) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(entries);
        result = 31 * result + (int) (rangeStart ^ (rangeStart >>> 32));
        result = 31 * result + (int) (rangeEnd ^ (rangeEnd >>> 32));
        result = 31 * result + (int) (since ^ (since >>> 32));
        result = 31 * result + (int) (sequenceHash ^ (sequenceHash >>> 32));
        result = 31 * result + (dataCenter != null ? dataCenter.hashCode() : 0);
        result = 31 * result + totalParticipants;
        result = 31 * result + dirtyParticipants;
        return result;
    }
}
