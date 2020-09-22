// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

class Location 
{
    protected int x;
    protected int y;
    
    Location(int x, int y)
    {
        this.x=x;
        this.y=y;
    }
    
    Location(Location pos)
    {
        this.x=pos.x;
        this.y=pos.y;
    }
    
    Location(String pos)
    {
        String a[] = pos.split(",");
        this.x = Integer.parseInt(a[0].substring(1));
        this.y = Integer.parseInt(a[1].substring(0,a[1].length()-1));
    }
    
    public double getDistanceTo(Location p)
    {
        return(Math.sqrt(Math.pow(x-p.x,2)+Math.pow(y-p.y,2)));
    }
    
    public int getX() { return x; }
    public int getY() { return y; }
    
    @Override
    public String toString()
    {
        return("["+x+","+y+"]");
    }
}
