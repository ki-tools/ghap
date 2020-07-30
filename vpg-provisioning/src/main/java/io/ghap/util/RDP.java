package io.ghap.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

/**
 * Utils for RDP protocol
 * @author Alexey Anischenko
 *
 */
public class RDP {
  private static Logger LOG = LoggerFactory.getLogger(RDP.class);
  
  private static final Map<String, String> paramNames = new HashMap<>();
  static { //RDP file parameters that are "scoped" to sign, and their special "sign scope" names
    paramNames.put("alternate full address:s", "Alternate Full Address");
    paramNames.put("alternate shell:s", "Alternate Shell");
    paramNames.put("audiomode:i", "AudioMode");
    paramNames.put("authentication level:i", "Authentication Level");
    paramNames.put("autoreconnection enabled:i", "AutoReconnection Enabled");
    paramNames.put("devicestoredirect:s", "DevicesToRedirect");
    paramNames.put("disableconnectionsharing:i", "DisableConnectionSharing");
    paramNames.put("drivestoredirect:s", "DrivesToRedirect");
    paramNames.put("enablecredsspsupport:i", "EnableCredSspSupport");
    paramNames.put("eventloguploadaddress:s", "EventLogUploadAddress");
    paramNames.put("full address:s", "Full Address");
    paramNames.put("gatewaycredentialssource:i", "GatewayCredentialsSource");
    paramNames.put("gatewayhostname:s", "GatewayHostname");
    paramNames.put("gatewayprofileusagemethod:i", "GatewayProfileUsageMethod");
    paramNames.put("gatewayusagemethod:i", "GatewayUsageMethod");
    paramNames.put("kdcproxyname:s", "KDCProxyName");
    paramNames.put("loadbalanceinfo:s", "LoadBalanceInfo");
    paramNames.put("negotiate security layer:i", "Negotiate Security Layer");
    paramNames.put("pcb:s", "PCB");
    paramNames.put("pre-authentication server address:s", "Pre-authentication server address");
    paramNames.put("prompt for credentials:i", "Prompt For Credentials");
    paramNames.put("promptcredentialonce:i", "PromptCredentialOnce");
    paramNames.put("rdgiskdcproxy:i", "RDGIsKDCProxy");
    paramNames.put("redirectclipboard:i", "RedirectClipboard");
    paramNames.put("redirectcomports:i", "RedirectCOMPorts");
    paramNames.put("redirectdirectx:i", "RedirectDirectX");
    paramNames.put("redirectdrives:i", "RedirectDrives");
    paramNames.put("redirectposdevices:i", "RedirectPOSDevices");
    paramNames.put("redirectprinters:i", "RedirectPrinters");
    paramNames.put("redirectsmartcards:i", "RedirectSmartCards");
    paramNames.put("remoteapplicationcmdline:s", "RemoteApplicationCmdLine");
    paramNames.put("remoteapplicationexpandcmdline:s", "RemoteApplicationExpandCmdLine");
    paramNames.put("remoteapplicationexpandworkingdir:s", "RemoteApplicationExpandWorkingdir");
    paramNames.put("remoteapplicationfile:s", "RemoteApplicationFile");
    paramNames.put("remoteapplicationfileextensions:s", "RemoteApplicationFileExtensions");
    paramNames.put("remoteapplicationguid:s", "RemoteApplicationGuid");
    paramNames.put("remoteapplicationicon:s", "RemoteApplicationIcon");
    paramNames.put("remoteapplicationmode:i", "RemoteApplicationMode");
    paramNames.put("remoteapplicationname:s", "RemoteApplicationName");
    paramNames.put("remoteapplicationprogram:s", "RemoteApplicationProgram");
    paramNames.put("require pre-authentication:i", "Require pre-authentication");
    paramNames.put("server port:i", "Server Port");
    paramNames.put("shell working directory:s", "Shell Working Directory");
    paramNames.put("support url:s", "Support URL");
    paramNames.put("use redirection server name:i", "Use Redirection Server Name");
  }
  
