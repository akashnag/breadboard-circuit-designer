// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.awt.Graphics;
import java.awt.Color;
import java.util.ArrayList;

class CircuitComponent 
{
    protected static final float ALPHA = 0.7f;
    
    protected ComponentType componentType;
    protected String name;
    protected Location location;                // top-left
    protected boolean hasBeenPlaced;
    
    private int maxHoles;
    private int width;
    private int height;
    
    CircuitComponent(ComponentType type, String n, Location pos, int w, int h, int maxHoles)
    {
        this.maxHoles=maxHoles;
        componentType=type;
        name=n;
        width=w;
        height=h;
        this.location = new Location(pos);
        hasBeenPlaced=false;
    }
    
    public Breadboard getBase() { return null; }
    public boolean isUsingBaseHole(int holeID) { return false; }    
    
    int getMaxHoles() { return maxHoles; }
    int getWidth() { return width; }
    int getHeight() { return height; }
    String getName() { return name; }
    
    void setName(String s) { name=s; }
    void setWidth(int w) { width=w; }
    void setHeight(int h) { height=h; }
    
    public void onClick(int x, int y)
    {
        // WARNING: Global coordinates
        // PLACEHOLDER
    }
    
    public boolean isHoleBlocked(int holeID, ArrayList<Wire> wires, ArrayList<CircuitComponent> components)
    {
        // PLACEHOLDER
        return false;
    }
    
    void moveTo(Location pos, boolean isFinal, Graphics g)
    {
        this.location = new Location(pos);
        hasBeenPlaced=isFinal;
        paint(g);
    }
        
    ComponentType getType() { return componentType; }
    Location getLocation() { return location; }
    boolean isLocked() { return hasBeenPlaced; }
    void lock() { hasBeenPlaced=true; }
    
    Location[] getBoundingBox()
    {
        Location box[] = new Location[4];
        
        box[0]=new Location(location);
        box[1]=new Location(location.x+width,location.y);
        box[2]=new Location(location.x+width,location.y+height);
        box[3]=new Location(location.x,location.y+height);
        
        return box;
    }
    
    boolean isPointWithinComponent(Location p)
    {
        Location box[] = getBoundingBox();        
        return Geometry.isPointWithin(p, box);
    }
            
    void paint(Graphics g)
    {
        // placeholder method
    }
    
    void paint(Graphics g, int holeIDs[])
    {
        // placeholder method
    }
    
    void paint(Graphics g, boolean highlight)
    {
        g.setColor(Color.YELLOW);
        g.drawRect(location.x-30,location.y-30,width+60,height+60);
        paint(g);
    }
    
    int getHoleRadius()
    {
        // placeholder method
        return 0;
    }
    
    Location getHoleCenter(int id)
    {
        // placeholder method
        return null;
    }
    
    Color getAlphaMix(Color c)
    {
        float m[] = c.getComponents(null);
        Color x = new Color(m[0],m[1],m[2],(isLocked() ? 1.0f : ALPHA));
        return x;
    }
    
    static Color getTranslucent(Color c)
    {
        return getTranslucent(c,ALPHA);
    }
 
    static Color getTranslucent(Color c, float opacity)
    {
        float m[] = c.getComponents(null);
        Color x = new Color(m[0],m[1],m[2],opacity);
        return x;
    }
    
    @Override
    public String toString()
    {
        // componentType;name;location;maxHoles;width;height
        return(componentType.toString()+";"+name+";"+location.toString()+";"+maxHoles+";"+width+";"+height);
    }
    
    public String getPropertiesAsString(ArrayList<CircuitComponent> components)
    {
        return toString();
    }    
}
