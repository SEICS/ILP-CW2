package uk.ac.ed.inf.aqmaps;

import java.util.ArrayList;
import java.util.List;

public class Drone {
	//X = longitude, Y = latitude
	//start location before moving
    private double startX;
    private double startY;
    //location after moving
    private double currentX;
    private double currentY;
    //number of move left
    private Integer restOfMoves;
    
    //sensor receiver for downloading readings from sensors over-the-air within 0.0002 degrees of the air quality sensor
    final double sensorReceiverDis = 0.0002;
    //drone moves at most 150 times
    final Integer maxMovesTimes = 150;
    //each time can move 0.0003 degrees at most(as the hypotenuse r)
    final double maxMoveEach = 0.0003;
    
    /**
    * Define what is a Drone. Each drone should have a start location before a fly
    * and also a location after this fly. The rest of movement the drone can make
    * will be updated after each fly.
    *
    * @param  startX  the x-coordinate of drone start location before flying
    * @param  startY  the x-coordinate of drone start location before flying
    * @param  currentX  the x-coordinate of drone current location after the fly 
    * @param  currentY  the y-coordinate of drone current location after the fly
    * @param  restOfMoves  number of movements left that drone could make 
    */
    public Drone(double startX, double startY) {
        this.startX = startX;
        this.startY = startY;
        this.currentX = startX;
        this.currentY = startY;
        this.restOfMoves = maxMovesTimes;
    }

    //define drone confinement area
    public static final double latitudeMax = 55.946233;
    public static final double latitudeMin = 55.942617;
    public static final double longitudeMax = -3.184319;
    public static final double longitudeMin = -3.192473;
    
    //set possible number of moves
    public void setrestOfMoves(Integer maxMoves) {
        this.restOfMoves = maxMoves;
    }
    
    //get possible number of moves
    public Integer getRestOfMoves() {
        return this.restOfMoves;
    }

    /**
     * Define the way for a fly or the way how does the drone move once.
     * Each time before the drone really flies, the destination location 
     * after this fly will be checked to see if this fly is allowed. If 
     * this fly is allowed, then updates the drone current location and 
     * decrements the number of possible movement left. If this fly is
     * not allowed, then adjusts the direction of the fly by adding 10
     * degrees each adjustment.
     *
     * @param  distance  the distance will be flied.
     * @param  direction the direction will be followed by the drone.
     */
    public void fly(double distance, Integer direction, List<ArrayList<ArrayList<Double>>> polygons) {
    	while (!isfly(distance, direction, polygons)) {
    		direction = direction + 10;
    		if (direction > 360) {
    			direction = 0;
    		}
    	}
 
    	System.out.print("Actual flying:" + "\n");
        System.out.println("distance " + distance + " direction " + direction);
        double sita = Math.toRadians(direction); //Integer to radians
        // update location
        this.currentX = this.currentX + distance * Math.cos(sita);
        this.currentY = this.currentY + distance * Math.sin(sita);
        		
        System.out.println("drone location: (" + this.currentX + ", " + this.currentY + ")"+ "\n" + "number of move left: " + this.restOfMoves);
        //successfully move to a new sensor/towards the target location, calculate the rest of possible move for flying
        this.restOfMoves = this.restOfMoves - 1;
    }
    
    /**
	    * This method simulates a fly movement and checks if the destionation of 
	    * this fly is allowed. If the destination location is outside of confinement
	    * area or it is inside the no-fly zones, then this fly will not be allowed.
	    * An allowable fly should have it destination location be inside the 
	    * confinement area and outside of each no-fly zones.
	    *
	    * @param  distance   the length of movement for each drone fly
	    * @param  direction  fly direction
	    * @param  polygons   list of arraylist of double that defines 
	    * 					 points of a polygon. Each arraylist represents
	    * 					 a single point(first element is x-coordinate and 
	    * 					 sencond element corresponds to y-coordinate).
	    * @return     	  a boolean value that tells if this fly is allowable.
	    */
    public boolean isfly(double distance, Integer direction, List<ArrayList<ArrayList<Double>>> polygons) {
    	boolean isFly = false;
    	System.out.print("Testing:" + "\n");
        System.out.println("distance " + distance + " direction " + direction);
        //if this move is allowed
        if (distance <= maxMoveEach) {
        	//if direction is correct and it is a multiple of 10
        	if (direction >= 0 && direction <= 360 && direction % 10 == 0) {
        		double sita = Math.toRadians(direction); //Integer to radians
        		// update location
        		double X = this.currentX;
        		double Y = this.currentY;
        		X = X + distance * Math.cos(sita);
        		Y = Y + distance * Math.sin(sita);
        		//check if the drone will fly inside the confinement area
        		
        		//check if avoid the non-fly zones
        		ArrayList<Boolean> isAvoidRecord = new ArrayList<Boolean>();
        		for (List<ArrayList<Double>> polygon : polygons) {
        			isAvoidRecord.add(this.avoidBuildings(polygon, X, Y, this.currentX, this.currentY));
        		}
        		
        		isAvoidRecord.add(this.isOutOfRegion(X,Y));
        		System.out.print(isAvoidRecord+"\n");
        		isFly = this.and(isAvoidRecord);
        		System.out.print(isFly+"\n");
        	}
        } else {
        	System.out.println("Wrong distance to fly: Distance out of range");
        }
        
        return isFly;
    }
    
