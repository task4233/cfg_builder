package dev.task4233.playground.controlFlowGraph;

import java.io.File;
import java.util.Iterator;
import java.util.Set;

import dev.task4233.playground.utils.AndroidUtil;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.InfoflowConfiguration.CallgraphAlgorithm;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class CFG {
    private String entrypointMethod = "onCreate";
    private final static String USER_HOME = System.getProperty("user.home");
    private String androidJar = USER_HOME + "/Library/Android/sdk/platforms";
    private CallgraphAlgorithm cgAlgorithm = InfoflowConfiguration.CallgraphAlgorithm.SPARK;

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
        SetupApplication app = new SetupApplication(config);

        // Create the callgraph without executing taint analysis
        app.constructCallgraph();

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
                for (Iterator<Edge> it = callGraph.edgesOutOf(sootMethod); it.hasNext(); it.next()) {
                    ++outgoingEdge;
                }

                System.out.println(String.format("\tMethod %s, #IncomingEdges: %d, #OutgoingEdges: %d",
                        sootMethod.getName(), incomingEdge, outgoingEdge));
            }
        }
    }

    public static void main(String[] args) {

        // classIdx = 0;

        // // fix dependencies here
        // AndroidCallGraphFilter androidCallGraphFilter = new
        // AndroidCallGraphFilter(AndroidUtil.getPackageName(apkPath));
        // for (SootClass sootClass: androidCallGraphFilter.getValidClasses()) {
        // System.out.println(String.format("Class %d: %s", ++classIdx,
        // sootClass.getName()));

        // for (SootMethod sootMethod : sootClass.getMethods()) {
        // int incomingEdge = 0;
        // for (Iterator<Edge> it = callGraph.edgesInto(sootMethod); it.hasNext();
        // it.next()) {
        // ++incomingEdge;
        // }
        // int outgoingEdge = 0;
        // for (Iterator<Edge> it = callGraph.edgesOutOf(sootMethod); it.hasNext();
        // it.next()) {
        // ++outgoingEdge;
        // }

        // System.out.println(String.format("\tMethod %s, #IncomingEdges: %d,
        // #OutgoingEdges: %d", sootMethod.getName(), incomingEdge, outgoingEdge));
        // }
        // }
    }

}
