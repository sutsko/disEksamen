package utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.gson.Gson;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import model.User;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public final class Token {

    private String token;
    private static ArrayList<String> tokensInUse = new ArrayList<String>();



    public Token(String token){
    this.token = token;
    }

    private static Date expDate() {
        //Kilde: https://www.owasp.org/index.php/JSON_Web_Token_(JWT)_Cheat_Sheet_for_Java
        Calendar c = Calendar.getInstance();
        Date now = c.getTime();
        c.add(Calendar.MILLISECOND, 300000);
        Date expirationDate = c.getTime();

        return expirationDate;
    }


    public static String generateToken(User user) {



        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            String token = JWT.create()
                    .withIssuer("auth0")
                    .withClaim("sub", user.getId())
                    .withExpiresAt(expDate())
                    .sign(algorithm);

            tokensInUse.add(token);
            return token;
        } catch (JWTCreationException exception) {
            //Invalid Signing configuration / Couldn't convert Claims.
        }
        return null;
    }


    public static boolean verifyToken(String token, int idUser) {

       boolean j=false;

       while (j==false)

        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .build(); //Reusable verifier instance
            DecodedJWT jwt = verifier.verify(token);

            String checker = new Gson().toJson(jwt);

            if (checker.contains("\"subject\":\""+idUser+"\"")) {
                j=true;
                return j;

            }
        } catch (JWTVerificationException exception){
            //Invalid signature/claims
        }
        return false;
    }

    public static String plainDecoder(String token) {

        try {
            Algorithm algorithm = Algorithm.HMAC256("secret");
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("auth0")
                    .build(); //Reusable verifier instance
            DecodedJWT jwt = verifier.verify(token);

            token = new Gson().toJson(jwt);

            return token;
        } catch (JWTDecodeException exception) {
            //Invalid token
        }
        return null;
    }

    public static void main(String [ ] args)
    {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOjQzLCJpc3MiOiJhdXRoMCIsImV4cCI6MTU0MjI5MzMxOH0.PSefRQOEhrAciGtCpB7sQjecqcAI6mja6ZCpV0MMExg";
        String json = new Gson().toJson(verifyToken(token, 1));
        System.out.println(json);
    }

    public String getToken() {
        return token;
    }

}