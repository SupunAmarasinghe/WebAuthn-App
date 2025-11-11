package com.example.webauth.controller;

import com.example.webauth.dto.UserDto;
import com.example.webauth.entity.Credentials;
import com.example.webauth.entity.AuthUser;
import com.example.webauth.repo.CredentialsRepository;
import com.example.webauth.repo.JpaCredentialRepository;
import com.example.webauth.repo.UserRepository;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/webauthn")
@CrossOrigin(origins = "https://web-authn-new-ui.vercel.app")
@Slf4j
public class WebAuthnController {

    private final RelyingParty relyingParty;
    private final JpaCredentialRepository repo;
    private final CredentialsRepository credRepo;
    private final UserRepository userRepo;
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, Object> sessionMap = new ConcurrentHashMap<>();

    public WebAuthnController(RelyingParty relyingParty, JpaCredentialRepository repo, CredentialsRepository credRepo,
                              UserRepository userRepo) {
        this.relyingParty = relyingParty;
        this.repo = repo;
        this.credRepo = credRepo;
        this.userRepo = userRepo;
        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
    }

    @PostMapping("/register/start")
    public ResponseEntity startRegistration(@RequestBody UserDto user) {
        String username = user.getUsername();
        UserIdentity userIdentity = UserIdentity.builder()
            .name(username)
            .displayName(username)
            .id(new com.yubico.webauthn.data.ByteArray(username.getBytes((StandardCharsets.UTF_8))))
            .build();

        PublicKeyCredentialCreationOptions options = relyingParty.startRegistration(
            StartRegistrationOptions.builder().user(userIdentity)
                .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                    .residentKey(ResidentKeyRequirement.DISCOURAGED)
                    .userVerification(UserVerificationRequirement.PREFERRED)
                    .build())
                .extensions(RegistrationExtensionInputs.builder()
                    .credProps(true)
                    .build())
                .timeout(60000L)
                .build());

        log.info("[Register_start:{}]", options);

        String json = null;
        try {
            json = jsonMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            return ResponseEntity.status(500)
                .body("{\"error\": \"Failed to process JSON\"}");
        }

        sessionMap.put(username, options);

        return ResponseEntity.ok(json);
    }

    @PostMapping("/register/finish")
    public ResponseEntity finishRegistration(@RequestBody Map<String, Object> body) throws Exception {
        Map<String, String> response = new HashMap<>();
        PublicKeyCredentialCreationOptions start = (PublicKeyCredentialCreationOptions) sessionMap.remove(body.get("username"));
        if (start == null) {
            throw new IllegalStateException("No registration in progress for " + body.get("username"));
        }

        try {
            Object credential = body.get("response");
            String credentialJson = jsonMapper.writeValueAsString(credential);

            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> pkc =
                PublicKeyCredential.parseRegistrationResponseJson(credentialJson);

            RegistrationResult result = relyingParty.finishRegistration(
                FinishRegistrationOptions.builder()
                    .request(start)
                    .response(pkc)
                    .build());

            log.info("[Register_finish:{}]", result);

            if (!result.isUserVerified()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Registration verification failed"));
            }

            AuthUser user = userRepo.findByUserName(body.get("username").toString())
                .orElseGet(() -> {
                    AuthUser u = AuthUser.builder().userName(body.get("username").toString()).displayName(body.get("username").toString())
                        .userHandle(Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(body.get("username").toString().getBytes())).build();
                    return userRepo.save(u);
                });

            Credentials cred = Credentials.builder().user(user)
                .credentialId(Base64.getUrlEncoder().withoutPadding().encodeToString(result.getKeyId().getId().getBytes()))
                .publicKeyCose((Base64.getUrlEncoder().withoutPadding().encodeToString(result.getPublicKeyCose().getBytes())))
                .signatureCount(result.getSignatureCount())
                .build();

            if (result.getKeyId().getTransports().isPresent()) {
                cred.setTransports(
                    result.getKeyId().getTransports().get().stream().map(AuthenticatorTransport::getId).collect(Collectors.toList())
                );
            }
            credRepo.save(cred);

            response.put("verified", String.valueOf(true));
            return ResponseEntity.ok(response);
        } catch(Exception e) {
            response.put("verified", String.valueOf(false));
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/authenticate/start")
    public ResponseEntity<?> startAuthentication(@RequestBody Map<String, Object> body) {
        try {
            String username = (String) body.get("username");
            if (username == null || username.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username required"));
            }

            var userOpt = userRepo.findByUserName(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }

            AssertionRequest request = relyingParty.startAssertion(
                StartAssertionOptions.builder()
                    .username(Optional.of(username))
                    .userVerification(UserVerificationRequirement.PREFERRED)
                    .timeout(60000L)
                    .build()
            );

            log.info("[Authenticate_start:{}]", request);

            PublicKeyCredentialRequestOptions options = request.getPublicKeyCredentialRequestOptions();
            sessionMap.put(username, request);

            String json = null;
            json = jsonMapper.writeValueAsString(options);

            return ResponseEntity.ok(json);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "verified", false,
                "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/authenticate/finish")
    public ResponseEntity<Map<String, Object>> finishAuthentication(@RequestBody Map<String, Object> body) {
        Map<String, Object> response = new HashMap<>();

        try {
            String username = (String) body.get("username");
            Object credentialObj = body.get("response");

            if (username == null || credentialObj == null) {
                response.put("verified", false);
                response.put("error", "Missing username or credential");
                return ResponseEntity.badRequest().body(response);
            }

            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(credentialObj);
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> pkc =
                PublicKeyCredential.parseAssertionResponseJson(json);

            AssertionRequest savedRequest = (AssertionRequest) sessionMap.remove(username);
            if (savedRequest == null) {
                response.put("verified", false);
                response.put("error", "No pending authentication request found for user: " + username);
                return ResponseEntity.badRequest().body(response);
            }

            AssertionResult result = relyingParty.finishAssertion(
                FinishAssertionOptions.builder()
                    .request(savedRequest)
                    .response(pkc)
                    .build()
            );

            log.info("[Register_finish:{}]", result);

            if (result.isSuccess()) {
                String credentialId = result.getCredential().getCredentialId().getBase64Url();
                Credentials credential = credRepo.findByCredentialId(credentialId)
                    .orElseThrow(() -> new RuntimeException("Credential not found"));

                credential.setSignatureCount(result.getSignatureCount());
                credRepo.save(credential);

                response.put("verified", true);
            } else {
                response.put("verified", false);
                response.put("error", "Assertion verification failed");
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("verified", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}