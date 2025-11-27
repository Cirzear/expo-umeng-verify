import { requireNativeView } from 'expo';
import * as React from 'react';

import { ExpoUmengVerifyViewProps } from './ExpoUmengVerify.types';

const NativeView: React.ComponentType<ExpoUmengVerifyViewProps> =
  requireNativeView('ExpoUmengVerify');

export default function ExpoUmengVerifyView(props: ExpoUmengVerifyViewProps) {
  return <NativeView {...props} />;
}
