// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import javax.imageio.ImageIO;


class MainWindow extends javax.swing.JFrame implements AdjustmentListener
{
    // ----------- menu related variable declarations -------------
    private MenuActions menuActions;
    private JMenuBar menuBar;
    private JMenu mnuFile, mnuEdit, mnuInsert, mnuView, mnuHelp;
    
    private JMenuItem mnuFileNew, mnuFileOpen, mnuFileClose, mnuFileSave, mnuFileSaveAs, mnuFileExport, mnuFileExit;
    
    private JMenuItem mnuEditDelete, mnuEditSetDimensions, mnuEditSetWireColor;
    private JMenuItem mnuEditWireMode, mnuEditSetIdleMode;
    
    private JMenuItem mnuInsertPowerSupply, mnuInsertBreadboard, mnuInsertDIP, mnuInsertResistor, mnuInsertLED, mnuInsert7LED;
    
    private JMenuItem mnuViewHoleStatus;
    private JMenuItem mnuHelpAbout;
    // ------------------------------------------------------------
    
    // --------------------- CIRCUIT RELATED VARIABLES ---------------------
    private JScrollPane scroller;
    private JPanel drawingPane;
    
    protected static final int DEFAULT_CIRCUIT_WIDTH = 1253;
    protected static final int DEFAULT_CIRCUIT_HEIGHT = 768;
    
    protected static int circuitWidth = 1253;
    protected static int circuitHeight = 768;
    
    private boolean holeStatusVisible = false;
    private boolean deleteMode = false;
    public static int breadboardCounter=0, powerSupplyCounter=0;
    private static final Color CIRCUIT_BACKGROUND_COLOR = Color.LIGHT_GRAY;
    
    // Do not use outside of the click events inside drawing pane
    boolean simulateFlag=false;
    boolean circuitChanged=false;
    // -----------------------------------
    
    private static boolean simulationMode=false;
    
    // ------------ Wire --------------
    private boolean wireEditingMode=false;
    private Color currentWireColor = Color.BLUE;
    private boolean wireSourceNext = true;
    private int wireSourceComponentIndex, wireSourceHoleID;
    // ---------------------------------
    
    // ------------- Resistor ------------
    private boolean resistorInsertMode=false;
    private boolean resistorInsertSourceNext=true;
    private int currentResistance=1000;
    private int resistorSourceComponentIndex=-1, resistorSourceHoleID=-1;
    // -----------------------------------
    
    // ------------- LED ------------------
    private boolean insertLEDMode=false;
    private Color currentLEDColor=Color.RED;
    // ------------------------------------
    
    // ------------- 7-SEGMENT LED --------
    private boolean insertSevenSegmentLEDMode=false;
    private Color current7LEDColor=Color.RED;
    private boolean current7LEDConfigIsCommonCathode=true;
    // ------------------------------------
    
    // -------------- IC ------------------
    private int currentICType = -1;
    private boolean insertICMode = false;
    // ------------------------------------
    
    private boolean floatingMode=false;
    
    // ------------- Power Supply -------------
    double powerSupplyFrequency;
    javax.swing.Timer clockTimer=null;
    // ----------------------------------------
    
    // ------------- Dragging ---------------
    private boolean dragMode=false;
    private int draggedComponentIndex=-1;
    private int dragComponentOffsetX=0, dragComponentOffsetY=0;
    // --------------------------------------
    // ---------------------------------------------------------------------
    
    // --------------------- FILE RELATED VARIABLES ------------------------
    private boolean isCircuitActive=false;
    private boolean hasBeenSaved;
    private boolean hasFileBeenAllotted;
    private String circuitFilePath;
    // ---------------------------------------------------------------------
    
    MainWindow() 
    {
        addComponentListener(new ComponentAdapter() 
        {  
                public void componentResized(ComponentEvent evt) 
                {
                    resizeScrollPane();
                    refreshCircuit();
                }
        });
        
        addWindowListener(new java.awt.event.WindowAdapter() 
        {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) 
            {
                if(mnuFileClose_Click()) System.exit(0);
            }
        });
        
        initComponents();
        initMenu();
        initGUI();
        
        isCircuitActive=false;
        hasBeenSaved=false;
        hasFileBeenAllotted=false;
        circuitFilePath=null;
        
