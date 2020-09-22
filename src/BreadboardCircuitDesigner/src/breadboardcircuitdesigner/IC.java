// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.io.*;
import java.util.ArrayList;

import static breadboardcircuitdesigner.BreadboardUtility.*;

class IC extends CircuitComponent 
{
    private static int counter = 0;
    
    private Breadboard base;
    private int firstPinHoleID;             // Pin #1
    
    private String icName;
    
    // ------------ to be read from file --------------
    private String icText;
    private String description;
    private int pinCount;
    private boolean isInput[];
    
    private ICType icType;
    private boolean isStorageMatrix;
    private int wordSize;
    private int storageArray[];             // either this is null
    private int storageMatrix[][];          // or this is null, depending on isStorageMatrix
    
    private int tableCount;
    private int rowCounts[];
    private int tablePins[][];
    private int tablePinType[][];           // 0=normal pin, 1=array, 2=matrix
    private int tablePinMemoryIndex[][][];  // last dimension is 3, but if pintype=1 then it stores only 2 data (index & bit), else 3 data (row,col,bit)
    private int arrayIndices[][][][];
    private int matrixIndices[][][][];
    private TruthValue functionTable[][][];
    private int holeIDs[];                      // to be computed based on firstPinHoleID
    // ------------------------------------------------
    
    public String getDescription() { return description; }
    public ICType getICType() { return icType; }
    public String getICText() { return icText; }
    public int getPinCount() { return pinCount; }
    public boolean[] getPinTypes() { return isInput; }
    public int getTableCount() { return tableCount; }
    public int[] getRowCounts() { return rowCounts; }
    public int[][] getTablePins() { return tablePins; }
    public TruthValue[][][] getFunctionTable() { return functionTable; }
    public int[] getHoleIDs() { return holeIDs; }
    public int getHoleIDOfPin(int pin) { return holeIDs[pin-1]; }
    public boolean isInputPin(int pin) { return isInput[pin-1]; }
    public boolean isOutputPin(int pin) { return !isInput[pin-1]; }
    
    IC(Breadboard b, int holeID, String n)
    {
        super(ComponentType.IC,
                getNewName(n),
                b.getHoleCenter(holeID),
                0,
                58, // height
                0
        );
        
        base=b;
        firstPinHoleID=holeID;
        icName=n;
        
        try {
            readData();
        } catch(IOException e) {
            Utility.alert("Error reading chip("+icName+") datasheet.");
            System.out.println(e);
            e.printStackTrace();
        }
        super.setWidth(getICWidth());
    }
    
    @Override
    public boolean isUsingBaseHole(int holeID)
    {
        for(int i=0; i<holeIDs.length; i++)
        {
            if(holeIDs[i]==holeID) return true;
        }
        return false;
    }
    
    @Override
    public Breadboard getBase() { return base; }
    
    private static String getNewName(String x)
    {
        return(x+"_"+(++counter));
    }
    
    private int getICWidth()
    {
        return ((pinCount/2)*16)-6+(2*2);
    }
    
    public static boolean canPositionIC(Breadboard base, String icName, int pin1HoleID, ArrayList<CircuitComponent> components, ArrayList<Wire> wires)
    {
        Breadboard b = new Breadboard(new Location(40,40));
        IC ic = new IC(b,pin1HoleID,icName);
        
        int pc = ic.getPinCount();
        int hids[] = ic.getHoleIDs();
        
        for(int i=0; i<hids.length; i++)
        {
            if(base.isHoleShadowed(hids[i], wires, components)) return false;
        }
        
        if(!areHolesInSameVirtualRow(hids,0,(pc/2)-1)) return false;
        if(!areHolesInSameVirtualRow(hids,(pc/2),pc-1)) return false;
        
        if(isHoleInPowerRails(pin1HoleID)) return false;
        return(!isHoleInPowerRails(hids[pc-1]));
    }
    
