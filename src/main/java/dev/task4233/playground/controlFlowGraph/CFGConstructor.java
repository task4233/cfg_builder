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
import soot.Scene;
import soot.SootMethod;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.collect.ConcurrentHashSet;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;

public class CFGConstructor implements Callable<ReturnedValue> {
    // private final static long apiSizeThreshold = 10101010;
    private final static int randomWalkThreshold = 10101;

    private final static String USER_HOME = System.getProperty("user.home");
    private final static String androidJar = System.getenv().containsKey("ANDROID_HOME")
            ? System.getenv("ANDROID_HOME") + File.separator + "platforms"
            : USER_HOME + "/Library/Android/sdk/platforms";
    private int idx = 0;
    private File apk = null;
    // family=> 0:benign, 1>=:malicious(might be expanded)
    // TODO: use enum
    private int family = 0;

    private Map<String, Integer> apiFreq = new ConcurrentHashMap<>();
    private SetupApplication app = null;
    private CallGraph callGraph = null;
    private CallgraphAlgorithm cgAlgorithm = CallgraphAlgorithm.SPARK;
    private Random rnd = new Random();
    private List<String> apiSequence = new LinkedList<>();
    private Set<String> randomWalkCache = new ConcurrentHashSet<>();

    public CFGConstructor(int idx, File apk, int family) {
        this.idx = idx;
        this.apk = apk;
        this.family = family;
    }

    @Override
    public ReturnedValue call() {
        if (!this.init()) {
            System.out.printf("failed to make CFG: %s\n", this.apk.getName());
            return new ReturnedValue(this.idx, this.family, this.apiFreq, this.apiSequence);
        }
        this.constructCFG();
        this.justifyCFG();
        this.genApiSequence();
        System.out.println("---");

        return new ReturnedValue(this.idx, this.family, this.apiFreq, this.apiSequence);
    }

    // constructCFG constructs CallFlowGraph with given index
    private void constructCFG() {
        System.out.printf("[%d] start constructCFG for %s\n", this.idx, this.apk.getName());
        long startTime = System.currentTimeMillis();

        this.traverseMethod(app.getDummyMainMethod());

        // ready callgraph
        // Set<SootClass> entrypointSet = app.getEntrypointClasses();
        // System.out.printf("callgraph done for %s\n", this.apk.getName());

        // dig callgraph
        // for (SootClass sootClass : entrypointSet) {
        // // System.out.println(String.format("Class %d: %s", ++classIdx,
        // // sootClass.getName()));

        // for (SootMethod sootMethod : sootClass.getMethods()) {
        // // skip methods except entrypointMethod
        // if (!sootMethod.getName().equals(entrypointMethod)) {
        // continue;
        // }
        // System.out.printf("constructCFG entrypoint: %s\n",
        // sootMethod.getSignature());
        // this.traverseMethod(sootMethod);

        // // int incomingEdge = 0;
        // // for (Iterator<Edge> it = callGraph.edgesInto(sootMethod); it.hasNext();
        // it.next()) {
        // // ++incomingEdge;
        // // }
        // // int outgoingEdge = 0;
        // // for (Iterator<Edge> it = callGraph.edgesOutOf(sootMethod); it.hasNext();)
        // {
        // // Edge edge = it.next();
        // // this.traverseMethod(edge.tgt());
        // // ++outgoingEdge;
        // // }

        // // System.out.println(String.format("\tMethod %s, #IncomingEdges: %d,
        // // #OutgoingEdges: %d",
        // // sootMethod.getName(), incomingEdge, outgoingEdge));
        // }
        // }

        long endTime = System.currentTimeMillis();
        System.out.printf("[%d] done constructCFG in %d[ms]\n", this.idx, endTime - startTime);
    }

    private void genApiSequence() {
        System.out.printf("[%d] start getApiSequence for %s\n", idx, this.apk.getName());
        long startTime = System.currentTimeMillis();

        // for (SootClass sootClass : entrypointSet) {
        // for (SootMethod sootMethod : sootClass.getMethods()) {
        // if (!sootMethod.getName().equals(entrypointMethod)) {
        // continue;
        // }
        // System.out.printf("randomwalk entrypoint: %s\n", app.getDummyMainMethod().getSignature());
        this.randomWalk(app.getDummyMainMethod(), 0);
        // this.randomWalk(sootMethod, 0);
        // }
        // }

        long endTime = System.currentTimeMillis();
        System.out.printf("[%d] done getApiSequence in %d[ms]\n\n", idx, endTime - startTime);
    }

