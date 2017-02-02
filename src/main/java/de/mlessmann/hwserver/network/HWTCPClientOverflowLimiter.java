package de.mlessmann.hwserver.network;

import de.mlessmann.hwserver.main.HWServer;

/**
 * Created by Life4YourGames on 06.11.16.
 */
public class HWTCPClientOverflowLimiter {

    private HWTCPClientHandler handler;
    private HWServer server;
    private int maxReqPerSec;
    private final ObjectHolder<Integer> holder = new ObjectHolder<Integer>();
    private MyRunnable runnable;

    public HWTCPClientOverflowLimiter(HWTCPClientHandler handler, HWServer server) {
        this.handler = handler;
        this.server = server;
    }

    public void start() {
        maxReqPerSec = server.getConfig().getNode("limit", "requestsPerSecond").optInt(10);
        synchronized (holder) {
            holder.setPayload(maxReqPerSec);
        }
        if (runnable == null) {
            runnable = new MyRunnable(holder, maxReqPerSec);
            new Thread(runnable).start();
        } else {
            runnable.terminate();
            runnable = new MyRunnable(holder, maxReqPerSec);
            new Thread(runnable).start();
        }
    }

    public void stop() {
        runnable.terminate();
    }

    public int newReq() {
        int current;
        synchronized (holder) {
            current = holder.getPayload() - 1;
            holder.setPayload(current);
        }
        int i = 0;
        if (current <= ((maxReqPerSec*2)*(-1))) {
            i = 2;
        } else if (current <= 0) {
            i = 1;
        }
        return i;
    }

    private class ObjectHolder<T> {

        T payload;

        void setPayload(T Obj) {
            payload = Obj;
        }

        T getPayload() {
            return payload;
        }

    }

    private class MyRunnable implements Runnable {

        private ObjectHolder<Integer> rHolder;
        private int max;
        private boolean terminate = false;

        public MyRunnable(ObjectHolder<Integer> holder, int max) {
            this.rHolder = holder;
            this.max = max;
        }

        @Override
        public void run() {
            while (!terminate) {
                synchronized (rHolder) {
                    int current = holder.getPayload();
                    if (current < max)
                        holder.setPayload(++current);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    //
                }
            }
        }

        synchronized void terminate() {
            terminate = true;
        }
    }

}