    public static int getICPinCount(String icName)
    {
        try 
        {
            InputStream file = IC.class.getResourceAsStream("resources/ic/" + icName + ".icds");
            BufferedReader br = new BufferedReader(new InputStreamReader(file));

            String icn = br.readLine();
            if(!icn.equalsIgnoreCase(icName)) return 0;

            br.readLine();
            int pinCount = Integer.parseInt(br.readLine());
            br.close();
            
            return pinCount;
        } catch(Exception e) {
            return 0;
        }
    }
    
    public static String getICDescription(String icName)
    {
        try 
        {
            InputStream file = IC.class.getResourceAsStream("resources/ic/" + icName + ".icds");
            BufferedReader br = new BufferedReader(new InputStreamReader(file));

            String icn = br.readLine();
            if(!icn.equalsIgnoreCase(icName)) return null;

            String s = br.readLine();
            br.close();
            
            return s;
        } catch(Exception e) {
            return null;
        }
    }
    
    public int[] getShadowedHoles()
    {
        // length of list = pinCount + C
        // where C = 0 or (2 * (pinCount/2)) depending on whether pins are in bridge border rows
        
        int shadowCount = (isHoleOnBridgeBorderBottom(firstPinHoleID) ? 0 : pinCount);
        int holes[] = new int[pinCount + shadowCount];
        
        System.arraycopy(holeIDs,0,holes,0,pinCount);
        if(shadowCount==0) return holes;
        
        // actually this is wrong insertion of IC, IC must be inserted in bridge
        // but anyway, let's compute the shadows...
        
        int x1 = holeIDs[pinCount-1] + 63;
        int x2 = x1 + 63;
        
        int j=pinCount-1;
        for(int i=x1; i<x1+(pinCount/2); i++) holes[++j]=i;
        for(int i=x2; i<x2+(pinCount/2); i++) holes[++j]=i;
        
        return holes;
    }
    
    public int getFirstPinHoleID() { return firstPinHoleID; }
    
