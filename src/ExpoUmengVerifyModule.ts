import { NativeModule, requireNativeModule } from 'expo';

import { ExpoUmengVerifyModuleEvents } from './ExpoUmengVerify.types';

declare class ExpoUmengVerifyModule extends NativeModule<ExpoUmengVerifyModuleEvents> {
  init(appKey: string, schemeSecret: string, channel: string): Promise<boolean>;
  checkEnvAvailable(): Promise<boolean>;
  getLoginToken(): Promise<any>;
  accelerateLoginPage(): Promise<any>;
  quitLoginPage(): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ExpoUmengVerifyModule>('ExpoUmengVerify');