  /**
   * Sign the RDP file with the specified cert and key
   * @param rdpFile the string with RDP file content
   * @param cert certificate
   * @param key key
   * @return signed RDP file string
   */
  public static String sign(String rdpFile, X509Certificate cert, PrivateKey key) {

    //the output
    StringBuilder rdpOut = new StringBuilder(20000);
    //parameters that take part in signing
    StringBuilder rdpOutToSign = new StringBuilder(20000);
    //parameter names that take part in signing, in "special" version
    StringBuilder signScopeOut = new StringBuilder(4000);
    boolean isAltAddressDefined = false;
    boolean isAuthLevelDefined = false;
    String fullAddr = "";
    
    for(String line : StringUtils.split(rdpFile, "\r\n")) { //split into lines, no matter Win or Unix line sep is used
      rdpOut.append(line).append("\r\n"); //this way we definitely separate rdpFile lines with Windows CRLF
      String paramName = StringUtils.substringBeforeLast(line, ":"); // get param name
      String signScope = paramNames.get(paramName);
      if (null != signScope) { //skip param name that is out of signing scope
        rdpOutToSign.append(line).append("\r\n"); //adding param to be signed to output
        if (signScopeOut.length() > 0) {
            signScopeOut.append(','); //adding comma before any signscope except the first one
        }
        signScopeOut.append(signScope);
        if ((!isAltAddressDefined) && signScope.equals("Alternate Full Address")) {
          isAltAddressDefined = true;
        } else if (fullAddr.isEmpty() && signScope.equals("Full Address")) {
          fullAddr = StringUtils.substringAfterLast(line, ":");
        } else if ((!isAuthLevelDefined) && signScope.equals("Authentication Level")) {
          isAuthLevelDefined = true;
        }
      }
    }
    if(!isAuthLevelDefined) { //disable warning on server cert e.t.c.
      rdpOut.append("authentication level:i:0\r\n");
      rdpOutToSign.append("authentication level:i:0\r\n");
      signScopeOut.append(",Authentication Level"); 
    }
    if ( (!fullAddr.isEmpty()) && (!isAltAddressDefined)) {//we have full addr and no alt.addr
    //putting full addr into alt. addr to prevent hacking
      rdpOut.append("alternate full address:s:").append(fullAddr).append("\r\n"); 
      rdpOutToSign.append("alternate full address:s:").append(fullAddr).append("\r\n");
      signScopeOut.append(",Alternate Full Address"); 
    }
    if (signScopeOut.length() == 0) {
      LOG.error("Empty signscope for RDP string: %s", rdpFile);
      return null;
    }
    //appending signscope to output
    rdpOut.append("signscope:s:").append(signScopeOut).append("\r\n").append("signature:s:");
    //appending signscope and zero char to the part that will be signed
    rdpOutToSign.append("signscope:s:").append(signScopeOut).append("\r\n").append('\0');
    //now let's start making the signature
    byte[] rdpOutUTF16LE = null;
    try {
      rdpOutUTF16LE = rdpOutToSign.toString().getBytes("UTF-16LE");
    } catch (UnsupportedEncodingException e) {
      LOG.error("RDPsign UTF-16LE conversion exception: %s", e);
      return null;
    }
    byte[] rdpOutUTF16LEsigned = null;
    try {
      Signature sign = Signature.getInstance("SHA1withRSA");
      sign.initSign(key);
      sign.update(rdpOutUTF16LE);
      rdpOutUTF16LEsigned =sign.sign();
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      LOG.error("RDPsign signing exception: %s", e);
      return null;
    }
    byte[] magicheader = {1,0,1,0,1,0,0,0}; //magic Microsoft header
    //Prepending a header + signature size
    byte[] fullSig = Arrays.copyOf(magicheader, 12 + rdpOutUTF16LEsigned.length); 
    //putting big-endian java int32 into byte[] as little-endian. 
    //TODO: Any less-ugly ways of doing this?
    fullSig[8] = (byte)(rdpOutUTF16LEsigned.length >>> 24);
    fullSig[9] = (byte)(rdpOutUTF16LEsigned.length >>> 16);
    fullSig[10] = (byte)(rdpOutUTF16LEsigned.length >>> 8);
    fullSig[11] = (byte)rdpOutUTF16LEsigned.length;
    //copying the signature array into the new one, after the header
    System.arraycopy(rdpOutUTF16LEsigned, 0, fullSig, 12, rdpOutUTF16LEsigned.length);
    //encoding ito base64 ASCII
    byte[] b64ncodedSig = Base64.getEncoder().encode(fullSig);
    //appending the signature to output
    rdpOut.append(new String(b64ncodedSig, Charset.forName("ASCII")));
    LOG.info("Signed RDP file string: %s", rdpOut);
    return rdpOut.toString();
  }

  /**
   * Sign the RDP file with the generated self-signed certificate
   * @param rdpFile the string with RDP file content
   * @return signed RDP file string
   */

  public static String sign(String rdpFile) {
    try {
      CertAndKeyGen keyTool=new CertAndKeyGen("RSA","SHA1WithRSA");
      keyTool.generate(2048);
      X509Certificate cert = keyTool.getSelfCertificate(new X500Name("CN=ghap.io"), (long)365*24*3600);
      LOG.info("Self-signed cert generated: %s", cert);
      return sign(rdpFile, cert, keyTool.getPrivateKey());
    } catch (NoSuchAlgorithmException | InvalidKeyException | CertificateException | SignatureException | IOException | NoSuchProviderException e) {
      LOG.error("RDPsign exception: %s", e);
    }
    return null;
  }

  //simple test
  public static void main(String[] args) {
    String signedrdp = sign("auto connect:i:1\nfull address:s:127.0.0.1\nprompt for credentials on client:i:1\nusername:s:SOME\\user");
    System.out.println("\n\n" + signedrdp);
  }
}
