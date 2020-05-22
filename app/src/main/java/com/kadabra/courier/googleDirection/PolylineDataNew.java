package com.kadabra.courier.googleDirection;

import com.google.android.gms.maps.model.Polyline;
import com.google.maps.model.DirectionsLeg;
import com.kadabra.courier.model.Stop;

public class PolylineDataNew {

    private Polyline polyline;
    private Legs leg;
    private Stop stop;


    public PolylineDataNew(Polyline polyline, Legs leg) {
        this.polyline = polyline;
        this.leg = leg;
    }
    public PolylineDataNew(Polyline polyline, Legs leg, Stop stop) {
        this.polyline = polyline;
        this.leg = leg;
        this.stop = stop;

    }

    public Stop getStop() {
        return stop;
    }

    public void setStop(Stop stop) {
        this.stop = stop;
    }

    public Polyline getPolyline() {
        return polyline;
    }

    public void setPolyline(Polyline polyline) {
        this.polyline = polyline;
    }

    public Legs getLeg() {
        return leg;
    }

    public void setLeg(Legs leg) {
        this.leg = leg;
    }

    @Override
    public String toString() {
        return "PolylineData{" +
                "polyline=" + polyline +
                ", leg=" + leg +
                ", stop=" + stop +
                '}';
    }
}