package com.cisco.spark.android.util;

import com.github.benoitdion.ln.Ln;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

//
// Inspired by: http://stackoverflow.com/questions/11739444/how-to-get-usage-of-each-cpu-core-on-android
//
// USAGE NOTE: Be sure to persist the CpuStat object between calls so previous values are available for
// comparison/computation.  Without that, data WILL NOT BE ACCURATE!
//

public class CpuStat {
    private File statFile;
    private CpuInfo mCpuInfoTotal;
    private ArrayList<CpuInfo> mCpuInfoList;
    private String cpuUsageString;

    public CpuStat() {
    }

    public void update() {
        try {
            parseFile();
        } catch (FileNotFoundException e) {
            statFile = null;
            Ln.e(false, "Error opening /proc/stat: " + e);
        } catch (IOException e) {
            Ln.e(false, "Error closing /proc/stat: " + e);
        }
    }

    private void parseFile() throws IOException {
        FileInputStream in = new FileInputStream(new File("/proc/stat"));
        try {
            // Read the data all at once, this file is very dynamic
            byte[] data = new byte[1024];
            int len = in.read(data);

            if (len <= 0)
                return;

            Scanner scanner = new Scanner(new String(data, 0, len));
            int cpuId = -1;
            while (scanner.hasNextLine()) {
                String cpuLine = scanner.nextLine();
                if (parseCpuLine(cpuId, cpuLine))
                    cpuId++;
            }
        } catch (IOException e) {
            Ln.e(false, "Error parsing file's CPU lines: " + e);
        } finally {
            if (in != null)
                in.close();
        }
    }

    private boolean parseCpuLine(int cpuId, String cpuLine) {
        if (cpuLine != null && cpuLine.length() > 0) {
            String[] parts = cpuLine.split("[ ]+");
            String cpuLabel = "cpu";
            if (parts.length > 0 && parts[0].indexOf(cpuLabel) != -1) {
                createCpuInfo(cpuId, parts);
                return true;
            }
        }
        return false;
    }

    private void createCpuInfo(int cpuId, String[] parts) {
        if (cpuId == -1) {
            if (mCpuInfoTotal == null)
                mCpuInfoTotal = new CpuInfo();
            mCpuInfoTotal.update(parts);
        } else {
            if (mCpuInfoList == null)
                mCpuInfoList = new ArrayList<CpuInfo>();
            if (cpuId < mCpuInfoList.size())
                mCpuInfoList.get(cpuId).update(parts);
            else {
                CpuInfo info = new CpuInfo();
                info.update(parts);
                mCpuInfoList.add(info);
            }
        }
    }

    public int getCpuUsage(int cpuId) {
        update();
        int usage = 0;
        if (mCpuInfoList != null) {
            int cpuCount = mCpuInfoList.size();
            if (cpuCount > 0) {
                cpuCount--;
                if (cpuId == cpuCount) { // -1 total cpu usage
                    usage = mCpuInfoList.get(0).getUsage();
                } else {
                    if (cpuId <= cpuCount)
                        usage = mCpuInfoList.get(cpuId).getUsage();
                    else
                        usage = -1;
                }
            }
        }
        return usage;
    }

    public int getTotalCpuUsage() {
        update();
        int usage = 0;
        if (mCpuInfoTotal != null)
            usage = mCpuInfoTotal.getUsage();
        return usage;
    }

    public String toString() {
        update();
        StringBuffer buf = new StringBuffer();
        if (mCpuInfoTotal != null) {
            buf.append("CPU usage = ");
            buf.append(mCpuInfoTotal.getUsage());
            buf.append("%");
        }
        if (mCpuInfoList != null) {
            buf.append(" (");
            for (int i = 0; i < mCpuInfoList.size(); i++) {
                CpuInfo info = mCpuInfoList.get(i);
                buf.append("core" + i + "= ");
                buf.append(info.getUsage());
                buf.append((i < mCpuInfoList.size() - 1) ? "%, " : "%");
                info.getUsage();
            }
            buf.append(")");
        }
        cpuUsageString = buf.toString();
        return cpuUsageString;
    }

    public String getCpuUsageString() {
        return cpuUsageString;
    }

    public class CpuInfo {
        private int mUsage;
        private long mLastTotal;
        private long mLastIdle;

        public CpuInfo() {
            mUsage = 0;
            mLastTotal = 0;
            mLastIdle = 0;
        }

        private int getUsage() {
            return mUsage;
        }

        public void update(String[] parts) {
            // the columns are:
            //      0 "cpu": the string "cpu" that identifies the line
            //      1 user: normal processes executing in user mode
            //      2 nice: niced processes executing in user mode
            //      3 system: processes executing in kernel mode
            //      4 idle: twiddling thumbs
            //      5 iowait: waiting for I/O to complete
            //      6 irq: servicing interrupts
            //      7 softirq: servicing softirqs
            //
            long idle = Long.parseLong(parts[4], 10);
            long total = 0;
            boolean head = true;
            for (String part : parts) {
                if (head) {
                    head = false;
                    continue;
                }
                total += Long.parseLong(part, 10);
            }
            long diffIdle = idle - mLastIdle;
            long diffTotal = total - mLastTotal;
            mUsage = (int) ((float) (diffTotal - diffIdle) / diffTotal * 100);
            mLastTotal = total;
            mLastIdle = idle;
            // For debugging only: Ln.d("CPU total=" + total + "; idle=" + idle + "; usage=" + mUsage);
        }
    }

}

