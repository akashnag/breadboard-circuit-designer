// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.awt.Graphics;
import java.awt.Font;
import java.awt.Color;
import java.util.ArrayList;

class PowerSupply extends CircuitComponent
{
    private static int counter = 0;
    
    static final int MAX_HOLES = 3;
    static final int WIDTH = 170;
    static final int HEIGHT = 160;
    
    private boolean isPowerON=false;
    private boolean isClockON=false;
    private boolean isClockPulseHigh=false;
    
    public static final float DUTY_CYCLE = 0.5f;
    private double frequency;
    private boolean holeCurrents[];
    
    PowerSupply(Location pos)
    {
        super(ComponentType.POWER_SUPPLY,getNewName(),pos,WIDTH,HEIGHT,MAX_HOLES);
        holeCurrents=new boolean[3];
    }
    
    private static String getNewName()
    {
        return("PowerSupply_"+(++counter));
    }
    
    public boolean isPoweredOn() { return isPowerON; }
    public boolean isClockOn() { return isClockON; }
    public boolean isClockPulseHigh() { return isClockPulseHigh; }
    
    public double getFrequency() { return frequency; }
    public void setFrequency(double f) { frequency=f; }            
    
    public void trigger()
    {
        if(!isPowerON || !isClockON) return;
        // ASSUMING trigger() has been called at the right-time (i.e. after proper frequency duration)
        
        isClockPulseHigh=!isClockPulseHigh;
    }
    
    public boolean click(int x, int y)
    {
        // WARNING: global coordinates
        Location loc = getLocation();
        int lx = loc.x, ly = loc.y;
        
        // Click on power button to toggle
        if(x>=lx+81 && y>=ly+96)
        {
            if(x<=lx+81+80 && y<=ly+96+18)
            {
                isPowerON = !isPowerON;
                setHoleCurrent(1,isPowerON);
                return true;
            }
        }
        
        // Click on green clock-LED to change frequency
        if(x>=lx+150 && y>=ly+20)
        {
            if(x<=lx+160 && y<=ly+30)
            {
                String p=Utility.inputBox("Enter frequency (in Hertz) [duty-cycle=50%]:", ""+frequency);
                if(p!=null) frequency=Double.parseDouble(p);
                return true;
            }
        }
                
        // click on Clock Power button to toggle
        if(x>=lx+81 && y>=ly+126)
        {
            if(x<=lx+81+80 && y<=ly+126+18)
            {
                isClockON=!isClockON;
                return true;
            }
        }
        
        return false;
    }
    
    public int getHoleCurrent(int holeID)
    {
        return(holeCurrents[holeID-1] ? 1 : 0);
    }
    
    public boolean isHoleHigh(int holeID)
    {
        return holeCurrents[holeID-1];
    }
    
    public void setHoleCurrent(int holeID, boolean setToHigh)
    {
        holeCurrents[holeID-1]=setToHigh;
    }
    
    @Override
    public boolean isHoleBlocked(int holeID, ArrayList<Wire> wires, ArrayList<CircuitComponent> components)
    {
        int n = wires.size();
        
        for(int i=0; i<n; i++)
        {
            Wire w = wires.get(i);
            if(w.getSource().getType()==ComponentType.POWER_SUPPLY)
            {
                PowerSupply p = (PowerSupply)w.getSource();
                if(p==this && holeID==w.getSourceHoleID()) return true;
            }
            
            if(w.getSink().getType()==ComponentType.POWER_SUPPLY)
            {
                PowerSupply p = (PowerSupply)w.getSink();
                if(p==this && holeID==w.getSinkHoleID()) return true;
            }
        }
        
        return false;
    }
    
    @Override
    public int getHoleRadius() { return 15; }
    
    @Override
    public Location getHoleCenter(int holeID)
    {
        // HoleID = 1(VCC), 2(GND), 3(CLK)
        Location loc = getLocation();
        int x = loc.x;
        int y = loc.y;
        
        int px[] = { 25, 65, 105 };
        return(new Location(x+15+px[holeID-1],y+50+15));
    }
            
