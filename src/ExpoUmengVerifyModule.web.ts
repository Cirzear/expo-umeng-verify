import { registerWebModule, NativeModule } from 'expo';

import { ExpoUmengVerifyModuleEvents } from './ExpoUmengVerify.types';

class ExpoUmengVerifyModule extends NativeModule<ExpoUmengVerifyModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onUMVerifyEvent', { type: 'onUserCancel' as const, data: { value } });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(ExpoUmengVerifyModule, 'ExpoUmengVerifyModule');
