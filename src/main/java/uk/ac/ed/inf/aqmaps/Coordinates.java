package uk.ac.ed.inf.aqmaps;

public class Coordinates {
	private double lng;
	private double lat;
	
	public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLng() {
        return this.lng;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLat() {
        return this.lat;
    }

    @Override
    public String toString() {
        return "location at (" + this.lng + ", " + this.lat + ")" + "\n";
    }
}
