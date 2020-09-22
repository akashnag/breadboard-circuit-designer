// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.util.ArrayList;
import static breadboardcircuitdesigner.CircuitUtility.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

class BreadboardUtility 
{
    public static final int MAX_ROWS = 8;
    public static final int MAX_COLUMNS = 126;
    public static final int ROW_SIZE = 25;
    public static final int COLUMN_SIZE = 5;
    
    private static int colIDs[]=null;
    
    public static boolean isHoleInPowerRails(int holeID)
    {
        return(holeID<=200);
    }
    
    public static boolean areHolesInSameVirtualRow(int ids[], int startIndex, int endIndex)
    {
        // both indices are inclusive
        int vrid = getVirtualRowID(ids[startIndex]);
        for(int i=startIndex+1; i<=endIndex; i++)
        {
            if(getVirtualRowID(ids[i])!=vrid) return false;
        }
        return true;
    }
    
    public static boolean isHoleIDValid(int holeID)
    {
        return(holeID>=1 && holeID<=Breadboard.MAX_HOLES);
    }
    
    public static int getVirtualRowID(int holeID)
    {
        if(holeID<=200)
            return(1+((holeID-1)/50));
        else {
            int x = holeID-201;
            return(1+(x/63));
        }
    }
    
    public static boolean isHoleOnBridgeBorder(int holeID)
    {
        return(holeID>=453 && holeID<=578);
    }
    
    public static boolean isHoleOnBridgeBorderTop(int holeID)
    {
        return(holeID>=453 && holeID<=515);
    }
    
    public static boolean isHoleOnBridgeBorderBottom(int holeID)
    {
        return(holeID>=516 && holeID<=578);
    }
    
    public static boolean isHoleInRow(int holeID)           
    {
        return(holeID<=(ROW_SIZE * MAX_ROWS));
    }
    
    public static int getRowIDFromHoleID(int holeID)        
    {
        if(!isHoleInRow(holeID)) return -1;
        return 1+((holeID-1)/ROW_SIZE);                           // row-ID = 1 to 8 (4 rows, each row sub-divided into 2 disconnected sub-rows)
    }
    
    public static int getColumnIDFromHoleID(int holeID) 
    {
        if(isHoleInRow(holeID)) return -1;
        if(colIDs==null)
        {
            colIDs = new int[Breadboard.MAX_HOLES];
            
            for(int r=1; r<=10; r++)
            {
                for(int c=1; c<=63; c++)
                {
                    int hid = 200+(((r-1)*63) + c);
                    colIDs[hid-1]=(r<=5 ? 0 : 63) + c;
                }
            }
        }
        
        return colIDs[holeID-1];                            // col-ID = 1 to 63 (for top-columns) and 64 to 126 (for bottom-columns)
    }
    
    public static int[] getHolesInRow(int rowID)
    {
        if(rowID<1 || rowID>MAX_ROWS) return null;
        
        int holes[] = new int[ROW_SIZE];
        for(int i=0; i<ROW_SIZE; i++) holes[i]=((rowID-1)*ROW_SIZE)+i+1;
        
        return holes;
    }
    
    public static int[] getHolesInColumn(int columnID)
    {
        if(columnID<1 || columnID>MAX_COLUMNS) return null;
        
        int holes[] = new int[COLUMN_SIZE];
        for(int i=201, j=-1; i<=830; i++)
        {
            if(getColumnIDFromHoleID(i)==columnID) holes[++j]=i;
        }
        
        return holes;
    }
    
