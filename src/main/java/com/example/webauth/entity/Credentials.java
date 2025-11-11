package com.example.webauth.entity;

import com.yubico.webauthn.data.AuthenticatorTransport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "credentials")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Credentials {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "users_id", nullable = false)
  private AuthUser user;
  private String userHandle;
  private String credentialId;
  private String publicKeyCose;
  private long signatureCount;

  @ElementCollection
  @CollectionTable(name = "credential_transports", joinColumns = @JoinColumn(name = "credential_id"))
  @Column(name = "transport")
  private String transports;

  @CreationTimestamp
  private LocalDateTime createdAt;

  public List<AuthenticatorTransport> getTransportsList() {
    if (transports == null || transports.isBlank()) {
      return Collections.emptyList();
    }
    return Arrays.stream(transports.split(","))
        .map(String::trim)
        .map(AuthenticatorTransport::valueOf)
        .collect(Collectors.toList());
  }

  public void setTransportsList(List<AuthenticatorTransport> transportList) {
    if (transportList == null || transportList.isEmpty()) {
      this.transports = null;
    } else {
      this.transports = transportList.stream()
          .map(AuthenticatorTransport::getId)
          .collect(Collectors.joining(","));
    }
  }
}
