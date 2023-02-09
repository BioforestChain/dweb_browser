"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.createServerCertificate = void 0;
const node_forge_1 = __importDefault(require("node-forge"));
const node_fs_1 = __importDefault(require("node:fs"));
const node_path_1 = __importDefault(require("node:path"));
// 读取 CA证书，后面需要根据它创建域名证书
// 【win】该CA证书需要在mmc控制台中导入到“受信任的根证书颁发机构”
const caKey = node_forge_1.default.pki.decryptRsaPrivateKey(node_fs_1.default.readFileSync(node_path_1.default.resolve(__dirname, "../cert/rootCA-key.pem"), "utf8"));
const caCert = node_forge_1.default.pki.certificateFromPem(node_fs_1.default.readFileSync(node_path_1.default.resolve(__dirname, "../cert/rootCA.pem"), "utf8"));
const certCache = new Map(); // 缓存证书
/**
 * 根据所给域名生成对应证书
 */
function createServerCertificate(domain) {
    let server_cert = certCache.get(domain);
    if (server_cert) {
        return server_cert;
    }
    const keys = node_forge_1.default.pki.rsa.generateKeyPair(2046);
    const cert = node_forge_1.default.pki.createCertificate();
    cert.publicKey = keys.publicKey;
    cert.serialNumber = `${new Date().getTime()}`;
    cert.validity.notBefore = new Date();
    cert.validity.notBefore.setFullYear(cert.validity.notBefore.getFullYear() - 1);
    cert.validity.notAfter = new Date();
    cert.validity.notAfter.setFullYear(cert.validity.notAfter.getFullYear() + 1);
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
    cert.sign(caKey, node_forge_1.default.md.sha256.create());
    certCache.set(domain, (server_cert = {
        key: keys.privateKey,
        cert,
    }));
    return server_cert;
}
exports.createServerCertificate = createServerCertificate;
