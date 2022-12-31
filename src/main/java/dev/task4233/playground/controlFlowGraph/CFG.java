package dev.task4233.playground.controlFlowGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fj.F;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import com.fasterxml.jackson.databind.ObjectMapper;

public class CFG {
    // output file names
    private final static String outputDirPath = "output" + File.separator;
    public final static String allApisFilePath = outputDirPath + "allApis.json";
    public final static String apiFrequenciesFilePath = outputDirPath + "apiFrequencies.json";
    public final static String apiSequenceIndicesFilePath = outputDirPath + "apiSequenceIndices.json";
    public final static String apiSequencesFilePath = outputDirPath + "apiSequences.json";

    private List<Map<String, Integer>> apiFreqs = new ArrayList<>();
    private List<List<String>> apiSequences = new ArrayList<>();
    private List<List<Integer>> apiSequenceIndices = new ArrayList();
    private Map<String, Integer> allApis = new LinkedHashMap<>();

    private String apkPath = System.getProperty("user.dir") + File.separator + "samples";
    // for identifying dataset family
    private final static String familyKey = "a05cba79-d480-44ad-8a41-1447d544d1b";
    private File[] benignApks = null;
    private File[] maliciousApks = null;

    public CFG() {
        // collect apks
        this.benignApks = this.collectApks(this.apkPath + File.separator + "benign");
        this.maliciousApks = this.collectApks(this.apkPath + File.separator + "malicious");
        for (int idx = 0; idx < benignApks.length + maliciousApks.length; ++idx) {
            apiFreqs.add(new ConcurrentHashMap<String, Integer>());
            apiSequences.add(new ArrayList<>());
            apiSequenceIndices.add(new ArrayList<>());
        }
    }

    // collectApks collects <= 4MB .apk files
    private File[] collectApks(String apkDirPath) {
        File dir = new File(apkDirPath);
        // unneeded because divided apks and others by changed directory structure
        // FilenameFilter filter = new FilenameFilter() {
        // public boolean accept(File file, String fileName) {
        // // ignore not endwith .apk and > 4MB
        // // if (!fileName.endsWith(".apk")) {
        // // return false;
        // // }
        // // if (file.length() > apiSizeThreshold) {
        // // return false;
        // // }

        // return true;
        // }
        // };
        // return dir.listFiles(filter);
        return dir.listFiles();
    }

    // constructCFG constructs CallFlowGraph and write the result as JSON
    public void constructCFG() {
        List<CFGConstructor> jobs = new ArrayList<>();
        for (int idx = 0; idx < benignApks.length; ++idx) {
            jobs.add(new CFGConstructor(idx, benignApks[idx], 0));
        }
        for (int idx = 0; idx  < maliciousApks.length; ++idx) {
            jobs.add(new CFGConstructor(idx, maliciousApks[idx], 1));
        }
        jobs.stream().forEach(job -> {
            ReturnedValue res = job.call();
            Map<String, Integer> apiFreq = res.getApiFreq();
            apiFreq.put(familyKey, res.getFamily());
            this.apiFreqs.set(res.getIdx(), apiFreq);

            List<String> apiSequence = res.getApiSequence();
            apiSequence.add(String.valueOf(res.getFamily()));
            this.apiSequences.set(res.getIdx(), apiSequence);
        });

        this.deriveAllApis();
        this.convertSignatureToIndexInSequence();
        this.writeJSON();
    }

    public void convertSignatureToIndexInSequence() {
        // signature -> index with allApis info
        for (int idx = 0; idx < apiSequences.size(); ++idx) {
            List<String> apiSequence = apiSequences.get(idx);
            List<Integer> apiSequenceIndex = new LinkedList<>();

            for (int idxJ = 0; idxJ < apiSequence.size(); ++idxJ) {
                if (idxJ == apiSequence.size()-1) {
                    apiSequenceIndex.add(Integer.parseInt(apiSequence.get(idxJ)));
                } else {
                    apiSequenceIndex.add(this.allApis.get(apiSequence.get(idxJ)));
                }
            }

            this.apiSequenceIndices.set(idx, apiSequenceIndex);
        }
    }

    // deriveAllApis derives all system-apis with gathering sum of sets
    private void deriveAllApis() {
        Set<String> allApiSet = new HashSet<>();
        for (int idx = 0; idx < apiFreqs.size(); ++idx) {
            allApiSet.addAll(apiFreqs.get(idx).keySet());
        }
        int idx = 0;
        for (String api : allApiSet) {
            if (api.equals(familyKey)) {
                continue;
            }
            this.allApis.put(api, idx++);
        }
    }

    private void writeJSON() {
        try {
            // TODO: make a new func for meeting the following flow
            // because these operations are duplicated
            ObjectMapper objectMapper = new ObjectMapper();

            // write apiFreq
            writeJSONWithFileName(objectMapper, apiFrequenciesFilePath, apiFreqs);
            // write allApiSets
            writeJSONWithFileName(objectMapper, allApisFilePath, allApis);

            // NOTE: As this log file is easilly big, commented out
            // write apiSequences
            // writeJSONWithFileName(objectMapper, apiSequencesFilePath, apiSequences);

            // write apiSequences coverted with index
            writeJSONWithFileName(objectMapper, apiSequenceIndicesFilePath, apiSequenceIndices);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void writeJSONWithFileName(ObjectMapper mapper, String dstFilePath, Object writtenData) throws Exception {
        String jsonAsString = mapper.writeValueAsString(writtenData);
        File file = new File(dstFilePath);
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.write(jsonAsString);
        fileWriter.close();
    }
}
