/**   
 * Light text for dark backgrounds.
 * "DARK"
 * 
 * Dark text for light backgrounds.
 * "LIGHT",
 * 
 * The style is based on the device appearance.
 * If the device is using Dark mode, the bar text will be light.
 * If the device is using Light mode, the bar text will be dark.
 * On Android the default will be the one the app was launched with.
 * "DEFAULT"
 */
  

export type $BAR_STYLE = 
  "DARK"      // Light text for dark backgrounds.
  | "LIGHT"   // Dark text for light backgrounds.
  | "DEFAULT" // The style is based on the device appearance.

export interface $BarState extends $InsetsState{
  color: string,
  style: $BAR_STYLE,
  visible: boolean
}

export interface $BarStateColorRGB extends $InsetsState{
  color: $ColorRGB,
  style: $BAR_STYLE,
  visible: boolean
}

export interface $SafeAreaState  {
  overlay: boolean;
  insets: $Insets;
  cutoutInsets: $Insets;
  outerInsets: $Insets;
}

export interface $VirtualKeyboardState extends $InsetsState{
  visible: boolean;
}

export interface $Insets{
  top: number,
  right: number,
  bottom: number,
  left: number
}

export interface $InsetsState{
  overlay: boolean;
  insets: $Insets;
}

export interface $ShareOptions{
  title: string;
  text: string;
  link: string;
  src: string;
}

export interface $ColorRGB{
  red: number;
  green: number;
  blue: number;
  alpha: number;
}

export type $ToastPosition = "top" | "bottom"
export type $ImpactLightStyle = "HEAVY" | "MEDIUM" | "LIGHT";
export type $NotificationStyle = "SUCCESS" | "WARNING" | "ERROR";


 


  