    /**
	    * Inputing a boolean arraylist to perform and operation, which is 
	    * to check if all elements inside the arraylist is true. If so, return 
	    * true. Otherwise, return false.
	    *
	    * @param  list	  a boolean arraylist 
	    * @return     	  a boolean value. Value will be true if all elements in 
	    * 				  list are true. Otherwise, value returned is false
	    */
    public boolean and(ArrayList<Boolean> list) {
    	for (boolean b : list) {
            if (!b) {
            	return false;
            }
        }
        return true;
    }
    
    /**
	    * This method checks if the location B which is the location
	    * after drone fly from a location A is inside the polygon(a 
	    * non-fly-zone building) defined by a list of points.
	    *
	    * @param  polygon  list of arraylist of double that defines 
	    * 						points of a polygon. Each arraylist represents
	    * 						a single point(first element is x-coordinate and 
	    * 						sencond element corresponds to y-coordinate).
	    * @param  X   the x-coordinate of location B after flying
	    * @param  Y   the y-coordinate of location B after flying
	    * @param  sX  the x-coordinate of the location before the fly
	    * @param  sY  the y-coordinate of the location before the fly
	    * @return     	  a boolean value that tells if the destination of this flying
	    * 				  is inside a no-fly-zone building.
	    */
    public boolean avoidBuildings(List<ArrayList<Double>> polygon,double X, double Y, double sX, double sY) {
    	boolean avoid = false;
    	ArrayList<Double> point = new ArrayList<Double>();
    	point.add(X);
    	point.add(Y);
    	//System.out.print(Obstacle.inside(polygon, point));
    	if (Obstacle.inside(polygon, point, sX, sY)) {
    		avoid = false;
    	} else {
    		avoid = true;
    	}
		return avoid;
    }

    /**
     * Check is the drone location(x,y) is inside the confinement area.
     * If so, return true. Otherwise, return false.
     *
     * @param	x   the x-coordinate of drone location
     * @param   y	the y-coordinate of drone location
     * @return      a boolean value representing if the location of
     * 				the drone is out of region allowed.
     */
    //forbid drone flying out of the confinement region
    public boolean isOutOfRegion(double x, double y) {
    	boolean insideRegion = false;
    	//if drone fly inside the confinement region
    	if (x >= longitudeMin && x <= longitudeMax) {
        	if (y >= latitudeMin && y <= latitudeMax) {
        		insideRegion = true;
        	}
        } else {
        	//if drone tries to fly out of the confinement region, then return error message 
        	insideRegion = false;
        	System.out.println("Drone will fly outside of the confinement area");
        }
    	
    	return insideRegion;
    }
    
    /**
     * Check if the sensor is in range such that
     * it can be read by the drone.
     *
     * @param		a sensor(with its air quality reading,its W3W
     * 				location, its actual coordinate corresponding
     * 				to its W3W location and its battery reading)
     * @return      a boolean value representing if the sensor 
     * 				could be read.
     */
    //check if the sensor to read is in range such that the drone could read it
    public boolean canRead(Sensor sensor) {
    	//calculate distance between the drone and the sensor
        double distance = Calculation.calculateDistance(this.getcurrentX(), this.getcurrentY(), sensor.getLng(), sensor.getLat());
        //if the actual distance is <0.0002 degree
        if (distance < sensorReceiverDis) {
        	//sensor is in range
            return true;
        } else {
        	//sensor out of range
        	return false;
        }
    }
    
    //update drone coordinate before the flying:
    //set X(longitude) of drone flight start location
    public void setstartX(double startX) {
        this.startX = startX;
    }

    //get X(longitude) of drone flight start location
    public double getstartX() {
        return this.startX;
    }
    
    //set Y(latitude) of drone flight start location
    public void setstartY(double startY) {
        this.startY = startY;
    }
    
    //set Y(latitude) of drone flight start location
    public double getstartY() {
        return this.startY;
    }

    //update drone coordinate after the flying:
    //set X(longitude) of drone flight current location
    public void setcurrentX(double currentX) {
        this.currentX = currentX;
    }

    //get X(longitude) of drone flight current location
    public double getcurrentX() {
        return this.currentX;
    }

    //get Y(latitude) of drone flight current location
    public void setcurrentY(double currentY) {
        this.currentY = currentY;
    }

    //get Y(latitude) of drone flight current location
    public double getcurrentY() {
        return this.currentY;
    }
}
