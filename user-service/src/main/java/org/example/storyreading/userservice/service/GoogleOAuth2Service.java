package org.example.storyreading.userservice.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.example.storyreading.userservice.dto.GoogleUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class GoogleOAuth2Service {

    @Value("${google.client.id}")
    private String googleClientId;

    private final GoogleIdTokenVerifier verifier;

    public GoogleOAuth2Service(@Value("${google.client.id}") String googleClientId) {
        this.googleClientId = googleClientId;
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    /**
     * Verify Google ID Token và lấy thông tin user
     * @param idToken Google ID Token từ frontend
     * @return GoogleUserInfo nếu token hợp lệ, null nếu không hợp lệ
     */
    public GoogleUserInfo verifyGoogleToken(String idToken) {
        try {
            GoogleIdToken googleIdToken = verifier.verify(idToken);

            if (googleIdToken != null) {
                GoogleIdToken.Payload payload = googleIdToken.getPayload();

                String googleId = payload.getSubject();
                String email = payload.getEmail();
                boolean emailVerified = payload.getEmailVerified();
                String name = (String) payload.get("name");
                String picture = (String) payload.get("picture");

                return new GoogleUserInfo(googleId, email, name, picture, emailVerified);
            }

            return null;
        } catch (Exception e) {
            System.err.println("Error verifying Google token: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}

