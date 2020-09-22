// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.awt.Graphics;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;

class Breadboard extends CircuitComponent
{
    private static int counter = 0;
    
    static final int MAX_HOLES = 830;
    static final int WIDTH = 1052;
    static final int HEIGHT = 379;
    
    private HoleState holeStates[];
    
    private int highlightedHoleID;
    private boolean holeStatusVisible;
    
    // WARNING: Do not modify these except in paint()
    private Location holeCenters[];
    private int holeIDCounter;
    // ----------------------------------------------
    
    Breadboard(Location pos)
    {
        super(ComponentType.BREADBOARD,getNewName(),pos,WIDTH,HEIGHT,MAX_HOLES);
        holeStates = new HoleState[MAX_HOLES];
        for(int i=0; i<MAX_HOLES; i++) holeStates[i]=HoleState.NO_CONNECTION;
        highlightedHoleID=-1;
    }
    
    private static String getNewName()
    {
        return("Breadboard_"+(++counter));
    }
    
    public void clearHighlights() { highlightedHoleID=-1; }
    public int getHighlightedHoleID() { return highlightedHoleID; }
    public void setHighlight(int hid) { highlightedHoleID=hid; }
    
    public void showHoleStatus() { holeStatusVisible=true; }
    public void hideHoleStatus() { holeStatusVisible=false; }
    public boolean isHoleStatusVisible() { return holeStatusVisible; }
    
    public static int getDistanceBetweenHoles(int holeCount)
    {
        Breadboard temp = new Breadboard(new Location(40,40));
        int n = (int)temp.getHoleCenter(1).getDistanceTo(temp.getHoleCenter(2));
        return(n*holeCount);
    }
    
    public boolean isHoleShadowed(int holeID, ArrayList<Wire> wires, ArrayList<CircuitComponent> components)
    {
        // Only ICs and 7-Segment LEDs can shadow holes
        // This function returns true if hole is either blocked or shadowed
        
        if(isHoleBlocked(holeID,wires,components)) return true;
        
        int n = components.size();
        for(int i=0; i<n; i++)
        {
            CircuitComponent c = components.get(i);
            int hid[] = null;
            if(c.getType()==ComponentType.IC)
            {
                hid = ((IC)c).getShadowedHoles();
            } else if(c.getType()==ComponentType.SEVEN_SEGMENT_LED) {
                hid = ((SevenSegmentLED)c).getShadowedHoles();
            }
            
            if(hid!=null)
            {
                for(int j=0; j<hid.length; j++)
                {
                    if(holeID==hid[j]) return true;
                }
            }
        }
        
        return false;
    }
    
    public HoleState getHoleState(int holeID)
    {
        return(holeStates[holeID-1]);
    }
        
    public void setHoleStates(HoleState states[])
    {
        holeStates=states;
    }
    
    public HoleState[] getHoleStates() { return holeStates; }
    
    @Override
    public boolean isHoleBlocked(int holeID, ArrayList<Wire> wires, ArrayList<CircuitComponent> components)
    {
        int nc = components.size(), nw = wires.size();
        HashSet<Integer> blocked = new HashSet<Integer>();
        
        for(int i=0; i<nc; i++)
        {
            CircuitComponent c = components.get(i);
            if(c.getType()==ComponentType.BREADBOARD || c.getType()==ComponentType.POWER_SUPPLY) continue;
            
            if(c.getType()==ComponentType.RESISTOR)
            {
                Resistor r = (Resistor)c;
                if(r.getBase()!=this) continue;
                
                blocked.add(r.getSourceHoleID());
                blocked.add(r.getSinkHoleID());
            } else if(c.getType()==ComponentType.LED) {
                LED l = (LED)c;
                if(l.getBase()!=this) continue;
                
                blocked.add(l.getAnodeHoleID());
                blocked.add(l.getCathodeHoleID());
            } else if(c.getType()==ComponentType.IC) {
                int hids[] = ((IC)c).getHoleIDs();
                for(int p:hids) blocked.add(p);
            } else if(c.getType()==ComponentType.SEVEN_SEGMENT_LED) {
                int hids[] = ((SevenSegmentLED)c).getHoleIDs();
                for(int p:hids) blocked.add(p);
            }
        }
        
        for(int i=0; i<nw; i++)
        {
            Wire w = wires.get(i);
            
            if(w.getSource().getType()==ComponentType.BREADBOARD)
            {
                Breadboard b=(Breadboard)w.getSource();
                if(b==this)
                {
                    blocked.add(w.getSourceHoleID());
                }
            }
            
            if(w.getSink().getType()==ComponentType.BREADBOARD)
            {
                Breadboard b=(Breadboard)w.getSink();
                if(b==this)
                {
                    blocked.add(w.getSinkHoleID());
                }
            }
        }
        
        return(blocked.contains(holeID));
    }
    
    @Override
    public int getHoleRadius() { return 3; }
    
    @Override
    public Location getHoleCenter(int holeID)
    {
        // 4 power rails (2 above, 2 below): each containing 50 holes
        // general: 10 rows, each containing 63 holes
        // TOTAL = 200 + 630 = 830 holes (ID = 1 to 830)
        
        if(holeCenters==null)
        {
            paint(null);            
        }
        
        return holeCenters[holeID-1];
    }
    
