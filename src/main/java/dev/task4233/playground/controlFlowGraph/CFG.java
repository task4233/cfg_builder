package dev.task4233.playground.controlFlowGraph;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

public class CFG {
    private String entrypointMethod = "onCreate";
    private final static String USER_HOME = System.getProperty("user.home");
    private String androidJar = USER_HOME + "/Library/Android/sdk/platforms";
    private CallgraphAlgorithm cgAlgorithm = InfoflowConfiguration.CallgraphAlgorithm.SPARK;
    private Set<String> cache = new HashSet<>();

    // TODO: coolecting all files in "samples directory"
    private String apkPath = System.getProperty("user.dir") + File.separator + "samples" + File.separator
            + "simple_calculator_14.apk";

    public CFG() {
        // update android_home
        if (System.getenv().containsKey("ANDROID_HOME")) {
            this.androidJar = System.getenv("ANDROID_HOME") + File.separator + "platforms";
        }
    }

    public void constructCFG() {
        // setup flowdroid
        final InfoflowAndroidConfiguration config = AndroidUtil.getFlowDroidConfig(this.apkPath, this.androidJar,
                this.cgAlgorithm);

        // construct cfg without executing taint analysis
        SetupApplication app = new SetupApplication(config);
        app.constructCallgraph();

        Set<String> userDefinedMethodSignatures = getUserDefinedMethodSignatures(this.apkPath);

        int classIdx = 0;
        CallGraph callGraph = Scene.v().getCallGraph();
        Set<SootClass> entrypointSet = app.getEntrypointClasses();

        for (SootClass sootClass : entrypointSet) {
            System.out.println(String.format("Class %d: %s", ++classIdx, sootClass.getName()));

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
                    traverseMethod(edge.tgt(), userDefinedMethodSignatures);
                    ++outgoingEdge;
                }

                System.out.println(String.format("\tMethod %s, #IncomingEdges: %d, #OutgoingEdges: %d",
                        sootMethod.getName(), incomingEdge, outgoingEdge));
            }
        }
    }

    private SootMethod traverseMethod(SootMethod now, Set<String> ignoredMethodSignatures) {
        if (now == null)
            return null;

        // ignore user-defined method
        // TODO: ignore user-defined method
        if (!now.getSignature().startsWith("<com")) {
            // skip traverse if it's reached
            if (!cache.add(now.getSignature())) {
                /// System.out.println(String.format("%s is skipped.", now.toString()));
                return null;
            }
            System.out.println(String.format("%s is called", now.getSignature()));
        }

        // check method content
        if (!now.hasActiveBody())
            return null;
        Body body = now.getActiveBody();
        for (Unit u : body.getUnits()) {
            Stmt stmt = (Stmt) u;
            if (stmt.containsInvokeExpr()) {
                InvokeExpr expr = stmt.getInvokeExpr();
                // System.out.println(String.format("\t\texpr = %s", expr.toString()));
                traverseMethod(expr.getMethod(), ignoredMethodSignatures);
            }

        }
        return null;
    }

    private Set<String> getUserDefinedMethodSignatures(String apkPath) {
        if (apkPath.isEmpty())
            return null;

        Set<String> signatures = new HashSet<>();

        List<SootClass> validClasses = new AndroidCallGraphFilter(AndroidUtil.getPackageName(apkPath))
                .getValidClasses();
        for (SootClass sootClass : validClasses) {
            for (SootMethod sootMethod : sootClass.getMethods()) {
                if (signatures.add(sootMethod.getSignature())) {
                    System.out.printf("%s is added\n", sootMethod.getSignature());
                }
            }
        }

        return signatures;
    }
}
