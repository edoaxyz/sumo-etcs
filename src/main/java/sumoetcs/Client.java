package sumoetcs;

import java.nio.file.Paths;

import sumoetcs.sumo.SumoManager;

public class Client {
    public static void main(String[] args) {
        if (args.length != 2) {
            printHelp();
            return;
        }
        Paths.get(args[1]).toFile().mkdirs(); // Create output folder
        SumoManager sumoManager = new SumoManager(
                Paths.get(args[0]).toString(),
                Paths.get(args[1], "fcd.xml").toString(),
                Paths.get(args[1], "statistics.xml").toString()
            );
        RBC rbc = new RBC(sumoManager);
        sumoManager.runAll();
    }

    public static void printHelp() {
        System.out.println("This tool applies simulates Radio Block Center connections for ETCS rail signalling system.");
        System.out.println("Usage: java -jar sumo-etcs.jar path_to_sumocfg path_to_output_folder");
    }
}
