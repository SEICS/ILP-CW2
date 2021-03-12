package uk.ac.ed.inf.aqmaps;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Obstacle {
	/**
	    * This method checks if the location B which is the location
	    * after drone fly from a location A is inside the polygon defined 
	    * by a list of points.
	    *
	    * @param  listOfPoints  list of arraylist of double that defines 
	    * 						points of a polygon. Each arraylist represents
	    * 						a single point(first element is x-coordinate and 
	    * 						sencond element corresponds to y-coordinate).
	    * @param  point   the location B after flying
	    * @param  startX  the x-coordinate of the location before the fly
	    * @param  startY  the y-coordinate of the location before the fly
	    * @return     	  a boolean value that tells if the destination of this flying
	    * 				  is inside the polygon.
	    */
	public static boolean inside(List<ArrayList<Double>> listOfPoints, ArrayList<Double> point, double startX, double startY) {
		ArrayList<Double> allX = new ArrayList<Double>();
		ArrayList<Double> allY = new ArrayList<Double>();
		for (ArrayList<Double> p : listOfPoints) {
			allX.add(p.get(0));
			allY.add(p.get(1));
		}
		double maxX = Collections.max(allX);
		double minX = Collections.min(allX);
		double maxY = Collections.max(allY);
		double minY = Collections.min(allY);
		
		double X3 = point.get(0);
    	double Y3 = point.get(1);
    	double X4 = startX;
    	double Y4 = startY;
		//point outside of the polygon
		if (X3<minX||X3>maxX||Y3<minY||Y3>maxY) {
			return false;	
		}
        
		boolean inside = false;
        for (int i = 0; i < listOfPoints.size(); i++) { 
        	//connecting p1 and p2 forms an edge of the polygon
        	ArrayList<Double> p1 = listOfPoints.get(i);  
        	//Back to the start point
        	ArrayList<Double> p2 = listOfPoints.get((i + 1) % listOfPoints.size());  
  
        	double X1 = p1.get(0);
        	double Y1 = p1.get(1);
        	double X2 = p2.get(0);
        	double Y2 = p2.get(1);
			inside = Line2D.linesIntersect(X1, Y1, X2, Y2, X3, Y3, X4, Y4);
			System.out.print(inside+"\n");
            if (inside) {
            	return true;
            }
        }
        return inside;  
	}
}