    public HoleState getOutputFromHole(int holeID)
    {
        if(holeID==1)
        {
            return(isPowerON ? HoleState.HIGH : HoleState.NO_CONNECTION);
        } else if(holeID==2) {
            return(isPowerON ? HoleState.LOW : HoleState.NO_CONNECTION);
        } else if(holeID==3) {
            if(!isPowerON) return HoleState.NO_CONNECTION;
            if(!isClockON) return HoleState.NO_CONNECTION;
            return(isClockPulseHigh ? HoleState.HIGH : HoleState.NO_CONNECTION);
        }
        return HoleState.LOW;       // dummy
    }
    
    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        
        Location loc = getLocation();
        int x = loc.x;
        int y = loc.y;
        
        g.setFont(new Font("Lucida Console", Font.BOLD, 14));
        
        g.setColor(getAlphaMix(new Color(0,116,186)));
        g.fillRect(x,y,WIDTH,HEIGHT);
        
        g.setColor(getAlphaMix(Color.WHITE));
        g.drawRect(x,y,WIDTH,HEIGHT);
        
        g.drawString("5V ", x+30, y+30);
        g.drawString("GND", x+70, y+30);
        g.drawString("CLK", x+110, y+30);
        
        g.fillOval(x+25, y+50, 30, 30);
        g.fillOval(x+65, y+50, 30, 30);
        g.fillOval(x+105, y+50, 30, 30);
        
        g.setColor(getAlphaMix(new Color(0,116,186)));
        g.fillOval(x+28, y+53, 24, 24);
        g.fillOval(x+68, y+53, 24, 24);
        g.fillOval(x+108, y+53, 24, 24);
        
        if(isPowerON)           // Red-aura
        {
            g.setColor(getTranslucent(Color.RED));
            g.fillOval(x+7,y+17,16,16);
        }
        
        g.setColor(getAlphaMix( isPowerON ? Color.RED : new Color(173,0,0) ));
        g.fillOval(x+10,y+20,10,10);
        
        
        if(isClockON && isClockPulseHigh)   // green-aura
        {
            g.setColor(getTranslucent(Color.GREEN));
            g.fillOval(x+147,y+17,16,16);
        }
        
        g.setColor(getAlphaMix( isClockON && isClockPulseHigh ? Color.GREEN : new Color(0,163,108) ));
        g.fillOval(x+150,y+20,10,10);
        
        g.setColor(getAlphaMix(Color.WHITE));
        g.drawString("POWER", x+20, y+110);
        g.drawString("CLOCK", x+20, y+140);
        
        g.drawRect(x+80,y+95,80,20);
        g.drawRect(x+80,y+125,80,20);
        
        g.setFont(new Font("Lucida Console", Font.BOLD, 10));
        
        g.setColor(getAlphaMix(isPowerON ? Color.BLACK : Color.WHITE));
        g.fillRect(x+81,y+96,38,18);
        g.setColor(getAlphaMix(!isPowerON ? Color.BLACK : Color.WHITE));
        g.drawString("OFF", x+90, y+108);
        g.setColor(getAlphaMix(!isPowerON ? Color.BLACK : Color.WHITE));
        g.fillRect(x+81+40,y+96,38,18);
        g.setColor(getAlphaMix(isPowerON ? Color.BLACK : Color.WHITE));
        g.drawString("ON", x+81+49, y+108);
        
        g.setColor(getAlphaMix(isClockON ? Color.BLACK : Color.WHITE));
        g.fillRect(x+81,y+126,38,18);
        g.setColor(getAlphaMix(!isClockON ? Color.BLACK : Color.WHITE));
        g.drawString("OFF", x+90, y+138);
        g.setColor(getAlphaMix(!isClockON ? Color.BLACK : Color.WHITE));
        g.fillRect(x+81+40,y+126,38,18);
        g.setColor(getAlphaMix(isClockON ? Color.BLACK : Color.WHITE));
        g.drawString("ON", x+81+49, y+138);
    }
    
    @Override
    public String getPropertiesAsString(ArrayList<CircuitComponent> components)
    {
        // componentType;name;location;maxHoles;width;height;frequency
        return super.getPropertiesAsString(components)+";"+this.getFrequency();
    }
    
    // Factory method
    public static PowerSupply parseFromString(String data)
    {
        // componentType;name;location;maxHoles;width;height;frequency
        String x[] = data.split(";");
        if(!x[0].equalsIgnoreCase("power_supply")) return null;
        
        Location loc = new Location(x[2]);
        PowerSupply ps = new PowerSupply(loc);
        ps.setFrequency(Double.parseDouble(x[6]));
        
        return ps;
    }
}
