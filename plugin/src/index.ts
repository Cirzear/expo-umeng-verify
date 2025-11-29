import { ConfigPlugin, withAndroidManifest, AndroidConfig } from 'expo/config-plugins';
import fs from 'fs';
import path from 'path';

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

const withUmengPermissions: ConfigPlugin = (config) => {
  return withAndroidManifest(config, async (config) => {
    const permissions = [
      'android.permission.INTERNET',
      'android.permission.ACCESS_WIFI_STATE',
      'android.permission.ACCESS_NETWORK_STATE',
      'android.permission.CHANGE_NETWORK_STATE',
      'android.permission.READ_PHONE_STATE',
    ];

    permissions.forEach((permission) => {
      AndroidConfig.Permissions.addPermission(config.modResults, permission);
    });

    return config;
  });
};

const withUmengVerify: ConfigPlugin = (config) => {
  config = withNetworkSecurityConfig(config);
  config = withUmengPermissions(config);
  return config;
};

export default withUmengVerify;
