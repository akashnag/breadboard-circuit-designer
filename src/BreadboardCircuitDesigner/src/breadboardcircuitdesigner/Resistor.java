// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import static breadboardcircuitdesigner.Wire.wireWidth;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

class Resistor extends CircuitComponent
{
    private static final Color codes[] ={   
                                            Color.BLACK, 
                                            new Color(105,50,48),
                                            Color.RED,
                                            Color.ORANGE,
                                            Color.YELLOW,
                                            Color.GREEN,
                                            Color.BLUE,
                                            new Color(203,103,253),
                                            Color.GRAY,
                                            Color.WHITE,
                                            new Color(210,166,62),
                                            Color.LIGHT_GRAY
                                        };
    
    private static int counter = 0;
    
    private Breadboard base;
    private int sourceHoleID, sinkHoleID;
    private int resistance;
    
    private Location sourceLocation, sinkLocation;
    
    Resistor(Breadboard b, int holeIDs[], int resistance)
    {
        super(ComponentType.RESISTOR,
                getNewName(),
                b.getHoleCenter(holeIDs[0]),
                (int)b.getHoleCenter(holeIDs[0]).getDistanceTo(b.getHoleCenter(holeIDs[1])),
                20, // height
                0
        );
        
        this.resistance=resistance;
        base=b;
        sourceHoleID=holeIDs[0];
        sinkHoleID=holeIDs[1];     
        
        updateLocations();      
    }
    
    @Override
    public boolean isUsingBaseHole(int holeID)
    {
        return(holeID==sourceHoleID || holeID==sinkHoleID);
    }
    
    @Override
    public Breadboard getBase() { return base; }
    
    public int getSourceHoleID() { return sourceHoleID; }
    public int getSinkHoleID() { return sinkHoleID; }
    
    private void updateLocations()
    {
        sourceLocation=base.getHoleCenter(sourceHoleID);
        sinkLocation=base.getHoleCenter(sinkHoleID);  
    }
    
    private static String getNewName()
    {
        return("Resistor_"+(++counter));
    }
    
    @Override
    public void paint(Graphics g)
    {
        updateLocations();
        
        Location start = sourceLocation.x <= sinkLocation.x ? sourceLocation : sinkLocation;
        Location end = sourceLocation.x > sinkLocation.x ? sourceLocation : sinkLocation;
        
        Graphics2D g2d = (Graphics2D)g;
        g2d.setStroke(new java.awt.BasicStroke(1));
        
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(start.x,start.y-2,super.getWidth(),4);   // connecting stick
        
        // body=42x20
        Location bodyStart = new Location(start.x+((end.x-start.x-42)/2),start.y-10);
                
        g.setColor(new Color(112,146,190));
        g.fillRect(bodyStart.x,bodyStart.y,42,20);          // body
        
        g.setColor(Color.BLACK);
        g.drawRect(bodyStart.x,bodyStart.y,42,20);          // body border
                
        // 4 bands, each band width = 6, 6x4=24, inter-band-gap=4
        // 12+24=36, side-gap=3 on each side
        
        int c[]=getCodes(resistance);
        
        Color band1 = codes[c[0]];
        Color band2 = codes[c[1]];
        Color band3 = codes[c[2]];
        Color band4 = codes[c[3]];
        
        g.setColor(band1);
        g.fillRect(bodyStart.x+3,bodyStart.y+1,6,19);
        
        g.setColor(band2);
        g.fillRect(bodyStart.x+13,bodyStart.y+1,6,19);
        
        g.setColor(band3);
        g.fillRect(bodyStart.x+22,bodyStart.y+1,6,19);
        
        g.setColor(band4);
        g.fillRect(bodyStart.x+32,bodyStart.y+1,6,19);        
    }
    
    private int[] getCodes(int r)
    {
        String x = ""+r;
        int c=0;
        for(int i=x.length()-1; i>=0; i--)
        {
            if(x.charAt(i)=='0') c++;
        }
        int p = (int)Math.pow(10,c);
        int m = r/p;
        
        int d[]=new int[3];
        for(int i=2; i>=0; i--)
        {
            d[i]=m%10;
            m/=10;
        }
        
        int bands[] = { d[0], d[1], d[2], c };
        return bands;
    }
    
    @Override
    public String getPropertiesAsString(ArrayList<CircuitComponent> components)
    {
        // <super>|breadboard-id;source-id;sink-id;resistance
        int bi = CircuitUtility.getIndexFromComponentReference(components, base);
        return super.getPropertiesAsString(components) + "|" + bi + ";" + sourceHoleID + ";" + sinkHoleID + ";" + resistance;
    }
    
    // Factory method
    public static Resistor parseFromString(String data, CircuitComponent list[])
    {
        String x[] = data.substring(data.indexOf('|')+1).split(";");
        
        Breadboard b = (Breadboard)list[Integer.parseInt(x[0])];
        int sourceHoleID = Integer.parseInt(x[1]);
        int sinkHoleID = Integer.parseInt(x[2]);
        int resistance = Integer.parseInt(x[3]);
        
        return(new Resistor(b, new int[] { sourceHoleID, sinkHoleID }, resistance));
    }
}
