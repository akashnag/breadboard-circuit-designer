// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

import static breadboardcircuitdesigner.BreadboardUtility.*;

class SevenSegmentLED extends CircuitComponent
{
    private static int counter = 0;
    
    private Breadboard base;
    private int topLeftHoleID;                  // position of pin #10
    private boolean commonCathode;
    private Color color;
    
    private boolean isON[];
    
    // ---------- computed -----------
    private int holeIDs[];
    // -------------------------------
    
    SevenSegmentLED(Breadboard b, int firstHoleID, boolean cathode, Color c)
    {
         super(
                 ComponentType.SEVEN_SEGMENT_LED,
                 getNewName(),
                 b.getHoleCenter(firstHoleID),
                 Breadboard.getDistanceBetweenHoles(4),
                 Breadboard.getDistanceBetweenHoles(7),
                 0
         );
         
         color=c;
         isON=new boolean[8];
         base=b;
         topLeftHoleID=firstHoleID;
         commonCathode=cathode;
         computeHoleIDs();
    }
    
    private int getSegmentIndex(char x)
    {
        String c = "abcdefg.";
        String d = c.toUpperCase();
        
        int k1 = c.indexOf(x);
        int k2 = c.indexOf(x);
        
        if(k1==-1 && k2==-1) return -1;
        int k=(k1==-1 ? k2 : k1);
        
        return k;
    }
    
    public boolean isON(char x)
    {
        int k = getSegmentIndex(x);
        return( k>-1 ? isON[k] : false);
    }
    
    public void turnON(char x)
    {
        int k = getSegmentIndex(x);
        if(k>-1) isON[k]=true;
    }
    
    public void turnOFF(char x)
    {
        int k = getSegmentIndex(x);
        if(k>-1) isON[k]=false;
    }
    
    public void turnOFF()
    {
        for(int i=0; i<8; i++) isON[i]=false;
    }
    
    public void turnOnOffSegments(HoleState states[])
    {
        // only segments are passed (in sequence: abcdefg. )
        turnOFF();
        String x = "abcdefg.";
        for(int i=0; i<8; i++)
        {
            if((commonCathode && states[i]==HoleState.HIGH)||(!commonCathode && states[i]==HoleState.LOW))
            {
                turnON(x.charAt(i));
            }
        }
    }
    
    public int getHoleID(char x)
    {
        int pinNumbers[] = { 7, 6, 4, 2, 1, 9, 10, 5 };
        int k = getSegmentIndex(x);
        int pin = pinNumbers[k];
        return holeIDs[pin-1];
    }
    
    public int[] getCommonHoleIDs()
    {
        return(new int[] { holeIDs[2], holeIDs[7] });
    }
    
    private static String getNewName()
    {
        return("7LED_"+(++counter));
    }
    
    private void computeHoleIDs()
    {
        holeIDs=new int[10];
        
        for(int i=10; i>=6; i--) holeIDs[i-1]=topLeftHoleID+(10-i);
        
        int x=topLeftHoleID;
        for(int i=1; i<=7; i++)
        {
            if(isHoleOnBridgeBorderTop(x)) {
                i+=2;
                x+=63;
            } else
                x+=63;
        }
        
        for(int i=0; i<5; i++) holeIDs[i]=(x+i);
        
        //Utility.printArray("7LED_computed-holes",holeIDs);
        //getShadowedHoles();
    }
    
    protected static boolean canPositionLED(Breadboard base, int pin10HoleID, ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        if(isHoleInPowerRails(pin10HoleID)) return false;
        
        Breadboard b = new Breadboard(new Location(40,40));
        SevenSegmentLED temp = new SevenSegmentLED(b,pin10HoleID,true,Color.RED);
        
        for(int i=0; i<10; i++)
        {
            if(!isHoleIDValid(temp.holeIDs[i])) return false;
        }
        
        for(int i=0; i<temp.holeIDs.length; i++)
        {
            if(base.isHoleShadowed(temp.holeIDs[i], wires, components)) return false;
        }
        
        if(!areHolesInSameVirtualRow(temp.holeIDs,0,4)) return false;
        if(!areHolesInSameVirtualRow(temp.holeIDs,5,9)) return false;
        
        return(!isHoleInPowerRails(temp.holeIDs[0]));
    }
    
    public int[] getShadowedHoles()
    {
        // length of list = 5 * 6
        // Although height = 8, if the LED is to be placed on the breadboard vertically,
        // it must cover the bridge (which is 2 holes in height)
        
        int holes[] = new int[30];
        
        int k=-1, x=holeIDs[9];
        for(int i=1; i<=8; i++)
        {
            if(isHoleOnBridgeBorderTop(x))
            {
                for(int j=0; j<5; j++) holes[++k]=x+j;
                i+=2;
                x+=63;
            } else {
                for(int j=0; j<5; j++) holes[++k]=x+j;
                x+=63;
            }
        }
        
        //Utility.printArray("7LED_shadowed-holes",holes);
        return holes;
    }
    
    public boolean isCommonCathode() { return commonCathode; }
    public boolean isCommonAnode() { return !commonCathode; }
    public int[] getHoleIDs() { return holeIDs; }
    
