package com.example.webauth.config;

import com.example.webauth.repo.JpaCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class WebAuthnConfig {

    @Bean
    public RelyingParty relyingParty(JpaCredentialRepository jpaCredentialRepository) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
            .id("web-authn-new-ui.vercel")
            .name("WebAuthn Demo")
            .build();

        return RelyingParty.builder()
            .identity(rpIdentity)
            .credentialRepository(jpaCredentialRepository)
            .origins(Set.of("https://web-authn-new-ui.vercel.app"))
            .build();
    }
}
