package com.mca.automate.service;

import com.mca.automate.dto.NewUserEntity;
import com.mca.automate.repository.UserRegistrationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/* JADX INFO: loaded from: UserRegistration.class */
@Service
public class UserRegistration {

    @Autowired
    UserRegistrationRepository userRegistrationRepository;

    public NewUserEntity createNewUser(NewUserEntity newUserEntity) {
        return (NewUserEntity) this.userRegistrationRepository.save(newUserEntity);
    }

    public String getDeviceId(String emailId) {
        return (String) this.userRegistrationRepository.findById(emailId).map((v0) -> {
            return v0.getDeviceId();
        }).orElse(null);
    }

    public java.util.List<NewUserEntity> getAllUsers() {
        return this.userRegistrationRepository.findAll();
    }
}
