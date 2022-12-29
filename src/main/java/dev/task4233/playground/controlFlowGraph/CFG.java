package dev.task4233.playground.controlFlowGraph;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
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
    private final String entrypointMethod = "onCreate";
    // private final static long apiSizeThreshold = 10101010;

    private final static String USER_HOME = System.getProperty("user.home");
    private String androidJar = USER_HOME + "/Library/Android/sdk/platforms";
    private CallgraphAlgorithm cgAlgorithm = InfoflowConfiguration.CallgraphAlgorithm.SPARK;
    private List<Map<String, Integer>> apiFreqs = new ArrayList<>();
    private List<String> allApis = null;

    private String apkPath = System.getProperty("user.dir") + File.separator + "samples";
    private File[] apks = null;

    public CFG() {
        // update android_home
        if (System.getenv().containsKey("ANDROID_HOME")) {
            this.androidJar = System.getenv("ANDROID_HOME") + File.separator + "platforms";
        }

        // collect apks
        this.apks = this.collectApks(this.apkPath);
        for (int idx = 0; idx < apks.length; ++idx) {
            apiFreqs.add(new ConcurrentHashMap<String, Integer>());
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
        for (int idx = 0; idx < apks.length; ++idx) {
            this.constructCFGWithIndex(idx);
            this.justifyCFGWithIndex(idx);
        }

        this.deriveAllApis();
        this.writeJSON();
    }

    // constructCFGWithIndex constructs CallFlowGraph with given index
    private void constructCFGWithIndex(int idx) {
        System.out.printf("start for %s\n", this.apks[idx].getName());

        long startTime = System.currentTimeMillis();

        final InfoflowAndroidConfiguration config = AndroidUtil.getFlowDroidConfig(
                this.apks[idx].getAbsolutePath(), this.androidJar, this.cgAlgorithm);
        System.out.println("config done");

        // construct cfg without executing taint analysis
        SetupApplication app = new SetupApplication(config);
        System.out.println("setup done");
        app.constructCallgraph(); // heavy
        System.out.println("construction done");

        // ready callgraph
        CallGraph callGraph = Scene.v().getCallGraph();
        Set<SootClass> entrypointSet = app.getEntrypointClasses();
        System.out.println("callgraph done");

        // dig callgraph
        for (SootClass sootClass : entrypointSet) {
            // System.out.println(String.format("Class %d: %s", ++classIdx, sootClass.getName()));

            for (SootMethod sootMethod : sootClass.getMethods()) {
                // skip methods except entrypointMethod
                if (!sootMethod.getName().equals(entrypointMethod)) {
                    continue;
                }

                int incomingEdge = 0;
                for (Iterator<Edge> it = callGraph.edgesInto(sootMethod); it.hasNext(); it.next()) {
                    ++incomingEdge;
                }
                int outgoingEdge = 0;
                for (Iterator<Edge> it = callGraph.edgesOutOf(sootMethod); it.hasNext();) {
                    Edge edge = it.next();
                    this.traverseMethod(idx, edge.tgt());
                    ++outgoingEdge;
                }

                // System.out.println(String.format("\tMethod %s, #IncomingEdges: %d, #OutgoingEdges: %d",
                //         sootMethod.getName(), incomingEdge, outgoingEdge));
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.printf("done in %d[ms]\n\n", endTime-startTime);
    }

    // deriveAllApis derives all system-apis with gathering sum of sets
    private void deriveAllApis() {
        Set<String> allApiSet = new HashSet<>();
        for (int idx=0; idx<apiFreqs.size(); ++idx) {
            allApiSet.addAll(apiFreqs.get(idx).keySet());
        }
        this.allApis = new ArrayList<>(allApiSet);
    }

    // justifyCFGWithIndex remove user-defined apis from apiFreqMap
    private void justifyCFGWithIndex(int idx) {
        Map<String, Integer> apiFreq = apiFreqs.get(idx);

        for (Map.Entry<String, Integer> freq : apiFreq.entrySet()) {
            if (freq.getKey().startsWith("<com")) {
                apiFreq.remove(freq.getKey());
                continue;
            }
            // System.out.printf("%s: %d\n", freq.getKey(), freq.getValue());
        }
    }

    // traverseMethod dig with given now method
    private SootMethod traverseMethod(int idx, SootMethod now) {
        if (now == null) {
            return null;
        }

        Map<String, Integer> apiFreq = apiFreqs.get(idx);

        // skip traverse if it's reached
        String signature = now.getSignature();
        if (apiFreq.containsKey(signature)) {
            apiFreq.put(signature, apiFreq.get(signature) + 1);
            return null;
        }
        apiFreq.put(signature, 1);

        // ignore user-defined method
        // TODO: might be good enhance this filter
        // if (!signature.startsWith("<com")) {
        //     System.out.println(String.format("%s is called", signature));
        // }

        // check method content
        if (!now.hasActiveBody())
            return null;
        Body body = now.getActiveBody();
        for (Unit u : body.getUnits()) {
            Stmt stmt = (Stmt) u;
            if (stmt.containsInvokeExpr()) {
                InvokeExpr expr = stmt.getInvokeExpr();
                // System.out.println(String.format("\t\texpr = %s", expr.toString()));
                traverseMethod(idx, expr.getMethod());
            }
        }
        return null;
    }

    private void writeJSON() {
        File file = null;
        FileWriter fileWriter = null;
        try {
            // TODO: make a new func for meeting the following flow
            // because these operations are duplicated

            // write apiFreq
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonAsString = objectMapper.writeValueAsString(apiFreqs);
            file = new File("./output/apiFreq.json");
            fileWriter = new FileWriter(file);
            fileWriter.write(jsonAsString);
            fileWriter.close();

            // write allApiSets
            objectMapper = new ObjectMapper();
            jsonAsString = objectMapper.writeValueAsString(allApis);
            file = new File("./output/allApis.json");
            fileWriter = new FileWriter(file);
            fileWriter.write(jsonAsString);
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
