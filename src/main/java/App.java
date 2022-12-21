import java.io.File;

import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;

public class App {
    private final static String USER_HOME = System.getProperty("user.home");
    private static String androidJar = USER_HOME + "/Library/Android/sdk/platforms";
    static String apkPath = "./simple_calculator_14.apk";

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
}
