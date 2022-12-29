package dev.task4233.playground.controlFlowGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.task4233.playground.utils.AndroidCallGraphFilter;
import dev.task4233.playground.utils.AndroidUtil;
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
    private List<Map<String, Integer>> apiFreqs = new ArrayList<>();
    private List<List<String>> apiSequences = new ArrayList<>();
    private List<String> allApis = null;


    private String apkPath = System.getProperty("user.dir") + File.separator + "samples";
    private File[] apks = null;

    public CFG() {
        // collect apks
        this.apks = this.collectApks(this.apkPath);
        for (int idx = 0; idx < apks.length; ++idx) {
            apiFreqs.add(new ConcurrentHashMap<String, Integer>());
            apiSequences.add(new ArrayList<>());
        }
    }

    // collectApks collects <= 4MB .apk files
    private File[] collectApks(String apkDirPath) {
        File dir = new File(apkDirPath);
        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File file, String fileName) {
                // ignore not endwith .apk and > 4MB
                if (!fileName.endsWith(".apk")) {
                    return false;
                }
                // if (file.length() > apiSizeThreshold) {
                //     return false;
                // }

                return true;
            }
        };
        return dir.listFiles(filter);
    }

    // constructCFG constructs CallFlowGraph and write the result as JSON
    public void constructCFG() {
        List<CFGConstructor> jobs = new ArrayList<>();
        for (int idx = 0; idx < apks.length; ++idx) {
            jobs.add(new CFGConstructor(idx, apks[idx]));
        }
        jobs.stream().forEach(job -> {
            ReturnedValue res = job.call();
            this.apiFreqs.set(res.getIdx(), res.getApiFreq());
            this.apiSequences.set(res.getIdx(), res.getApiSequence());
        });

        this.deriveAllApis();
        this.writeJSON();
    }

    // deriveAllApis derives all system-apis with gathering sum of sets
    private void deriveAllApis() {
        Set<String> allApiSet = new HashSet<>();
        for (int idx=0; idx<apiFreqs.size(); ++idx) {
            allApiSet.addAll(apiFreqs.get(idx).keySet());
        }
        this.allApis = new ArrayList<>(allApiSet);
    }

    private void writeJSON() {
        try {
            // TODO: make a new func for meeting the following flow
            // because these operations are duplicated
            ObjectMapper objectMapper = new ObjectMapper();

            // write apiFreq
            writeJSONWithFileName(objectMapper, "./output/apiFreq.json", apiFreqs);
            // write allApiSets
            writeJSONWithFileName(objectMapper, "./output/allApis.json", allApis);
            // write apiSequences
            writeJSONWithFileName(objectMapper, "./output/apiSequece.json", apiSequences);
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
