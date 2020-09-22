// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

class LED extends CircuitComponent
{
    private static int counter = 0;
    
    private Breadboard base;
    private int anodeHoleID, cathodeHoleID;
    private Color color, darkColor;
    
    private boolean isON;
    
    LED(Breadboard b, int anodeID, int cathodeID, Color c)
    {
        super(ComponentType.LED,
                getNewName(),
                b.getHoleCenter(cathodeID),
                (int)b.getHoleCenter(cathodeID).getDistanceTo(b.getHoleCenter(anodeID)),
                100, // height
                0
        );
        
        color=c;
        
        darkColor = new Color(
                                (color.getRed()/255.0f)*0.6f, 
                                (color.getGreen()/255.0f)*0.6f, 
                                (color.getBlue()/255.0f)*0.6f,
                                ALPHA
                            );
        
        base=b;
        anodeHoleID=anodeID;
        cathodeHoleID=cathodeID;        
    }
    
    public Color getColor() { return color; }
    
    @Override
    public boolean isUsingBaseHole(int holeID)
    {
        return(holeID==anodeHoleID || holeID==cathodeHoleID);
    }
    
    @Override
    public Breadboard getBase() { return base; }
    
    public int getAnodeHoleID() { return anodeHoleID; }
    public int getCathodeHoleID() { return cathodeHoleID; }
    
    public boolean isON() { return isON; }
    public boolean isOFF() { return !isON; }
    public void turnON() { isON=true; }
    public void turnOFF() { isON=false; }
    
    private static String getNewName()
    {
        return("LED_"+(++counter));
    }
    
    @Override
    public void paint(Graphics g)
    {
        // cathode on left, anode on right
        Graphics2D g2d = (Graphics2D)g;
        g2d.setStroke(new java.awt.BasicStroke(1));
        
        g.setColor(Color.GRAY);
        
        Location cathode = base.getHoleCenter(cathodeHoleID);
        Location anode = base.getHoleCenter(anodeHoleID);
        
        g.fillRect(cathode.x-2,cathode.y-60,4,60);
        
        g.fillRect(anode.x-10,anode.y-60,4,6);
        g.fillRect(anode.x-10,anode.y-54,12,4);
        g.fillRect(anode.x-2,anode.y-54,4,54);
        
        g.setColor(darkColor);
        
        int w=anode.x+4-(cathode.x-4);
        g.fillRect(cathode.x-4,cathode.y-100,w,40);
        
        if(isON)
        {
            g.setColor(color);
            g.fillOval(cathode.x-4 + (w/4), cathode.y-90, w/2, 20);
            
            g.setColor(getTranslucent(color,0.7f));
            g.fillOval(cathode.x-4,cathode.y-100,w,40);
            
            g.setColor(getTranslucent(color,0.2f));
            g.fillOval(cathode.x-15,cathode.y-111,w+22,40+22);
        }
    }
    
    @Override
    public String getPropertiesAsString(ArrayList<CircuitComponent> components)
    {
        // <super>|breadboard-index;anode-id;cathode-id;color
        int bi = CircuitUtility.getIndexFromComponentReference(components, base);
        return super.getPropertiesAsString(components)+"|"+bi+";"+anodeHoleID+";"+cathodeHoleID+";"+color.getRGB();
    }
    
    // Factory method
    public static LED parseFromString(String data, CircuitComponent list[])
    {
        String x[] = data.substring(data.indexOf('|')+1).split(";");
        
        Breadboard b = (Breadboard)list[Integer.parseInt(x[0])];
        int anodeID = Integer.parseInt(x[1]);
        int cathodeID = Integer.parseInt(x[2]);
        Color color = new Color(Integer.parseInt(x[3]));
        
        return(new LED(b, anodeID, cathodeID, color));
    }
}