    public HoleState[] getOutputs(HoleState inputs[])
    {
        if(inputs.length!=pinCount) return null;
        
        HoleState out[]=new HoleState[pinCount];
        for(int i=0; i<pinCount; i++)
        {
            if(isInput[i]) 
                out[i]=inputs[i];
            else
                out[i]=HoleState.NO_CONNECTION;
        }
        
        for(int t=0; t<tableCount; t++)
        {
            for(int r=0; r<rowCounts[t]; r++)
            {
                // Check if the row-values in the input-columns form a satisfiable condition
                boolean f=true;
                for(int c=0; f && c<tablePins[t].length; c++)
                {
                    TruthValue tv = functionTable[t][r][c];
                    int pinIndex=tablePins[t][c]-1;
                    
                    if(tablePinType[t][c]==0)       // actual pin
                    {
                        if(!isInput[pinIndex]) continue;

                        boolean cond = areEqual(tv,inputs[pinIndex]);
                        
                        if(tv==TruthValue.MEMORY)
                        {
                            // actual-pin column cannot have memory reference as value because it is INPUT column
                            Utility.alert("Invalid condition #1");
                        } else {
                            f=f && cond;
                        }
                    } else {
                        // when table-column header is itself a memory reference
                        // array or matrix: therefore it is always input (because it is stored internally)
                        // WARNING: 'pinIndex' is bogus here
                        if(tablePinType[t][c]>2) continue;     // output
                        
                        // check only inputs
                        // if it is memory and is an input column then it can only have values
                        if(tv==TruthValue.MEMORY)
                        {
                            // NOT POSSIBLE: since table-column is memory itself, it cannot have yet another memory reference
                            Utility.alert("Invalid condition #2");
                        } else {
                            if(isStorageMatrix)
                            {
                                f = f && areEqual(tv,getHoleStateFromMatrixStorage(tablePinMemoryIndex[t][c]));
                            } else {
                                f = f && areEqual(tv,getHoleStateFromArrayStorage(tablePinMemoryIndex[t][c]));
                            }
                        }
                    }
                }
                
                if(f)
                {
                    // Condition is satisfied, so determine the output
                    for(int c=0; f && c<tablePins[t].length; c++)
                    {
                        TruthValue tv = functionTable[t][r][c];
                        int pinIndex=tablePins[t][c]-1;
                        
                        if(tablePinType[t][c]==0)
                        {
                            // actual pin
                            if(isInput[pinIndex]) continue;

                            if(tv==TruthValue.HIGH)
                                out[pinIndex]=HoleState.HIGH;
                            else if(tv==TruthValue.LOW)
                                out[pinIndex]=HoleState.LOW;
                            else if(tv==TruthValue.MEMORY) {
                                if(isStorageMatrix)
                                {
                                    out[pinIndex]=getHoleStateFromMatrixStorage(matrixIndices[t][r][c]);
                                } else {
                                    if(arrayIndices[t][r][c][0]==-1)
                                    {
                                        // pin reference
                                        out[pinIndex]=inputs[arrayIndices[t][r][c][1]];
                                    } else {
                                        out[pinIndex]=getHoleStateFromArrayStorage(arrayIndices[t][r][c]);
                                    }
                                }
                            }
                        } else {
                            // memory
                            // WARNING: 'pinIndex' is bogus here
                            if(tablePinType[t][c]<=2) continue;     // input
                            
                            HoleState ov=null;
                            if(tv==TruthValue.HIGH)
                                ov=HoleState.HIGH;
                            else if(tv==TruthValue.LOW)
                                ov=HoleState.LOW;
                            else if(tv==TruthValue.MEMORY) {
                                if(isStorageMatrix)
                                {
                                    ov=getHoleStateFromMatrixStorage(matrixIndices[t][r][c]);
                                } else {
                                    if(arrayIndices[t][r][c][0]==-1)
                                    {
                                        // pin reference
                                        ov=inputs[arrayIndices[t][r][c][1]];
                                    } else {
                                        ov=getHoleStateFromArrayStorage(arrayIndices[t][r][c]);
                                    }
                                }
                            }
                            
                            // check only outputs
                            if(tablePinType[t][c]==3)
                            {
                                // store into array: storageArray[tablePinMemoryIndex[t][c][0]] at bit position tablePinMemoryIndex[t][c][1]
                                int pos = tablePinMemoryIndex[t][c][0];
                                int bit = tablePinMemoryIndex[t][c][1];
                                setBitInMemoryArray(pos,bit,ov);
                            } else if(tablePinType[t][c]==4) {
                                // store into matrix
                                int row = tablePinMemoryIndex[t][c][0];
                                int col = tablePinMemoryIndex[t][c][1];
                                int bit = tablePinMemoryIndex[t][c][2];
                                setBitInMemoryMatrix(row,col,bit,ov);
                            }
                        }
                    }
                }
            }
        }
        
        return out;
    }
    
    
    private boolean areEqual(TruthValue tv, HoleState hs)
    {
        if(tv==TruthValue.HIGH && hs==HoleState.HIGH) return true;
        if(tv==TruthValue.LOW && hs==HoleState.LOW) return true;
        if(tv==TruthValue.DONT_CARE) return true;
        return false;
    }
    
    private void setBitInMemoryArray(int pos, int bit, HoleState val)
    {
        int k = (val==HoleState.HIGH ? (1 << bit) : ~(1 << bit));
        storageArray[pos] |= k;
    }
    
    private void setBitInMemoryMatrix(int row, int col, int bit, HoleState val)
    {
        int k = (val==HoleState.HIGH ? (1 << bit) : ~(1 << bit));
        storageMatrix[row][col] |= k;
    }
        
    private HoleState getHoleStateFromMatrixStorage(int index[])
    {
        int row = index[0], col = index[1], bit = index[2];
        return(getBitAsHoleState(storageMatrix[row][col],bit));
    }
    