    public static boolean areRowsConnected(Breadboard b, int rid1, int rid2, ArrayList<Wire> allWires, ArrayList<CircuitComponent> components)
    {
        if(rid1==rid2) return true;
        
        ArrayList<Wire> wires = getWiresOn(b,allWires,WireConnectionType.ON_SAME_COMPONENT);
        ArrayList<Resistor> resistors = getResistorsOn(b,getResistors(components,allWires));
        
        int wc = wires.size();
        int rc = resistors.size();
        
        for(int i=0; i<wc; i++)
        {
            Wire w = wires.get(i);
            
            int sourceRowID = getRowIDFromHoleID(w.getSourceHoleID());
            int sinkRowID = getRowIDFromHoleID(w.getSinkHoleID());
            
            if((sourceRowID==rid1 && sinkRowID==rid2)||(sourceRowID==rid2 && sinkRowID==rid1)) return true;
        }
        
        for(int i=0; i<rc; i++)
        {
            Resistor res = resistors.get(i);
            
            int sourceRowID = getRowIDFromHoleID(res.getSourceHoleID());
            int sinkRowID = getRowIDFromHoleID(res.getSinkHoleID());
            
            if((sourceRowID==rid1 && sinkRowID==rid2)||(sourceRowID==rid2 && sinkRowID==rid1)) return true;
        }
        
        return false;
    }
    
    public static boolean areColumnsConnected(Breadboard b, int cid1, int cid2, ArrayList<Wire> allWires, ArrayList<CircuitComponent> components)
    {
        if(cid1==cid2) return true;
        
        ArrayList<Wire> wires = getWiresOn(b,allWires,WireConnectionType.ON_SAME_COMPONENT);
        ArrayList<Resistor> resistors = getResistorsOn(b,getResistors(components,allWires));
        
        int wc = wires.size();
        int rc = resistors.size();
        
        for(int i=0; i<wc; i++)
        {
            Wire w = wires.get(i);
            
            int sourceColID = getColumnIDFromHoleID(w.getSourceHoleID());
            int sinkColID = getColumnIDFromHoleID(w.getSinkHoleID());
            
            if((sourceColID==cid1 && sinkColID==cid2)||(sourceColID==cid2 && sinkColID==cid1)) return true;
        }
        
        for(int i=0; i<rc; i++)
        {
            Resistor res = resistors.get(i);
            
            int sourceColID = getColumnIDFromHoleID(res.getSourceHoleID());
            int sinkColID = getColumnIDFromHoleID(res.getSinkHoleID());
            
            if((sourceColID==cid1 && sinkColID==cid2)||(sourceColID==cid2 && sinkColID==cid1)) return true;
        }
        
        return false;
    }
    
    public static boolean areRowColumnsConnected(Breadboard b, int rid, int cid, ArrayList<Wire> allWires, ArrayList<CircuitComponent> components)
    {
        ArrayList<Wire> wires = getWiresOn(b,allWires,WireConnectionType.ON_SAME_COMPONENT);
        ArrayList<Resistor> resistors = getResistorsOn(b,getResistors(components,allWires));
        
        int wc = wires.size();
        int rc = resistors.size();
        
        for(int i=0; i<wc; i++)
        {
            Wire w = wires.get(i);
            
            int rid1 = getRowIDFromHoleID(w.getSourceHoleID());
            int rid2 = getRowIDFromHoleID(w.getSinkHoleID());
            int cid1 = getColumnIDFromHoleID(w.getSourceHoleID());
            int cid2 = getColumnIDFromHoleID(w.getSinkHoleID());
            
            if((rid1==rid && cid2==cid)||(rid2==rid && cid1==cid)) return true;
        }
        
        for(int i=0; i<rc; i++)
        {
            Resistor res = resistors.get(i);
            
            int rid1 = getRowIDFromHoleID(res.getSourceHoleID());
            int rid2 = getRowIDFromHoleID(res.getSinkHoleID());
            int cid1 = getColumnIDFromHoleID(res.getSourceHoleID());
            int cid2 = getColumnIDFromHoleID(res.getSinkHoleID());
            
            if((rid1==rid && cid2==cid)||(rid2==rid && cid1==cid)) return true;
        }
        
        return false;
    }
    
    public static boolean[][] getRowConnectionsMatrix(Breadboard b, ArrayList<Wire> allWires, ArrayList<CircuitComponent> components)
    {
        boolean m[][]=new boolean[MAX_ROWS][MAX_ROWS];
        
        for(int i=0; i<MAX_ROWS; i++)
        {
            for(int j=0; j<MAX_ROWS; j++)
            {
                if(i==j)
                    m[i][j]=true;
                else
                    m[i][j]=areRowsConnected(b,i+1,j+1,allWires,components);
            }
        }
        
        return m;
    }
    
