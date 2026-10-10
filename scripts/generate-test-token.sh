#!/usr/bin/env bash
# Gix Gym — Generate Test JWT
# Owner: dinesh-sh886
# Usage: ./scripts/generate-test-token.sh [org_id] [branch_id] [roles...] [subject]
# Requires: Java (for proper JWT signing with Nimbus JOSE+JWT)
# Environment: JWT_SECRET (required, min 32 chars), JWT_ISSUER, JWT_AUDIENCE

set -euo pipefail

ORG_ID="${1:-00000000-0000-0000-0000-000000000001}"
BRANCH_ID="${2:-00000000-0000-0000-0000-000000000001}"
ROLES="${3:-MEMBER}"
SUBJECT="${4:-test-user}"

# JWT_SECRET is required - no default for security
SECRET="${JWT_SECRET:-}"
ISSUER="${JWT_ISSUER:-gix-gym}"
AUDIENCE="${JWT_AUDIENCE:-gix-gym-clients}"

# Validate secret is provided
if [[ -z "$SECRET" ]]; then
    echo "ERROR: JWT_SECRET environment variable is required" >&2
    echo "Usage: JWT_SECRET=your-secret ./scripts/generate-test-token.sh [org_id] [branch_id] [roles] [subject]" >&2
    exit 1
fi

# Validate secret strength
if [[ ${#SECRET} -lt 32 ]]; then
    echo "ERROR: JWT_SECRET must be at least 32 characters (256 bits for HS256)" >&2
    echo "Current secret length: ${#SECRET}" >&2
    exit 1
fi

# Use Java to generate the token with proper JWT library
cat > /tmp/GenerateToken.java << 'JAVA_EOF'
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class GenerateToken {
    public static void main(String[] args) throws Exception {
        String orgId = args[0];
        String branchId = args[1];
        String roles = args[2];
        String subject = args[3];
        String secret = args[4];
        String issuer = args[5];
        String audience = args[6];

        JWSSigner signer = new MACSigner(secret.getBytes(StandardCharsets.UTF_8));
        
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("org_id", orgId)
                .claim("branch_id", branchId)
                .claim("roles", Arrays.asList(roles.split(",")))
                .issuer(issuer)
                .audience(audience)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 15 * 60 * 1000)) // 15 min
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(),
                claimsSet
        );
        signedJWT.sign(signer);
        
        System.out.println(signedJWT.serialize());
    }
}
JAVA_EOF

# Compile and run with the project's dependencies
cd "$(dirname "$0")/../services/membership"
mvn -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt 2>/dev/null
CP=$(cat /tmp/cp.txt)

javac -cp "target/classes:$CP" /tmp/GenerateToken.java 2>/dev/null
java -cp "/tmp:target/classes:$CP" GenerateToken "$ORG_ID" "$BRANCH_ID" "$ROLES" "$SUBJECT" "$SECRET" "$ISSUER" "$AUDIENCE"

# Cleanup
rm -f /tmp/GenerateToken.java /tmp/GenerateToken.class /tmp/cp.txt