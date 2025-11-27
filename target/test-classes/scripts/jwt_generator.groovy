import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

// --------------------------------------------------------
// 1. CONFIGURATION
// --------------------------------------------------------

def CLIENT_ID = System.getenv("CLIENT_ID") 
def USERNAME = System.getenv("SALESFORCE_USERNAME")
def AUDIENCE = System.getenv("AUDIENCE")
def ALGORITHM = "RS256"
def CURRENT_TIME = System.currentTimeMillis() / 1000L;
def EXPIRATION_TIME = CURRENT_TIME + 300L; 

log.info("Client ID: " + CLIENT_ID);

def getPrivateKeyContent() {

    def CI_CONTENT_PROPERTY = "SALESFORCE_PRIVATE_KEY"
    def LOCAL_PATH_ENV_VAR = "PRIVATE_KEY_PATH"

    def content = System.getenv(CI_CONTENT_PROPERTY);

    if (content != null && !content.isEmpty()) {
        log.info("Key source: CI property (SALESFORCE_PRIVATE_KEY) - Assuming the value provided is already Base64 encoded.");
        byte[] decodedBytes = Base64.getDecoder().decode(content);
        String rawPemContent = new String(decodedBytes, "UTF-8");
        return rawPemContent;
    }
    
    // 2. Fallback: Attempt to get path from local environment variable
    def keyPath = System.getenv(LOCAL_PATH_ENV_VAR);

    if (keyPath != null && !keyPath.isEmpty()) {
        def keyFile = new File(keyPath);
        if (keyFile.exists()) {
            log.info("Key source: Local file path ($LOCAL_PATH_ENV_VAR) - Assuming plain text content.");
            return keyFile.getText("UTF-8");
        } else {
            throw new Exception("ERROR: Private key file not found at path: " + keyPath);
        }
    }
    
    // 3. Failure
    throw new Exception("ERROR: Private key content missing! Set property '$CI_CONTENT_PROPERTY' (CI) or environment variable '$LOCAL_PATH_ENV_VAR' (local).");
}


// --------------------------------------------------------
// 2. PAYLOAD CREATION AND ENCODING (JWS Header)
// --------------------------------------------------------

def header = """{"alg":"${ALGORITHM}","typ":"JWT"}"""
def payload = """{
  "iss":"${CLIENT_ID}",
  "sub":"${USERNAME}",
  "aud":"${AUDIENCE}",
  "exp":${EXPIRATION_TIME}
}"""

def encoder = Base64.getUrlEncoder().withoutPadding()
def encodedHeader = encoder.encodeToString(header.getBytes("UTF-8"))
def encodedPayload = encoder.encodeToString(payload.getBytes("UTF-8"))

def tokenToSign = "${encodedHeader}.${encodedPayload}"

// --------------------------------------------------------
// 3. SIGNATURE (RSA with SHA-256)
// --------------------------------------------------------

def keyContent = getPrivateKeyContent(); 

keyContent = keyContent
  .replaceAll("-----BEGIN PRIVATE KEY-----", "")
  .replaceAll("-----END PRIVATE KEY-----", "")
  .replaceAll("\\s", "") 
  .replaceAll("\\n", "") 
  .replaceAll("\\r", ""); 

try {
    def keySpec = new PKCS8EncodedKeySpec(java.util.Base64.getDecoder().decode(keyContent));

    def keyFactory = KeyFactory.getInstance("RSA");
    def privateKey = keyFactory.generatePrivate(keySpec);

    def signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(privateKey);
    signer.update(tokenToSign.getBytes("UTF-8"));
    def signatureBytes = signer.sign();

    // --------------------------------------------------------
    // 4. FINAL JWT CREATION AND VARIABLE ASSIGNMENT
    // --------------------------------------------------------

    def encodedSignature = encoder.encodeToString(signatureBytes);
    def finalJWT = "${tokenToSign}.${encodedSignature}";

    vars.put("JWT_ASSERTION", finalJWT);

} catch (Exception e) {
    log.error("ERROR during JWT key decoding or signing: " + e.getMessage());
    throw new Exception("JWT Generation Failed: Check Private Key format and Base64 encoding. Error: " + e.getMessage());
}