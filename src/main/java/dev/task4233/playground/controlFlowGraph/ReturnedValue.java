package dev.task4233.playground.controlFlowGraph;

import java.util.List;
import java.util.Map;

public class ReturnedValue {
    private int idx = 0;
    private int family = 0;
    private Map<String, Integer> apiFreq = null;
    private List<String> apiSequece = null;

    public ReturnedValue(int idx, int family, Map<String, Integer> apiFreq, List<String> apiSequece) {
        this.idx = idx;
        this.family = family;
        this.apiFreq = apiFreq;
        this.apiSequece = apiSequece;
    }

    public int getIdx() {
        return this.idx;
    }

    public int getFamily() {
        return this.family;
    }

    public Map<String, Integer> getApiFreq() {
        return this.apiFreq;
    }

    public List<String> getApiSequence() {
        return this.apiSequece;
    }
}
