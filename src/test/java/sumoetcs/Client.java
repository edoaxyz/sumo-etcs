package sumoetcs;

import java.nio.file.Paths;

import sumoetcs.RBC;
import sumoetcs.sumo.SumoManager;

public class Client {
    public static void main(String[] args) {
        SumoManager sumoManager = new SumoManager(Paths.get("resources", "test", "test2.sumocfg").toString());
        RBC rbc = new RBC(sumoManager);
        sumoManager.runAll();
        
    }
}
