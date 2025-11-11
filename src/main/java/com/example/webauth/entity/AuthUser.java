package com.example.webauth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_user")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthUser {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String userName;
  private String displayName;

  @CreationTimestamp
  private LocalDateTime createdAt;

}
