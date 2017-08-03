package com.cisco.spark.android.locus.model;
// File copied from Locus repo: https://sqbu-github.cisco.com/WebExSquared/cisco-spark-base/blob/5f3d3cd0a9168135c9e4c094e86fc19df5543b87/cloud-apps-common/src/main/java/com/cisco/wx2/dto/locus/LocusSequenceInfo.java

import android.annotation.TargetApi;

import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

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
public class LocusSequenceInfo extends DataTransferObject implements Cloneable {

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

    private final long[] entries;
    private final long rangeStart;
    private final long rangeEnd;
    private long since;
    private long sequenceHash;
    private String dataCenter;
    private int totalParticipants;
    private int dirtyParticipants;
    private LocusSequenceSyncDebug syncDebug = new LocusSequenceSyncDebug();

    @JsonIgnore
    private BitSet cleanParticipantsBitSet;
    private byte[] cleanParticipants;

    public LocusSequenceInfo() {
        this(0, 0);
    }

    @TargetApi(24)
    public LocusSequenceInfo(long rangeStart, long rangeEnd, List<Long> entries) {
        this(rangeStart, rangeEnd, entries.stream().mapToLong(l -> l).toArray());
    }
    /*
     * Note : entries list must be sorted
     */
    public LocusSequenceInfo(long rangeStart, long rangeEnd, long... entries) {
        this(rangeStart, rangeEnd, entries, 0, 0, null);
    }

