package sumoetcs.connection;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.sumo.libtraci.VehicleType;

public abstract class Connection {
    public Connection(String sumoTypeId) {
        this.sumoTypeId = sumoTypeId;
    }

    public void addObserver(IConnectionObserver observer) {
        observers.add(observer);
    }

    public String getSumoTypeId() {
        return sumoTypeId;
    }

    public abstract boolean isActive();

    protected String getTypeParameter(String param) {
        return VehicleType.getParameter(sumoTypeId, param);
    }

    protected String getTypeParameter(String param, String defaultValue) {
        String value = VehicleType.getParameter(sumoTypeId, param);
        return value.equals("") ? defaultValue : value;
    }

    protected void notifyObservers() {
        for (var obs: observers) {
            obs.connectionChanged();
        }
    }

    private String sumoTypeId;
    private List<IConnectionObserver> observers = new LinkedList<>();
}
