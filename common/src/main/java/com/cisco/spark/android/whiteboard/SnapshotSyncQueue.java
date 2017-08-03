package com.cisco.spark.android.whiteboard;

import com.github.benoitdion.ln.Ln;

import java.util.concurrent.LinkedBlockingQueue;


public class SnapshotSyncQueue {
    private LinkedBlockingQueue<SnapshotRequest> snapshotRequestsQ = new LinkedBlockingQueue();

    public void add(SnapshotRequest request) {
        snapshotRequestsQ.add(request);
    }

    public SnapshotRequest poll() {
        SnapshotRequest request = snapshotRequestsQ.poll();
        if (request != null && request.isTimeOutRequest()) {
            Ln.i("SnapshotSyncQueue: This request timeout over " + SnapshotRequest.TIMEOUT_DURATION + " seconds.");
            Ln.i("SnapshotSyncQueue: This request " + request.getRequestId() + " has been removed due to timeout.");
            poll();
        }
        return request;
    }

    public void clear() {
        snapshotRequestsQ.clear();
    }

    public int size() {
        return snapshotRequestsQ.size();
    }
}

