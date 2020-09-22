// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

class Geometry 
{
    private static final double DELTA = 0.01;
    
    static boolean isPointWithin(Location point, Location box[])
    {
        // checks if the point is within the parallelogram defined by box[]
        double total = getBoxArea(box[0],box[1],box[2],box[3]);
        
        double t1 = getTriangleArea(point,box[0],box[1]);
        double t2 = getTriangleArea(point,box[1],box[2]);
        double t3 = getTriangleArea(point,box[2],box[3]);
        double t4 = getTriangleArea(point,box[3],box[0]);
        
        return(Math.abs((t1+t2+t3+t4)-total)<=DELTA);
    }
    
    private static double getBoxArea(Location p1, Location p2, Location p3, Location p4)
    {
        // 2*Area = (x1y2 - x2y1) + (x2y3 - x3y2) + (x3y4 - x4y3) + (x4y1 - x1y4)
        
        double term1 = ((p1.x*p2.y)-(p2.x*p1.y));
        double term2 = ((p2.x*p3.y)-(p3.x*p2.y));
        double term3 = ((p3.x*p4.y)-(p4.x*p3.y));
        double term4 = ((p4.x*p1.y)-(p1.x*p4.y));
        
        return(Math.abs((term1+term2+term3+term4)/2));
    }
    
    private static double getTriangleArea(Location p1, Location p2, Location p3)
    {
        double a = getDistanceBetween(p1,p2);
        double b = getDistanceBetween(p2,p3);
        double c = getDistanceBetween(p3,p1);
        
        double s = (a + b + c) / 2;
        return(Math.sqrt(s * (s-a) * (s-b) * (s-c)));
    }
    
    private static double getDistanceBetween(Location a, Location b)
    {
        return(Math.sqrt(Math.pow(b.x-a.x,2)+Math.pow(b.y-a.y,2)));
    }
}