    @Override
    public Breadboard getBase() { return base; }
    
    @Override
    public Location getLocation() { return base.getHoleCenter(topLeftHoleID); }
    
    @Override
    public void paint(Graphics g)
    {
        Location loc = getLocation();
        int x = loc.x, y = loc.y, w = this.getWidth(), h = this.getHeight();
        
        Color border = new Color(32,32,32);
        
        Graphics2D g2d = (Graphics2D)g;
        g2d.setStroke(new java.awt.BasicStroke(1));
        
        g.setColor(border);        
        g.fillRect(x-8,y,16+w,h);
        
        int sx = x+5+6+2;
        int sy = y+5+6+2;
        
        int tempA[][]=null, tempB[][]=null, tempC[][]=null, tempD[][]=null;
        int tempE[][]=null, tempF[][]=null, tempG[][]=null;
                
        tempF=drawVerticalBar(g, sx, sy, border, (isON('f') ? color : Color.DARK_GRAY)); 
        tempE=drawVerticalBar(g, tempF[0][3]-1, tempF[1][3], border, (isON('e') ? color : Color.DARK_GRAY));         
        tempB=drawVerticalBar(g, sx+49, sy, border, (isON('b') ? color : Color.DARK_GRAY));                        
        tempC=drawVerticalBar(g, tempB[0][3]-1, tempB[1][3], border, (isON('c') ? color : Color.DARK_GRAY));  
       
        tempA=drawHorizontalBar(g, sx, sy, border, (isON('a') ? color : Color.DARK_GRAY));
        tempG=drawHorizontalBar(g, tempF[0][3], tempF[1][3], border, (isON('g') ? color : Color.DARK_GRAY));
        tempD=drawHorizontalBar(g, tempE[0][3], tempE[1][3], border, (isON('d') ? color : Color.DARK_GRAY));
        
        g.setColor((isON('.') ? color : Color.DARK_GRAY));
        g.fillOval(tempC[0][4], tempD[1][2], 13, 13);
    }
    
    private int[][] drawHorizontalBar(Graphics g, int x1, int y1, Color border, Color fill)
    {
        /*
                2_________3
              1 /         \ 4
                \_________/
                5         6
        */
        
        
        int x5 = x1+5;          
        int y5 = y1+5;            
        
        int x2 = x5;          
        int y2 = y1-5;          
        
        int x3 = x2+38;
        int y3 = y2;
        
        int x4 = x3+5;             
        int y4 = y1; 
        
        int x6 = x3;
        int y6 = y5;
        
        int xPoints[] = { x1, x2, x3, x4, x6, x5 };
        int yPoints[] = { y1, y2, y3, y4, y6, y5 };
        int allPoints[][] = new int[2][];
        allPoints[0]=xPoints;
        allPoints[1]=yPoints;
        
        g.setColor(fill);
        g.fillPolygon(xPoints, yPoints, 6);
        
        g.setColor(border);
        g.drawPolygon(xPoints, yPoints, 6);
        
        return allPoints;
    }
    
    private int[][] drawVerticalBar(Graphics g, int x1, int y1, Color border, Color fill)
    {
        /*
                 1
                /\
              2|  |3
               |  |
               |  |
              4 \/ 5
                6
        */
        
        
        int x2 = x1-5;          
        int y2 = y1+3;          
        
        int x3 = x1+5;          
        int y3 = y2;            
        
        int x4 = x2-5;             
        int y4 = y2+37;             
        
        int x5 = x4+10;
        int y5 = y4;
        
        int x6 = x4+5;
        int y6 = y4+3;
        
        int xPoints[] = { x1, x2, x4, x6, x5, x3 };
        int yPoints[] = { y1, y2, y4, y6, y5, y3 };
        int allPoints[][] = new int[2][];
        allPoints[0]=xPoints;
        allPoints[1]=yPoints;
        
        g.setColor(fill);
        g.fillPolygon(xPoints, yPoints, 6);
        
        g.setColor(border);
        g.drawPolygon(xPoints, yPoints, 6);
        
        return allPoints;
    }
    
    @Override
    public String getPropertiesAsString(ArrayList<CircuitComponent> components)
    {
        // <super>|breadboard-index;pin10holeid;isCommonCathode;color
        int bi = CircuitUtility.getIndexFromComponentReference(components, base);
        return super.getPropertiesAsString(components)+"|"+bi+";"+topLeftHoleID+";"+commonCathode+";"+color.getRGB();
    }
    
    // Factory method
    public static SevenSegmentLED parseFromString(String data, CircuitComponent list[])
    {    
        String x[] = data.substring(data.indexOf('|')+1).split(";");
        
        Breadboard b = (Breadboard)list[Integer.parseInt(x[0])];
        int topLeftHoleID = Integer.parseInt(x[1]);
        boolean isCommonCathode=Boolean.parseBoolean(x[2]);
        Color color = new Color(Integer.parseInt(x[3]));
        
        return(new SevenSegmentLED(b, topLeftHoleID, isCommonCathode, color));
    }
}
