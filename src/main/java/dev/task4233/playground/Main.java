package dev.task4233.playground;

import dev.task4233.playground.controlFlowGraph.CFG;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("please add run option");
        }

        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel","ERROR");

        switch (args[0]) {
            case "cfg":
                CFG cfg = new CFG();
                cfg.constructCFG();
                break;
            default:
                System.err.print(args[0] + "is not defined");
        }
    }
}
