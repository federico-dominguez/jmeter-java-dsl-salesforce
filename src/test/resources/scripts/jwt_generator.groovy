// --------------------------------------------------------
// 1. CONFIGURATION
// --------------------------------------------------------

def CLIENT_ID = "3MVG9rZjd7MXFdLg_QQqBX7kDdLAAvhL1zIxwugJa6S56L94nPzq9vLbGSONrYtARPhCX0iBH6771WgYTm3ax" 
def USERNAME = "sdominguez.federico718@agentforce.com"
def AUDIENCE = "https://orgfarm-b8d4a27e18-dev-ed.develop.my.salesforce.com" 
def PRIVATE_KEY_PATH = "server.key" 
def ALGORITHM = "RS256"
def CURRENT_TIME = System.currentTimeMillis() / 1000L;
def EXPIRATION_TIME = CURRENT_TIME + 300L; // Token expires in 5 minutes (300 seconds)

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

def keyContent = new File(PRIVATE_KEY_PATH).getText("UTF-8");

// Aggressive cleaning: Remove headers, footers, and all whitespace characters.
keyContent = keyContent
        .replaceAll("-----BEGIN PRIVATE KEY-----", "")
        .replaceAll("-----END PRIVATE KEY-----", "")
        .replaceAll("\\s", "") 
        .replaceAll("\\n", "") 
        .replaceAll("\\r", ""); 

// Decode the clean Base64 content
def keySpec = new PKCS8EncodedKeySpec(java.util.Base64.getDecoder().decode(keyContent));

def keyFactory = KeyFactory.getInstance("RSA");
def privateKey = keyFactory.generatePrivate(keySpec);

// Sign the token
def signer = Signature.getInstance("SHA256withRSA");
signer.initSign(privateKey);
signer.update(tokenToSign.getBytes("UTF-8"));
def signatureBytes = signer.sign();

// --------------------------------------------------------
// 4. FINAL JWT CREATION AND VARIABLE ASSIGNMENT
// --------------------------------------------------------

def encodedSignature = encoder.encodeToString(signatureBytes);
def finalJWT = "${tokenToSign}.${encodedSignature}";

// Assign the complete JWT to a JMeter variable for use in the HTTP Request
vars.put("JWT_ASSERTION", finalJWT);

log.info("Successfully generated JWT: " + finalJWT); 

import java.io.File
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64