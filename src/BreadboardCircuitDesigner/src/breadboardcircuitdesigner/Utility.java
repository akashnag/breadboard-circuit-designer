// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

import java.util.ArrayList;
import javax.swing.JOptionPane;

class Utility 
{
    public static void alert(String msg)
    {
        JOptionPane.showMessageDialog(null,msg);
    }
    
    public static String inputBox(String msg, String initVal)
    {
        return JOptionPane.showInputDialog(null, msg, initVal);
    }
    
    public static boolean confirm(String msg, String title)
    {
        int c = JOptionPane.showConfirmDialog(null, msg, title, JOptionPane.YES_NO_OPTION);
        return(c==JOptionPane.YES_OPTION);
    }
    
    public static int confirmWithCancel(String msg, String title)
    {
        int c = JOptionPane.showConfirmDialog(null, msg, title, JOptionPane.YES_NO_CANCEL_OPTION);
        return c;
    }
    
    public static void printArray(String title, int list[])
    {
        System.out.print("\n"+title+"["+list.length+"]="+list[0]);
        for(int i=1; i<list.length; i++) System.out.print(","+list[i]);
        System.out.println();
    }
}
