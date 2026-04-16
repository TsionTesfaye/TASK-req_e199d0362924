package com.rescuehub.security;

import com.rescuehub.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;
import java.util.List;

public class UserAuthentication implements Authentication {
    private final User user;
    private final String workstationId;

    public UserAuthentication(User user, String workstationId) {
        this.user = user;
        this.workstationId = workstationId;
    }

    public User getUser() { return user; }
    public String getWorkstationId() { return workstationId; }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public Object getCredentials() { return null; }
    @Override public Object getDetails() { return null; }
    @Override public Object getPrincipal() { return user; }
    @Override public boolean isAuthenticated() { return true; }
    @Override public void setAuthenticated(boolean isAuthenticated) {}
    @Override public String getName() { return user.getUsername(); }
}
