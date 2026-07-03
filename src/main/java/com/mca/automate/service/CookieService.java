package com.mca.automate.service;

import com.mca.automate.dto.CookieEntity;
import com.mca.automate.dto.CookieForSession;
import com.mca.automate.repository.CookieRepository;
import com.mca.automate.util.Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/* JADX INFO: loaded from: CookieService.class */
@Service
public class CookieService {

    @Autowired
    private CookieRepository repo;

    @Autowired
    private Util util;

    @Autowired
    private UserRegistration userRegistration;

    public void saveCookies(String emailId, String cookieHeader) {
        CookieEntity entity = new CookieEntity();
        entity.setEmailId(emailId);
        entity.setCookies(cookieHeader);
        entity.setUpdatedAt(System.currentTimeMillis());
        this.repo.save(entity);
    }

    public String getCookies(String emailId) {
        return (String) this.repo.findById(emailId).map((v0) -> {
            return v0.getCookies();
        }).orElse(null);
    }

    public CookieForSession getCookiesWithType(String emailId) {
        return (CookieForSession) this.repo.findById(emailId).map(entity -> {
            return new CookieForSession(entity.getCookies(), entity.getCookieType(), entity.getMobileNo(), entity.getPassword());
        }).orElse(null);
    }

    public boolean hasCookies(String emailId) {
        return this.repo.existsById(emailId);
    }
}
