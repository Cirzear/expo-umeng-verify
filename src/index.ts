import { EventSubscription } from 'expo-modules-core';
import ExpoUmengVerifyModule from './ExpoUmengVerifyModule';
import type { 
  GetLoginTokenConfig, 
  TokenResult, 
  UMVerifyEventHandler 
} from './ExpoUmengVerify.types';

/**
 * Initialize Umeng Verify SDK
 * @param appKey - Umeng App Key
 * @param schemeSecret - Umeng Scheme Secret for verification
 * @param channel - Channel identifier (e.g., 'App Store', 'Google Play')
 */
export function init(appKey: string, schemeSecret: string, channel: string): Promise<boolean> {
  return ExpoUmengVerifyModule.init(appKey, schemeSecret, channel);
}

/**
 * Check if current environment supports one-click login
 * Returns true if sim card and network environment support verification
 */
export function checkEnvAvailable(): Promise<boolean> {
  return ExpoUmengVerifyModule.checkEnvAvailable();
}

/**
 * Get login token with optional UI configuration
 * @param config - Optional configuration for UI and timeout
 * @returns Promise with token result
 */
export function getLoginToken(config?: GetLoginTokenConfig): Promise<TokenResult> {
  return ExpoUmengVerifyModule.getLoginToken(config);
}

/**
 * Accelerate login page loading
 * Pre-fetches resources to speed up auth page display
 */
export function accelerateLoginPage(): Promise<any> {
  return ExpoUmengVerifyModule.accelerateLoginPage();
}

/**
 * Quit/close the login authorization page
 */
export function quitLoginPage(): Promise<void> {
  return ExpoUmengVerifyModule.quitLoginPage();
}

/**
 * Add event listener for Umeng Verify events
 * @param eventHandler - Callback function to handle events
 * @returns EventSubscription that can be used to remove the listener
 */
export function addEventListener(eventHandler: UMVerifyEventHandler): EventSubscription {
  return ExpoUmengVerifyModule.addListener('onUMVerifyEvent', eventHandler);
}

/**
 * Remove event listener
 * @param subscription - The subscription returned from addEventListener
 */
export function removeEventListener(subscription: EventSubscription): void {
  subscription.remove();
}

export { default as ExpoUmengVerifyView } from './ExpoUmengVerifyView';
export * from  './ExpoUmengVerify.types';
