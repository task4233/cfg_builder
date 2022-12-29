package dev.task4233.playground.controlFlowGraph;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import dev.task4233.playground.utils.AndroidUtil;
import soot.Body;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class CFGConstructor implements Callable<ReturnedValue> {
    // private final static long apiSizeThreshold = 10101010;
    private final static String USER_HOME = System.getProperty("user.home");
    private final static String androidJar = System.getenv().containsKey("ANDROID_HOME")
            ? System.getenv("ANDROID_HOME") + File.separator + "platforms"
            : USER_HOME + "/Library/Android/sdk/platforms";
    private final String entrypointMethod = "onCreate";
    private int idx = 0;
    private File apk = null;
    private Map<String, Integer> apiFreq = new ConcurrentHashMap<>();

    private CallgraphAlgorithm cgAlgorithm = CallgraphAlgorithm.SPARK;

    public CFGConstructor(int idx, File apk) {
        this.idx = idx;
        this.apk = apk;
    }

    @Override
    public ReturnedValue call() {
        this.constructCFG();
        this.justifyCFG();

        return new ReturnedValue(this.idx, this.apiFreq);
    }

    // constructCFG constructs CallFlowGraph with given index
    private void constructCFG() {
        System.out.printf("start for %s\n", this.apk.getName());

        long startTime = System.currentTimeMillis();

        final InfoflowAndroidConfiguration config = AndroidUtil.getFlowDroidConfig(
                this.apk.getAbsolutePath(), androidJar, this.cgAlgorithm);
        // System.out.printf("config done for %s\n", this.apk.getName());

        // construct cfg without executing taint analysis
        SetupApplication app = new SetupApplication(config);
        // System.out.println("setup done");
        app.constructCallgraph(); // heavy
        // System.out.printf("construction done for %s\n", this.apk.getName());

        // ready callgraph
        CallGraph callGraph = Scene.v().getCallGraph();
        Set<SootClass> entrypointSet = app.getEntrypointClasses();
        // System.out.printf("callgraph done for %s\n", this.apk.getName());

        // dig callgraph
        for (SootClass sootClass : entrypointSet) {
            // System.out.println(String.format("Class %d: %s", ++classIdx,
            // sootClass.getName()));

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
                    this.traverseMethod(edge.tgt());
                    ++outgoingEdge;
                }

                // System.out.println(String.format("\tMethod %s, #IncomingEdges: %d,
                // #OutgoingEdges: %d",
                // sootMethod.getName(), incomingEdge, outgoingEdge));
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.printf("done in %d[ms]\n\n", endTime - startTime);
    }

    // justifyCFG remove user-defined apis from apiFreqMap
    private void justifyCFG() {
        for (Map.Entry<String, Integer> freq : this.apiFreq.entrySet()) {
            if (freq.getKey().startsWith("<com")) {
                this.apiFreq.remove(freq.getKey());
                continue;
            }
            // System.out.printf("%s: %d\n", freq.getKey(), freq.getValue());
        }
    }

    // traverseMethod dig with given now method
    private SootMethod traverseMethod(SootMethod now) {
        if (now == null) {
            return null;
        }

        // skip traverse if it's reached
        String signature = now.getSignature();
        if (this.apiFreq.containsKey(signature)) {
            this.apiFreq.put(signature, this.apiFreq.get(signature) + 1);
            return null;
        }
        this.apiFreq.put(signature, 1);

        // ignore user-defined method
        // TODO: might be good enhance this filter
        // if (!signature.startsWith("<com")) {
        // System.out.println(String.format("%s is called", signature));
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
                traverseMethod(expr.getMethod());
            }
        }
        return null;
    }
}
