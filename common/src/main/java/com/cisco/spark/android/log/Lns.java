package com.cisco.spark.android.log;

import com.github.benoitdion.ln.Ln;
import com.github.benoitdion.ln.NaturalLog;

public class Lns {
    private static final String CONTENT = "Content";
    private static final String UXTRACE = "UXTrace";
    private static final String MERCURY = "MercuryTrace";
    private static final String CALLFLOW = "CallFlowTrace";
    private static final String APPLICATION = "ApplicationController";

    private static NaturalLog contentLn;
    private static NaturalLog uxLn;
    private static NaturalLog mercuryLn;
    private static NaturalLog appLn;
    private static NaturalLog cftLn;
    private static CallFlowTrace callFlowTrace;


    public static NaturalLog content() {
        if (contentLn == null)
            contentLn = Ln.get(CONTENT);
        return contentLn;
    }

    public static NaturalLog ux() {
        if (uxLn == null)
            uxLn = Ln.get(UXTRACE);
        return uxLn;
    }

    public static NaturalLog mercury() {
        if (mercuryLn == null)
            mercuryLn = Ln.get(MERCURY);
        return mercuryLn;
    }

    public static NaturalLog application() {
        if (appLn == null)
            appLn = Ln.get(APPLICATION);
        return appLn;
    }

    public static NaturalLog cft() {
        if (cftLn == null)
            cftLn = Ln.get(CALLFLOW);
        return cftLn;
    }


    public static CallFlowTrace callFlow() {
        if (callFlowTrace == null)
            callFlowTrace = new CallFlowTrace();
        return callFlowTrace;
    }

    public static class CallFlowTrace {

        public static StateDiagramContract i = new StateDiagramContract() {
            @Override
            public void message(String from, String to, String message) {
                cft().i("%s->%s: %s", from, to, message);
            }
            @Override
            public void message(String from, String to) {
                cft().i("%s->%s", from, to);
            }
        };

        public static StateDiagramContract w = new StateDiagramContract() {
            @Override
            public void message(String from, String to, String message) {
                cft().w("%s->%s: %s", from, to, message);
            }
            @Override
            public void message(String from, String to) {
                cft().w("%s->%s", from, to);
            }
        };
    }

    public interface StateDiagramContract {
        void message(String from, String to, String message);
        void message(String from, String to);
    }

}