    public static boolean[][] getColumnConnectionsMatrix(Breadboard b, ArrayList<Wire> allWires, ArrayList<CircuitComponent> components)
    {
        boolean m[][]=new boolean[MAX_COLUMNS][MAX_COLUMNS];
        
        for(int i=0; i<MAX_COLUMNS; i++)
        {
            for(int j=0; j<MAX_COLUMNS; j++)
            {
                if(i==j)
                    m[i][j]=true;
                else
                    m[i][j]=areColumnsConnected(b,i+1,j+1,allWires,components);
            }
        }
        
        return m;
    }
    
    public static boolean[][] getRowColumnConnectionsMatrix(Breadboard b, ArrayList<Wire> allWires, ArrayList<CircuitComponent> components)
    {
        boolean m[][]=new boolean[MAX_ROWS][MAX_COLUMNS];
        
        for(int i=0; i<MAX_ROWS; i++)
        {
            for(int j=0; j<MAX_COLUMNS; j++)
            {
                m[i][j]=areRowColumnsConnected(b,i+1,j+1,allWires,components);
            }
        }
        
        return m;
    }
    
    public static boolean areDirectlyConnected(CircuitComponent c1, CircuitComponent c2, ArrayList<Wire> allWires)
    {
        // c1 and c2 may either be breadboards or power supplies
        if(c1==c2) return true;
        
        int wc = allWires.size();
        for(int i=0; i<wc; i++)
        {
            Wire w = allWires.get(i);
            
            CircuitComponent s = w.getSource();
            CircuitComponent t = w.getSink();
            
            if((c1==s && c2==t) || (c1==t && c2==s)) return true;
        }
        return false;
    }
    
    public static ArrayList<CircuitComponent> getExternalConnectedComponents(CircuitComponent comp, ArrayList<Wire> allWires, ArrayList<CircuitComponent> components)
    {
        ArrayList<CircuitComponent> e = new ArrayList<CircuitComponent>();
        
        int cc = components.size();
        for(int i=0; i<cc; i++)
        {
            CircuitComponent c = components.get(i);
            if(c==comp) continue;
            
            if(areDirectlyConnected(comp,c,allWires)) e.add(c);                
        }
        
        return e;
    }
    
    protected static boolean hasBreadboardPower(Breadboard b, ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        ArrayList<CircuitComponent> external = getExternalConnectedComponents(b,wires,components);
        int ec = external.size();
        for(int i=0; i<ec; i++)
        {
            if(external.get(i).getType()==ComponentType.POWER_SUPPLY)
            {
                return true;
            }
        }
        return false;
    }
    
    
    
    
    
    
    