    public LocusSequenceInfo(long rangeStart, long rangeEnd, long[] entries, long since, long sequenceHash, String dataCenter) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.entries = entries == null ? new long[0] : entries;
        this.since = since;
        this.sequenceHash = sequenceHash;
        this.dataCenter = dataCenter;
    }

    @TargetApi(24)
    public List<Long> getEntries() {
        return Arrays.stream(entries).mapToObj(l -> l).collect(Collectors.toList());
    }

    public Long getRangeStart() {
        return rangeStart;
    }

    public Long getRangeEnd() {
        return rangeEnd;
    }

    @JsonSerialize(using = ToStringSerializer.class)
    public Long getSince() {
        return since == 0 ? null : since;
    }

    public Long getSequenceHash() {
        return sequenceHash == 0 ? null : sequenceHash;
    }

    public String getDataCenter() {
        return dataCenter;
    }

    public void setSequenceHash(String sequenceHash) {
        this.sequenceHash = sequenceHash != null ? Long.parseLong(sequenceHash) : 0;
    }

    public void setSince(Long since) {
        this.since = since;
    }

    public void setDataCenter(String dataCenter) {
        this.dataCenter = dataCenter;
    }

    public int getTotalParticipants() {
        return totalParticipants;
    }

    public int getDirtyParticipants() {
        return dirtyParticipants;
    }

    public void setDirtyParticipants(int dirtyParticipants) {
        this.dirtyParticipants = dirtyParticipants;
    }

    public void setTotalParticipants(int totalParticipants) {
        this.totalParticipants = totalParticipants;
    }

    public String getSyncDebug() {
        return syncDebug.build();
    }

    @TargetApi(24)
    @JsonProperty(value = "participants")
    public byte[] getCleanParticipants() {
        if (cleanParticipants == null) {
            if (cleanParticipantsBitSet != null) {
                cleanParticipants = cleanParticipantsBitSet.toByteArray();
            }
        }
        return cleanParticipants;
    }

    @JsonProperty(value = "participants")
    public void setCleanParticipants(byte[] cleanParticipants) {
        this.cleanParticipants = cleanParticipants;
    }

    @TargetApi(24)
    @JsonIgnore
    public BitSet getCleanParticipantsBitSet() {
        if (cleanParticipantsBitSet == null) {
            if (cleanParticipants != null) {
                cleanParticipantsBitSet = BitSet.valueOf(cleanParticipants);
            }
        }
        return cleanParticipantsBitSet;
    }

    @JsonIgnore
    void setCleanParticipantsBitSet(BitSet cleanParticipantsBitSet) {
        this.cleanParticipantsBitSet = cleanParticipantsBitSet;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return (entries == null || entries.length == 0) && rangeStart == 0 && rangeEnd == 0;
    }

    @JsonIgnore
    // utility to create a sequence that will cause the client to perform a GET
    public LocusSequenceInfo createDesync(boolean delta) {
        final LocusSequenceInfo ret;
        if (getRangeStart() == 0L) {
            if (getEntries().isEmpty()) {
                ret = new LocusSequenceInfo(0L,  0L);
            } else if (getEntries().size() == 1) {
                ret = new LocusSequenceInfo(0L,  0L, getEntries().get(0));
            } else {
                ret = new LocusSequenceInfo(0L, 0L,
                        getEntries().get(0), getEntries().get(getEntries().size() - 1));
            }
        } else {
            if (getEntries().isEmpty()) {
                ret = new LocusSequenceInfo(0L, 0L, getRangeStart(), getRangeEnd());
            } else if (getEntries().size() == 1) {
                ret = new LocusSequenceInfo(0L,  0L, getRangeStart(), getEntries().get(0));
            } else {
                ret = new LocusSequenceInfo(0L, 0L,
                        getRangeStart(), getEntries().get(getEntries().size() - 1));
            }
        }
        ret.setSince(getSince());
        ret.setSequenceHash(getSequenceHash() != null ? getSequenceHash().toString() : null);
        ret.setDataCenter(getDataCenter());
        ret.setTotalParticipants(getTotalParticipants());
        ret.setDirtyParticipants(getDirtyParticipants());
        return ret;
    }

    // apply a new Locus
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
            syncDebug.initializeWorkingToTarget(this, newLocus, compareResult);
        }
        return retVal;
    }

    // apply a Locus delta event
    public OverwriteWithResult overwriteWith(LocusSequenceInfo baseSequence, LocusSequenceInfo targetSequence) {
        OverwriteWithResult ret = this.overwriteWith(targetSequence);
        if (ret == OverwriteWithResult.TRUE && baseSequence != null) {
            CompareResult baselineCompare = compareTo(this, baseSequence);
            switch (baselineCompare) {
                case GREATER_THAN:
                case EQUAL:
                    ret = OverwriteWithResult.TRUE;
                    break;
                case LESS_THAN:
                case DESYNC:
                    ret = OverwriteWithResult.DESYNC;
                    break;
            }
            syncDebug.initializeBaseToWorkingAndTarget(baseSequence, this, targetSequence, baselineCompare);
        }
        return ret;
    }

    @TargetApi(24)
    // see: https://sqbu-github.cisco.com/WebExSquared/cloud-apps/wiki/Locus-Sequence-Comparison-Algorithm
    public static CompareResult compareTo(LocusSequenceInfo a, LocusSequenceInfo b) {
        // if all of a's values are less than b's, b is newer
        // if all of a's values are greater than b's, a is newer
        if (a.getMax() < b.getMin()) return CompareResult.LESS_THAN;
        if (a.getMin() > b.getMax()) return CompareResult.GREATER_THAN;

        // calculate "only in a's" list and "only in b's" list
        final int indexA = 0, indexB = 1;
        BitSet[] unique = findUnique(a, b);

        if (unique[indexA].isEmpty() && unique[indexB].isEmpty()) {
            if (a.rangeEnd - a.getMin() >  b.rangeEnd - b.getMin()) {
                return CompareResult.GREATER_THAN;
            } else if (a.rangeEnd - a.getMin() < b.rangeEnd - b.getMin()) {
                return CompareResult.LESS_THAN;
            } else {
                return CompareResult.EQUAL;
            }
        } else if (!unique[indexA].isEmpty() && unique[indexB].isEmpty()) {
            // if a has nothing unique but b does, then b is newer
            return CompareResult.GREATER_THAN;
        } else if (!unique[indexB].isEmpty() && unique[indexA].isEmpty()) {
            // If b has nothing unique and a does, then a is newer
            return CompareResult.LESS_THAN;
        } else {
            // both have unique
            if ((a.rangeStart == 0 && b.rangeStart == 0) ||
                    unique[indexA].stream().asLongStream().map(i -> a.entries[(int) i]).anyMatch(i -> i > b.getMin() && i < b.getMax()) ||
                    unique[indexB].stream().asLongStream().map(i -> b.entries[(int) i]).anyMatch(i -> i > a.getMin() && i < a.getMax())) {
                return CompareResult.DESYNC;
            } else if (a.entries[unique[indexA].nextSetBit(0)] > b.entries[unique[indexB].nextSetBit(0)]) {
                return CompareResult.GREATER_THAN;
            } else {
                return CompareResult.LESS_THAN;
            }
        }
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

    private static BitSet[] findUnique(LocusSequenceInfo a, LocusSequenceInfo b) {
        BitSet ua = new BitSet(a.entries.length);
        BitSet ub = new BitSet(b.entries.length);

        int idxa = 0, idxb = 0, lena = a.entries.length, lenb = b.entries.length;

        while (idxa < lena && idxb < lenb) {
            long aval = a.entries[idxa], bval = b.entries[idxb];

            if (aval == bval) {
                idxa++; idxb++;
            } else if (aval < bval) {
                if (!b.inRange(aval)) ua.set(idxa);
                idxa++;
            } else {
                if (!a.inRange(bval)) ub.set(idxb);
                idxb++;
            }
        }

        while (idxa < lena && !b.inRange(a.entries[idxa])) {
            ua.set(idxa); idxa++;
        }
        while (idxb < lenb && !a.inRange(b.entries[idxb])) {
            ub.set(idxb); idxb++;
        }

        return new BitSet[] {ua, ub};
    }

    @Override
    public LocusSequenceInfo clone() {
        try {
            return (LocusSequenceInfo) super.clone();
        } catch (CloneNotSupportedException e) { }
        return null;
    }

    @TargetApi(19)
    @JsonIgnore
    public LocusSequenceInfo updateDirtyInfo(int dirtyParticipants, int totalParticipants, BitSet cleanParticipants) {
        LocusSequenceInfo ret = this;
        if (dirtyParticipants != this.dirtyParticipants || totalParticipants != this.totalParticipants ||
                !Objects.equals(cleanParticipants, this.cleanParticipantsBitSet)) {
            ret = clone();
            ret.setDirtyParticipants(dirtyParticipants);
            ret.setTotalParticipants(totalParticipants);
            ret.setCleanParticipantsBitSet(cleanParticipants.cardinality() == 0 ? null : cleanParticipants);
        }
        return ret;
    }

    public boolean sequenceEqualsExact(LocusSequenceInfo other) {
        return rangeStart == other.rangeStart && rangeEnd == other.rangeEnd && Arrays.equals(entries, other.entries);
    }

    @TargetApi(19)
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        LocusSequenceInfo that = (LocusSequenceInfo) o;

        if (rangeStart != that.rangeStart) return false;
        if (rangeEnd != that.rangeEnd) return false;
        if (since != that.since) return false;
        if (sequenceHash != that.sequenceHash) return false;
        if (totalParticipants != that.totalParticipants) return false;
        if (dirtyParticipants != that.dirtyParticipants) return false;
        if (!Arrays.equals(entries, that.entries)) return false;
        if (dataCenter != null ? !dataCenter.equals(that.dataCenter) : that.dataCenter != null) return false;
//        if (participantsBitSet != null ? !participantsBitSet.equals(that.participantsBitSet) : that.participantsBitSet != null)
//            return false;
        return Arrays.equals(cleanParticipants, that.cleanParticipants);
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
//        result = 31 * result + (participantsBitSet != null ? participantsBitSet.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(cleanParticipants);
        return result;
    }
}
