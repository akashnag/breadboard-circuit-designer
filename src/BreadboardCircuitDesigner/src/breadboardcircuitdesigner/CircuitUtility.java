// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.util.ArrayList;

class CircuitUtility 
{
    public static int getIndexFromComponentReference(java.util.ArrayList<CircuitComponent> components, CircuitComponent c)
    {
        int n = components.size();
        for(int i=0; i<n; i++)
        {
            if(c==components.get(i)) return i;
        }
        return -1;
    }
    
    public static ComponentType getComponentTypeFromString(String s)
    {
        // componentType;name;location;maxHoles;width;height
        String x = s.substring(0,s.indexOf(';')).trim().toUpperCase();
        ComponentType t = ComponentType.valueOf(x);
        return t;
    }
    
    public static ArrayList<Wire> getWiresOn(Breadboard base, ArrayList<Wire> allWires, WireConnectionType type)
    {
        ArrayList<Wire> result = new ArrayList<Wire>();
        
        int n = allWires.size();
        for(int i=0; i<n; i++)
        {
            Wire w = allWires.get(i);
            
            Breadboard source = (w.getSource().getType()==ComponentType.BREADBOARD ? (Breadboard)w.getSource() : null);
            Breadboard sink = (w.getSink().getType()==ComponentType.BREADBOARD ? (Breadboard)w.getSink() : null);
            
            if(source!=base && sink!=base) continue;
            
            if(type==WireConnectionType.ANY)
                result.add(w);
            else if(type==WireConnectionType.ON_SAME_COMPONENT && source==sink)
                result.add(w);
            else if(type==WireConnectionType.ON_DIFFERENT_COMPONENTS && source!=sink)
                result.add(w);
        }
        
        return result;
    }
    
    public static ArrayList<LED> getLEDs(ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        Object x[] = categorizeComponents(components,wires);
        return((ArrayList<LED>)x[5]);
    }
    
    public static ArrayList<Resistor> getResistors(ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        Object x[] = categorizeComponents(components,wires);
        return((ArrayList<Resistor>)x[4]);
    }
    
    public static ArrayList<IC> getICsOn(Breadboard b, ArrayList<CircuitComponent> components)
    {
        ArrayList<IC> ics = new ArrayList<IC>();
        int cc = components.size();
        
        for(int i=0; i<cc; i++)
        {
            if(components.get(i).getType()!=ComponentType.IC) continue;
            
            IC ic = (IC)components.get(i);
            if(b==ic.getBase()) ics.add(ic);
        }
        
        return ics;
    }
    
    public static ArrayList<Resistor> getResistorsOn(Breadboard b, ArrayList<Resistor> res)
    {
        ArrayList<Resistor> r = new ArrayList<Resistor>();
        int n = res.size();
        for(int i=0; i<n; i++)
        {
            Resistor t = res.get(i);
            if(t.getBase()==b) r.add(t);
        }
        
        return r;
    }
    
    public static Object[] categorizeComponents(ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        ArrayList<Breadboard> bb = new ArrayList<Breadboard>();
        ArrayList<PowerSupply> ps = new ArrayList<PowerSupply>();
        ArrayList<IC> ic = new ArrayList<IC>();
        ArrayList<Resistor> res = new ArrayList<Resistor>();
        ArrayList<LED> led = new ArrayList<LED>();
        
        int n = components.size();
        for(int i=0; i<n; i++)
        {
            CircuitComponent c = components.get(i);
            ComponentType type = c.getType();
            
            switch(type)
            {
                case BREADBOARD:
                    bb.add((Breadboard)c);
                    break;
                    
                case POWER_SUPPLY:
                    ps.add((PowerSupply)c);
                    break;
                    
                case IC:
                    ic.add((IC)c);
                    break;
                    
                case RESISTOR:
                    res.add((Resistor)c);
                    break;
                    
                case LED:
                    led.add((LED)c);
                    break;
            }
        }
        
        Object result[] = { bb, ps, wires, ic, res, led };
        return result;
    }
    
    protected static int[] getPowerSupplyTargetHoleIDs(PowerSupply p, Breadboard b, ArrayList<Wire> wires)
    {
        int hid[] = new int[3];
        int wc = wires.size();
        for(int i=0; i<wc; i++)
        {
            Wire w = wires.get(i);
            if(w.getSource()==p && w.getSink()==b) 
                hid[w.getSourceHoleID()-1]=w.getSinkHoleID();
            else if(w.getSink()==p && w.getSource()==b) 
                hid[w.getSinkHoleID()-1]=w.getSourceHoleID();            
        }
        return hid;
    }
    
    
}
