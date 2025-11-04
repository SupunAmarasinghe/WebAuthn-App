package com.example.webauth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

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
  private List<String> transports;

  @CreationTimestamp
  private LocalDateTime createdAt;
}
