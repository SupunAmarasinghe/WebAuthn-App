package com.example.webauth.config;

import com.example.webauth.repo.DynamicCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class WebAuthnConfig {

    @Bean
    public RelyingParty relyingParty(DynamicCredentialRepository repo) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
            .id("web-authn-new-ui.vercel.app")
            .name("WebAuthn Demo")
            .build();

        return RelyingParty.builder()
            .identity(rpIdentity)
            .credentialRepository(repo)
            .origins(Set.of("https://web-authn-new-ui.vercel.app"))
            .build();
    }
}
