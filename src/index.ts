import ExpoUmengVerifyModule from './ExpoUmengVerifyModule';

export function init(appKey: string, channel: string): Promise<boolean> {
  return ExpoUmengVerifyModule.init(appKey, channel);
}

export function checkEnvAvailable(): Promise<boolean> {
  return ExpoUmengVerifyModule.checkEnvAvailable();
}

export function getLoginToken(): Promise<any> {
  return ExpoUmengVerifyModule.getLoginToken();
}

export function accelerateLoginPage(): Promise<any> {
  return ExpoUmengVerifyModule.accelerateLoginPage();
}

export function quitLoginPage(): Promise<void> {
  return ExpoUmengVerifyModule.quitLoginPage();
}

export { default as ExpoUmengVerifyView } from './ExpoUmengVerifyView';
export * from  './ExpoUmengVerify.types';
