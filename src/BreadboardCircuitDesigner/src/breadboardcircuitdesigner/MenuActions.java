// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

class MenuActions implements ActionListener
{
    private MainWindow window;
    
    public MenuActions(MainWindow w)
    {
        window=w;
    }
    
    @Override
    public void actionPerformed(ActionEvent e)
    {
        String c = e.getActionCommand().trim().toLowerCase();
        //MainWindow.msgBox(c);
        switch (c) {
            // File-Menu
            case "new circuit":
                window.mnuFileNew_Click();
                break;
            case "open...":
                window.mnuFileOpen_Click();
                break;
            case "close":
                window.mnuFileClose_Click();
                break;
            case "save":
                window.mnuFileSave_Click();
                break;
            case "save as...":
                window.mnuFileSaveAs_Click();
                break;
            case "export as image":
                window.mnuFileExport_Click();
                break;
            case "exit":
                window.mnuFileExit_Click();
                break;
            
            // Edit Menu
            case "start delete mode":
            case "stop delete mode":
                window.mnuEditDelete_Click();
                break;
            case "start wire editing mode":
            case "stop wire editing mode":
                window.mnuEditWireMode_Click();
                break;
            case "set wire colour":
                window.mnuEditSetWireColor_Click();
                break;
            case "set dimensions":
                window.mnuEditSetDimensions_Click();
                break;
            case "set to idle mode":
                window.mnuEditSetToIdleMode_Click();
                break;
                
            // Insert Menu
            case "breadboard":
                window.mnuInsertBreadboard_Click();
                break;
            case "power supply":
                window.mnuInsertPowerSupply_Click();
                break;
            case "resistor":
                window.mnuInsertResistor_Click();
                break;
            case "led":
                window.mnuInsertLED_Click();
                break;
            case "ic (dip)":
                window.mnuInsertIC_Click();
                break;
            case "7-segment led":
                window.mnuInsertSevenSegmentLED_Click();
                break;
                
            // View Menu
            case "show breadboard hole status":
            case "hide breadboard hole status":
                window.mnuViewHoleStatus_Click();
                break;
            
            // Help Menu
            case "about...":
                window.mnuHelpAbout_Click();
                break;
            default:
                break;
        }
    }
}
