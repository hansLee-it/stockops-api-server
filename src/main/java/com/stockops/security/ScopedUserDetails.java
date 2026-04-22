package com.stockops.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * UserDetails implementation carrying effective scope metadata.
 *
 * @author StockOps Team
 * @since 2.0
 */
public class ScopedUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;
    private final ScopeAccessProfile scopeAccessProfile;

    /**
     * Creates scoped user details.
     *
     * @param userId user identifier
     * @param username username/email
     * @param password encoded password
     * @param enabled whether enabled
     * @param authorities granted authorities
     * @param scopeAccessProfile effective scope profile
     */
    public ScopedUserDetails(final Long userId,
                             final String username,
                             final String password,
                             final boolean enabled,
                             final Collection<? extends GrantedAuthority> authorities,
                             final ScopeAccessProfile scopeAccessProfile) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.enabled = enabled;
        this.authorities = authorities;
        this.scopeAccessProfile = scopeAccessProfile;
    }

    public Long getUserId() {
        return userId;
    }

    public ScopeAccessProfile getScopeAccessProfile() {
        return scopeAccessProfile;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
