package utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import model.User;
import java.util.Date;

public final class Token {

    private String token;

    public Token(String token) {
        this.token = token;
    }


    public static String generateToken(User user) {

        try {
            /**Den der secret skal gemmes et andet sted**/
            Algorithm algorithm = Algorithm.HMAC256("secret");
            String token = JWT.create()
                    .withIssuer("auth0")
                    .withIssuedAt(new Date(System.currentTimeMillis()))
                    .withExpiresAt(new Date(System.currentTimeMillis() + 900000)) // equals 15 minutes
                    .withClaim("sub", user.getId())
                    .withSubject(Integer.toString(user.getId()))
                    .sign(algorithm);

            return token;
        } catch (JWTCreationException exception) {
            //Invalid Signing configuration / Couldn't convert Claims.
        }
        return null;
    }


    public static boolean verifyToken(String token, User user) {


        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .withSubject(Integer.toString(user.getId()))
                    .build(); //Reusable verifier instance

            verifier.verify(token);

        } catch (JWTVerificationException exception) {
            //Invalid signature/claims
        }
        return false;
    }


}

