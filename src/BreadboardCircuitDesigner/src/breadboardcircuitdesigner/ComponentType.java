// ---------------------------------------------------------------------------------------------
//  Copyright (c) Akash Nag. All rights reserved.
//  Licensed under the MIT License. See LICENSE.md in the project root for license information.
// ---------------------------------------------------------------------------------------------

package breadboardcircuitdesigner;

public enum ComponentType 
{
    // Order matters: increasing ordinal means increasing z-Index while painting on screen
    BREADBOARD, POWER_SUPPLY, WIRE, IC, RESISTOR, LED, SEVEN_SEGMENT_LED
}
