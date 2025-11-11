package com.example.webauth.repo;

import com.example.webauth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface UserRepository extends JpaRepository<AuthUser, Long> {
  Optional<AuthUser> findByUserName(String userName);
  Optional<AuthUser> findByUserHandle(String userHandle);
}

