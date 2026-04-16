package com.rescuehub.security;

import com.rescuehub.entity.User;
import com.rescuehub.enums.Role;
import com.rescuehub.exception.ForbiddenException;
import org.springframework.stereotype.Component;

@Component
public class RoleGuard {
    public void require(User user, Role... roles) {
        for (Role r : roles) {
            if (user.getRole() == r) return;
        }
        throw new ForbiddenException("Insufficient role. Required: " + java.util.Arrays.toString(roles));
    }

    public boolean hasRole(User user, Role... roles) {
        for (Role r : roles) {
            if (user.getRole() == r) return true;
        }
        return false;
    }
}