    private HoleState getHoleStateFromArrayStorage(int index[])
    {
        int pos = index[0], bit = index[1];
        return(getBitAsHoleState(storageArray[pos],bit));
    }
    
    private HoleState getBitAsHoleState(int data, int bit)
    {
        // 0 <= bit < wordSize
        int d = data % (int)Math.pow(2,wordSize);
        
        // right-shift 'bit' times, and then mask with 0x00000001
        int d2 = (d >> bit) & 0x00000001;
        return(d2==0 ? HoleState.LOW : HoleState.HIGH);
    }
    
    private void readData() throws IOException
    {
        /*
            -----------------------------
                    FILE FORMAT (7408)
            -----------------------------
            ICName   = 7408
            Description = Quad 2-input AND gate
            ICText   = SN74LS08
            PinCount = 14
            IIOIIOIOIIOIII  (for pins 1 to 14) [I=input, O=output, VCC and GND are inputs]
            C               (C=Combinational circuit, A/M=Flip-flop/memory, A(n) or M(x,y) = size of array or 2D matrix (memory storage)
            TableCount=4    (4 tables for the 4 AND gates)
            4,7,14,1,2,3    <rows,pins> (rows=4, table-columns=pins 7,14,1,2 and 3)
            4,7,14,4,5,6
            4,7,14,10,9,8
            4,7,14,13,12,11
            
            0,1,0,0,0       (Table-1: IF P7=0, P14=1, P1=0, P2=0, then P3=0)
            0,1,0,1,0
            0,1,1,0,0
            0,1,1,1,1
        
            ...             (similarly for Tables 2-4)
        */
        
        InputStream file = IC.class.getResourceAsStream("resources/ic/" + icName + ".icds");
        BufferedReader br = new BufferedReader(new InputStreamReader(file));
        
        String icn = br.readLine();
        if(!icn.equalsIgnoreCase(icName))
        {
            Utility.alert("Invalid chip("+icName+") datasheet: IC-Name does not match.");
            br.close();
            return;
        }
        
        description = br.readLine();
        icText = br.readLine();
        pinCount = Integer.parseInt(br.readLine());
        String ioString = br.readLine().toLowerCase();
        
        if(ioString.length()!=pinCount)
        {
            Utility.alert("Invalid chip("+icName+") datasheet: Pin-count does not match.");
            br.close();
            return;
        }
        
        // ----------- Computation -----------------------------
        isInput = new boolean[pinCount];
        holeIDs = new int[pinCount];        
        for(int i=0; i<pinCount; i++) isInput[i]=(ioString.charAt(i)=='i');
        // -----------------------------------------------------
        
        // ----------- Determine IC-Type -----------------------
        String temp = br.readLine().trim().toLowerCase();        // C or A(w,n) or M(w,x,y), where C=combinational, A=array, M=matrix, w=Word-size in bits, n=length of array, (x,y) are matrix dimensions
        if(temp.charAt(0)=='c')
        {
            icType=ICType.COMBINATIONAL;
        } else {
            icType=ICType.MEMORY;
            isStorageMatrix=(temp.charAt(0)=='m');
            if(temp.charAt(1)!='(' || temp.charAt(temp.length()-1)!=')')
            {
                Utility.alert("Invalid chip data.");
                br.close();
                return;
            } else {
                String x[] = temp.substring(2,temp.length()-1).split(",");
                if((isStorageMatrix && x.length!=3)||(!isStorageMatrix && x.length!=2))
                {
                    Utility.alert("Invalid chip data.");
                    br.close();
                    return;
                } else {
                    wordSize=Integer.parseInt(x[0]);
                    if(wordSize>32)
                    {
                        Utility.alert("Chip Word-Size unsupported.");
                        br.close();
                        return;
                    }
                    
                    if(isStorageMatrix)
                    {
                        storageArray = null;
                        storageMatrix = new int[Integer.parseInt(x[1])][Integer.parseInt(x[2])];
                    } else {
                        storageMatrix = null;
                        storageArray = new int[Integer.parseInt(x[1])];
                    }
                }
            }
        }
        // -----------------------------------------------------
        
        tableCount = Integer.parseInt(br.readLine());
        rowCounts=new int[tableCount];
        
        tablePins=new int[tableCount][];
        tablePinType=new int[tableCount][];
        tablePinMemoryIndex=new int[tableCount][][];
        
        functionTable=new TruthValue[tableCount][][];
        if(isStorageMatrix)
            matrixIndices=new int[tableCount][][][];
        else
            arrayIndices=new int[tableCount][][][];
        
        for(int i=0; i<tableCount; i++)
        {
            String x[] = br.readLine().toUpperCase().split(",");
            rowCounts[i]=Integer.parseInt(x[0]);
            
            tablePins[i]=new int[x.length-1];
            tablePinType[i]=new int[x.length-1];
            tablePinMemoryIndex[i]=new int[x.length-1][3];
            
            functionTable[i]=new TruthValue[rowCounts[i]][x.length-1];
            if(isStorageMatrix)
                matrixIndices[i]=new int[rowCounts[i]][x.length-1][3];
            else
                arrayIndices[i]=new int[rowCounts[i]][x.length-1][2];
            
            // Read Table Information
            for(int j=1; j<x.length; j++) 
            {
                char y = x[j].charAt(0);
                if(y=='M' || y=='A')                                // x[j]=AI(n:b) or AO(n:b) or MI(x;y:b) or MO(x;y:b)
                {
                    tablePins[i][j-1]=-1;
                    char z = x[j].charAt(1);
                    String g=x[j].substring(3,x[j].length()-1);     // remove brackets and I/O
                    int q=g.indexOf(':');
                    
                    if(y=='A')
                    {
                        tablePinType[i][j-1]=(z=='I' ? 1 : 3);
                        tablePinMemoryIndex[i][j-1][0]=Integer.parseInt(g.substring(0,q));
                        tablePinMemoryIndex[i][j-1][1]=Integer.parseInt(g.substring(q+1));                        
                    } else {
                        tablePinType[i][j-1]=(z=='I' ? 2 : 4);
                        int p=g.indexOf(';');
                        tablePinMemoryIndex[i][j-1][0]=Integer.parseInt(g.substring(0,p));
                        tablePinMemoryIndex[i][j-1][1]=Integer.parseInt(g.substring(p+1,q));
                        tablePinMemoryIndex[i][j-1][2]=Integer.parseInt(g.substring(q+1));
                    }
                } else {
                    tablePins[i][j-1]=Integer.parseInt(x[j]);
                    tablePinType[i][j-1]=0;
                    tablePinMemoryIndex[i][j-1]=null;
                }
            }
        }
        
        // Read Table data (rows)
        for(int t=0; t<tableCount; t++)
        {
            br.readLine();                  // blank line before each table data
            for(int r=0; r<rowCounts[t]; r++)
            {
                String x[]=br.readLine().toUpperCase().split(",");
                for(int c=0; c<tablePins[t].length; c++)
                {
                    char y = x[c].charAt(0);
                    if(y=='X')
                    {
                        functionTable[t][r][c]=TruthValue.DONT_CARE;
                    } else if(y=='0') {
                        functionTable[t][r][c]=TruthValue.LOW;
                    } else if(y=='1') {
                        functionTable[t][r][c]=TruthValue.HIGH;
                    } else if(y=='A' || y=='M' || y=='{') {
                        functionTable[t][r][c]=TruthValue.MEMORY;
                        // x[c] = A(n:b) or M(x,y:b) or {(pin)}
                        String g=x[c].substring(2,x[c].length()-1);     // remove parenthesis
                        if(y=='A' || y=='M')
                        {
                            if(y=='A') {
                                int q = g.indexOf(':');
                                arrayIndices[t][r][c][0]=Integer.parseInt(g.substring(0,q));
                                arrayIndices[t][r][c][1]=Integer.parseInt(g.substring(q+1));
                            } else {
                                int p = g.indexOf(';');
                                int q = g.indexOf(':');
                                matrixIndices[t][r][c][0]=Integer.parseInt(g.substring(0,p));
                                matrixIndices[t][r][c][1]=Integer.parseInt(g.substring(p+1,q));
                                matrixIndices[t][r][c][2]=Integer.parseInt(g.substring(q+1));
                            }
                        } else {
                            // pin reference
                            arrayIndices[t][r][c][0]=-1;        // -1 indicates that next data is pin reference and not array index
                            arrayIndices[t][r][c][1]=Integer.parseInt(g);
                        }
                    }
                }
            }
        }
        
        br.close();
        
        int factor=63*(firstPinHoleID>=516 && firstPinHoleID<=578 ? 1 : 3);
        holeIDs=new int[pinCount];
        for(int i=0; i<(pinCount/2); i++) holeIDs[i]=(firstPinHoleID+i);
        for(int i=pinCount-1, j=0; i>=(pinCount/2); i--,j++)
        {
            holeIDs[i]=(firstPinHoleID+j-factor);       
        }
    }
    
