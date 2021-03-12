package uk.ac.ed.inf.aqmaps;

public class W3Wdetails {
	//For taking serial json object by TypeToken
	//https://github.com/google/gson/blob/master/UserGuide.md#TOC-Serializing-and-Deserializing-Generic-Types
    private Coordinates coordinates;

    public void setCoordinates(Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }
    
    @Override
    public String toString() {
        return "W3WDetails: " + coordinates + "\n";
    }
}
