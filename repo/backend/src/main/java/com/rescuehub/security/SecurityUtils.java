package com.rescuehub.security;

import com.rescuehub.entity.User;
import com.rescuehub.exception.AuthException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {

    public static UserAuthentication currentAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof UserAuthentication ua) return ua;
        throw new AuthException("Not authenticated");
    }

    public static User currentUser() {
        return currentAuth().getUser();
    }

    public static String currentWorkstationId() {
        return currentAuth().getWorkstationId();
    }
}
