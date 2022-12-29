package dev.task4233.playground.controlFlowGraph;

import java.util.List;
import java.util.Map;

public class ReturnedValue {
    private int idx = 0;
    private Map<String, Integer> apiFreq = null;
    private List<String> apiSequece = null;

    public ReturnedValue(int idx, Map<String, Integer> apiFreq, List<String> apiSequece) {
        this.idx = idx;
        this.apiFreq = apiFreq;
        this.apiSequece = apiSequece;
    }

    public int getIdx() {
        return this.idx;
    }

    public Map<String, Integer> getApiFreq() {
        return this.apiFreq;
    }

    public List<String> getApiSequence() {
        return this.apiSequece;
    }
}
