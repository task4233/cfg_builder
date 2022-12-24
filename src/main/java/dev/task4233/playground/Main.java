package dev.task4233.playground;

import java.util.Arrays;

import dev.task4233.playground.cfg.CFG;

public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("please add run option");
        }

        String[] cmdArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (args[0]) {
            case "cfg":
                CFG.main(cmdArgs);
                break;
            default:
                System.err.print(args[0] + "is not defined");
        }
    }
}
