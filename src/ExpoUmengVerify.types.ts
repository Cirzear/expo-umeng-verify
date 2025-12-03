import type { StyleProp, ViewStyle } from 'react-native';

// ============================================================================
// Privacy Policy Configuration
// ============================================================================

export type PrivacyPolicy = {
  name: string;
  url: string;
};

export type PrivacyConfig = {
  /** Custom privacy policy 1 */
  privacyOne?: PrivacyPolicy;
  /** Custom privacy policy 2 */
  privacyTwo?: PrivacyPolicy;
  /** Custom privacy policy 3 */
  privacyThree?: PrivacyPolicy;
  /** Privacy text color (normal and clickable) */
  privacyColor?: {
    normal?: string; // Default: gray
    clickable?: string; // Default: blue
  };
  /** Connector texts between policies, e.g., [",", "", "和"] */
  privacyConnectTexts?: [string, string, string];
  /** Index of operator privacy policy (0-2) */
  privacyOperatorIndex?: number;
  /** Whether privacy checkbox is checked by default */
  privacyState?: boolean;
  /** Vendor privacy prefix, e.g., "《" */
  vendorPrivacyPrefix?: string;
  /** Vendor privacy suffix, e.g., "》" */
  vendorPrivacySuffix?: string;
};

// ============================================================================
// UI Customization Configuration
// ============================================================================

export type UIMode = 
  | 'fullscreen-portrait'  // Full screen vertical
  | 'fullscreen-landscape' // Full screen horizontal
  | 'dialog-portrait'      // Dialog vertical
  | 'dialog-landscape'     // Dialog horizontal
  | 'dialog-bottom';       // Bottom sheet dialog

export type LogoConfig = {
  /** Logo image path (Android: drawable name without extension, iOS: asset name) */
  imagePath?: string;
  /** Logo width in dp */
  width?: number;
  /** Logo height in dp */
  height?: number;
  /** Logo offset from top in dp */
  offsetY?: number;
  /** Whether to hide logo */
  hidden?: boolean;
};

export type PhoneNumberConfig = {
  /** Phone number text size in sp */
  textSize?: number;
  /** Phone number text color */
  textColor?: string;
  /** Phone number offset from top in dp */
  offsetY?: number;
};

export type SloganConfig = {
  /** Slogan text (e.g., "为了您的账号安全，请先绑定手机号") */
  text?: string;
  /** Slogan text size in sp */
  textSize?: number;
  /** Slogan text color */
  textColor?: string;
  /** Slogan offset from top in dp */
  offsetY?: number;
  /** Whether to hide slogan */
  hidden?: boolean;
};

export type LoginButtonConfig = {
  /** Button text */
  text?: string;
  /** Button text size in sp */
  textSize?: number;
  /** Button text color */
  textColor?: string;
  /** Button width in dp */
  width?: number;
  /** Button height in dp */
  height?: number;
  /** Button offset from top in dp */
  offsetY?: number;
  /** Button left and right margin in dp */
  marginLeftAndRight?: number;
  /** Button background drawable path (Android only) */
  backgroundPath?: string;
  /** Button background color */
  backgroundColor?: string;
  /** Button corner radius in dp */
  cornerRadius?: number;
};

export type SwitchButtonConfig = {
  /** Switch button text */
  text?: string;
  /** Switch button text size in sp */
  textSize?: number;
  /** Switch button text color */
  textColor?: string;
  /** Whether to hide switch button */
  hidden?: boolean;
  /** Switch button offset from top in dp */
  offsetY?: number;
};

export type CheckboxConfig = {
  /** Whether to hide checkbox */
  hidden?: boolean;
  /** Checkbox default state (checked/unchecked) */
  defaultState?: boolean;
  /** Checked image path */
  checkedImagePath?: string;
  /** Unchecked image path */
  uncheckedImagePath?: string;
  /** Checkbox size in dp */
  size?: number;
};

export type NavigationBarConfig = {
  /** Navigation bar title */
  title?: string;
  /** Navigation bar title color */
  titleColor?: string;
  /** Navigation bar background color */
  backgroundColor?: string;
  /** Whether to hide navigation bar */
  hidden?: boolean;
  /** Whether to hide return button */
  returnButtonHidden?: boolean;
  /** Return button image path */
  returnButtonImagePath?: string;
};

