package dev.task4233.playground.controlFlowGraph;
import java.io.File;
import java.io.IOException;


import org.xmlpull.v1.XmlPullParserException;

import dev.task4233.playground.utils.AndroidCallGraphFilter;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class CFG {
    private final static String USER_HOME = System.getProperty("user.home");
    private static String androidJar = USER_HOME + "/Library/Android/sdk/platforms";
    // TODO: it's good to collect APIs from directory
    static String apkPath = System.getProperty("user.dir") + File.separator + "samples" + File.separator + "simple_calculator_14.apk";

    public static void main(String[] args) {
        // update android_home
        if (System.getenv().containsKey("ANDROID_HOME")) {
            androidJar = System.getenv("ANDROID_HOME") + File.separator + "platforms";
        }

        // set CG algorithm
        InfoflowConfiguration.CallgraphAlgorithm cgAlgorithm = InfoflowConfiguration.CallgraphAlgorithm.SPARK;

        // setup flowdroid
        final InfoflowAndroidConfiguration config = getFlowDroidConfig(apkPath, androidJar, cgAlgorithm);
        SetupApplication app = new SetupApplication(config);

        // Create the callgraph without executing taint analysis
        app.constructCallgraph();

        CallGraph callGraph = Scene.v().getCallGraph();
        int classIdx = 0;

        // fix dependencies here
        AndroidCallGraphFilter androidCallGraphFilter = new AndroidCallGraphFilter(getPackageName(apkPath));
        for (SootClass sootClass: androidCallGraphFilter.getValidClasses()) {
            System.out.println(String.format("Class %d: %s", ++classIdx, sootClass.getName()));
        }

        // collect API info here
    }

    public static InfoflowAndroidConfiguration getFlowDroidConfig(String apkPath, String androidJar, InfoflowConfiguration.CallgraphAlgorithm cgAlgorithm) {
        final InfoflowAndroidConfiguration config = new InfoflowAndroidConfiguration();
        config.getAnalysisFileConfig().setTargetAPKFile(apkPath);
        config.getAnalysisFileConfig().setAndroidPlatformDir(androidJar);
        config.setCodeEliminationMode(InfoflowConfiguration.CodeEliminationMode.NoCodeElimination);
        config.setEnableReflection(true);
        config.setCallgraphAlgorithm(cgAlgorithm);
        return config;
    }

    public static String getPackageName(String apkPath) {
        String packageName = "";
        try {
            ProcessManifest manifest = new ProcessManifest(apkPath);
            packageName = manifest.getPackageName();
            manifest.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
        return packageName;
    }
}
