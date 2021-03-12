package uk.ac.ed.inf.aqmaps;

public class Sensor {
	//the What3Words location string
    private String location;
    private double battery;
    private String reading;
    //coordinates corresponding to the W3Wlocation (lng, lat)
    private double lng;
    private double lat;

    
    /**
     * Define the same hierarchy and attibutes seen in
     * air-quality-data.json. This makes it easier to 
     * parse Json from air-quality-data.json for a single
     * sensor 
     *
     * @param  location  W3W location of the sensor
     * @param  battery   battery reading of the sensor
     * @param  reading   air quality reading of the sensor
     */
    public Sensor(String location, double battery, String reading) {
    	//What3Words location
        this.location = location;
        //sensor battery reading
        this.battery = battery;
        //air quality reading
        this.reading = reading;
    }

    //set/get method of W3W locations
    public void setLocation(String location) {
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
    
    //set/get method of air quality reading
    public void setReading(String reading) {
        this.reading = reading;
    }
    
    public String getReading() {
        if(this.battery<10){
            return "null";
        }
        return reading;
    }

    //set/get method of the battery reading
    public void setBattery(double battery) {
        this.battery = battery;
    }
    
    public double getBattery() {
        return battery;
    }

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
        return "W3Wlocation = " + location + '\n' +
        		"battery= " + battery + "\n" +
        		"reading = " + reading + '\'' +
                "(lng,lat) = " + "(" + this.lng + ", " + this.lat + ")" + "\n";            
    }
}