    @Override
    public void paint(Graphics g, int holeIDs[])
    {
        // to highlight specific holes, call this function
        this.paint(g);
        
        g.setColor(getAlphaMix(Color.MAGENTA));
        for(int i=0; i<holeIDs.length; i++)
        {
            Location pos = getHoleCenter(holeIDs[i]);
            int x = pos.x, y = pos.y;
            g.fillOval(x-3, y-3, 6, 6);
            g.drawOval(x-6, y-6, 12, 12);
        }
    }
    
    @Override
    public void paint(Graphics g)
    {
        if(g!=null) super.paint(g);
        
        // initialize (these are set in drawHole()
        holeIDCounter=-1;
        holeCenters=new Location[830];
        
        Location loc = getLocation();
        int x = loc.x;
        int y = loc.y;
        
        if(g!=null)
        {
            g.setColor(getAlphaMix(new Color(232,232,232)));
            g.fillRect(x,y,WIDTH,HEIGHT);
        }
        
        int rowOffsetX = 62, colOffsetX = 30, offsetY = 30, rowGap = 16;
        
        // draw the top power-rails (ID=1 to 100 (1st row: 1-50, 2nd row: 51-100)
        for(int i=0; i<2; i++)
        {
            int hy = offsetY + (i * rowGap);
            for(int j=0; j<10; j++)
            {
                for(int k=0; k<5; k++)
                {
                    int hx = (rowOffsetX + (k*16) + (j * ((5*16)+16)));                    
                    drawHole(g,x+hx,y+hy);               
                }
            }
        }
        
        // draw the bottom power-rails (ID=101 to 200 (1st row: 101-150, 2nd row: 151-200)
        for(int i=0; i<2; i++)
        {
            int hy = 304 + 29 + (i * rowGap);
            for(int j=0; j<10; j++)
            {
                for(int k=0; k<5; k++)
                {
                    // hole-radius = 2, gap=1 on ALL sides
                    int hx = (rowOffsetX + (k*16) + (j * ((5*16)+16)));                    
                    drawHole(g,x+hx,y+hy);               
                }
            }
        }
        
        // draw top-divider
        if(isLocked() && g!=null)          // performance optimization
        {
            g.setColor(getAlphaMix(new Color(208,208,208)));
            g.drawLine(x,y+75,x+WIDTH-1,y+75);
            g.setColor(getAlphaMix(new Color(191,191,191)));
            g.drawLine(x,y+76,x+WIDTH-1,y+76);
        }
        
        // draw top columns (ID=201 to 515)
        for(int i=0; i<5; i++)
        {
            int hy = 76 + 26 + (i * 16);
            for(int j=0; j<63; j++)
            {
                int hx = colOffsetX + (j * 16);
                drawHole(g,x+hx,y+hy);
            }
        }
        
        // draw bridge
        if(isLocked() && g!=null)          // performance optimization
        {
            g.setColor(getAlphaMix(new Color(220,220,220)));
            g.fillRect(x+colOffsetX,y+(76+26+64+10+6),(WIDTH-(2*colOffsetX)),16);
        }
        
        // draw bottom columns (ID=516 to 830)
        for(int i=0; i<5; i++)
        {
            int hy = 166 + 10 + (6+10+6+10+6) + (i * 16);
            for(int j=0; j<63; j++)
            {
                int hx = colOffsetX + (j * 16);
                drawHole(g,x+hx,y+hy);
            }
        }
        
        // draw bottom-divider
        if(isLocked() && g!=null)          // performance optimization
        {
            g.setColor(getAlphaMix(new Color(208,208,208)));
            g.drawLine(x,y+303,x+WIDTH-1,y+303);
            g.setColor(getAlphaMix(new Color(191,191,191)));
            g.drawLine(x,y+304,x+WIDTH-1,y+304);
        }
    }
    
    private void drawHole(Graphics g, int x, int y)
    {
        holeCenters[++holeIDCounter]=new Location(x,y);
        if(g==null) return;
        
        if(isLocked())          // performance optimization
        {
            g.setColor(getAlphaMix(new Color(193,193,193)));        // top shadow
            g.fillArc(x-5, y-5, 10, 10, 180, -180);

            g.setColor(getAlphaMix(new Color(227,227,227)));        // bottom shadow
            g.fillArc(x-5, y-5, 10, 10, 0, -180);
        }
                    
        g.setColor(getAlphaMix(new Color(53,53,53)));           // hole
        g.fillOval(x-3, y-3, 6, 6);
        
        if(isLocked())
        {
            if(holeStatusVisible && holeStates[holeIDCounter]!=HoleState.NO_CONNECTION)
            {
                g.setColor(holeStates[holeIDCounter]==HoleState.HIGH ? Color.RED : Color.BLUE);
                g.drawOval(x-7, y-7, 14, 14);
            }
            
            if(holeIDCounter==highlightedHoleID-1)
            {
                g.setColor(Color.GREEN);
                g.drawOval(x-8, y-8, 16, 16);
                highlightedHoleID=-1;
            }
        }
    }
    
    @Override
    public String getPropertiesAsString(ArrayList<CircuitComponent> components)
    {
        return super.getPropertiesAsString(components);
    }
    
    public static Breadboard parseFromString(String s)
    {
        // componentType;name;location;maxHoles;width;height
        String x[] = s.split(";");
        if(!x[0].equalsIgnoreCase("breadboard")) return null;
        
        Location loc = new Location(x[2]);
        Breadboard b = new Breadboard(loc);
        
        b.setName(x[1]);
        return b;
    }
}
