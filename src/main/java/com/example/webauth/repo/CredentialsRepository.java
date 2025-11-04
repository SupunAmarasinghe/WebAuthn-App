package com.example.webauth.repo;

import com.example.webauth.entity.Credentials;
import com.example.webauth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
  List<Credentials> findByUser(AuthUser user);
  Optional<Credentials> findByCredentialId(String credentialId);
}