    @Override
    public Location getLocation() { return base.getHoleCenter(firstPinHoleID); }
    
    @Override
    public void paint(Graphics g)
    {
        // IC spans 2 rows in height, and its pins go into 1st and 4th rows relativistically speaking
                
        Location hcb = getLocation();
        int bhx = hcb.x, bhy = hcb.y;
        int ahx = bhx, ahy = bhy-43;
        
        int w = getICWidth();
        
        Graphics2D g2d = (Graphics2D)g;
        g2d.setStroke(new java.awt.BasicStroke(1));
        
        // body width spans from holecenter-8 to holecenter+8
        // body height spans from holecenter-5 to holecenter+5
        g.setColor(new Color(48,48,48));
        g.fillRect(ahx-7, ahy-2, w, 40);                // main body
        g.setColor(new Color(32,32,32));
        g.fillRect(ahx-6, ahy-2, 3, 40);                // vertical left inset
        g.setColor(Color.BLACK);
        g.fillRect(ahx-7+w-2, ahy-2, 2, 40);            // vertical right inset
        g.fillRect(ahx-7, ahy+37, w, 4);                // horizontal bottom inset
        
        g.setColor(new Color(232,232,232));
        for(int i=0; i<pinCount; i++)
        {
            Location p = base.getHoleCenter(holeIDs[i]);
            g.fillRect(p.x-2,p.y-2,4,4);
        }
        
        g2d.setStroke(new java.awt.BasicStroke(5));
        g2d.setColor(new Color(32,32,32));
        g2d.drawArc(ahx-7-10+6, ahy-2+10, 20, 20, 90, -90);
        g2d.setColor(new Color(64,64,64));
        g2d.drawArc(ahx-7-10+6, ahy-2+10, 20, 20, 270, 90);
     
        g2d.setStroke(new java.awt.BasicStroke(1));
        g.setFont(new Font("Lucida Console", Font.PLAIN, 8));
        g.setColor(Color.GRAY);
        g.drawString(icText, ahx-7+((w-20)/2), ahy+(40/2) );
    }
    
    @Override
    public String getPropertiesAsString(ArrayList<CircuitComponent> components)
    {
        // <super>|breadboard-index;pin-1-holeid;IC-name
        int bi = CircuitUtility.getIndexFromComponentReference(components, base);
        return super.getPropertiesAsString(components)+"|"+bi+";"+firstPinHoleID+";"+icName;
    }
    
    // Factory method
    public static IC parseFromString(String data, CircuitComponent list[])
    {
        String x[] = data.substring(data.indexOf('|')+1).split(";");
        
        Breadboard b = (Breadboard)list[Integer.parseInt(x[0])];
        int holeID = Integer.parseInt(x[1]);
        String n = x[2];
        
        return(new IC(b, holeID, n));
    }
}