    // --------------------------------------------------------
    /*
    private static void propagateCurrents(Breadboard b, HashSet<Breadboard> visited, ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        visited.add(b);
        
        HoleState state[] = getHoleState(b,components,wires);
        b.setHoleStates(state);
        
        turnOnOffLEDs(b,components);
        ArrayList<CircuitComponent> external = getExternalConnectedComponents(b,wires,components);
        int ec = external.size();
        for(int i=0; i<ec; i++)
        {
            if(external.get(i).getType()==ComponentType.BREADBOARD)
            {
                Breadboard tb = (Breadboard)external.get(i);
                if(!visited.contains(tb))
                {
                    propagateInterBreadboardConnections(b,tb,components,wires);
                    propagateCurrents(tb,visited,components,wires);
                } else {
                    // if there is a loop
                }
            }
        }
    }
    
    private static void propagateInterBreadboardConnections(Breadboard source, Breadboard target, ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        HoleState shs[] = source.getHoleStates();
        HashSet<Integer> visitedRows = new HashSet<Integer>();
        HashSet<Integer> visitedCols = new HashSet<Integer>();
        
        // THS = target hole state: holestate of the target breadboard
        HoleState ths[] = new HoleState[Breadboard.MAX_HOLES];
        for(int i=0; i<ths.length; i++) ths[i]=HoleState.NO_CONNECTION;
        
        int wc = wires.size();
        for(int i=0; i<wc; i++)
        {
            Wire w = wires.get(i);
            
            if(w.getSource()==w.getSink()) continue;
            if(w.getSource().getType()!=ComponentType.BREADBOARD) continue;
            if(w.getSink().getType()!=ComponentType.BREADBOARD) continue;
            
            if(w.getSource()==source || w.getSink()==source)
            {
                if(w.getSink()==target || w.getSource()==target)
                {
                    int sid = (w.getSource()==source ? w.getSourceHoleID() : w.getSinkHoleID());
                    int tid = (w.getSource()==target ? w.getSourceHoleID() : w.getSinkHoleID());
                    
                    ths[tid-1]=shs[sid-1];      // doesnt matter: it will be overwritten! rectify this
                
                    updateHoleStates(target,tid,shs[sid-1],ths,visitedRows,visitedCols,components,wires);
                }
            }
        }
        
        target.setHoleStates(ths);
    }
    */
    // --------------------------------------------
    
    
    
    
    
    
    
    
    
    
    
    /*
    private static boolean areHolesConnected(Breadboard b, int hole1, int hole2, ArrayList<Wire> wires, HashSet<Integer> visited)
    {
        // WARNING: Recursive function
        if(hole1==hole2) return true;
        
        int pairs1[]=getOppositeHoleIDsOfAllWiresConnectedToTheSameColumnAsHole(b,hole1,wires);
        if(pairs1==null) return false;
        
        int pairs2[]=getOppositeHoleIDsOfAllWiresConnectedToTheSameColumnAsHole(b,hole2,wires);
        if(pairs2==null) return false;
        
        System.out.println("Called with hole1="+hole1+" and hole2="+hole2);
                
        for(int i=0; i<pairs1.length; i++)
        {
            if(visited.contains(pairs1[i])) continue;
            visited.add(pairs1[i]);
            
            for(int j=0; j<pairs2.length; j++)
            {
                if(visited.contains(pairs2[i])) continue;
                visited.add(pairs2[i]);
                
                if(areHolesConnected(b,pairs1[i],pairs2[i],wires,visited)) return true;
            }
        }
        
        return false;
    }
    
    private static int[] getOppositeHoleIDsOfAllWiresConnectedToTheSameColumnAsHole(Breadboard b, int holeID, ArrayList<Wire> wires)
    {
        int sourceList[]=null;
        if(isHoleInRow(holeID))
            return null;
        else
            sourceList=getHolesInColumn(getColumnIDFromHoleID(holeID));
        
        ArrayList<Integer> result = new ArrayList<Integer>();
        int wc = wires.size();
        for(int i=0; i<wc; i++)
        {
            Wire w = wires.get(i);
            if(w.getSource()!=b || w.getSink()!=b) continue;
            
            if(linearSearch(sourceList,w.getSourceHoleID())>-1) 
                result.add(w.getSinkHoleID());
            else if(linearSearch(sourceList,w.getSinkHoleID())>-1)
                result.add(w.getSourceHoleID());
        }
        
        Integer dummy[] = new Integer[result.size()];
        dummy=result.toArray(dummy);
        
        int res[] = new int[dummy.length];
        for(int i=0; i<dummy.length; i++) res[i]=dummy[i].intValue();
        
        return res;
    }
    
    private static int linearSearch(int a[], int s)
    {
        for(int i=0; i<a.length; i++)
        {
            if(s==a[i]) return i;
        }
        return -1;
    }
    */
    
    
    
    protected static int getFirstHoleIDInRow(int rowID)
    {
        return(((rowID-1)*25)+1);       // can also use:  getHolesInRow(rowID)[0]
    }
    
    protected static int getFirstHoleIDInColumn(int colID)
    {
        return getHolesInColumn(colID)[0];
    }
}
