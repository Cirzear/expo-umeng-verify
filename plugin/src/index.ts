import { ConfigPlugin, withAndroidManifest, AndroidConfig } from 'expo/config-plugins';
import { Paths } from '@expo/config-plugins/build/android';
import fs from 'fs';
import path from 'path';

const withNetworkSecurityConfig: ConfigPlugin = (config) => {
  return withAndroidManifest(config, async (config) => {
    const mainApplication = AndroidConfig.Manifest.getMainApplicationOrThrow(config.modResults);
    mainApplication.$['android:networkSecurityConfig'] = '@xml/network_security_config';
    
    // Create the xml file
    const resDir = await Paths.getResourceFolderAsync(config.modRequest.projectRoot);
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

const withUmengVerify: ConfigPlugin = (config) => {
  config = withNetworkSecurityConfig(config);
  return config;
};

export default withUmengVerify;
