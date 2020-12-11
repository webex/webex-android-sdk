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

package com.ciscowebex.androidsdk.utils;

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
            if (exitCode != 0) {
                Ln.w("ps command failed to list duplicate instances (exit code=%d)", exitCode);
            }
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
        void onResult(PSResult result);

        void onException(Exception ex);
    }
}
