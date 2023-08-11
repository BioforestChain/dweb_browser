export type $WindowRawState = {
  topBarOverlay: boolean;
  topBarContentColor: $WindowStyleColor;
  topBarBackgroundColor: $WindowStyleColor;
  bottomBarOverlay: boolean;
  bottomBarContentColor: $WindowStyleColor;
  bottomBarBackgroundColor: $WindowStyleColor;
  themeColor: $WindowStyleColor;
};
export type $WindowState = $WindowRawState;
export type $WindowStyleColor = "auto" | `#${string}`;
