package com.cisco.spark.android.core;

import com.github.benoitdion.ln.Ln;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PSUtils {
    public static void iteratePSresults(String psName, String matchName, PSResultHandler handler) {
        BufferedReader reader = null;
        try {
            // Query process list, which returns:
            //   USER      PID   PPID  VSIZE  RSS     WCHAN    PC         NAME
            //   u0_a141   7716  1     884    180   ffffffff 00000000 S logcat
            //   u0_a141   8028  1     884    180   ffffffff 00000000 S logcat
            //   u0_a141   8211  8189  884    180   ffffffff 00000000 S logcat
            // Orphaned processes have a parent (PPID) of 1
            java.lang.Process ps = new ProcessBuilder("ps", psName).start();
            int exitCode = ps.waitFor();
            if (exitCode != 0)
                Ln.w("ps command failed to list duplicate instances (exit code=%d)", exitCode);
            reader = new BufferedReader(new InputStreamReader(ps.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.endsWith((matchName != null) ? matchName : psName)) {
                    String[] element = line.split("\\s+");
                    if (element.length == 9) {
                        PSResult result = new PSResult(element[0], Integer.parseInt(element[1]), Integer.parseInt(element[2]), element[8]);
                        handler.onResult(result);
                    }

                }
            }
        } catch (IOException ex) {
            Ln.w("Encountered an IOException while cleaning up duplicate apps: %s", ex.getMessage());
            handler.onException(ex);
        } catch (InterruptedException ex) {
            Ln.w("The process searching for duplicate apps was interrupted: %s", ex.getMessage());
            handler.onException(ex);
        } catch (NumberFormatException ex) {
            Ln.w("The failed to convert pid/ppid's to int's", ex.getMessage());
            handler.onException(ex);
        } finally {
            try {
                if (reader != null)
                    reader.close();
            } catch (IOException ex) {
                handler.onException(ex);
            }
        }
    }

    public static class PSResult {
        private String user;
        private int pid;
        private int ppid;
        private String name;

        public PSResult(String user, int pid, int ppid, String name) {
            this.user = user;
            this.pid = pid;
            this.ppid = ppid;
            this.name = name;
        }

        public String getUser() {
            return user;
        }

        public int getPid() {
            return pid;
        }

        public int getPpid() {
            return ppid;
        }

        public String getName() {
            return name;
        }
    }

    public interface PSResultHandler {
        public abstract void onResult(PSResult result);

        public abstract void onException(Exception ex);
    }
}
