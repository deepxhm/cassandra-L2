package org.apache.cassandra.c3;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ScoreTracker
{
    // Cubic score for replica selection, updated on a per-request level
    private static final double ALPHA = 0.9;
    private double queueSizeEMA = 0;
    private double serviceTimeEMA = 0;
    private double responseTimeEMA = 0;

    //added for L2
    private double responseTimeNOEMA = 0;

    public synchronized void updateNodeScore(int queueSize, double serviceTime, double latency)
    {
        final double responseTime = latency - serviceTime;

        queueSizeEMA = getEMA(queueSize, queueSizeEMA);
        serviceTimeEMA = getEMA(serviceTime, serviceTimeEMA);
        responseTimeEMA = getEMA(responseTime, responseTimeEMA);
	responseTimeNOEMA = responseTime;
        assert serviceTime <= latency;
    }

    private synchronized double getEMA(double value, double previousEMA)
    {
        return ALPHA * value + (1 - ALPHA) * previousEMA;
    }

    public synchronized double getScore(ConcurrentHashMap<InetAddress, AtomicInteger> pendingRequests, InetAddress endpoint)
    {
        AtomicInteger counter = pendingRequests.get(endpoint);
        if (counter == null)
        {
            return 0.0;
        }

        // number of clients times the outstanding requests

        // changed for L2 
        //double concurrencyCompensation = pendingRequests.size() * counter.get();
        double OSKs = counter.get() * Math.pow(10,5);
        return responseTimeNOEMA + OSKs;
    }
}