export type StatusBarConfig = {
  /** Status bar color */
  color?: string;
  /** Status bar UI flag (Android only) */
  uiFlag?: number;
  /** Whether status bar is light color (affects icon color) */
  lightColor?: boolean;
};

export type DialogConfig = {
  /** Dialog width in dp (for dialog mode) */
  width?: number;
  /** Dialog height in dp (for dialog mode) */
  height?: number;
  /** Whether dialog is bottom aligned */
  bottom?: boolean;
  /** Whether tapping mask closes dialog */
  tapMaskToClose?: boolean;
};

export type PageConfig = {
  /** Page background color */
  backgroundColor?: string;
  /** Page background image path */
  backgroundImagePath?: string;
  /** Page enter animation (Android: anim resource name) */
  enterAnimation?: string;
  /** Page exit animation (Android: anim resource name) */
  exitAnimation?: string;
  /** Screen orientation */
  orientation?: 'portrait' | 'landscape' | 'sensor-portrait' | 'sensor-landscape';
};

export type UIConfig = {
  /** UI mode */
  mode?: UIMode;
  /** Privacy policy configuration */
  privacy?: PrivacyConfig;
  /** Logo configuration */
  logo?: LogoConfig;
  /** Phone number display configuration */
  phoneNumber?: PhoneNumberConfig;
  /** Slogan configuration */
  slogan?: SloganConfig;
  /** Login button configuration */
  loginButton?: LoginButtonConfig;
  /** Switch account button configuration */
  switchButton?: SwitchButtonConfig;
  /** Privacy checkbox configuration */
  checkbox?: CheckboxConfig;
  /** Navigation bar configuration */
  navigationBar?: NavigationBarConfig;
  /** Status bar configuration */
  statusBar?: StatusBarConfig;
  /** Dialog configuration (for dialog modes) */
  dialog?: DialogConfig;
  /** Page configuration */
  page?: PageConfig;
  /** Whether to hide default toast when checkbox is unchecked */
  hideLoginToast?: boolean;
};

// ============================================================================
// Event Callbacks
// ============================================================================

export type UMVerifyEventType = 
  | 'onUserCancel'          // User clicked back/cancel button
  | 'onSwitchAccount'       // User clicked switch to other login method
  | 'onLoginButtonClick'    // User clicked login button
  | 'onCheckboxChange'      // Privacy checkbox state changed
  | 'onProtocolClick';      // User clicked privacy policy link

export type UMVerifyEvent = {
  type: UMVerifyEventType;
  data?: any;
};

export type UserCancelEvent = {
  type: 'onUserCancel';
};

export type SwitchAccountEvent = {
  type: 'onSwitchAccount';
};

export type LoginButtonClickEvent = {
  type: 'onLoginButtonClick';
  data: {
    isChecked: boolean; // Whether privacy checkbox is checked
  };
};

export type CheckboxChangeEvent = {
  type: 'onCheckboxChange';
  data: {
    isChecked: boolean;
  };
};

export type ProtocolClickEvent = {
  type: 'onProtocolClick';
  data: {
    name: string; // Privacy policy name
    url: string;  // Privacy policy URL
  };
};

export type UMVerifyEventHandler = (event: UMVerifyEvent) => void;

// ============================================================================
// Token Response
// ============================================================================

export type TokenResult = {
  token: string;
  code?: string;
  msg?: string;
  operator?: string; // Operator type: CMCC, CUCC, CTCC
};

// ============================================================================
// Module Configuration
// ============================================================================

export type InitConfig = {
  appKey: string;
  schemeSecret: string;
  channel: string;
};

export type GetLoginTokenConfig = {
  /** UI configuration */
  ui?: UIConfig;
  /** Timeout in milliseconds (default: 5000) */
  timeout?: number;
};

// ============================================================================
// Module Events (for EventEmitter)
// ============================================================================

export type ExpoUmengVerifyModuleEvents = {
  onUMVerifyEvent: (event: UMVerifyEvent) => void;
};

export type ChangeEventPayload = {
  value: string;
};

// ============================================================================
// View Props (for custom view component, if needed)
// ============================================================================

export type OnLoadEventPayload = {
  url: string;
};

export type ExpoUmengVerifyViewProps = {
  url: string;
  onLoad: (event: { nativeEvent: OnLoadEventPayload }) => void;
  style?: StyleProp<ViewStyle>;
};