    private boolean init() {
        System.out.printf("[%d] start init for %s\n", idx, this.apk.getName());

        final InfoflowAndroidConfiguration config = AndroidUtil.getFlowDroidConfig(
                this.apk.getAbsolutePath(), androidJar, this.cgAlgorithm);
        // System.out.printf("config done for %s\n", this.apk.getName());

        // construct cfg without executing taint analysis
        try {
            this.app = new SetupApplication(config);
            this.app.constructCallgraph(); // heavy
            this.callGraph = Scene.v().getCallGraph();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        
        System.out.printf("[%d] done init for %s\n", idx, this.apk.getName());

        return true;
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
        // System.out.printf("now: %s\n", now.getSignature());

        boolean isUserDefinedAPI = now.getSignature().startsWith("<com");
        this.randomWalkCache.add(now.getSignature());
        if (!isUserDefinedAPI) {
            this.apiSequence.add(now.getSignature());
        }

        Iterator<Edge> inIt = callGraph.edgesInto(now);
        Iterator<Edge> outIt = callGraph.edgesOutOf(now);
        int inSize = Iterators.size(inIt);
        int outSize = Iterators.size(outIt);
        // System.out.printf("[%d] %s, in: %d, out: %d\n", count, now.getSignature(),
        // inSize, outSize);

        inIt = callGraph.edgesInto(now);
        outIt = callGraph.edgesOutOf(now);
        if (!inIt.hasNext() && !outIt.hasNext()) {
            System.out.printf("in & out edge is not found");
            return;
        }

        // 0->backward, 1->forward
        int choice = this.rnd.nextInt(2);
        if (!inIt.hasNext() || now.getName().equals(app.getDummyMainMethod().getName())) {
            choice = 1;
        }
        if (!outIt.hasNext()) {
            choice = 0;
            boolean canBackward = false;
            for (inIt = callGraph.edgesInto(now); inIt.hasNext();) {
                Edge e = inIt.next();
                canBackward |= randomWalkCache.contains(e.src().getSignature());
                // System.out.printf("[<-|] src: %s, tgt: %s\n", e.src().getSignature(), e.tgt().getSignature());
            }
            if (!canBackward) {
                System.out.println("failed backward");
                return;
            }
        }

        // omit because it might be same as callGraph.edgesOutOf(now)
        // if (now.hasActiveBody()) {
        // Body body = now.getActiveBody();
        // for (Unit u : body.getUnits()) {
        // Stmt stmt = (Stmt) u;
        // if (stmt.containsInvokeExpr()) {
        // InvokeExpr expr = stmt.getInvokeExpr();
        // System.out.println(String.format("\t\texpr = %s", expr.toString()));
        // }
        // }
        // }

        int nextIdx;
        Edge next = null;
        switch (choice) {
            case 0:
                // backward
                do {
                    nextIdx = this.rnd.nextInt(inSize);
                    // System.out.printf("[0] %s, next: %d, size: %d\n", now.getSignature(),
                    //    nextIdx, inSize);
                    inIt = callGraph.edgesInto(now);
                    for (int idx=0; idx<nextIdx+1; ++idx) {
                        next = inIt.next();
                    }
                } while (next == null || next.src() == null || !randomWalkCache.contains(next.src().getSignature()));
                // System.out.printf("[<- ] src: %s, tgt: %s\n", next.src().getSignature(), next.tgt().getSignature());
                randomWalk(next.src(), count + (isUserDefinedAPI ? 0 : 1));
                return;
            case 1:
                // forward
                do {
                    nextIdx = this.rnd.nextInt(outSize);
                    // System.out.printf("[1] %s, next: %d, size: %d\n", now.getSignature(),
                    // nextIdx, outSize);
                    outIt = callGraph.edgesOutOf(now);
                    for (int idx=0; idx<nextIdx+1; ++idx) {
                        next = outIt.next();
                    }
                } while (next == null || next.tgt() == null);
                // System.out.printf("[-> ] src: %s, tgt: %s\n", next.src().getSignature(), next.tgt().getSignature());
                randomWalk(next.tgt(), count + (isUserDefinedAPI ? 0 : 1));
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
        for (Iterator<Edge> it = callGraph.edgesOutOf(now); it.hasNext();) {
            Edge next = it.next();
            if (next.tgt() == null)
                continue;
            traverseMethod(next.tgt());
        }

        // if (!now.hasActiveBody())
        // return null;
        // Body body = now.getActiveBody();
        // for (Unit u : body.getUnits()) {
        // Stmt stmt = (Stmt) u;
        // if (stmt.containsInvokeExpr()) {
        // InvokeExpr expr = stmt.getInvokeExpr();
        // // System.out.println(String.format("\t\texpr = %s", expr.toString()));
        // traverseMethod(expr.getMethod());
        // }
        // }
        return null;
    }
}
