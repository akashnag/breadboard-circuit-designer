// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.io.*;
import java.util.ArrayList;

class FileIO 
{
    private static final double VERSION = 1.0;
    
    /*
        ------------------------------
                FILE FORMAT (*.bcf)
        ------------------------------
        BCF,<VERSION>
        <datasheetPath>
        <Width,Heigh>
        <# of components>,<# of wires>
        <component-data>
        <wire-data>
        ------------------------------
    */
    
    protected static void saveData(MainWindow.DrawingPane pane, String filePath) throws IOException
    {
        BufferedWriter bw = new BufferedWriter(new FileWriter(filePath));
        
        writeLine(bw,"BCF,"+VERSION);
        writeLine(bw,MainWindow.circuitWidth+","+MainWindow.circuitHeight);
        
        ArrayList<CircuitComponent> components = pane.getAllComponents();
        ArrayList<Wire> wires = pane.getAllWires();
        
        int cc = components.size();
        int wc = wires.size();
        
        writeLine(bw,cc+","+wc);
        
        for(int i=0; i<cc; i++) writeLine(bw,components.get(i).getPropertiesAsString(components));
        for(int i=0; i<wc; i++) writeLine(bw,wires.get(i).getPropertiesAsString(components));
                
        bw.close();
    }
    
    private static void writeLine(BufferedWriter bw, String data) throws IOException
    {
        if(bw==null) return;
        bw.write(data+"\n");
    }
    
    protected static Object[] openCircuit(String filePath) throws Exception
    {
        // ----------------------
        // 1st Pass (for independent components)
        // ----------------------
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String s = br.readLine();
        if(!s.substring(0,3).equalsIgnoreCase("bcf"))
        {
            Utility.alert("Invalid file format detected.");
            br.close();
            return null;
        }
        
        if(Double.parseDouble(s.substring(s.indexOf(',')+1))!=VERSION)
        {
            Utility.alert("This file was created with a different version of this software, which is not supported.");
            br.close();
            return null;
        }
        
        String x[] = br.readLine().split(",");
        MainWindow.circuitWidth = Integer.parseInt(x[0]);
        MainWindow.circuitHeight = Integer.parseInt(x[1]);
        
        x = br.readLine().split(",");
        int cc = Integer.parseInt(x[0]), wc = Integer.parseInt(x[1]);
        
        CircuitComponent compArray[] = new CircuitComponent[cc];
        for(int i=0; i<cc; i++)
        {
            s=br.readLine();
            ComponentType t = CircuitUtility.getComponentTypeFromString(s);
            
            CircuitComponent c = null;
            if(t==ComponentType.BREADBOARD) {
                MainWindow.breadboardCounter++;
                c = Breadboard.parseFromString(s);
            } else if(t==ComponentType.POWER_SUPPLY) {
                c = PowerSupply.parseFromString(s);
                MainWindow.powerSupplyCounter++;
            }
            
            if(c!=null) c.lock();
            compArray[i]=c;
        }
        br.close();
        // ---------------------------------
        
        // ---------------------------------
        // 2nd Pass (for dependent components)
        // ---------------------------------
        ArrayList<CircuitComponent> components=new ArrayList<CircuitComponent>(cc);
        ArrayList<Wire> wires=new ArrayList<Wire>(wc);
        
        br = new BufferedReader(new FileReader(filePath));
        br.readLine();          // file format and version
        br.readLine();          // circuit dimensions
        br.readLine();          // no. of components, and no. of wires
        
        for(int i=0; i<cc; i++)
        {
            s=br.readLine();
            ComponentType t = CircuitUtility.getComponentTypeFromString(s);
            
            CircuitComponent c = null;
            
            switch(t)
            {
                case RESISTOR:
                    c = Resistor.parseFromString(s, compArray);
                    break;
                    
                case LED:
                    c = LED.parseFromString(s, compArray);
                    break;
                    
                case IC:
                    c = IC.parseFromString(s, compArray);
                    break;
                    
                case SEVEN_SEGMENT_LED:
                    c = SevenSegmentLED.parseFromString(s, compArray);
                    break;
                    
                default:
                    continue;
            }
            
            compArray[i]=c;
        }
        for(int i=0; i<cc; i++) components.add(compArray[i]);
        for(int i=0; i<wc; i++) wires.add(Wire.parseFromString(br.readLine(),components));
        br.close();
        // -------------------------------------
        
        
        
        return(new Object[] { components, wires });
    }
}
