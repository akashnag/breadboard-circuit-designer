// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.util.ArrayList;

class Wire 
{
    private static final float ALPHA = 0.5f;
    
    private static int counter = 0;
    
    private CircuitComponent source;
    private CircuitComponent sink;
    private int sourceHoleID, sinkHoleID;
    
    protected static final int wireWidth = 6;
    private Color color;
        
    Wire(Color c, CircuitComponent link[], int holeIDs[])
    {
        float rgb[]=c.getRGBColorComponents(null);
        color=new Color(rgb[0], rgb[1], rgb[2], ALPHA);
        
        source = link[0];
        sink = link[1];
        sourceHoleID = holeIDs[0];
        sinkHoleID = holeIDs[1];
    }
    
    private static String getNewName()
    {
        return("Wire_"+(++counter));
    }
    
    public CircuitComponent getSource() { return source; }
    public CircuitComponent getSink() { return sink; }
    public int getSourceHoleID() { return sourceHoleID; }
    public int getSinkHoleID() { return sinkHoleID; }
    
    Location[] getBoundingBox()
    {
        Location start = source.getHoleCenter(sourceHoleID);
        Location end = sink.getHoleCenter(sinkHoleID);
        
        int offset = wireWidth/2;
        
        Location box[] = new Location[4];
        
        box[0]=new Location(start.x-offset,start.y);
        box[1]=new Location(start.x+offset,start.y);
        box[2]=new Location(end.x+offset,end.y);
        box[3]=new Location(end.x-offset,end.y);
        
        return box;
    }
    
    boolean isPointWithinComponent(Location p)
    {
        return(Geometry.isPointWithin(p, getBoundingBox()));
    }
    
    void paint(Graphics g)
    {
        Graphics2D g2d = (Graphics2D)g;
        g2d.setStroke(new java.awt.BasicStroke(wireWidth));
        g2d.setColor(CircuitComponent.getTranslucent(color,ALPHA));
        
        Location s = source.getHoleCenter(sourceHoleID);
        Location e = sink.getHoleCenter(sinkHoleID);
        g2d.drawLine(s.x, s.y, e.x, e.y);        
    }
    
    public String getPropertiesAsString(ArrayList<CircuitComponent> components)
    {
        // color;XY;component-id-1;component-id-2;hole-1;hole-2
        // X=hole-1 component type, Y=hole-2 component type
        // X,Y = { B, P }
        
        int sourceIndex = CircuitUtility.getIndexFromComponentReference(components, source);
        int sinkIndex = CircuitUtility.getIndexFromComponentReference(components, sink);
        
        char x = (source.getType()==ComponentType.BREADBOARD ? 'B' : 'P');
        char y = (source.getType()==ComponentType.BREADBOARD ? 'B' : 'P');
        String xy = "" + x + "" + y;
        
        return color.getRGB() + ";" + xy + ";" + sourceIndex + ";" + sinkIndex + ";" + sourceHoleID + ";" + sinkHoleID;
    }
    
    // Factory method
    public static Wire parseFromString(String data, ArrayList<CircuitComponent> components)
    {
        String x[] = data.split(";");
        
        Color color = new Color(Integer.parseInt(x[0]));
        
        CircuitComponent source = components.get(Integer.parseInt(x[2]));
        CircuitComponent sink = components.get(Integer.parseInt(x[3]));
        
        int sourceHoleID = Integer.parseInt(x[4]);
        int sinkHoleID = Integer.parseInt(x[5]);
        
        return(new Wire(color, new CircuitComponent[] { source, sink }, new int[] { sourceHoleID, sinkHoleID }));
    }
}
