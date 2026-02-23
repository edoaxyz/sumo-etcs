package sumoetcs;

import java.nio.file.Paths;

import sumoetcs.sumo.SumoManager;

public class Client {
    public static void main(String[] args) {
        String lineID = "av_belfioreFXB";
        Paths.get("out", lineID).toFile().mkdirs(); // Create output folder
        SumoManager sumoManager = new SumoManager(
                Paths.get("resources", "lineData", lineID, "sumo.sumocfg").toString(),
                Paths.get("out", lineID, "fcd.xml").toString()
            );
        RBC rbc = new RBC(sumoManager);
        sumoManager.runAll();
    }
}
