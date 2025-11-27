import * as React from 'react';

import { ExpoUmengVerifyViewProps } from './ExpoUmengVerify.types';

export default function ExpoUmengVerifyView(props: ExpoUmengVerifyViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
