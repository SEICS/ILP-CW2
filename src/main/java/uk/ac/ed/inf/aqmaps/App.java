package uk.ac.ed.inf.aqmaps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Geometry;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;

public class App {
	//sensor receiver for downloading readings from sensors over-the-air within 0.0002 degrees of the air quality sensor
    final static double sensorReceiverDis = 0.0002;
    //drone moves at most 150 times
    final static Integer maxNumOfMoves = 150;
    //each time can move 0.0003 degrees at most(as the hypotenuse r)
    final static double maxMoveEach = 0.0003;
    
    public static void main(String[] args) throws IOException, InterruptedException {
    	//run project command: java -jar aqmaps.jar 15 06 2021 55.9444 -3.1878 5678 80 
    	//read record by date 15/06/2021
        String day = args[0];
        String month = args[1];
        String year = args[2];
        //drone start location (Latitude, Longitude) = (-3.1878, 55.9444)
        double startY = Double.parseDouble(args[3]);
        double startX =  Double.parseDouble(args[4]);
        //my drone
        Drone drone = new Drone(startX, startY);
        //port 80
        String port = args[6];
        HttpClient client = HttpClient.newHttpClient();
        //current drone location (X,Y)
        List<Double> currentX = new ArrayList<>();
        List<Double> currentY = new ArrayList<>();
        //next nearest location
        List<Double> nextX = new ArrayList<>();
        List<Double> nextY = new ArrayList<>();
        //record sensor on flightpath
        List<String> flightpathSensor = new ArrayList<>();
        //record direction for flight path
        List<Integer> flightpathDirection = new ArrayList<>();
        //List for storing output geojson
        List<Feature> map = new ArrayList<Feature>();
        
        //Get all Json needed:   
        //Read 'maps' folder for each day sensor reading from webserver
        String airQualityUrl  = "http://localhost:" + port + "/maps/" + year + "/" + month + "/" + day + "/" + "air-quality-data.json";
        HttpRequest airQualityRequest = HttpRequest.newBuilder().uri(URI.create(airQualityUrl)).build();
        HttpResponse<String> airQualityResponse = client.send(airQualityRequest, HttpResponse.BodyHandlers.ofString());
        //jsonString
        String airQualityJson = airQualityResponse.body();
        //https://www.baeldung.com/gson-list
        //https://mkyong.com/java/how-do-convert-java-object-to-from-json-format-gson-api/
        Type airQualityType = new TypeToken<ArrayList<Sensor>>() {}.getType();
        //JSON file to serial Java object
        ArrayList<Sensor> listOfSensor = new Gson().fromJson(airQualityJson, airQualityType);
        //System.out.print(listOfSensor);
        //Get W3W location details.json
        for (Sensor sensor : listOfSensor) {
        	//read the W3W location for each sensor
        	String detailsJson = sensor.getLocation();
        	//replace all '.' in W3W location string with '/'
        	String detailsPath = detailsJson.replace('.', '/');
        	String W3WlocationUrl = "http://localhost:"+ port + "/words/"+ detailsPath + "/" + "details.json";
        	//request the server
        	HttpRequest W3WlocationRequest = HttpRequest.newBuilder().uri(URI.create(W3WlocationUrl)).build();
        	HttpResponse<String> W3WlocationResponse = client.send(W3WlocationRequest, HttpResponse.BodyHandlers.ofString());
            String W3WlocationJson = W3WlocationResponse.body();
            //get the sensor coordinate(translate sensor W3W location into numbers)
            W3Wdetails W3Wdetails = new Gson().fromJson(W3WlocationJson, W3Wdetails.class);
            Coordinates W3Wcoord = W3Wdetails.getCoordinates();
            double lng = W3Wcoord.getLng();
            double lat = W3Wcoord.getLat();
            //System.out.print(lng);
            //set the coordinate into sensor class
            sensor.setLng(lng);
            sensor.setLat(lat);
        }
        
        //read 'buildings' folder (non-fly buildings) from webserver
        String nonFlyBuildingUrl = "http://localhost:"+port+"/buildings/no-fly-zones.geojson";
        HttpRequest nonFlyBuildingUrlRequest = HttpRequest.newBuilder().uri(URI.create(nonFlyBuildingUrl)).build();
        HttpResponse<String> nonFlyBuildingResponse = client.send(nonFlyBuildingUrlRequest, HttpResponse.BodyHandlers.ofString());
        String nonFlyBuildingJson = nonFlyBuildingResponse.body();
        
        FeatureCollection nonFlyBuildingFeatureCollection = FeatureCollection.fromJson(nonFlyBuildingJson);
        List<Feature> nonFlyBuildingListOfFeatures = nonFlyBuildingFeatureCollection.features();
 
        //points of all buildings
        List<ArrayList<ArrayList<Double>>> buildingPoints = new ArrayList<ArrayList<ArrayList<Double>>>();
        //get coordinates of nonFlyBuilding
        for (Feature feature : nonFlyBuildingListOfFeatures) {
        	Geometry building = feature.geometry();
        	List<List<Point>> coordinates = Polygon.fromJson(building.toJson()).coordinates();
        	//list of points for one buildings
            ArrayList<ArrayList<Double>> listOfPoints = new ArrayList<ArrayList<Double>>();
        	for (List<Point> points : coordinates) {
        		for (Point P : points) {
        			//List storing coordinates for nonFlyBuildings
        			ArrayList<Double> singlePoint = new ArrayList<Double>();
        			singlePoint.add(P.longitude());
        			singlePoint.add(P.latitude());
        			listOfPoints.add(singlePoint);
        			
        		}	
        	}
        	//System.out.print(listOfPoints + "\n");
        	buildingPoints.add(listOfPoints);
        }
        //System.out.print(buildingPoints.get(0) + "\n");
        

        //if drone can still move(move less than 150 times before) and haven't finished reading 33 sensors
        while (drone.getRestOfMoves()>0 && listOfSensor.size()!=0) {
        	//find the sensor nearest to the drone current location
    		//At the start, the nearest sensor is the first one in the list
            Integer nearest = 0;
            //Initiate the minimum distance between drone and a sensor with a extremely large number
            double minDistance = 1000000000.0;
            
            //find the distance between drone current location and a sensor
            for (int i=0; i<listOfSensor.size(); i++) {
                Sensor sensor = listOfSensor.get(i);
                double distance = Calculation.calculateDistance(drone.getcurrentX(), drone.getcurrentY(), sensor.getLng(), sensor.getLat());
                //if a new calculated distance is smaller than the previous one
                if (distance < minDistance) {
                	//record this new sensor
                    minDistance = distance;
                    nearest = i;
                }
            }
            //this is the next sensor to read(nearest sensor)
    		//System.out.print("Sensor not visited: " + ListOfSensor + "\n");
            Sensor targetSensor = listOfSensor.get(nearest);
            
            //find when to go back to the start point by Trigonometric:
            //calculate the distance between current location and the next sensor drone will read 
            double distanceCurrentSensor = Calculation.calculateDistance(drone.getcurrentX(), drone.getcurrentY(), targetSensor.getLng(), targetSensor.getLat());
            //calculate the distance between next sensor drone will read and the start location of the drone flight
            double distanceSensorStart = Calculation.calculateDistance(drone.getstartX(), drone.getstartY(), targetSensor.getLng(), targetSensor.getLat());
            //this is the distance from current location back to the start location
            double distanceBackToStart = distanceCurrentSensor + distanceSensorStart;
            //calculate the biggest possible move distance for the rest of the move
            double distanceleftToMove = drone.getRestOfMoves() * maxMoveEach;
    		
            //check if it is time to finish the flight and be back to the start location:
            //1.fly back to start point
            if (distanceleftToMove < distanceBackToStart) {
            	double distanceFromStart = Calculation.calculateDistance(drone.getcurrentX(), drone.getcurrentY(), drone.getstartX(), drone.getstartY());
    			//No sensor to be read
            	
                while (distanceFromStart > sensorReceiverDis) {
                	//calculate corresponding direction
                	Integer direction = Calculation.calculateDirection(drone.getcurrentX(), drone.getcurrentY(), drone.getstartX(), drone.getstartY());
                	//calculate the distance between drone current location and the start location
                	double distance = Calculation.calculateDistance(drone.getcurrentX(), drone.getcurrentY(), drone.getstartX(), drone.getstartY());
                	
                	// no more moves possible, stop at current location
                    if (drone.getRestOfMoves() < 1) {
                        break;
                    }
                    if (distance < maxMoveEach) {
                    	//still can move but move less than 0.0003 degrees
                        currentX.add(drone.getcurrentX());
                        currentY.add(drone.getcurrentY());
                        drone.fly(distance, direction, buildingPoints);
                        flightpathDirection.add(direction);
                        nextX.add(drone.getcurrentX());
                        nextY.add(drone.getcurrentY());

                    } else {
                    	//move 0.0003 degrees
                        currentX.add(drone.getcurrentX());
                        currentY.add(drone.getcurrentY());
                        drone.fly(maxMoveEach, direction, buildingPoints);
                        flightpathDirection.add(direction);
                        nextX.add(drone.getcurrentX());
                        nextY.add(drone.getcurrentY());
                    }
                    flightpathSensor.add(null);
                }
                break;
            } else {
            	//2.No need back to start location
                System.out.println("target X:" + targetSensor.getLng() + " target Y:" + targetSensor.getLat());
                while (drone.canRead(targetSensor) == false) {
                	Integer direction = Calculation.calculateDirection(drone.getcurrentX(), drone.getcurrentY(), targetSensor.getLng(), targetSensor.getLat());
                    double distance = Calculation.calculateDistance(drone.getcurrentX(), drone.getcurrentY(), targetSensor.getLng(), targetSensor.getLat());
                    
                    if (drone.getRestOfMoves() < 1) {
                        break;
                    } else if (distance < maxMoveEach) {
                        currentX.add(drone.getcurrentX());
                        currentY.add(drone.getcurrentY());
                        drone.fly(distance, direction, buildingPoints);
                        flightpathDirection.add(direction);
                        nextX.add(drone.getcurrentX());
                        nextY.add(drone.getcurrentY());
                    } else {
                        currentX.add(drone.getcurrentX());
                        currentY.add(drone.getcurrentY());
                        drone.fly(maxMoveEach, direction, buildingPoints);
                        flightpathDirection.add(direction);
                        nextX.add(drone.getcurrentX());
                        nextY.add(drone.getcurrentY());
                    }
                    
                    //possible to read this sensor and record this sensor on the flightpath
                    if (drone.canRead(targetSensor) == true) {
                        flightpathSensor.add(targetSensor.getLocation());
                    } else {
                    	//no sensor read, just move 0.0003 degrees towards that sensor
                        flightpathSensor.add(null);
                    }
                }
                
                //Read the sensor
                System.out.print("read the sensor" + "\n");
                Geometry geometry = (Geometry) Point.fromLngLat(targetSensor.getLng(), targetSensor.getLat());
                Feature sensorFeature = Feature.fromGeometry(geometry);
       
                sensorFeature.addStringProperty("location", targetSensor.getLocation());
                sensorFeature.addStringProperty("rgb-string", getRgbString(targetSensor.getReading()));
                sensorFeature.addStringProperty("marker-color", getRgbString(targetSensor.getReading()));
                sensorFeature.addStringProperty("marker-symbol", getMarkerSymbol(targetSensor.getReading()));
                map.add(sensorFeature);
                //Remove the read sensor from the list(list includes all sensors waiting to be read)
                listOfSensor.remove(targetSensor);
            }
        }
        
        //calculate the distance between current and start location
        double distanceCurrentStart = Calculation.calculateDistance(drone.getcurrentX(), drone.getcurrentY(), drone.getstartX(), drone.getstartY());
        //no sensor to read, back to start location directly
        while (distanceCurrentStart > sensorReceiverDis) {
        	Integer direction = Calculation.calculateDirection(drone.getcurrentX(), drone.getcurrentY(), drone.getstartX(), drone.getstartY());
            double distance = Calculation.calculateDistance(drone.getcurrentX(), drone.getcurrentY(), drone.getstartX(), drone.getstartY());
            
            if (drone.getRestOfMoves() < 1) {
                break;
            }else if (distance < maxMoveEach) {
            	//move less than 0.0003 degrees and back to the start location
                currentX.add(drone.getcurrentX());
                currentY.add(drone.getcurrentY());
                drone.fly(distance, direction, buildingPoints);
                flightpathDirection.add(direction);
                nextX.add(drone.getcurrentX());
                nextY.add(drone.getcurrentY());
            } else {
            	//move 0.0003 degrees towards the start location
                currentX.add(drone.getcurrentX());
                currentY.add(drone.getcurrentY());
                drone.fly(maxMoveEach, direction, buildingPoints);
                flightpathDirection.add(direction);
                nextX.add(drone.getcurrentX());
                nextY.add(drone.getcurrentY());
            }
            //no sensor read
            flightpathSensor.add(null);
        }
        
        System.out.print("Write files" + "\n");
        //Write the geojson file:
        int actualMove = maxNumOfMoves - drone.getRestOfMoves();
        List<Point> listOfLocationDuringFlight = new ArrayList<>();
        for (int i = 0; i < actualMove; i++) {
        	listOfLocationDuringFlight.add(Point.fromLngLat(nextX.get(i), nextY.get(i)));
        }
        LineString lineString = LineString.fromLngLats(listOfLocationDuringFlight);
        map.add(Feature.fromGeometry((Geometry) lineString));
        //mark not visited sensors
        for (Sensor sensorNotVisited : listOfSensor) {
            Geometry geometry = (Geometry) Point.fromLngLat(sensorNotVisited.getLng(), sensorNotVisited.getLat());
            Feature sensorFeature = Feature.fromGeometry(geometry);
            sensorFeature.addStringProperty("location", sensorNotVisited.getLocation());
            sensorFeature.addStringProperty("rgb-string", "#aaaaaa");
            sensorFeature.addStringProperty("marker-color", "#aaaaaa");
            map.add(sensorFeature);
        }

        FeatureCollection featureCollection2 = FeatureCollection.fromFeatures(map);
        String geoJson = featureCollection2.toJson();
        //System.out.println(geoJson);
        BufferedWriter bw = null;
        try {
        	File file = new File("readings-" + day + "-" + month + "-" + year + ".geojson");
            FileWriter fw = new FileWriter(file);
            bw = new BufferedWriter(fw);
            bw.write(geoJson);
        } catch (IOException ioe) {
        	ioe.printStackTrace();	
        } finally {
        	try {
        		if (bw!=null) {
        			//stop writing
        	        bw.close();
        	        System.out.print("finish writing geoJson file" + "\n");
        		}
        	} catch (Exception ex) {
        		System.out.println("Error in closing the BufferedWriter:" + ex);
            }
        }
        
       //Write the TXT file:
        //https://beginnersbook.com/2014/01/how-to-write-to-file-in-java-using-bufferedwriter/
        BufferedWriter bw2 = null;
        try {
        	File file2 = new File("flightpath-" + day + "-" + month + "-" + year + ".txt");
            FileWriter fw2 = new FileWriter(file2);
            bw2 = new BufferedWriter(fw2);
            
            for (int j = 1; j <= actualMove ; j++) {
                bw2.write(j + ",");
                bw2.write(currentX.get(j - 1) + ",");
                bw2.write(currentY.get(j - 1) + ",");
                bw2.write(flightpathDirection.get(j - 1) + ",");
                bw2.write(nextX.get(j - 1) + ",");
                bw2.write(nextY.get(j - 1) + ",");
                if (flightpathSensor.get(j - 1) != null) {
                    bw2.write(flightpathSensor.get(j - 1));
                }
                bw2.newLine();
            }
        }catch (IOException ioe) {
        	ioe.printStackTrace();
        } finally {
        	try {
        		if (bw2!=null) {
        			//stop writing
        	        bw2.close();
        	        System.out.print("finish writing TXT file" + "\n");
        		}
        	} catch (Exception ex) {
        		System.out.println("Error in closing the BufferedWriter:" + ex);
            }
        }
    }
    
    
    /**
	    * read json from path and store it as a string
	    *
	    * @param  jsonPath  the path or url to get the json
	    * @return     		a string of the read json from the path
	    */
    public static String readJsonData(String jsonPath) throws IOException {

        StringBuffer stringBuffer = new StringBuffer();
        File jsonFile = new File(jsonPath);
        //if the path doesn't exist, give error message
        if (!jsonFile.exists()) {
            System.err.println(jsonPath+" doesn't exist");
        }
        try {
        	//path exist, then read the json
            FileInputStream stream = new FileInputStream(jsonPath);
            InputStreamReader inputStreamReader = new InputStreamReader(stream);
            BufferedReader in = new BufferedReader(inputStreamReader);
            String data;
            while ((data = in.readLine()) != null) {
                stringBuffer.append(data);
            }
            //finish read all json
            in.close();
        } catch (IOException e) {
        	//IO error message
            e.getStackTrace();
        }
        //return read json data together
        return stringBuffer.toString();
    }
    