        breadboardCounter=0;
        powerSupplyCounter=0;
    }

    private void initGUI()
    {
        // Load wire colors
        cmbWireColor.removeAllItems();
        String x[] = { "BLACK", "BLUE", "RED", "GREEN", "YELLOW", "CYAN", "MAGENTA", "PINK" };
        for(String p:x) cmbWireColor.addItem(p);
        
        // Load LED colors
        cmbLEDColor.removeAllItems();
        cmbLEDColor.addItem("BLUE");
        cmbLEDColor.addItem("RED");
        cmbLEDColor.addItem("GREEN");
        cmbLEDColor.addItem("YELLOW");

        // Load 7-Segment LED colors and configuration
        cmb7LEDColor.removeAllItems();
        cmb7LEDColor.addItem("RED");
        cmb7LEDColor.addItem("GREEN");
        cmb7LEDColor.addItem("BLUE");
        
        cmb7LEDConfig.removeAllItems();
        cmb7LEDConfig.addItem("Common Cathode");
        cmb7LEDConfig.addItem("Common Anode");
                
        // ------- Set the text of the About dialog box ---------------
        String htmlHeader="<html><body style='width:370px; text-align:center;'>";
        String htmlFooter="</body></html>";
        String piracyText="All Rights Reserved. Unauthorized reproduction of this program, or any portion of it, may result in severe civil and criminal penalties, and will be prosecuted to the maximum extent possible under the law.";
        lblPiracyWarning.setText(htmlHeader+piracyText+htmlFooter);
        
        //String thanksText="Special thanks to these lovely women of the 2018 Batch of B.Sc.(Computer Science) of MUC Women's College, Burdwan:<br/><br/>Anindita Pal, Prathama Dey, Sujata Sarkar, Sonali Mondal, Debasmita Roy, Anusuya Garai, Genius Khatun, Arpita Ghosh, Sayani Banerjee, Kabita Mondal, Sudeshna Dutta, Ekta Mishra, Susmita Guha, Papri Mondal, Soumita Garai, Debalina Dutta, Baisakhi Roy, Sucharita Sinha, and Nishi Ahir.";
        //lblThanks.setText(htmlHeader+thanksText+htmlFooter);
        
        jLabel2.setText("\u0041\u006b\u0061\u0073\u0068\u0020\u004e\u0061\u0067");
        // ---------------------------------------------------------
        
        // --------- Set Filters for the Open/Save dialogs ------------
        jFileChooser1.removeChoosableFileFilter(jFileChooser1.getAcceptAllFileFilter());
        jFileChooser2.removeChoosableFileFilter(jFileChooser2.getAcceptAllFileFilter());
        jFileChooser3.removeChoosableFileFilter(jFileChooser3.getAcceptAllFileFilter());
        
        jFileChooser1.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() 
        {
            public String getDescription() 
            {
                return "Breadboard Circuit Designer Files (*.bcf)";
            }

            public boolean accept(java.io.File f) 
            {
               if (f.isDirectory()) 
               {
                   return true;
               } else {
                   return f.getName().toLowerCase().endsWith(".bcf");
               }
            }
        });
        
        jFileChooser2.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() 
        {
            public String getDescription() 
            {
                return "Breadboard Circuit Designer Files (*.bcf)";
            }

            public boolean accept(java.io.File f) 
            {
               if (f.isDirectory()) 
               {
                   return true;
               } else {
                   return f.getName().toLowerCase().endsWith(".bcf");
               }
            }
        });
        
        // ------------ Add Image File Types for Export --------------
        jFileChooser3.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() 
        {
            public String getDescription() 
            {
                return "Portable Network Graphics Images (*.png)";
            }

            public boolean accept(java.io.File f) 
            {
               if (f.isDirectory()) 
               {
                   return true;
               } else {
                   String x = f.getName().toLowerCase();
                   return x.endsWith(".png");
               }
            }
        });
        
        jFileChooser3.addChoosableFileFilter(new javax.swing.filechooser.FileFilter() 
        {
            public String getDescription() 
            {
                return "Graphics Interchange Format Images (*.gif)";
            }

            public boolean accept(java.io.File f) 
            {
               if (f.isDirectory()) 
               {
                   return true;
               } else {
                   String x = f.getName().toLowerCase();
                   return x.endsWith(".gif");
               }
            }
        });
        // -----------------------------------
        
        // Load IC list
        try {
            String s=null;
            InputStream file = MainWindow.class.getResourceAsStream("resources/ic/iclist.data");
            BufferedReader br = new BufferedReader(new InputStreamReader(file));
            cmbIC.removeAllItems();
            while((s=br.readLine())!=null) cmbIC.addItem(s);
            br.close();
        } catch(java.io.IOException e) {
            Utility.alert("Error reading IC list.");
        }
        
        // Set dialog box dimensions
        dlgFileOpen.setBounds(200, 100, 590, 430);
        dlgFileSave.setBounds(200, 100, 590, 430);
        dlgExport.setBounds(200, 100, 590, 430);
        dlgAbout.setBounds(200,100,520,270);
        dlgInsertLED.setBounds(200,100,318,115);
        dlgInsertResistor.setBounds(200,100,310,145);
        dlgSetWireColor.setBounds(200,100,295,120);
        dlgSetDimensions.setBounds(200,100,205,150);
        dlgInsertSevenSegmentLED.setBounds(200,100,330,150);
        dlgInsertIC.setBounds(200,100,387,115);
    }
    
    private void initMenu()
    {
        menuActions = new MenuActions(this);
        
        // create the menu bar
        menuBar = new JMenuBar();
        
        // create top level menus
        mnuFile = new JMenu("File");
        mnuFile.setMnemonic(KeyEvent.VK_F);
        menuBar.add(mnuFile);
        
        mnuEdit = new JMenu("Edit");
        mnuEdit.setMnemonic(KeyEvent.VK_E);
        menuBar.add(mnuEdit);
        
        mnuInsert = new JMenu("Insert");
        mnuInsert.setMnemonic(KeyEvent.VK_I);
        menuBar.add(mnuInsert);
        
        mnuView = new JMenu("View");
        mnuView.setMnemonic(KeyEvent.VK_V);
        menuBar.add(mnuView);
        
        mnuHelp = new JMenu("Help");
        mnuHelp.setMnemonic(KeyEvent.VK_H);
        menuBar.add(mnuHelp);
        
        // create file-menu items
        mnuFileNew = new JMenuItem("New Circuit", KeyEvent.VK_N);
        mnuFileNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        mnuFile.add(mnuFileNew);
        mnuFileNew.addActionListener(menuActions);
        
        mnuFileOpen = new JMenuItem("Open...", KeyEvent.VK_O);
        mnuFileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        mnuFile.add(mnuFileOpen);
        mnuFileOpen.addActionListener(menuActions);
        
        mnuFileClose = new JMenuItem("Close", KeyEvent.VK_C);
        mnuFile.add(mnuFileClose);
        mnuFileClose.addActionListener(menuActions);
        
        mnuFile.addSeparator();
        
        mnuFileSave = new JMenuItem("Save", KeyEvent.VK_S);
        mnuFileSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        mnuFile.add(mnuFileSave);
        mnuFileSave.addActionListener(menuActions);
        
        mnuFileSaveAs = new JMenuItem("Save As...", KeyEvent.VK_A);
        mnuFile.add(mnuFileSaveAs);
        mnuFileSaveAs.addActionListener(menuActions);
        
        mnuFile.addSeparator();
        
        mnuFileExport = new JMenuItem("Export as image", KeyEvent.VK_E);
        mnuFile.add(mnuFileExport);
        mnuFileExport.addActionListener(menuActions);
        
        mnuFile.addSeparator();
        
        mnuFileExit = new JMenuItem("Exit", KeyEvent.VK_X);
        mnuFile.add(mnuFileExit);
        mnuFileExit.addActionListener(menuActions);
        // ----------------------------------
        
        // create edit-menu items ---------------
        
        mnuEditSetDimensions = new JMenuItem("Set Dimensions", KeyEvent.VK_S);
        mnuEdit.add(mnuEditSetDimensions);
        mnuEditSetDimensions.addActionListener(menuActions);
        
        mnuEditSetWireColor = new JMenuItem("Set Wire Colour", KeyEvent.VK_C);
        mnuEditSetWireColor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
        mnuEdit.add(mnuEditSetWireColor);
        mnuEditSetWireColor.addActionListener(menuActions);
        
        mnuEdit.addSeparator();
        
        mnuEditWireMode = new JMenuItem("Start wire editing mode", KeyEvent.VK_M);
        mnuEditWireMode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        mnuEdit.add(mnuEditWireMode);
        mnuEditWireMode.addActionListener(menuActions);
        
        mnuEditDelete = new JMenuItem("Start delete mode", KeyEvent.VK_D);
        mnuEditDelete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));
        mnuEdit.add(mnuEditDelete);
        mnuEditDelete.addActionListener(menuActions);
        
        mnuEdit.addSeparator();
        
        mnuEditSetIdleMode = new JMenuItem("Set to idle mode", KeyEvent.VK_T);
        mnuEditSetIdleMode.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
        mnuEdit.add(mnuEditSetIdleMode);
        mnuEditSetIdleMode.addActionListener(menuActions);
        
        
        // add Insert menu items
        
        mnuInsertPowerSupply = new JMenuItem("Power Supply", KeyEvent.VK_P);
        mnuInsertPowerSupply.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.CTRL_MASK));
        mnuInsert.add(mnuInsertPowerSupply);
        mnuInsertPowerSupply.addActionListener(menuActions);
        
        mnuInsertBreadboard = new JMenuItem("Breadboard", KeyEvent.VK_B);
        mnuInsertBreadboard.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK));
        mnuInsert.add(mnuInsertBreadboard);
        mnuInsertBreadboard.addActionListener(menuActions);
        
        mnuInsert.addSeparator();
        
        mnuInsertDIP = new JMenuItem("IC (DIP)", KeyEvent.VK_I);
        mnuInsertDIP.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.CTRL_MASK));
        mnuInsert.add(mnuInsertDIP);
        mnuInsertDIP.addActionListener(menuActions);
        
        mnuInsertResistor = new JMenuItem("Resistor", KeyEvent.VK_R);
        mnuInsertResistor.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK));
        mnuInsert.add(mnuInsertResistor);
        mnuInsertResistor.addActionListener(menuActions);
        
        mnuInsertLED = new JMenuItem("LED", KeyEvent.VK_L);
        mnuInsertLED.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK));
        mnuInsert.add(mnuInsertLED);
        mnuInsertLED.addActionListener(menuActions);
        
        mnuInsert7LED = new JMenuItem("7-Segment LED", KeyEvent.VK_E);
        mnuInsert7LED.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_7, ActionEvent.CTRL_MASK));
        mnuInsert.add(mnuInsert7LED);
        mnuInsert7LED.addActionListener(menuActions);
                
        
        // --------------------------------
        
        // create the view menu items
        mnuViewHoleStatus = new JMenuItem("Show breadboard hole status", KeyEvent.VK_S);
        mnuViewHoleStatus.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        mnuView.add(mnuViewHoleStatus);
        mnuViewHoleStatus.addActionListener(menuActions);
        
        // --------------------------
        
        // create the help menu items
        mnuHelpAbout = new JMenuItem("About...", KeyEvent.VK_A);
        mnuHelpAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        mnuHelp.add(mnuHelpAbout);
        mnuHelpAbout.addActionListener(menuActions);
        // --------------------------
        
        // add the menu-bar to the frame
        this.setJMenuBar(menuBar);
    }
    
    private void resizeScrollPane()
    {
        if(scroller!=null) 
        {
            scroller.setPreferredSize(new Dimension(this.getWidth()-17,this.getHeight()-63));
            scroller.setBounds(1, 1, this.getWidth()-17, this.getHeight()-63);
        }
    }
    
    private void refreshCircuit()
    {
        if(scroller!=null) scroller.setBorder(new javax.swing.border.LineBorder(Color.GRAY));
    }
    
    
    
    
    // WARNING: Inner-Class (DrawingPane)
    class DrawingPane extends JPanel implements MouseListener, MouseMotionListener
    {
        private int lastX, lastY;
        
        private ArrayList<CircuitComponent> components;
        private ArrayList<Wire> wires;
                
        public DrawingPane()
        {
            super();
            floatingMode = false;
            components = new ArrayList<CircuitComponent>();
            wires = new ArrayList<Wire>();
        }
        
        public void setData(ArrayList<CircuitComponent> c, ArrayList<Wire> w)
        {
            components = c;
            wires = w;
            
            simulateCircuit();
            revalidate();
            repaint();
            refreshCircuit();
        }
        
        private void simulateCircuit()
        {
            simulationMode=true;
            setTitle();
            Thread.yield();
            Simulation.simulateCircuit(components,wires);
            simulationMode=false;
            setTitle();
            Thread.yield();
        }
        
        public ArrayList<CircuitComponent> getAllComponents() { return components; }
        public ArrayList<Wire> getAllWires() { return wires; }
        
        @Override
        public void paintComponent(Graphics g)
        {
            repaint();
        }
        
        public void addComponent(ComponentType type)
        {
            if(floatingMode)
            {
                components.get(components.size()-1).moveTo(new Location(lastX,lastY),true,this.getGraphics());
                floatingMode=false;
            }
            
            if(type==ComponentType.BREADBOARD) 
            {
                if(breadboardCounter>0)
                    Utility.alert("Cannot insert more than 1 breadboard.");
                else {
                    breadboardCounter++;
                    components.add(new Breadboard(new Location(lastX,lastY)));
                }
            } else if(type==ComponentType.POWER_SUPPLY) {
                if(powerSupplyCounter>0)
                    Utility.alert("Cannot insert more than 1 power-supply.");
                else {
                    powerSupplyCounter++;
                    components.add(new PowerSupply(new Location(lastX,lastY)));
                }
            }
            
            floatingMode = true;
            hasBeenSaved=false;
            setTitle();
            repaint();
            refreshCircuit();
        }
        
        private int getComponentIndexFromLocation(Location p, boolean shouldHaveHoles)
        {
            int n = components.size();
            for(int i=0; i<n; i++)
            {
                CircuitComponent c = components.get(i);
                if(!shouldHaveHoles || c.getType()==ComponentType.BREADBOARD || c.getType()==ComponentType.POWER_SUPPLY)
                {
                    if(c.isPointWithinComponent(p)) return i;
                }
            }
            return -1;
        }
        
        private int getHoleIDFromComponent(int compIndex, Location p)
        {
            if(compIndex==-1) return -1;
            CircuitComponent c = components.get(compIndex);
            int n = c.getMaxHoles(), r = c.getHoleRadius();
            for(int i=1; i<=n; i++)
            {
                Location hc = c.getHoleCenter(i);
                if(p.x >= hc.x-r && p.x <= hc.x+r)
                {
                    if(p.y >= hc.y-r && p.y <= hc.y+r)
                    {
                        return i;
                    }
                }
            }
            return -1;
        }
        
        // ------------------------------------------
        // Mouse click event handler
        // Each of the following is called from DrawingPane::mouseClicked()
        // ------------------------------------------
        private void clickedInFloatingMode()
        {
            if(!floatingMode) return;
            components.get(components.size()-1).moveTo(new Location(lastX,lastY),true,this.getGraphics());
            floatingMode=false;
            simulateFlag=false;
            circuitChanged=true;
        }
        
        private void clickedInWireEditingMode()
        {
            if(!wireEditingMode) return;
            Location pos = new Location(lastX,lastY);
            int ci = getComponentIndexFromLocation(pos,true);
            int hi = getHoleIDFromComponent(ci,pos);
                
            if(ci==-1 || hi==-1) return;
                
            if(wireSourceNext)
            {
                if(components.get(ci).isHoleBlocked(hi, wires, components))
                {
                    // remove the wire
                    int x=Utility.confirmWithCancel("Click YES to remove this wire, click NO to move this wire, or click CANCEL to do nothing.","Modify Wire");
                        
                    if(x==JOptionPane.YES_OPTION)
                    {
                        removeWireAt(ci,hi);
                        circuitChanged=true;
                    } else if(x==JOptionPane.NO_OPTION) {
                        Object temp[] = removeWireAt(ci,hi);
                          
                        Wire rw = (Wire)temp[0];
                        boolean src = (Boolean)temp[1];
                            
                        wireSourceComponentIndex=CircuitUtility.getIndexFromComponentReference(components, (!src ? rw.getSource() : rw.getSink()));
                        wireSourceHoleID=(src ? rw.getSinkHoleID() : rw.getSourceHoleID());
                        wireSourceNext=false;
                        simulateFlag=false;
                        circuitChanged=true;
                    }
                } else {
                    // start a new wire from here
                    wireSourceComponentIndex=ci;
                    wireSourceHoleID=hi;
                    wireSourceNext=false;
                    simulateFlag=false;
                }
            } else {
                // end the existing wire here
                if(hi==wireSourceHoleID)
                {
                    Utility.alert("Cannot start and end wire at the same hole.");
                    return;
                }
                    
                if(components.get(ci).isHoleBlocked(hi, wires, components))
                {
                    Utility.alert("The selected hole is already blocked.");
                    return;
                }
                    
                CircuitComponent link[] = { components.get(wireSourceComponentIndex), components.get(ci) };
                int holeIDs[] = { wireSourceHoleID, hi };
                wires.add(new Wire(currentWireColor,link,holeIDs));
                wireSourceNext=true;
                circuitChanged=true;
            }
        }
        
        private void clickedInResistorInsertMode()
        {
            if(!resistorInsertMode) return;
            Location pos = new Location(lastX,lastY);
            
            int ci = getComponentIndexFromLocation(pos,true);
            int hi = getHoleIDFromComponent(ci,pos);
                
            if(ci==-1 || hi==-1) return;
            if(components.get(ci).getType()!=ComponentType.BREADBOARD) return;
                 
            if(components.get(ci).isHoleBlocked(hi, wires, components))
            {
                Utility.alert("The selected hole is already blocked.");
                return;
            }
                
            if(resistorInsertSourceNext)
            {
                resistorSourceComponentIndex=ci;
                resistorSourceHoleID=hi;
                resistorInsertSourceNext=false;
                simulateFlag=false;
            } else {
                // only allow horizontal resistor connection
                if(!isHorizontal(resistorSourceComponentIndex,resistorSourceHoleID,ci,hi)) 
                {
                    Utility.alert("Resistors can only be connected horizontally.");
                    resistorSourceComponentIndex=-1;
                    resistorSourceHoleID=-1;
                    resistorInsertSourceNext=true;
                    simulateFlag=false;
                    return;
                }
                    
                if(ci!=resistorSourceComponentIndex)
                {
                    Utility.alert("Resistors cannot connect different breadboards.");
                    simulateFlag=false;
                    return;
                }
                    
                if(Math.abs(resistorSourceHoleID-hi)<4)
                {
                    Utility.alert("Connection too short.");
                    simulateFlag=false;
                    return;
                }
                    
                if(Math.abs(resistorSourceHoleID-hi)>10)
                {
                    Utility.alert("Connection too long.");
                    simulateFlag=false;
                    return;
                }
                    
                int holeIDs[] = { resistorSourceHoleID, hi };
                components.add(new Resistor((Breadboard)components.get(ci),holeIDs,currentResistance));
                resistorInsertSourceNext=true;
                resistorInsertMode=false;
                circuitChanged=true;
            }
        }
        
        private void clickedInLEDInsertMode()
        {
            if(!insertLEDMode) return;
            Location pos = new Location(lastX,lastY);
            
            int ci = getComponentIndexFromLocation(pos,true);
            int hi = getHoleIDFromComponent(ci,pos);
                
            if(ci==-1 || hi==-1) return;
            if(components.get(ci).getType()!=ComponentType.BREADBOARD) return;
                
            if(hi==1 || hi==51 || hi==101 || hi==151) return;        // no space to position cathode of LED
            if((hi-201)%63==0) return;
                
            // User has to click on Anode hole
            // Cathode is one hole to the left of anode
                
            if(components.get(ci).isHoleBlocked(hi, wires, components))
            {
                Utility.alert("The selected anode hole is already blocked.");
                insertLEDMode=false;
                return;
            }
                
            if(components.get(ci).isHoleBlocked(hi-1, wires, components))
            {
                Utility.alert("The selected cathode hole is already blocked.");
                insertLEDMode=false;
                return;
            }
                
            components.add(new LED((Breadboard)components.get(ci),hi,hi-1,currentLEDColor));
            insertLEDMode=false;
            circuitChanged=true;
        }
        
        private void clickedInICInsertMode()
        {
            if(!insertICMode) return;
            Location pos = new Location(lastX,lastY);
            
            int ci = getComponentIndexFromLocation(pos,true);
            int hi = getHoleIDFromComponent(ci,pos);
                
            if(ci==-1 || hi==-1) return;
            if(components.get(ci).getType()!=ComponentType.BREADBOARD) return;
                
            Breadboard b=(Breadboard)components.get(ci);
            String icName=cmbIC.getItemAt(currentICType);
            if(!IC.canPositionIC(b,icName,hi,components,wires))
            {
                Utility.alert("Cannot place IC here.");
                insertICMode=false;
                return;       
            }
                
            // User has to click on Pin-1 hole
            components.add(new IC(b,hi,icName));
            insertICMode=false;
            circuitChanged=true;
        }
        
        private void clickedInSevenSegmentLEDInsertMode()
        {
            if(!insertSevenSegmentLEDMode) return;
            Location pos = new Location(lastX,lastY);
            
            int ci = getComponentIndexFromLocation(pos,true);
            int hi = getHoleIDFromComponent(ci,pos);
                
            if(ci==-1 || hi==-1) return;
            if(components.get(ci).getType()!=ComponentType.BREADBOARD) return;
                
            Breadboard b=(Breadboard)components.get(ci);
            if(!SevenSegmentLED.canPositionLED(b,hi,components,wires))
            {
                Utility.alert("Cannot place the 7-Segment LED here.");
                return;       
            }
                
            // User has to click on Pin-1 hole
            components.add(new SevenSegmentLED(b,hi,current7LEDConfigIsCommonCathode,current7LEDColor));
            insertSevenSegmentLEDMode=false;
            circuitChanged=true;
        }
        
        private void clickedInDeleteMode()
        {
            if(!deleteMode) return;
            Location pos = new Location(lastX,lastY);
            
            int ci = getComponentIndexFromLocation(pos,true);
            int hi = getHoleIDFromComponent(ci,pos);
                
            if(ci==-1 || hi==-1) return;
            if(components.get(ci).getType()!=ComponentType.BREADBOARD) return;
                
            Breadboard b=(Breadboard)components.get(ci);
               
            int n = components.size();
            for(int i=0; i<n; i++)
            {
                CircuitComponent x = components.get(i);
                Breadboard bs = x.getBase();
                    
                if(bs!=null && bs==b)
                {
                    if(x.isUsingBaseHole(hi))
                    {
                        if(Utility.confirm("Are you sure you want to delete this "+x.getType().toString().toUpperCase()+"?", "Delete Component"))
                        {
                            components.remove(i);
                            circuitChanged=true;
                            break;
                        }
                    }
                }
            }
        }
        
        private void clickedOnComponent()
        {
            Location pos = new Location(lastX,lastY);
            
            int ci = getComponentIndexFromLocation(pos,true);
            int hi = getHoleIDFromComponent(ci,pos);
                
            if(ci==-1) return;
                
            if(components.get(ci).getType()==ComponentType.BREADBOARD && hi!=-1)
            {
                simulateFlag=false;
                Breadboard b=(Breadboard)components.get(ci);
                Utility.alert("Status of hole("+hi+") is "+b.getHoleState(hi));
            } else if(components.get(ci).getType()==ComponentType.POWER_SUPPLY) {
                PowerSupply ps=(PowerSupply)components.get(ci);
                boolean cs=ps.click(lastX,lastY);
                if(!cs) simulateFlag=false;
                    
                if(!ps.isClockOn() || !ps.isPoweredOn() || ps.getFrequency()==0)
                {
                    if(clockTimer!=null)
                    {
                        clockTimer.stop();
                        clockTimer=null;
                    }
                }
                    
                if(ps.isClockOn() && ps.isPoweredOn() && ps.getFrequency()>0)
                {
                    if(clockTimer!=null)
                    {
                        clockTimer.stop();
                        clockTimer=null;
                    }
                        
                    int delay = (int)(1000 * PowerSupply.DUTY_CYCLE * (1.0 / ps.getFrequency()));
                    clockTimer = new Timer(delay, new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            ps.trigger();
                            simulateCircuit();
                            revalidate();
                            repaint();
                            refreshCircuit();
                        }
                    });
                        
                    powerSupplyFrequency=ps.getFrequency();
                    clockTimer.start();
                }
            }
        }
        // ------------------------------------------
        
        // ------------------------------------------
        // Drawing pane mouse handlers
        // ------------------------------------------
        @Override
        public void mouseMoved(MouseEvent e)
        {
            lastX = e.getX();
            lastY = e.getY();
            
            if(floatingMode)
            {
                components.get(components.size()-1).moveTo(new Location(lastX,lastY),false,this.getGraphics());
            } else {
                Location pos = new Location(lastX,lastY);
                
                int ci = getComponentIndexFromLocation(pos,true);
                int hi = getHoleIDFromComponent(ci,pos);
                
                if(ci==-1 || hi==-1) { refreshCircuit(); return; }
                if(components.get(ci).getType()==ComponentType.BREADBOARD)
                {
                    Breadboard b = (Breadboard)components.get(ci);
                    b.setHighlight(hi);
                }
            }
            
            refreshCircuit();
        }
        
        @Override
        public void mouseClicked(MouseEvent e)
        {
            simulateFlag=true;
            circuitChanged=false;
            
            lastX = e.getX();
            lastY = e.getY();
            
            
            if(floatingMode)
            {
                clickedInFloatingMode();
            } else if(wireEditingMode) {
                clickedInWireEditingMode();
            } else if(resistorInsertMode) {
                clickedInResistorInsertMode();
            } else if(insertLEDMode) {
                clickedInLEDInsertMode();
            } else if(insertICMode) {
                clickedInICInsertMode();
            } else if(insertSevenSegmentLEDMode) {
                clickedInSevenSegmentLEDInsertMode();              
            } else if(deleteMode) {
                clickedInDeleteMode();
            } else {
                clickedOnComponent();    
            }
            
            if(simulateFlag) 
            {
                simulateCircuit();
            }
                
            if(circuitChanged)
            {
                hasBeenSaved=false;
                setTitle();
            }
            
            revalidate();
            repaint();
            refreshCircuit();
            setTitle();
        }

        
        
        @Override
        public void mouseDragged(MouseEvent e) 
        {
            if(!dragMode) return;
            
            lastX = e.getX();
            lastY = e.getY();
            
            Location pos = new Location(lastX-dragComponentOffsetX,lastY-dragComponentOffsetY);
            components.get(draggedComponentIndex).moveTo(pos, true, this.getGraphics());
            
            hasBeenSaved=false;
            setTitle();
                
            revalidate();
            repaint();
            refreshCircuit();
        }
        
        @Override
        public void mouseReleased(MouseEvent e)
        {
            dragMode=false;
            draggedComponentIndex=-1;
        }
        
        @Override
        public void mouseEntered(MouseEvent e){}

        @Override
        public void mouseExited(MouseEvent e){}

        @Override
        public void mousePressed(MouseEvent e)
        {
            // drag start
            if(!isCircuitActive) return;
            if(floatingMode) return;
            if(wireEditingMode) return;
            
            lastX = e.getX();
            lastY = e.getY();
            Location pos = new Location(lastX,lastY);
            int ci = getComponentIndexFromLocation(pos,false);
                        
            if(ci==-1) return;
            
            dragComponentOffsetX = lastX - components.get(ci).getLocation().x;
            dragComponentOffsetY = lastY - components.get(ci).getLocation().y;
            
            dragMode=true;
            draggedComponentIndex=ci;
        }
        // ------------------------------------------
    
        private Object[] removeWireAt(int compIndex, int holeIndex)
        {
            CircuitComponent c = components.get(compIndex);
            int n = wires.size();
            for(int i=0; i<n; i++)
            {
                Wire w = wires.get(i);
                if((w.getSource()==c && w.getSourceHoleID()==holeIndex)||(w.getSink()==c && w.getSinkHoleID()==holeIndex))
                {
                    Wire temp = w;
                    Object x[] = { temp, (w.getSourceHoleID()==holeIndex) };
                    wires.remove(i);
                    return x;
                }
            }
            
            return null;
        }
        
        private boolean isHorizontal(int ci1, int hi1, int ci2, int hi2)
        {
            Location p1 = components.get(ci1).getHoleCenter(hi1);
            Location p2 = components.get(ci2).getHoleCenter(hi2);
            
            return (p1.y==p2.y);
        }
        
        @Override
        public void paint(Graphics g)
        {
            g.setColor(CIRCUIT_BACKGROUND_COLOR);
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            
            int n = components.size();
            int k = ComponentType.values().length;
            
            for(int i=0; i<k; i++)
            {
                if(ComponentType.WIRE.ordinal()==i)
                {
                    int m = wires.size();
                    for(int j=0; j<m; j++) wires.get(j).paint(g);
                } else {
                    for(int j=0; j<n; j++) 
                    {
                        CircuitComponent c = components.get(j);
                        if(c.getType()==ComponentType.values()[i]) c.paint(g);
                    }
                }
            }
        }
        
        public void updateBreadboardHoleStatusVisibility()
        {
            int n = components.size();
            for(int i=0; i<n; i++)
            {
                if(components.get(i).getType()!=ComponentType.BREADBOARD) continue;
                
                Breadboard b=(Breadboard)components.get(i);
                if(holeStatusVisible)
                    b.showHoleStatus();
                else
                    b.hideHoleStatus();
            }
            
            revalidate();
            repaint();
            refreshCircuit();
        }
    }
    
    
    
    
    
    
    
    
    
    
    
    // ------------------------------------------
    // Menu-click handlers
    // ------------------------------------------
    
    // ------------ FILE MENU --------------------
    protected void mnuFileNew_Click()
    {
        mnuFileClose_Click();
        
        circuitWidth = DEFAULT_CIRCUIT_WIDTH;
        circuitHeight = DEFAULT_CIRCUIT_HEIGHT;
        
        drawingPane = new DrawingPane();
        drawingPane.setBounds(0,0,circuitWidth,circuitHeight);
        drawingPane.setPreferredSize(new Dimension(circuitWidth,circuitHeight));
        drawingPane.addMouseListener((DrawingPane)drawingPane);
        drawingPane.addMouseMotionListener((DrawingPane)drawingPane);
        
        scroller = new JScrollPane(drawingPane);
        resizeScrollPane();
        
        add(scroller);
        scroller.getHorizontalScrollBar().addAdjustmentListener(this);
        scroller.getVerticalScrollBar().addAdjustmentListener(this);
        
        isCircuitActive=true;
        hasBeenSaved=false;
        hasFileBeenAllotted=false;
        circuitFilePath=null;
        
        setTitle();
        repaint();
        revalidate();
        refreshCircuit();
    }
    
    protected boolean mnuFileClose_Click()
    {
        if(!isCircuitActive) return true;
        if(!hasBeenSaved)
        {
            int choice=Utility.confirmWithCancel("Do you want to save the changes?", "Save Circuit");
            if(choice==JOptionPane.CANCEL_OPTION) return false;
            
            if(choice==JOptionPane.YES_OPTION)
            {
                mnuFileSave_Click();
            }
        }
        
        scroller.remove(drawingPane);
        this.remove(scroller);
        
        drawingPane=null;
        scroller=null;
        
        isCircuitActive=false;
        hasBeenSaved=false;
        hasFileBeenAllotted=false;
        circuitFilePath=null;
        
        breadboardCounter=0;
        powerSupplyCounter=0;
        
        setTitle();
        
        repaint();
        
        return true;
    }
    
    protected void mnuFileSave_Click()
    {
        if(!isCircuitActive || hasBeenSaved) return;
        if(hasFileBeenAllotted)
        {
            try {
                FileIO.saveData((DrawingPane)drawingPane,circuitFilePath);
                hasBeenSaved=true;
                setTitle();
            } catch(java.io.IOException e) {
                Utility.alert("Error while saving file.");
            }
        } else {
            mnuFileSaveAs_Click();
        }
    }
    
    protected void mnuFileSaveAs_Click()
    {
        dlgFileSave.setVisible(true);
    }
    
    protected void mnuFileOpen_Click()
    {
        mnuFileClose_Click();
        dlgFileOpen.setVisible(true);
    }
    
    protected void mnuFileExport_Click()
    {
        dlgExport.setVisible(true);
    }
    
    protected void mnuFileExit_Click()
    {
        if(mnuFileClose_Click()) System.exit(0);
    }
    // ------------------------------------------------
    
    // --------------- EDIT MENU ----------------------
    protected void mnuEditDelete_Click()
    {
        deleteMode = !deleteMode;
        mnuEditDelete.setText((deleteMode ? "Stop" : "Start")+" delete mode");
        setTitle();
    }
    
    protected void mnuEditSetWidth_Click()
    {
        if(!isCircuitActive) return;
        String w = Utility.inputBox("Enter new width: ", ""+circuitWidth);
        if(w!=null)
        {
            circuitWidth = Integer.parseInt(w);
            hasBeenSaved=false;
            setTitle();
            drawingPane.setPreferredSize(new java.awt.Dimension(circuitWidth,circuitHeight));
            drawingPane.revalidate();
            drawingPane.repaint();
        }
    }
    
    protected void mnuEditSetHeight_Click()
    {
        if(!isCircuitActive) return;
        String h = Utility.inputBox("Enter new height: ", ""+circuitHeight);
        if(h!=null)
        {
            circuitHeight = Integer.parseInt(h);
            hasBeenSaved=false;
            setTitle();
            drawingPane.setPreferredSize(new java.awt.Dimension(circuitWidth,circuitHeight));
            drawingPane.revalidate();
            drawingPane.repaint();
        }
    }
    
    protected void mnuEditSetDimensions_Click()
    {
        txtCircuitWidth.setText("" + circuitWidth);
        txtCircuitHeight.setText("" + circuitHeight);
        dlgSetDimensions.setVisible(true);
    }
    
    protected void mnuEditWireMode_Click()
    {
        if(!isCircuitActive) {
            mnuEditWireMode.setText("Start wire editing mode");
            wireEditingMode=false;
        } else {
            wireEditingMode=!wireEditingMode;
            wireSourceNext = true;
            mnuEditWireMode.setText((wireEditingMode ? "Stop" : "Start")+" wire editing mode");
        }
        setTitle();
    }
    
    protected void mnuEditSetWireColor_Click()
    {
        String x[] = { "BLACK", "BLUE", "RED", "GREEN", "YELLOW", "CYAN", "MAGENTA", "PINK" };
        Color y[] = { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.PINK };
        int k=-1;
        for(int i=0; i<y.length; i++)
        {
            if(currentWireColor==y[i]) k=i;
        }
        
        cmbWireColor.setSelectedItem(x[k]);
        dlgSetWireColor.setVisible(true);
    }
    
    protected void mnuEditSetToIdleMode_Click()
    {
        mnuEditWireMode.setText("Start wire editing mode");
        wireEditingMode=false;
        
        mnuEditDelete.setText("Start delete mode");
        deleteMode=false;
        
        resistorInsertMode=false;
        insertICMode=false;
        insertLEDMode=false;
        insertSevenSegmentLEDMode=false;
        
        setTitle();
    }
    // -------------------------------------------------
    
    // ---------------- INSERT MENU --------------------
    protected void mnuInsertBreadboard_Click()
    {
        if(isCircuitActive) ((DrawingPane)drawingPane).addComponent(ComponentType.BREADBOARD);
    }
    
    protected void mnuInsertPowerSupply_Click()
    {
        if(isCircuitActive) ((DrawingPane)drawingPane).addComponent(ComponentType.POWER_SUPPLY);
    }
        
    protected void mnuInsertResistor_Click()
    {
        if(!isCircuitActive || wireEditingMode || floatingMode) return;
        if(breadboardCounter==0)
        {
            Utility.alert("Insert a breadboard first.");
            return;
        }
        mnuEditSetToIdleMode_Click();
        txtResistance.setText(""+currentResistance);
        dlgInsertResistor.setVisible(true);
    }
    
    protected void mnuInsertLED_Click()
    {
        if(!isCircuitActive || wireEditingMode || floatingMode) return;
        if(breadboardCounter==0)
        {
            Utility.alert("Insert a breadboard first.");
            return;
        }
        mnuEditSetToIdleMode_Click();
        dlgInsertLED.setVisible(true);
    }
    
    protected void mnuInsertSevenSegmentLED_Click()
    {
        if(!isCircuitActive || wireEditingMode || floatingMode) return;
        if(breadboardCounter==0)
        {
            Utility.alert("Insert a breadboard first.");
            return;
        }
        mnuEditSetToIdleMode_Click();        
        dlgInsertSevenSegmentLED.setVisible(true);
    }
    
    protected void mnuInsertIC_Click()
    {
        if(!isCircuitActive || wireEditingMode || floatingMode) return;
        if(breadboardCounter==0)
        {
            Utility.alert("Insert a breadboard first.");
            return;
        }
        mnuEditSetToIdleMode_Click();
        dlgInsertIC.setVisible(true);
    }
    // ------------------------------------------------
    
    // ------------- View Menu ------------------------
    protected void mnuViewHoleStatus_Click()
    {
        holeStatusVisible=!holeStatusVisible;
        mnuViewHoleStatus.setText((holeStatusVisible ? "Hide" : "Show")+" breadboard hole status");
        if(isCircuitActive)
        {
            ((DrawingPane)drawingPane).updateBreadboardHoleStatusVisibility();
        }
        setTitle();
    }
    // ------------------------------------------------
    
    
    // ------------- Help Menu ------------------------
    protected void mnuHelpAbout_Click()
    {
        dlgAbout.setVisible(true);
    }
    // ------------------------------------------------
    
    @Override
    public void adjustmentValueChanged(AdjustmentEvent evt) 
    {
        refreshCircuit();
    }
    
    // ---------------------------------
    // UTILITY FUNCTIONS
    // ---------------------------------
    protected void setTitle()
    {
        if(!isCircuitActive)
        {
            setTitle("Breadboard Circuit Designer 1.0");
            return;
        }
        
        String mode="";
        if(wireEditingMode)
            mode="[Wire Editing Mode]";
        else if(floatingMode)
            mode="[Breadboard/Power-Supply Insertion Mode]";
        else if(resistorInsertMode)
            mode="[Resistor Insertion Mode]";
        else if(insertICMode)
            mode="[IC Insertion Mode]";
        else if(insertLEDMode)
            mode="[LED Insertion Mode]";
        else if(insertSevenSegmentLEDMode)
            mode="[7-Segment LED Insertion Mode]";
        else if(deleteMode)
            mode="[Deletion Mode]";
        
        if(holeStatusVisible) mode+=(mode.length()>0 ? " " : "")+"[Current-Flow Visible]";                    
        if(simulationMode) mode+=(mode.length()>0 ? " " : "")+"[Simulating...]";                    
        
        if(mode.length()>0) mode=" "+mode;
        
        String fileName = (hasFileBeenAllotted ? circuitFilePath.substring(circuitFilePath.lastIndexOf("\\")+1) : "Untitled");
        fileName = "[" + fileName + "]"+mode+" - Breadboard Circuit Designer 1.0";
        if(!hasBeenSaved) fileName = "*" + fileName;
        setTitle(fileName);
    }
    
    // ---------------------------------
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents()
    {

        dlgFileOpen = new javax.swing.JDialog();
        jFileChooser1 = new javax.swing.JFileChooser();
        dlgFileSave = new javax.swing.JDialog();
        jFileChooser2 = new javax.swing.JFileChooser();
        dlgSetWireColor = new javax.swing.JDialog();
        jLabel4 = new javax.swing.JLabel();
        cmbWireColor = new javax.swing.JComboBox<>();
        btnWireColorOK = new javax.swing.JButton();
        btnWireColorCancel = new javax.swing.JButton();
        dlgInsertResistor = new javax.swing.JDialog();
        jLabel6 = new javax.swing.JLabel();
        txtResistance = new javax.swing.JTextField();
        btnResistorCancel = new javax.swing.JButton();
        btnResistorInsert = new javax.swing.JButton();
        jLabel18 = new javax.swing.JLabel();
        dlgInsertLED = new javax.swing.JDialog();
        jLabel7 = new javax.swing.JLabel();
        cmbLEDColor = new javax.swing.JComboBox<>();
        btnLEDCancel = new javax.swing.JButton();
        btnInsertLED = new javax.swing.JButton();
        jLabel17 = new javax.swing.JLabel();
        dlgInsertIC = new javax.swing.JDialog();
        jLabel8 = new javax.swing.JLabel();
        cmbIC = new javax.swing.JComboBox<>();
        btnCancelIC = new javax.swing.JButton();
        btnInsertIC = new javax.swing.JButton();
        jLabel15 = new javax.swing.JLabel();
        lblICDescription = new javax.swing.JLabel();
        dlgAbout = new javax.swing.JDialog();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        btnAboutOK = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        lblPiracyWarning = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        dlgSetDimensions = new javax.swing.JDialog();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        txtCircuitWidth = new javax.swing.JTextField();
        txtCircuitHeight = new javax.swing.JTextField();
        btnDimensionCancel = new javax.swing.JButton();
        btnDimensionOK = new javax.swing.JButton();
        dlgInsertSevenSegmentLED = new javax.swing.JDialog();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        cmb7LEDColor = new javax.swing.JComboBox<>();
        cmb7LEDConfig = new javax.swing.JComboBox<>();
        btnCancel7LED = new javax.swing.JButton();
        btnInsert7LED = new javax.swing.JButton();
        jLabel14 = new javax.swing.JLabel();
        dlgExport = new javax.swing.JDialog();
        jFileChooser3 = new javax.swing.JFileChooser();

        dlgFileOpen.setTitle("Open Circuit");
        dlgFileOpen.setAlwaysOnTop(true);
        dlgFileOpen.setModal(true);

        jFileChooser1.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jFileChooser1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dlgFileOpenLayout = new javax.swing.GroupLayout(dlgFileOpen.getContentPane());
        dlgFileOpen.getContentPane().setLayout(dlgFileOpenLayout);
        dlgFileOpenLayout.setHorizontalGroup(
            dlgFileOpenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgFileOpenLayout.createSequentialGroup()
                .addComponent(jFileChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        dlgFileOpenLayout.setVerticalGroup(
            dlgFileOpenLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgFileOpenLayout.createSequentialGroup()
                .addComponent(jFileChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        dlgFileSave.setTitle("Save Circuit");
        dlgFileSave.setAlwaysOnTop(true);
        dlgFileSave.setModal(true);

        jFileChooser2.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        jFileChooser2.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jFileChooser2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dlgFileSaveLayout = new javax.swing.GroupLayout(dlgFileSave.getContentPane());
        dlgFileSave.getContentPane().setLayout(dlgFileSaveLayout);
        dlgFileSaveLayout.setHorizontalGroup(
            dlgFileSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 582, Short.MAX_VALUE)
            .addGroup(dlgFileSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(dlgFileSaveLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jFileChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        dlgFileSaveLayout.setVerticalGroup(
            dlgFileSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 397, Short.MAX_VALUE)
            .addGroup(dlgFileSaveLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(dlgFileSaveLayout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jFileChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );

        dlgSetWireColor.setTitle("Set Wire Colour");
        dlgSetWireColor.setAlwaysOnTop(true);
        dlgSetWireColor.setModal(true);

        jLabel4.setText("Wire Colour:");

        cmbWireColor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnWireColorOK.setText("OK");
        btnWireColorOK.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnWireColorOKActionPerformed(evt);
            }
        });

        btnWireColorCancel.setText("Cancel");
        btnWireColorCancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnWireColorCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dlgSetWireColorLayout = new javax.swing.GroupLayout(dlgSetWireColor.getContentPane());
        dlgSetWireColor.getContentPane().setLayout(dlgSetWireColorLayout);
        dlgSetWireColorLayout.setHorizontalGroup(
            dlgSetWireColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgSetWireColorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgSetWireColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dlgSetWireColorLayout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cmbWireColor, javax.swing.GroupLayout.PREFERRED_SIZE, 201, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dlgSetWireColorLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(btnWireColorOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnWireColorCancel)))
                .addContainerGap())
        );
        dlgSetWireColorLayout.setVerticalGroup(
            dlgSetWireColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgSetWireColorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgSetWireColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(cmbWireColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(dlgSetWireColorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnWireColorCancel)
                    .addComponent(btnWireColorOK))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        dlgInsertResistor.setTitle("Insert Resistor");
        dlgInsertResistor.setAlwaysOnTop(true);
        dlgInsertResistor.setModal(true);

        jLabel6.setText("Resistance (in Ohms):");

        txtResistance.setText("1000");

        btnResistorCancel.setText("Cancel");
        btnResistorCancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnResistorCancelActionPerformed(evt);
            }
        });

        btnResistorInsert.setText("Insert");
        btnResistorInsert.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnResistorInsertActionPerformed(evt);
            }
        });

        jLabel18.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel18.setText("Click on the holes you want to connect, to insert the resistor");

        javax.swing.GroupLayout dlgInsertResistorLayout = new javax.swing.GroupLayout(dlgInsertResistor.getContentPane());
        dlgInsertResistor.getContentPane().setLayout(dlgInsertResistorLayout);
        dlgInsertResistorLayout.setHorizontalGroup(
            dlgInsertResistorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgInsertResistorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgInsertResistorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, dlgInsertResistorLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(dlgInsertResistorLayout.createSequentialGroup()
                        .addComponent(jLabel18)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addGroup(dlgInsertResistorLayout.createSequentialGroup()
                .addGap(147, 147, 147)
                .addGroup(dlgInsertResistorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(txtResistance)
                    .addGroup(dlgInsertResistorLayout.createSequentialGroup()
                        .addComponent(btnResistorInsert)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnResistorCancel)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        dlgInsertResistorLayout.setVerticalGroup(
            dlgInsertResistorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgInsertResistorLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgInsertResistorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtResistance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(dlgInsertResistorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnResistorInsert)
                    .addComponent(btnResistorCancel))
                .addGap(15, 15, 15)
                .addComponent(jLabel18)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        dlgInsertLED.setTitle("Insert LED");
        dlgInsertLED.setAlwaysOnTop(true);
        dlgInsertLED.setModal(true);

        jLabel7.setText("Colour:");

        cmbLEDColor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnLEDCancel.setText("Cancel");
        btnLEDCancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnLEDCancelActionPerformed(evt);
            }
        });

        btnInsertLED.setText("Insert");
        btnInsertLED.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnInsertLEDActionPerformed(evt);
            }
        });

        jLabel17.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel17.setText("Click on the anode hole to insert");

        javax.swing.GroupLayout dlgInsertLEDLayout = new javax.swing.GroupLayout(dlgInsertLED.getContentPane());
        dlgInsertLED.getContentPane().setLayout(dlgInsertLEDLayout);
        dlgInsertLEDLayout.setHorizontalGroup(
            dlgInsertLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgInsertLEDLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgInsertLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dlgInsertLEDLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cmbLEDColor, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(dlgInsertLEDLayout.createSequentialGroup()
                        .addComponent(jLabel17)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnInsertLED)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnLEDCancel)))
                .addContainerGap())
        );
        dlgInsertLEDLayout.setVerticalGroup(
            dlgInsertLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgInsertLEDLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgInsertLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(cmbLEDColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(dlgInsertLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnLEDCancel)
                    .addComponent(btnInsertLED)
                    .addComponent(jLabel17))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        dlgInsertIC.setTitle("Insert IC");
        dlgInsertIC.setAlwaysOnTop(true);
        dlgInsertIC.setModal(true);
        dlgInsertIC.setResizable(false);

        jLabel8.setText("IC:");

        cmbIC.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbIC.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                cmbICActionPerformed(evt);
            }
        });

        btnCancelIC.setText("Cancel");
        btnCancelIC.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnCancelICActionPerformed(evt);
            }
        });

        btnInsertIC.setText("Insert");
        btnInsertIC.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnInsertICActionPerformed(evt);
            }
        });

        jLabel15.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel15.setText("Click on Pin #1 hole to insert");

        lblICDescription.setFont(new java.awt.Font("Tahoma", 1, 10)); // NOI18N
        lblICDescription.setText("Description");

        javax.swing.GroupLayout dlgInsertICLayout = new javax.swing.GroupLayout(dlgInsertIC.getContentPane());
        dlgInsertIC.getContentPane().setLayout(dlgInsertICLayout);
        dlgInsertICLayout.setHorizontalGroup(
            dlgInsertICLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgInsertICLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgInsertICLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dlgInsertICLayout.createSequentialGroup()
                        .addGroup(dlgInsertICLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(dlgInsertICLayout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addGap(0, 91, Short.MAX_VALUE))
                            .addComponent(lblICDescription, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnInsertIC)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancelIC))
                    .addGroup(dlgInsertICLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cmbIC, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        dlgInsertICLayout.setVerticalGroup(
            dlgInsertICLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgInsertICLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgInsertICLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(cmbIC, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(dlgInsertICLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dlgInsertICLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnCancelIC)
                        .addComponent(btnInsertIC))
                    .addGroup(dlgInsertICLayout.createSequentialGroup()
                        .addComponent(lblICDescription)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel15)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        dlgAbout.setTitle("About");
        dlgAbout.setAlwaysOnTop(true);
        dlgAbout.setModal(true);

        jLabel1.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel1.setText("Developed by:");

        jLabel2.setText("----");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("(c) Copyright Akash Nag, All Rights Reserved.");

        btnAboutOK.setText("OK");
        btnAboutOK.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnAboutOKActionPerformed(evt);
            }
        });

        jLabel5.setText("Version 1.0");

        jLabel9.setFont(new java.awt.Font("Tahoma", 1, 18)); // NOI18N
        jLabel9.setText("Breadboard Circuit Designer");

        lblPiracyWarning.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPiracyWarning.setText("Warning:");

        jLabel16.setText("For any queries/support, drop a mail at: akash.nag.cs@gmail.com");

        javax.swing.GroupLayout dlgAboutLayout = new javax.swing.GroupLayout(dlgAbout.getContentPane());
        dlgAbout.getContentPane().setLayout(dlgAboutLayout);
        dlgAboutLayout.setHorizontalGroup(
            dlgAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgAboutLayout.createSequentialGroup()
                .addGroup(dlgAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dlgAboutLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(lblPiracyWarning))
                    .addGroup(dlgAboutLayout.createSequentialGroup()
                        .addGap(81, 81, 81)
                        .addGroup(dlgAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(dlgAboutLayout.createSequentialGroup()
                                .addGap(27, 27, 27)
                                .addComponent(jLabel9))
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 311, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel16)))
                    .addGroup(dlgAboutLayout.createSequentialGroup()
                        .addGap(168, 168, 168)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel2))
                    .addGroup(dlgAboutLayout.createSequentialGroup()
                        .addGap(209, 209, 209)
                        .addComponent(jLabel5)))
                .addContainerGap(83, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, dlgAboutLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(btnAboutOK, javax.swing.GroupLayout.PREFERRED_SIZE, 66, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(205, 205, 205))
        );
        dlgAboutLayout.setVerticalGroup(
            dlgAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgAboutLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addGap(18, 18, 18)
                .addGroup(dlgAboutLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addGap(5, 5, 5)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel3)
                .addGap(13, 13, 13)
                .addComponent(lblPiracyWarning)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 63, Short.MAX_VALUE)
                .addComponent(btnAboutOK)
                .addContainerGap())
        );

        dlgSetDimensions.setTitle("Set Dimensions");
        dlgSetDimensions.setAlwaysOnTop(true);
        dlgSetDimensions.setModal(true);

        jLabel10.setText("Circuit Width:");

        jLabel11.setText("Circuit Height:");

        btnDimensionCancel.setText("Cancel");
        btnDimensionCancel.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnDimensionCancelActionPerformed(evt);
            }
        });

        btnDimensionOK.setText("OK");
        btnDimensionOK.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnDimensionOKActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dlgSetDimensionsLayout = new javax.swing.GroupLayout(dlgSetDimensions.getContentPane());
        dlgSetDimensions.getContentPane().setLayout(dlgSetDimensionsLayout);
        dlgSetDimensionsLayout.setHorizontalGroup(
            dlgSetDimensionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgSetDimensionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgSetDimensionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(dlgSetDimensionsLayout.createSequentialGroup()
                        .addComponent(btnDimensionOK)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDimensionCancel))
                    .addGroup(dlgSetDimensionsLayout.createSequentialGroup()
                        .addGroup(dlgSetDimensionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel11)
                            .addComponent(jLabel10))
                        .addGap(18, 18, 18)
                        .addGroup(dlgSetDimensionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtCircuitWidth)
                            .addComponent(txtCircuitHeight, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        dlgSetDimensionsLayout.setVerticalGroup(
            dlgSetDimensionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgSetDimensionsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgSetDimensionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(txtCircuitWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(dlgSetDimensionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(txtCircuitHeight, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(dlgSetDimensionsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnDimensionCancel)
                    .addComponent(btnDimensionOK))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        dlgInsertSevenSegmentLED.setTitle("Insert 7-Segment LED");
        dlgInsertSevenSegmentLED.setAlwaysOnTop(true);
        dlgInsertSevenSegmentLED.setModal(true);

        jLabel12.setText("LED Color:");

        jLabel13.setText("Configuration:");

        cmb7LEDColor.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        cmb7LEDConfig.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnCancel7LED.setText("Cancel");
        btnCancel7LED.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnCancel7LEDActionPerformed(evt);
            }
        });

        btnInsert7LED.setText("Insert");
        btnInsert7LED.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                btnInsert7LEDActionPerformed(evt);
            }
        });

        jLabel14.setFont(new java.awt.Font("Tahoma", 0, 10)); // NOI18N
        jLabel14.setText("Click on the hole for Pin#10");

        javax.swing.GroupLayout dlgInsertSevenSegmentLEDLayout = new javax.swing.GroupLayout(dlgInsertSevenSegmentLED.getContentPane());
        dlgInsertSevenSegmentLED.getContentPane().setLayout(dlgInsertSevenSegmentLEDLayout);
        dlgInsertSevenSegmentLEDLayout.setHorizontalGroup(
            dlgInsertSevenSegmentLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgInsertSevenSegmentLEDLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgInsertSevenSegmentLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(dlgInsertSevenSegmentLEDLayout.createSequentialGroup()
                        .addComponent(jLabel14)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 41, Short.MAX_VALUE)
                        .addComponent(btnInsert7LED)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCancel7LED))
                    .addGroup(dlgInsertSevenSegmentLEDLayout.createSequentialGroup()
                        .addGroup(dlgInsertSevenSegmentLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel13)
                            .addComponent(jLabel12))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(dlgInsertSevenSegmentLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cmb7LEDColor, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cmb7LEDConfig, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap())
        );
        dlgInsertSevenSegmentLEDLayout.setVerticalGroup(
            dlgInsertSevenSegmentLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgInsertSevenSegmentLEDLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(dlgInsertSevenSegmentLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(cmb7LEDColor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(dlgInsertSevenSegmentLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(cmb7LEDConfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(dlgInsertSevenSegmentLEDLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCancel7LED)
                    .addComponent(btnInsert7LED)
                    .addComponent(jLabel14))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        dlgExport.setTitle("Export As Image");
        dlgExport.setAlwaysOnTop(true);
        dlgExport.setModal(true);

        jFileChooser3.setDialogType(javax.swing.JFileChooser.SAVE_DIALOG);
        jFileChooser3.setApproveButtonText("Export");
        jFileChooser3.setDialogTitle("");
        jFileChooser3.addActionListener(new java.awt.event.ActionListener()
        {
            public void actionPerformed(java.awt.event.ActionEvent evt)
            {
                jFileChooser3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout dlgExportLayout = new javax.swing.GroupLayout(dlgExport.getContentPane());
        dlgExport.getContentPane().setLayout(dlgExportLayout);
        dlgExportLayout.setHorizontalGroup(
            dlgExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgExportLayout.createSequentialGroup()
                .addComponent(jFileChooser3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        dlgExportLayout.setVerticalGroup(
            dlgExportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(dlgExportLayout.createSequentialGroup()
                .addComponent(jFileChooser3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Breadboard Circuit Designer 1.0");
        setBackground(new java.awt.Color(153, 153, 153));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1024, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 768, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void openCircuitFile(String filePath)
    {
        try {
            String cfp = filePath;
            Object obj[]=FileIO.openCircuit(cfp);
            if(obj==null) return;
            mnuFileNew_Click();

            circuitFilePath=cfp;
            isCircuitActive=true;
            hasBeenSaved=true;
            hasFileBeenAllotted=true;

            ArrayList<CircuitComponent> components = (ArrayList<CircuitComponent>)obj[0];
            ArrayList<Wire> wires = (ArrayList<Wire>)obj[1];
            ((DrawingPane)drawingPane).setData(components,wires);
            ((DrawingPane)drawingPane).updateBreadboardHoleStatusVisibility();

            setTitle();
        } catch(Exception e) {
            System.out.println(e);
            e.printStackTrace();
            Utility.alert("An error occurred while opening the circuit file.");
        }
    }
    
    // ----------------------------------------------------
    // Inner Dialog-box button actions
    // ----------------------------------------------------
    
    private void jFileChooser1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooser1ActionPerformed
        dlgFileOpen.setVisible(false);
        
        String s = evt.getActionCommand().trim().toLowerCase();      // open
        if(s.equals("cancelselection")) return;
        if(!s.equals("approveselection")) return;
        
        try {
            java.io.File selFile = jFileChooser1.getSelectedFile();
            if(!selFile.exists() || selFile.isDirectory())
            {
                Utility.alert("The file does not exist.");
                return;
            }
            
            String cfp = selFile.getAbsolutePath();
            if(!cfp.toLowerCase().endsWith(".bcf"))
            {
                cfp += ".bcf";
            }
            
            openCircuitFile(cfp);            
        } catch(Exception e) {
            System.out.println(e);
            e.printStackTrace();
            Utility.alert("An error occurred while opening the circuit file.");
        }
    }//GEN-LAST:event_jFileChooser1ActionPerformed

    private void jFileChooser2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooser2ActionPerformed
        dlgFileSave.setVisible(false);
        
        String s = evt.getActionCommand().trim().toLowerCase();      // save
        if(s.equals("cancelselection")) return;
        if(!s.equals("approveselection")) return;
        
        circuitFilePath = jFileChooser2.getSelectedFile().getAbsolutePath();
        if(!circuitFilePath.toLowerCase().endsWith(".bcf"))
        {
            circuitFilePath += ".bcf";
        }
        
        try {
            FileIO.saveData((DrawingPane)drawingPane,circuitFilePath);
            hasBeenSaved=true;
            hasFileBeenAllotted=true;
            circuitFilePath = jFileChooser2.getSelectedFile().getAbsolutePath();
            setTitle();
        } catch(java.io.IOException e) {
            Utility.alert("Error while saving file.");
        }
    }//GEN-LAST:event_jFileChooser2ActionPerformed

    private void btnWireColorCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnWireColorCancelActionPerformed
        dlgSetWireColor.setVisible(false);
    }//GEN-LAST:event_btnWireColorCancelActionPerformed

    private void btnWireColorOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnWireColorOKActionPerformed
        String x[] = { "BLACK", "BLUE", "RED", "GREEN", "YELLOW", "CYAN", "MAGENTA", "PINK" };
        Color y[] = { Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA, Color.PINK };
        
        String s=cmbWireColor.getSelectedItem().toString();
        for(int i=0; i<x.length; i++)
        {
            if(s.equalsIgnoreCase(x[i])) { currentWireColor=y[i]; break; }
        }
        dlgSetWireColor.setVisible(false);
    }//GEN-LAST:event_btnWireColorOKActionPerformed

    private void btnResistorCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResistorCancelActionPerformed
        dlgInsertResistor.setVisible(false);
        resistorInsertMode=false;
        setTitle();
    }//GEN-LAST:event_btnResistorCancelActionPerformed

    private void btnResistorInsertActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResistorInsertActionPerformed
        resistorInsertMode=true;
        resistorInsertSourceNext=true;
        currentResistance=Integer.parseInt(txtResistance.getText());
        dlgInsertResistor.setVisible(false);
        setTitle();
    }//GEN-LAST:event_btnResistorInsertActionPerformed

    private void btnLEDCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLEDCancelActionPerformed
        dlgInsertLED.setVisible(false);
        insertLEDMode=false;
        setTitle();
    }//GEN-LAST:event_btnLEDCancelActionPerformed

    private void btnInsertLEDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInsertLEDActionPerformed
        insertLEDMode=true;
        String x[] = { "BLUE", "RED", "GREEN", "YELLOW" };
        Color y[] = { Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW };
        
        String s=cmbLEDColor.getSelectedItem().toString();
        for(int i=0; i<x.length; i++)
        {
            if(s.equalsIgnoreCase(x[i])) { currentLEDColor=y[i]; break; }
        }
        dlgInsertLED.setVisible(false);
        setTitle();
    }//GEN-LAST:event_btnInsertLEDActionPerformed

    private void btnCancelICActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelICActionPerformed
        dlgInsertIC.setVisible(false);
        insertICMode=false;
        setTitle();
    }//GEN-LAST:event_btnCancelICActionPerformed

    private void btnInsertICActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInsertICActionPerformed
        currentICType = cmbIC.getSelectedIndex();
        insertICMode = true;
        dlgInsertIC.setVisible(false);
        setTitle();
    }//GEN-LAST:event_btnInsertICActionPerformed

    private void btnAboutOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAboutOKActionPerformed
        dlgAbout.setVisible(false);
    }//GEN-LAST:event_btnAboutOKActionPerformed

    private void btnDimensionCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDimensionCancelActionPerformed
        dlgSetDimensions.setVisible(false);
    }//GEN-LAST:event_btnDimensionCancelActionPerformed

    private void btnDimensionOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDimensionOKActionPerformed
        circuitWidth=Integer.parseInt(txtCircuitWidth.getText());
        circuitHeight=Integer.parseInt(txtCircuitHeight.getText());
        dlgSetDimensions.setVisible(false);
        
        if(isCircuitActive)
        {
            drawingPane.setBounds(0,0,circuitWidth,circuitHeight);
            drawingPane.setPreferredSize(new Dimension(circuitWidth,circuitHeight));
            drawingPane.revalidate();
            drawingPane.repaint();
            refreshCircuit();
        }
    }//GEN-LAST:event_btnDimensionOKActionPerformed

    private void btnCancel7LEDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancel7LEDActionPerformed
        dlgInsertSevenSegmentLED.setVisible(false);
        insertSevenSegmentLEDMode=false;
        setTitle();
    }//GEN-LAST:event_btnCancel7LEDActionPerformed

    private void btnInsert7LEDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnInsert7LEDActionPerformed
        dlgInsertSevenSegmentLED.setVisible(false);
        
        Color c[] = { Color.RED, Color.GREEN, Color.BLUE };
        current7LEDColor = c[cmb7LEDColor.getSelectedIndex()];
        
        current7LEDConfigIsCommonCathode = (cmb7LEDConfig.getSelectedIndex()==0);
        
        insertSevenSegmentLEDMode=true;
        setTitle();
    }//GEN-LAST:event_btnInsert7LEDActionPerformed

    private void cmbICActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbICActionPerformed
        if(cmbIC.getSelectedItem()==null) return;
        lblICDescription.setText(IC.getICDescription(cmbIC.getSelectedItem().toString()));
    }//GEN-LAST:event_cmbICActionPerformed

    private void jFileChooser3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jFileChooser3ActionPerformed
        dlgExport.setVisible(false);
        
        String s = evt.getActionCommand().trim().toLowerCase();      // export as image
        if(s.equals("cancelselection")) return;
        if(!s.equals("approveselection")) return;
        
        String imageFilePath = jFileChooser3.getSelectedFile().getAbsolutePath();
        try {
            BufferedImage bImg = new BufferedImage(drawingPane.getWidth(), drawingPane.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D cg = bImg.createGraphics();
            drawingPane.paintAll(cg);
            
            String x = jFileChooser3.getFileFilter().getDescription();
            x = x.substring(0, x.indexOf(' ')).trim().toLowerCase();
            
            String y = "";
            if(x.equals("graphics"))
                y="gif";
            else
                y="png";
            
            if(!imageFilePath.toLowerCase().endsWith("."+y))
            {
                imageFilePath += "."+y;
            }
            
            if(!ImageIO.write(bImg, y, new File(imageFilePath)))
            {
                Utility.alert("Error while exporting image.");
            }
        } catch(Exception e) {
            Utility.alert("Error while exporting image.");
        }
    }//GEN-LAST:event_jFileChooser3ActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAboutOK;
    private javax.swing.JButton btnCancel7LED;
    private javax.swing.JButton btnCancelIC;
    private javax.swing.JButton btnDimensionCancel;
    private javax.swing.JButton btnDimensionOK;
    private javax.swing.JButton btnInsert7LED;
    private javax.swing.JButton btnInsertIC;
    private javax.swing.JButton btnInsertLED;
    private javax.swing.JButton btnLEDCancel;
    private javax.swing.JButton btnResistorCancel;
    private javax.swing.JButton btnResistorInsert;
    private javax.swing.JButton btnWireColorCancel;
    private javax.swing.JButton btnWireColorOK;
    private javax.swing.JComboBox<String> cmb7LEDColor;
    private javax.swing.JComboBox<String> cmb7LEDConfig;
    private javax.swing.JComboBox<String> cmbIC;
    private javax.swing.JComboBox<String> cmbLEDColor;
    private javax.swing.JComboBox<String> cmbWireColor;
    private javax.swing.JDialog dlgAbout;
    private javax.swing.JDialog dlgExport;
    private javax.swing.JDialog dlgFileOpen;
    private javax.swing.JDialog dlgFileSave;
    private javax.swing.JDialog dlgInsertIC;
    private javax.swing.JDialog dlgInsertLED;
    private javax.swing.JDialog dlgInsertResistor;
    private javax.swing.JDialog dlgInsertSevenSegmentLED;
    private javax.swing.JDialog dlgSetDimensions;
    private javax.swing.JDialog dlgSetWireColor;
    private javax.swing.JFileChooser jFileChooser1;
    private javax.swing.JFileChooser jFileChooser2;
    private javax.swing.JFileChooser jFileChooser3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel lblICDescription;
    private javax.swing.JLabel lblPiracyWarning;
    private javax.swing.JTextField txtCircuitHeight;
    private javax.swing.JTextField txtCircuitWidth;
    private javax.swing.JTextField txtResistance;
    // End of variables declaration//GEN-END:variables
}
