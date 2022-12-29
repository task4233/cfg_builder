package dev.task4233.playground.controlFlowGraph;

import java.util.Map;

public class ReturnedValue {
    private int idx = 0;
    private Map<String, Integer> apiFreq = null;

    public ReturnedValue(int idx, Map<String, Integer> apiFreq) {
        this.idx = idx;
        this.apiFreq = apiFreq;
    }

    public int getIdx() {
        return this.idx;
    }

    public Map<String, Integer> getApiFreq() {
        return this.apiFreq;
    }
}