    /**
	    * return corresponding colour for the air 
	    * quality reading.
	    *
	    * @param  x   air quality reading of a sensor
	    * @return     corresponding rgb-string of the
	    * 			  air quality reading
	    */
    public static String getRgbString(String x) {
        if(x.equals("null")||x.equals("NaN")){
            x = "low battery";
        }

        if (x.equals("low battery")) {
        	//black
            return "#000000";
        }
        if (x.equals("not visited")) {
        	//grey
            return "#aaaaaa";
        }
        
        Double X = Double.parseDouble(x);
        
        if (X >= 0 && X < 32) {
        	//green
            return "#00ff00";
        }
        if (X >= 32 && X < 64) {
        	//medium green
            return "#40ff00";
        }
        if (X >= 64 && X < 96) {
        	//light green
            return "#80ff00";
        }
        if (X >= 96 && X < 128) {
        	//lime green
            return "#c0ff00";
        }
        if (X >= 128 && X < 160) {
        	//gold
            return "#ffc000";
        }
        if (X >= 160 && X < 192) {
        	//orange
            return "#ff8000";
        }
        if (X >= 192 && X < 224) {
        	//red or orange
            return "#ff4000";
        }
        if (X >= 224 && X < 256) {
        	//red
            return "#ff0000";
        }
        else {
            System.out.println(x);
            return "Air quality reading is not in range: Unable to mark.";
        }
    }

    
    /**
	    * get the colour name corresponding to the air
	    * quality reading.
	    *
	    * @param  x   air quality reading of a sensor
	    * @return     corresponding colour name to the
	    * 			  air quality reading
	    */
    public static String getMarkerColor(String x) {
        if(x.equals("null")||x.equals("NaN")){
            x = "low battery";
        }
        if (x.equals("low battery")) {
            return "Black";
        }
        if (x.equals("not visited")) {
            return "Gray";
        }
        
        Double X = Double.parseDouble(x);
        
        if (X >= 0 && X < 32) {
            return "Green";
        }
        if (X >= 32 && X < 64) {
            return "MediumGreen";
        }
        if (X >= 64 && X < 96) {
            return "Light Green";
        }
        if (X >= 96 && X < 128) {
            return "Lime Green";
        }
        if (X >= 128 && X < 160) {
            return "Gold";
        }
        if (X >= 160 && X < 192) {
            return "Orange";
        }
        if (X >= 192 && X < 224) {
            return "Red / Orange";
        }
        if (X >= 224 && X < 256) {
            return "Red";
        }
        return "Air quality reading is not in range: Unable to mark.";
    }
    
    /**
	    * get marker corresponding to the air
	    * quality reading.
	    *
	    * @param  x   air quality reading of a sensor
	    * @return     corresponding marker to the
	    * 			  air quality reading
	    */
    public static String getMarkerSymbol(String x) {
        if(x.equals("null")||x.equals("NaN")){
            x = "low battery";
        }
        if (x.equals("low battery")) {
            return "cross";
        }
        if (x.equals("not visited")) {
            return "no symbol";
        }
        
        Double X = Double.parseDouble(x);
        
        if (X >= 0 && X < 128) {
            return "lighthouse";
        }
        if (X >= 128 && X < 256) {
            return "danger";
        }
        return "Air quality reading is not in range: Unable to mark.";
    }
    
}
