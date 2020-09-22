// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import static breadboardcircuitdesigner.BreadboardUtility.*;
import static breadboardcircuitdesigner.CircuitUtility.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

class Simulation 
{
    private static boolean conMatrix[][];
    private static boolean IODependencyMatrix[][];
    
    private static boolean isCyclic;
    private static boolean isHalfCyclic;
    private static boolean stopSimulation;
    
    public static boolean simulationRunning;
    public static final int MAX_ITERATIONS = 10;
    
    /*
            FUNCTION DEPENDENCIES IN THIS MODULE
            ------------------------------------------------
            simulateCircuit ---> getHoleStates
                            |
                            ---> hasStateChanged
                            |
                            ---> turnOnOffLEDs
    
            getHoleStates   ---> sortICsByDependency
                            |
                            ---> updateHoleStates
    
            sortICsByDependency     --->    getDirectHoleConnectionsMatrix
                                    |
                                    --->    areConnectedInOrder
    
            getDirectHoleConnectionsMatrix  --->    areHolesDirectlyConnectedInternally
                                            |
                                            --->    areHolesDirectlyConnectedViaWire
                                            |
                                            --->    areHolesDirectlyConnectedViaResistor
    
            areConnectedInOrder ---> areHolesConnected [depends on output of getDirectHoleConnectionsMatrix]
    */
    
    public static void stopSimulation()
    {
        stopSimulation=true;
    }
    
    public static void simulateCircuit(ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        stopSimulation=false;
        conMatrix=null;
        IODependencyMatrix=null;
        isCyclic=false;
        isHalfCyclic=false;
        
        //  WARNING: This function assumes that there is only 1 breadboard and 1 power supply
        
        // Step-1: Find the only breadboard which is connected to power supply
        Breadboard b = null;
        int cc = components.size();
        for(int i=0; i<cc; i++)
        {
            if(components.get(i).getType()==ComponentType.BREADBOARD)
            {
                // Check if it is connected to power supply
                b=(Breadboard)components.get(i);
                
                if(hasBreadboardPower(b,components,wires))
                    break;
                else
                    b=null;
            } else if(components.get(i).getType()==ComponentType.POWER_SUPPLY) {
                PowerSupply ps = (PowerSupply)components.get(i);
                if(!ps.isPoweredOn()) return;
            }
        }
        
        if(b==null) return;     // if no such breadboard then abort
        
        int c=0;
        boolean cont=true;
        
        simulationRunning=true;
        HoleState state[]=null;
        do {
            cont=true;
            state = (state==null ? getHoleStates(b,components,wires) : getHoleStates(state,b,components,wires));
            
            if(c>MAX_ITERATIONS) cont=false;
            if(c!=0 && !hasStateChanged(state,b.getHoleStates())) cont=false;            
            
            // Step-2: get the holestates
            b.setHoleStates(state);
            
            // Step-3: turn on/off LEDs
            turnOnOffLEDs(b,components);
            
            c++;
            
            //System.out.println("AFTER C="+c);
            //for(int i=0; i<state.length; i++) System.out.print(state[i].toString().charAt(0));
            //System.out.println();
            
            if(!isHalfCyclic && !isCyclic) cont=false;
            Thread.yield();
        } while(cont && !stopSimulation);
        
        simulationRunning=false;
    }
    
    private static boolean hasStateChanged(HoleState a[], HoleState b[])
    {
        for(int i=0; i<a.length; i++)
        {
            if(b[i]!=a[i]) return true;
        }
        return false;
    }
    
    private static HoleState[] getHoleStates(Breadboard b, ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        HoleState state[] = new HoleState[Breadboard.MAX_HOLES];
        for(int i=0; i<state.length; i++) state[i]=HoleState.NO_CONNECTION;
        return getHoleStates(state,b,components,wires);
    }

