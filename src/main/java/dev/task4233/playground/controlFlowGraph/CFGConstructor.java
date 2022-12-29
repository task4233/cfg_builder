package dev.task4233.playground.controlFlowGraph;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.Iterators;

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
    private final static int randomWalkThreshold = 10101;

    private final static String USER_HOME = System.getProperty("user.home");
    private final static String androidJar = System.getenv().containsKey("ANDROID_HOME")
            ? System.getenv("ANDROID_HOME") + File.separator + "platforms"
            : USER_HOME + "/Library/Android/sdk/platforms";
    private final String entrypointMethod = "onCreate";
    private int idx = 0;
    private File apk = null;
    private Map<String, Integer> apiFreq = new ConcurrentHashMap<>();

    private SetupApplication app = null;
    private CallGraph callGraph = null;
    private Set<SootClass> entrypointSet = null;
    private CallgraphAlgorithm cgAlgorithm = CallgraphAlgorithm.SPARK;
    private Random rnd = new Random();
    private List<String> apiSequence = new LinkedList<>();

    public CFGConstructor(int idx, File apk) {
        this.idx = idx;
        this.apk = apk;
    }

    @Override
    public ReturnedValue call() {
        this.init();
        this.constructCFG();
        this.justifyCFG();
        this.genApiSequence();

        return new ReturnedValue(this.idx, this.apiFreq, this.apiSequence);
    }

    // constructCFG constructs CallFlowGraph with given index
    private void constructCFG() {
        System.out.printf("start constructCFG for %s\n", this.apk.getName());
        long startTime = System.currentTimeMillis();

        // ready callgraph
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
        System.out.printf("done constructCFG in %d[ms]\n\n", endTime - startTime);
    }

    private void genApiSequence() {
        System.out.printf("start getApiSequence for %s\n", this.apk.getName());
        long startTime = System.currentTimeMillis();

        for (SootClass sootClass : entrypointSet) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (!sootMethod.getName().equals(entrypointMethod)) {
                    continue;
                }
                this.randomWalk(sootMethod, 0);
            }
        }

        long endTime = System.currentTimeMillis();
        System.out.printf("done getApiSequence in %d[ms]\n\n", endTime - startTime);
    }

    private void init() {
        System.out.printf("start init for %s\n", this.apk.getName());

        final InfoflowAndroidConfiguration config = AndroidUtil.getFlowDroidConfig(
                this.apk.getAbsolutePath(), androidJar, this.cgAlgorithm);
        // System.out.printf("config done for %s\n", this.apk.getName());

        // construct cfg without executing taint analysis
        this.app = new SetupApplication(config);
        this.app.constructCallgraph(); // heavy
        this.callGraph = Scene.v().getCallGraph();
        this.entrypointSet = this.app.getEntrypointClasses();

        System.out.printf("done init for %s\n", this.apk.getName());
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

    private void randomWalk(SootMethod now, int count) {
        if (now == null) {
            return;
        }
        if (count >= randomWalkThreshold) {
            return;
        }

        // boolean isUserDefinedAPI = now.getSignature().startsWith("<com");
        this.apiSequence.add(now.getSignature());

        Iterator<Edge> inIt = callGraph.edgesInto(now);
        Iterator<Edge> outIt = callGraph.edgesOutOf(now);
        int inSize = Iterators.size(inIt);
        int outSize = Iterators.size(outIt);
        // System.out.printf("[%d] %s, in: %d, out: %d\n", count, now.getSignature(), inSize, outSize);

        inIt = callGraph.edgesInto(now);
        outIt = callGraph.edgesOutOf(now);
        if (!inIt.hasNext() && !outIt.hasNext()) {
            System.out.printf("in & out edge is not found");
            return;
        }

        // 0->backward, 1->forward
        int choice = this.rnd.nextInt(2);
        if (!inIt.hasNext()) {
            choice = 1;
        }
        if (!outIt.hasNext()) {
            choice = 0;
        }

        // omit because it might be same as callGraph.edgesOutOf(now)
        // if (now.hasActiveBody()) {
        //     Body body = now.getActiveBody();
        //     for (Unit u : body.getUnits()) {
        //         Stmt stmt = (Stmt) u;
        //         if (stmt.containsInvokeExpr()) {
        //             InvokeExpr expr = stmt.getInvokeExpr();
        //             System.out.println(String.format("\t\texpr = %s", expr.toString()));
        //         }
        //     }
        // }

        int nextIdx;
        Edge next = null;
        switch (choice) {
            case 0:
                // backward
                do {
                    nextIdx = this.rnd.nextInt(inSize);
                    // System.out.printf("[0] %s, next: %d, size: %d\n", now.getSignature(), nextIdx, inSize);
                    inIt = callGraph.edgesInto(now);
                    if (nextIdx > 0) {
                        next = Iterators.get(inIt, nextIdx);
                    }
                    next = inIt.next();
                } while(next == null || next.src() == null);
                randomWalk(next.src(), count + 1);
                return;
            case 1:
                // forward
                do {
                    nextIdx = this.rnd.nextInt(outSize);
                    // System.out.printf("[1] %s, next: %d, size: %d\n", now.getSignature(), nextIdx, outSize);
                    outIt = callGraph.edgesOutOf(now);
                    if (nextIdx > 0) {
                        next = Iterators.get(outIt, nextIdx);
                    }
                    next = outIt.next();
                } while (next == null || next.tgt() == null);
                randomWalk(next.tgt(), count + 1);
                return;
            default:
                System.out.println("invalid choice");
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
        for (Iterator<Edge> it = callGraph.edgesOutOf(now); it.hasNext(); it.hasNext()) {
            Edge next = it.next();
            if (next.tgt() == null) continue;
            traverseMethod(next.tgt());
        }

        // if (!now.hasActiveBody())
        //     return null;
        // Body body = now.getActiveBody();
        // for (Unit u : body.getUnits()) {
        //     Stmt stmt = (Stmt) u;
        //     if (stmt.containsInvokeExpr()) {
        //         InvokeExpr expr = stmt.getInvokeExpr();
        //         // System.out.println(String.format("\t\texpr = %s", expr.toString()));
        //         traverseMethod(expr.getMethod());
        //     }
        // }
        return null;
    }
}
