import forge from "node-forge";
import fs from "node:fs";
import path from "node:path";

// 读取 CA证书，后面需要根据它创建域名证书
const caKey = forge.pki.decryptRsaPrivateKey(
  fs.readFileSync(
    path.resolve(__dirname, "../../../cert/rootCA-key.pem"),
    "utf8"
  )
);
const caCert = forge.pki.certificateFromPem(
  fs.readFileSync(path.resolve(__dirname, "../../../cert/rootCA.pem"), "utf8")
);
const certCache = new Map<
  string,
  {
    pki: {
      key: forge.pki.rsa.PrivateKey;
      cert: forge.pki.Certificate;
    };
    pem: {
      key: string;
      cert: string;
    };
  }
>(); // 缓存证书

/**
 * 根据所给域名生成对应证书
 */
export function createServerCertificate(domain: string) {
  let server_cert = certCache.get(domain);
  if (server_cert) {
    return server_cert;
  }

  const keys = forge.pki.rsa.generateKeyPair(2046);
  const cert = forge.pki.createCertificate();
  cert.publicKey = keys.publicKey;
  cert.serialNumber = `${new Date().getTime()}`;
  cert.validity.notBefore = new Date();
  cert.validity.notBefore.setFullYear(
    cert.validity.notBefore.getFullYear() - 1
  );
  cert.validity.notAfter = new Date();
  cert.validity.notAfter.setFullYear(cert.validity.notAfter.getFullYear() + 2); // 给1~2年的有效期
  cert.setIssuer(caCert.subject.attributes);
  let _subject = JSON.parse(JSON.stringify(caCert.subject.attributes[0]));
  _subject.value = domain;
  cert.setSubject([_subject]);
  cert.setExtensions([
    {
      name: "basicConstraints",
      critical: true,
      cA: false,
    },
    {
      name: "keyUsage",
      critical: true,
      digitalSignature: true,
      contentCommitment: true,
      keyEncipherment: true,
      dataEncipherment: true,
      keyAgreement: true,
      keyCertSign: true,
      cRLSign: true,
      encipherOnly: true,
      decipherOnly: true,
    },
    {
      name: "subjectAltName",
      altNames: [
        {
          type: 2,
          value: domain,
        },
      ],
    },
    {
      name: "subjectKeyIdentifier",
    },
    {
      name: "extKeyUsage",
      serverAuth: true,
      clientAuth: true,
      codeSigning: true,
      emailProtection: true,
      timeStamping: true,
    },
    {
      name: "authorityKeyIdentifier",
    },
  ]);
  cert.sign(caKey, forge.md.sha256.create());
  certCache.set(
    domain,
    (server_cert = {
      pki: {
        key: keys.privateKey,
        cert: cert,
      },
      get pem() {
        return {
          key: forge.pki.privateKeyToPem(keys.privateKey),
          cert: forge.pki.certificateToPem(cert),
        };
      },
    })
  );
  return server_cert;
}
