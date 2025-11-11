package com.example.webauth.repo;

import com.example.webauth.entity.AuthUser;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.*;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JpaCredentialRepository implements CredentialRepository {

  private final UserRepository userRepo;
  private final CredentialsRepository credRepo;

  public JpaCredentialRepository(UserRepository userRepo, CredentialsRepository credRepo) {
    this.userRepo = userRepo;
    this.credRepo = credRepo;
  }

  @Override
  public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
    return userRepo.findByUserName(username)
        .map(user -> credRepo.findByUser(user).stream()
            .map(cred -> PublicKeyCredentialDescriptor.builder()
                .id(new ByteArray(Base64.getUrlDecoder().decode(cred.getCredentialId())))
                .type(PublicKeyCredentialType.PUBLIC_KEY)
                .transports(Set.copyOf(cred.getTransportsList()))
                .build())
            .collect(Collectors.toSet()))
        .orElse(Collections.emptySet());
  }

  @Override
  public Optional<ByteArray> getUserHandleForUsername(String username) {
    return userRepo.findByUserName(username)
        .map(u -> new ByteArray(Base64.getUrlDecoder().decode(u.getId().toString())));
  }

  @Override
  public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
    Long id = Long.parseLong(userHandle.getBase64Url());
    return userRepo.findById(id).map(AuthUser::getUserName);
  }

  @Override
  public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
    String credIdEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(credentialId.getBytes());
    return credRepo.findByCredentialId(credIdEncoded)
        .map(cred -> RegisteredCredential.builder()
            .credentialId(new ByteArray(Base64.getUrlDecoder().decode(cred.getCredentialId())))
            .userHandle(new ByteArray(Base64.getUrlDecoder().decode(cred.getUser().getId().toString())))
            .publicKeyCose(new ByteArray(Base64.getUrlDecoder().decode(cred.getPublicKeyCose())))
            .signatureCount(cred.getSignatureCount())
            .build());
  }

  @Override
  public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
    String credIdEncoded = Base64.getUrlEncoder().withoutPadding().encodeToString(credentialId.getBytes());
    return credRepo.findByCredentialId(credIdEncoded)
        .stream()
        .map(cred -> RegisteredCredential.builder()
            .credentialId(new ByteArray(Base64.getUrlDecoder().decode(cred.getCredentialId())))
            .userHandle(new ByteArray(Base64.getUrlDecoder().decode(cred.getUser().getId().toString())))
            .publicKeyCose(new ByteArray(Base64.getUrlDecoder().decode(cred.getPublicKeyCose())))
            .signatureCount(cred.getSignatureCount())
            .build())
        .collect(Collectors.toSet());
  }
}