    private static HoleState[] getHoleStates(HoleState state[], Breadboard b, ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {    
        boolean rowMatrix[][] = getRowConnectionsMatrix(b,wires,components);
        boolean colMatrix[][] = getColumnConnectionsMatrix(b,wires,components);
        boolean rowColMatrix[][] = getRowColumnConnectionsMatrix(b,wires,components);
        
        ArrayList<CircuitComponent> external = getExternalConnectedComponents(b,wires,components);
        ArrayList<Resistor> resistors = (ArrayList<Resistor>)categorizeComponents(components,wires)[4];
        
        HashSet<Integer> visitedRows = new HashSet<Integer>();
        HashSet<Integer> visitedCols = new HashSet<Integer>();
                
        /*
            STEPS
            ----------------
            1.  (a) Scan connected power supplies
                (b) Set holestates based on VCC/GND from those power supplies
        
            2.  (a) Scan ICs
                (b) Set holestates based on IC outputs
        
            3.  Ignore other connected breadboards (for now): TOO COMPLICATED!
        */
        
        // Step-1: Scan connected power supplies
        int ec = external.size();
        for(int i=0; i<ec; i++)
        {
            if(external.get(i).getType()!=ComponentType.POWER_SUPPLY) continue;
            
            PowerSupply ps = (PowerSupply)external.get(i);
            int holeIDs[] = getPowerSupplyTargetHoleIDs(ps,b,wires);
            
            for(int j=0; j<3; j++)
            {
                if(holeIDs[j]>0)        // only for those holes which are connected to this breadboard
                {
                    int chid = holeIDs[j];
                    HoleState ns = ps.getOutputFromHole(j+1);
                    
                    updateHoleStates(chid, ns, state, rowMatrix, colMatrix, rowColMatrix, visitedRows, visitedCols);
                }
            }
        }
        
        // Step-2: Scan ICs
        ArrayList<IC> oics = getICsOn(b,components);
        ArrayList<IC> ics = sortICsByDependency(oics,wires,resistors);
        
        /*
        System.out.println("BEFORE SORTING:");
        for(int i=0; i<oics.size(); i++) System.out.println(oics.get(i).getName());
        System.out.println("\nAFTER SORTING:");
        for(int i=0; i<ics.size(); i++) System.out.println(ics.get(i).getName());
        */
            
        
        
        int icsc = ics.size();
        
        int ichids[][]=new int[icsc][];
        boolean isInput[][]=new boolean[icsc][];
        HoleState inputHoleStates[][]=new HoleState[icsc][];
        HoleState out[][]=new HoleState[icsc][];
        
        for(int iteration=1; iteration<=MAX_ITERATIONS; iteration++)
        {        
            HoleState oldState[] = new HoleState[state.length];
            System.arraycopy(state,0,oldState,0,state.length);
            
            for(int i=0; i<icsc; i++)
            {
                IC ic = ics.get(i);
                ichids[i] = ic.getHoleIDs();
                isInput[i] = ic.getPinTypes();
                inputHoleStates[i] = new HoleState[ic.getPinCount()];

                // Form the input array
                for(int j=0; j<inputHoleStates[i].length; j++)
                {
                    if(isInput[i][j])
                    {
                        // INPUT PIN
                        inputHoleStates[i][j]=state[ichids[i][j]-1];
                    } else {
                        // OUTPUT PIN
                        if(state[ichids[i][j]-1]!=HoleState.NO_CONNECTION)
                        {
                            // User has connected something (power/ground) to
                            // the output pin of IC: this is not allowed!
                            //Utility.alert("Error in circuit! No connections are allowed to output pins of ICs, but a connection has been found to output pin "+(j+1)+" of IC:" + ic.getName());
                        }

                        inputHoleStates[i][j]=state[ichids[i][j]-1]; //HoleState.NO_CONNECTION;
                    }
                }
            }

            visitedRows.clear();
            visitedCols.clear();

            for(int i=0; i<icsc; i++)
            {
                IC ic = ics.get(i);

                // Get the inputs to the IC
                //System.out.println("\nInputs for "+ic.getName());
                //for(int p=0; p<inputHoleStates[i].length; p++) System.out.print(inputHoleStates[i][p].toString().charAt(0));
                //System.out.println();

                out[i]=ic.getOutputs(inputHoleStates[i]);

                //System.out.println("Outputs for "+ic.getName());
                //for(int p=0; p<out[i].length; p++) System.out.print(out[i][p].toString().charAt(0));
                //System.out.println();


                // Update the hole states based on outputs
                for(int j=0; j<inputHoleStates[i].length; j++)
                {
                    if(!isInput[i][j])
                    {
                        // OUTPUT PIN
                        HoleState ns = out[i][j];
                        updateHoleStates(ichids[i][j], ns, state, rowMatrix, colMatrix, rowColMatrix, visitedRows, visitedCols);
                    }
                }
            }

            //printState(oldState);
            //printState(state);

            if(!hasStateChanged(state,oldState)) return state;
            //System.out.println("NOT EQUAL, SO CONTINUING...");
        }
        
        return state;
    }
     
    private static void printState(HoleState state[])
    {
        System.out.println();
        for(HoleState s:state) System.out.print(s.toString().charAt(0));
        System.out.println();
    }
    
    private static void turnOnOffLEDs(Breadboard b, ArrayList<CircuitComponent> components)
    {
        HoleState state[] = b.getHoleStates();
        int cc = components.size();
        
        for(int i=0; i<cc; i++)
        {
            if(components.get(i).getType()==ComponentType.LED)
            {
                LED led = (LED)components.get(i);
                if(led.getBase()==b)
                {
                    HoleState anodeState = state[led.getAnodeHoleID()-1];
                    HoleState cathodeState = state[led.getCathodeHoleID()-1];

                    if(anodeState==HoleState.HIGH && cathodeState==HoleState.LOW)
                        led.turnON();
                    else
                        led.turnOFF();
                }
            } else if(components.get(i).getType()==ComponentType.SEVEN_SEGMENT_LED) {
                SevenSegmentLED led = (SevenSegmentLED)components.get(i);
                if(led.getBase()==b)
                {
                    boolean commonCathode = led.isCommonCathode();
                    int commonHoleIDs[] = led.getCommonHoleIDs();
                    
                    HoleState com1 = state[commonHoleIDs[0]-1];
                    HoleState com2 = state[commonHoleIDs[1]-1];
                                        
                    if(commonCathode)
                    {
                        // common holes must be grounded
                        if(com1!=HoleState.LOW || com2!=HoleState.LOW)
                        {
                            led.turnOFF();
                            continue;
                        }
                    } else {
                        if(com1!=HoleState.HIGH || com2!=HoleState.HIGH)
                        {
                            led.turnOFF();
                            continue;
                        }
                    }
                    
                    HoleState y[]=new HoleState[8];
                    String x = "abcdefg.";
                    for(int j=0; j<8; j++)
                    {
                        int hid = led.getHoleID(x.charAt(j));
                        y[j]=state[hid-1];
                    }
                    led.turnOnOffSegments(y);
                }
            }
        }
    }
    
    private static boolean updateHoleStates(Breadboard b, int holeID, HoleState newState, HoleState state[], HashSet<Integer> visitedRows, HashSet<Integer> visitedCols, ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        boolean rowMatrix[][] = getRowConnectionsMatrix(b,wires,components);
        boolean colMatrix[][] = getColumnConnectionsMatrix(b,wires,components);
        boolean rowColMatrix[][] = getRowColumnConnectionsMatrix(b,wires,components);
        
        return updateHoleStates(holeID,newState,state,rowMatrix,colMatrix,rowColMatrix,visitedRows,visitedCols);
    }
    
    private static boolean updateHoleStates(int holeID, HoleState newState, HoleState state[], boolean rowMatrix[][], boolean colMatrix[][], boolean rowColMatrix[][], HashSet<Integer> visitedRows, HashSet<Integer> visitedCols)
    {
        // Update only if NO_CONNECTION
        //if(state[holeID-1]!=HoleState.NO_CONNECTION) return false;
        
        int ids[];
        state[holeID-1]=newState;
        if(isHoleInRow(holeID))
        {
            ids = getHolesInRow(getRowIDFromHoleID(holeID));
        } else {
            ids = getHolesInColumn(getColumnIDFromHoleID(holeID));
        }
        
        for(int i=0; i<ids.length; i++)
        {
            state[ids[i]-1]=newState;
        }
        
        if(isHoleInRow(holeID))
        {
            int rid = getRowIDFromHoleID(holeID);
            visitedRows.add(rid);
            
            //Utility.alert("ROWID="+rid);
            
            // scan row-connections
            for(int i=0; i<rowMatrix[rid-1].length; i++)
            {
                if(i==rid-1) continue;
                if(rowMatrix[rid-1][i])
                {
                    // has connection to (i+1)-th row                    
                    if(!visitedRows.contains(i+1))
                    {
                        int frid = getFirstHoleIDInRow(i+1);
                        updateHoleStates(frid, newState, state, rowMatrix, colMatrix, rowColMatrix, visitedRows, visitedCols);
                    }
                }
            }
            
            // scan row-col connections
            for(int i=0; i<rowColMatrix[rid-1].length; i++)
            {
                if(rowColMatrix[rid-1][i])
                {
                    //Utility.alert("ROW has connection to "+(i+1)+" col");
                    // has connection to (i+1)-th col
                    if(!visitedCols.contains(i+1))
                    {
                        //Utility.alert("NOT VISITED");
                        
                        visitedCols.add(i+1);
                        int fcid = getFirstHoleIDInColumn(i+1);
                        updateHoleStates(fcid, newState, state, rowMatrix, colMatrix, rowColMatrix, visitedRows, visitedCols);
                    }
                }
            }
        } else {
            int cid = getColumnIDFromHoleID(holeID);
            visitedCols.add(cid);
            
            for(int i=0; i<colMatrix[cid-1].length; i++)
            {
                if(i==cid-1) continue;
                if(colMatrix[cid-1][i])
                {
                    // has connection to (i+1)-th column                    
                    if(!visitedCols.contains(i+1))
                    {
                        int fcid = getFirstHoleIDInColumn(i+1);
                        updateHoleStates(fcid, newState, state, rowMatrix, colMatrix, rowColMatrix, visitedRows, visitedCols);
                    }
                }
            }
            
            // scan row-col connections
            for(int i=0; i<rowColMatrix.length; i++)
            {
                if(rowColMatrix[i][cid-1])
                {
                    // has connection to (i+1)-th row                    
                    if(!visitedRows.contains(i+1))
                    {
                        visitedRows.add(i+1);
                        int frid = getFirstHoleIDInRow(i+1);
                        updateHoleStates(frid, newState, state, rowMatrix, colMatrix, rowColMatrix, visitedRows, visitedCols);
                    }
                }
            }
        }
        
        return true;
    }
    
    private static ArrayList<IC> sortICsByDependency(ArrayList<IC> list, ArrayList<Wire> wires, ArrayList<Resistor> resistors)
    {
        // WARNING: Assuming only 1 breadboard exists
        if(list.size()==0) return list;
        
        Breadboard b = list.get(0).getBase();
        
        if(conMatrix==null)         // speed-optimization, do not recalculate: conMatrix is set to null each time simulateCircuit is called [whenever circuit changes]
        {
            conMatrix=getDirectHoleConnectionsMatrix(b,wires,resistors);
        }
        
        // Convert to array
        IC array[] = new IC[list.size()];
        for(int i=0; i<array.length; i++) array[i]=list.get(i);
        
        // Step-1: Create the adjacency matrix of all IC output->input connections
        if(IODependencyMatrix==null)    // Speed-optimization
        {            
            IODependencyMatrix=new boolean[array.length][array.length];

            for(int i=0; i<array.length; i++)
            {
                int pcI = array[i].getPinCount();

                for(int j=0; j<array.length; j++)
                {
                    //if(i==j) continue;

                    int pcJ = array[j].getPinCount();
                    for(int pc1=1; pc1<=pcI; pc1++)
                    {
                        if(array[i].isInputPin(pc1)) continue;
                        for(int pc2=1; pc2<=pcJ; pc2++)
                        {
                            if(array[j].isOutputPin(pc2)) continue;

                            if(i==j && pc1==pc2) continue;              // can be same IC but cannot be same pin of the same IC simultaneously
                            
                            if(areConnectedInOrder(array[i],pc1,array[j],pc2,wires,resistors))
                            {
                                IODependencyMatrix[i][j]=true;
                                isHalfCyclic=true;
                            }
                        }
                    }
                }
            }


            // Check if cyclic
            for(int i=0; i<IODependencyMatrix.length; i++)
            {
                for(int j=0; j<IODependencyMatrix[i].length; j++)
                {
                    if(IODependencyMatrix[i][j] && IODependencyMatrix[j][i])
                    {
                        isCyclic=true;
                        return list;
                    }
                }
            }
        }
        
        // ------------ DIAGNOSTICS -------------
        /*
        for(int i=0; i<IODependencyMatrix.length; i++)
        {
            for(int j=0; j<IODependencyMatrix[i].length; j++)
            {
                System.out.print(IODependencyMatrix[i][j]+"\t");
            }
            System.out.println();
        }
        */
        // ---------------------------------------
        
        
        // Step-2: Compute in-degrees
        int inDegree[]=new int[array.length];
        for(int i=0; i<array.length; i++)
        {
            for(int j=0; j<array.length; j++)
            {
                if(IODependencyMatrix[i][j]) inDegree[j]++;
            }
        }
        
        // Step-3: Initialize visited to 0, push into queue all those vertices which have indegree=0
        int visitCounter=0;
        HashSet<Integer> visited = new HashSet<Integer>(array.length);
        ArrayList<IC> sorted = new ArrayList<IC>(list.size());
        Queue<Integer> queue = new LinkedList<Integer>();
        for(int i=0; i<inDegree.length; i++)
        {
            if(inDegree[i]==0) queue.add(i);
        }
        
        // Step-4
        while(!queue.isEmpty())
        {
            int index=queue.remove();                           // pop from queue
            visitCounter++;                                     // increment visited counter
            visited.add(index);                                 // add the popped index to hashset
            sorted.add(list.get(index));                        // add to sorted list
            
            for(int j=0; j<IODependencyMatrix[index].length; j++)
            {
                if(IODependencyMatrix[index][j] && !visited.contains(j)) // neighbour
                {
                    inDegree[j]--;                              // decrement in-degree of neighbour
                    if(inDegree[j]==0) queue.add(j);            // if in-degree reaches 0, add it to queue
                }
            }
        }
        
        if(visitCounter!=array.length)
            return list;                                        // Cyclic dependency, so return the original list itself
        else
            return sorted;                                      // Topological sort complete, return the sorted list
    }
    
    private static boolean areConnectedInOrder(IC ic1, int pin1, IC ic2, int pin2, ArrayList<Wire> wires, ArrayList<Resistor> resistors)
    {
        // Preconditions:
        // 1. pin1 of ic1 is output pin
        // 2. pin2 of ic2 is input pin
        // Condition:
        // if pin1 is connected to pin2 via possibly 1 or more wires then return true
    
        if(ic1.isInputPin(pin1)) return false;
        if(ic2.isOutputPin(pin2)) return false;
        
        int hole1 = ic1.getHoleIDOfPin(pin1), hole2 = ic2.getHoleIDOfPin(pin2);
        Breadboard b1 = ic1.getBase(), b2 = ic2.getBase();
        
        if(b1!=b2) return false;    // FOR NOW DO NOT THINK OF MORE THAN 1 BREADBOARD: TOO COMPLICATED!
        
        return areHolesConnected(b1,hole1,hole2,wires);
    }
    
    private static boolean areHolesConnected(Breadboard b, int hole1, int hole2, ArrayList<Wire> wires)
    {
        if(hole1==hole2) return true;

        // Perform Depth-First-Search
        Stack<Integer> stack = new Stack<Integer>();
        HashSet<Integer> visited = new HashSet<Integer>(Breadboard.MAX_HOLES);
        
        stack.push(hole1);
        while(!stack.isEmpty())
        {
            int holeID = stack.pop();
            if(holeID==hole2) return true;
            
            visited.add(holeID);
            for(int i=0; i<conMatrix[holeID-1].length; i++)
            {
                if(conMatrix[holeID-1][i])
                {
                    if(!visited.contains(i+1)) stack.push(i+1);
                }
            }
        }
        
        return false;
    }
    
    private static boolean[][] getDirectHoleConnectionsMatrix(Breadboard b, ArrayList<Wire> wires, ArrayList<Resistor> resistors)
    {
        boolean adjMatrix[][]=new boolean[Breadboard.MAX_HOLES][Breadboard.MAX_HOLES];
        
        for(int i=0; i<Breadboard.MAX_HOLES; i++)
        {
            for(int j=i; j<Breadboard.MAX_HOLES; j++)
            {
                if(i==j)
                    adjMatrix[i][j]=true;
                else
                    adjMatrix[i][j]=areHolesDirectlyConnectedInternally(i+1,j+1) || areHolesDirectlyConnectedViaWire(b,i+1,j+1,wires) || areHolesDirectlyConnectedViaResistor(b,i+1,j+1,resistors);
                
                adjMatrix[j][i]=adjMatrix[i][j];
            }            
        }
        
        return adjMatrix;
    }
       
    private static boolean areHolesDirectlyConnectedInternally(int hole1, int hole2)
    {
        if(hole1==hole2) {
            return true;
        } else if(isHoleInRow(hole1) && isHoleInRow(hole2)) {
            int rid1 = getRowIDFromHoleID(hole1);
            int rid2 = getRowIDFromHoleID(hole2);
            
            return(rid1==rid2);
        } else if(!isHoleInRow(hole1) && !isHoleInRow(hole2)) {
            int cid1 = getColumnIDFromHoleID(hole1);
            int cid2 = getColumnIDFromHoleID(hole2);
            
            return(cid1==cid2);
        } else {
            return false;
        }
    }
    
    private static boolean areHolesDirectlyConnectedViaWire(Breadboard b, int hole1, int hole2, ArrayList<Wire> wires)
    {
        int wc = wires.size();
        
        for(int i=0; i<wc; i++)
        {
            Wire w = wires.get(i);
            if(w.getSource()!=b || w.getSink()!=b) continue;
            
            int src = w.getSourceHoleID(), tar = w.getSinkHoleID();
            if((src==hole1 && tar==hole2) || (src==hole2 && tar==hole1)) return true;
        }
        
        return false;
    }
    
    private static boolean areHolesDirectlyConnectedViaResistor(Breadboard b, int hole1, int hole2, ArrayList<Resistor> resistors)
    {
        int rc = resistors.size();
        
        for(int i=0; i<rc; i++)
        {
            Resistor r = resistors.get(i);
            if(r.getBase()!=b) continue;
            
            int src = r.getSourceHoleID(), tar = r.getSinkHoleID();
            if((src==hole1 && tar==hole2) || (src==hole2 && tar==hole1)) return true;
        }
        
        return false;
    }
}
