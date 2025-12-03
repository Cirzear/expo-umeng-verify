import {
  ConfigPlugin,
  createRunOncePlugin,
  AndroidConfig,
  withAndroidManifest,
} from 'expo/config-plugins';
import fs from 'fs';
import path from 'path';

// Plugin configuration type
export type ExpoUmengVerifyPluginConfig = {
  androidAppKey?: string;
  iosAppKey?: string;
  androidSchemeSecret?: string;
  iosSchemeSecret?: string;
  channel?: string;
};

/**
 * Apply network security configuration for Android
 * Creates network_security_config.xml to allow cleartext traffic
 */
const withNetworkSecurityConfig: ConfigPlugin = (config) => {
  return withAndroidManifest(config, async (config) => {
    const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(config.modResults);
    mainApplication.$['android:networkSecurityConfig'] = '@xml/network_security_config';
    
    // Create the xml file
    const resDir = await AndroidConfig.Paths.getResourceFolderAsync(config.modRequest.projectRoot);
    const xmlDir = path.join(resDir, 'xml');
    if (!fs.existsSync(xmlDir)) {
      fs.mkdirSync(xmlDir, { recursive: true });
    }
    
    const securityConfigContent = `<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>`;

    fs.writeFileSync(path.join(xmlDir, 'network_security_config.xml'), securityConfigContent);

    return config;
  });
};

/**
 * Add required Umeng permissions to AndroidManifest.xml
 */
const withUmengPermissions: ConfigPlugin = (config) => {
  return withAndroidManifest(config, async (config) => {
    const permissions = [
      'android.permission.INTERNET',
      'android.permission.ACCESS_WIFI_STATE',
      'android.permission.ACCESS_NETWORK_STATE',
      'android.permission.CHANGE_NETWORK_STATE',
    ];

    permissions.forEach((permission) => {
      AndroidConfig.Permissions.addPermission(config.modResults, permission);
    });

    return config;
  });
};

/**
 * Add Umeng configuration metadata to AndroidManifest.xml
 */
const withUmengMetadata: ConfigPlugin<ExpoUmengVerifyPluginConfig> = (config, props) => {
  return withAndroidManifest(config, async (config) => {
    const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(config.modResults);

    // Add meta-data for Umeng keys if provided
    if (props.androidAppKey) {
      const metaData = mainApplication['meta-data'] || [];
      
      // Check if meta-data already exists
      const existingKeyIndex = metaData.findIndex(
        (item: any) => item.$['android:name'] === 'UMENG_APPKEY'
      );

      const appKeyData = {
        $: {
          'android:name': 'UMENG_APPKEY',
          'android:value': props.androidAppKey,
        },
      };

      if (existingKeyIndex >= 0) {
        metaData[existingKeyIndex] = appKeyData;
      } else {
        metaData.push(appKeyData);
      }

      mainApplication['meta-data'] = metaData;
    }

    if (props.channel) {
      const metaData = mainApplication['meta-data'] || [];
      
      const existingChannelIndex = metaData.findIndex(
        (item: any) => item.$['android:name'] === 'UMENG_CHANNEL'
      );

      const channelData = {
        $: {
          'android:name': 'UMENG_CHANNEL',
          'android:value': props.channel,
        },
      };

      if (existingChannelIndex >= 0) {
        metaData[existingChannelIndex] = channelData;
      } else {
        metaData.push(channelData);
      }

      mainApplication['meta-data'] = metaData;
    }

    return config;
  });
};

/**
 * Main config plugin
 * Applies platform-specific configurations
 */
const withUmengVerify: ConfigPlugin<ExpoUmengVerifyPluginConfig | void> = (config, props = {}) => {
  // Ensure props is an object
  const pluginProps = props || {};
  
  // Apply Android configuration
  config = withNetworkSecurityConfig(config);
  config = withUmengPermissions(config);
  config = withUmengMetadata(config, pluginProps);

  // Store config in extra for runtime access
  if (!config.extra) {
    config.extra = {};
  }

  config.extra.umengVerify = {
    androidAppKey: pluginProps.androidAppKey,
    iosAppKey: pluginProps.iosAppKey,
    androidSchemeSecret: pluginProps.androidSchemeSecret,
    iosSchemeSecret: pluginProps.iosSchemeSecret,
    channel: pluginProps.channel || 'default',
  };

  return config;
};

export default createRunOncePlugin(
  withUmengVerify,
  'expo-umeng-verify',
  '0.1.7'
);

