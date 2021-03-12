package uk.ac.ed.inf.aqmaps;

public class Calculation {
	/**
	    * calculate distance between 2 points on plane
	    *
	    * @param  x1  the x-coordinate of location A
	    * @param  y1  the y-coordinate of location A
	    * @param  x2  the x-coordinate of location B 
	    * @param  y2  the y-coordinate of location B
	    * @return     the distance between 2 points on plane
	    */
    public static double calculateDistance(double x1, double y1, double x2, double y2) {
    	double pow1 = Math.pow(x2-x1, 2);
    	double pow2 = Math.pow(y2-y1, 2);
    	double distance = Math.sqrt(pow1+pow2);
        return distance;
    }

    /**
	    * calculate direction needed to fly from current 
	    * location to the next target location by trigonometrics
	    *
	    * @param  x1  the x-coordinate of location A
	    * @param  y1  the y-coordinate of location A
	    * @param  x2  the x-coordinate of location B 
	    * @param  y2  the y-coordinate of location B
	    * @return     the direction needed to fly from
	    * 			  location A to location B
	    */
    public static Integer calculateDirection(double x1, double y1, double x2, double y2) {
        double tan = Math.abs((y2 - y1)/(x2 - x1));
        double angleInRadian = Math.atan(tan);
        double angleInDegree = Math.toDegrees(angleInRadian);
        Integer direction = ((int) Math.round(angleInDegree/10)) * 10;
        
        if (x2 - x1 >= 0 && y2 - y1 <= 0) {
            direction =  360 - direction;
        } else if (x2 - x1 <= 0 && y2 - y1 <= 0) {
            direction = 180 + direction;
        } else if (x2 - x1 <= 0 && y2 - y1 >= 0) {
            direction = 180 - direction;
        } 
          
        return direction;
    }
}
