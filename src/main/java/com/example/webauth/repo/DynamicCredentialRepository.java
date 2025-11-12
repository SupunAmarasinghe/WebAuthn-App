package com.example.webauth.repo;

import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

@Component
public class DynamicCredentialRepository implements CredentialRepository {

  private final JpaCredentialRepository delegate;
  private final ThreadLocal<Boolean> multiDeviceMode = ThreadLocal.withInitial(() -> false);

  public DynamicCredentialRepository(JpaCredentialRepository delegate) {
    this.delegate = delegate;
  }

  public void enableMultiDeviceMode(boolean enabled) {
    multiDeviceMode.set(enabled);
  }

  @Override
  public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
    if (multiDeviceMode.get()) {
      return Collections.emptySet(); // allow multi-device registration
    }
    return delegate.getCredentialIdsForUsername(username);
  }

  @Override
  public Optional<ByteArray> getUserHandleForUsername(String username) {
    return delegate.getUserHandleForUsername(username);
  }

  @Override
  public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
    return delegate.getUsernameForUserHandle(userHandle);
  }

  @Override
  public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
    return delegate.lookup(credentialId, userHandle);
  }

  @Override
  public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
    return delegate.lookupAll(credentialId);
  }
}
