package com.mca.automate.repository;

import com.mca.automate.dto.NewUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/* JADX INFO: loaded from: UserRegistrationRepository.class */
@Repository
public interface UserRegistrationRepository extends JpaRepository<NewUserEntity, String> {
}
