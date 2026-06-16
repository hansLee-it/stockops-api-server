package com.stockops.auth;

import com.stockops.entity.Role;
import com.stockops.entity.Store;
import com.stockops.entity.User;
import com.stockops.repository.RoleRepository;
import com.stockops.repository.StoreRepository;
import com.stockops.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthDataLoaderTest {

    private static final Long LINKED_STORE_ID = 7L;
    private static final String LINKED_STORE_CODE = "ST007";

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    void createsAdminAndRoleTestAccountsWhenPasswordIsConfigured() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName(anyString())).thenAnswer(invocation -> Optional.of(role(invocation.getArgument(0))));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));
        // No store exists yet, except the branch the store accounts link to.
        when(storeRepository.findByCode(anyString())).thenReturn(Optional.empty());
        when(storeRepository.findByCode(LINKED_STORE_CODE)).thenReturn(Optional.of(store(LINKED_STORE_ID, LINKED_STORE_CODE)));

        AuthDataLoader loader = loader("test-password");
        loader.run(null);

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(6)).save(users.capture());

        List<User> created = users.getAllValues();
        assertThat(created).extracting(User::getEmail)
                .containsExactly("admin@stockops.com", "general-admin@stockops.com",
                        "center-manager@stockops.com", "warehouse-manager@stockops.com",
                        "store-manager@stockops.com", "store-staff@stockops.com");
        assertThat(created).extracting(user -> user.getRole().getName())
                .containsExactly("ADMIN", "GENERAL_ADMIN", "CENTER_MANAGER",
                        "WAREHOUSE_MANAGER", "STORE_MANAGER", "STORE_STAFF");
        assertThat(created).allMatch(User::isEnabled);

        // Only the two store-role accounts are linked to a branch; the rest carry no store.
        assertThat(created).filteredOn(user -> user.getRole().getName().startsWith("STORE_"))
                .extracting(User::getStoreId)
                .containsExactly(LINKED_STORE_ID, LINKED_STORE_ID);
        assertThat(created).filteredOn(user -> !user.getRole().getName().startsWith("STORE_"))
                .allMatch(user -> user.getStoreId() == null);
    }

    @Test
    void seedsSampleStoresWhenPasswordIsConfigured() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(roleRepository.findByName(anyString())).thenAnswer(invocation -> Optional.of(role(invocation.getArgument(0))));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));
        when(storeRepository.findByCode(anyString())).thenReturn(Optional.empty());

        AuthDataLoader loader = loader("test-password");
        loader.run(null);

        ArgumentCaptor<Store> stores = ArgumentCaptor.forClass(Store.class);
        verify(storeRepository, times(45)).save(stores.capture());
        assertThat(stores.getAllValues()).extracting(Store::getCode)
                .contains("ST001", "ST007", "ST045")
                .doesNotHaveDuplicates();
        assertThat(stores.getAllValues()).allMatch(Store::isActive);
        assertThat(stores.getAllValues()).noneMatch(Store::isDeleted);
    }

    @Test
    void skipsFixturesWhenPasswordIsBlank() {
        when(userRepository.findByEmail("admin@stockops.com")).thenReturn(Optional.empty());
        when(roleRepository.findByName(anyString())).thenAnswer(invocation -> Optional.of(role(invocation.getArgument(0))));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "encoded-" + invocation.getArgument(0));

        AuthDataLoader loader = loader("");
        loader.run(null);

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(users.capture());
        assertThat(users.getValue().getEmail()).isEqualTo("admin@stockops.com");
        verify(userRepository, never()).findByEmail("store-manager@stockops.com");
        verify(storeRepository, never()).save(any(Store.class));
    }

    @Test
    void doesNotRecreateOrRelinkWhenAccountsAlreadyHaveStore() {
        // Every store already exists (no seeding) and every account already exists with a store set.
        when(storeRepository.findByCode(anyString())).thenReturn(Optional.of(store(LINKED_STORE_ID, LINKED_STORE_CODE)));
        when(userRepository.findByEmail(anyString())).thenAnswer(invocation -> {
            User user = new User();
            user.setEmail(invocation.getArgument(0));
            user.setStoreId(LINKED_STORE_ID);
            return Optional.of(user);
        });

        AuthDataLoader loader = loader("test-password");
        loader.run(null);

        verify(userRepository, never()).save(any(User.class));
        verify(storeRepository, never()).save(any(Store.class));
    }

    @Test
    void backfillsStoreLinkForExistingStoreAccountWithoutStore() {
        when(storeRepository.findByCode(anyString())).thenReturn(Optional.of(store(LINKED_STORE_ID, LINKED_STORE_CODE)));
        User storeManager = new User();
        storeManager.setEmail("store-manager@stockops.com");
        when(userRepository.findByEmail(anyString())).thenAnswer(invocation -> {
            String email = invocation.getArgument(0);
            if ("store-manager@stockops.com".equals(email)) {
                return Optional.of(storeManager);
            }
            // Pretend the other accounts already exist with no missing linkage to isolate the backfill path.
            User other = new User();
            other.setEmail(email);
            other.setStoreId(email.startsWith("store-") ? LINKED_STORE_ID : null);
            return Optional.of(other);
        });

        AuthDataLoader loader = loader("test-password");
        loader.run(null);

        ArgumentCaptor<User> users = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(users.capture());
        assertThat(users.getValue().getEmail()).isEqualTo("store-manager@stockops.com");
        assertThat(users.getValue().getStoreId()).isEqualTo(LINKED_STORE_ID);
    }

    private AuthDataLoader loader(final String testAccountPassword) {
        return new AuthDataLoader(userRepository, roleRepository, storeRepository, passwordEncoder, testAccountPassword);
    }

    private static Role role(final String name) {
        Role role = new Role();
        role.setName(name);
        return role;
    }

    private static Store store(final Long id, final String code) {
        Store store = new Store();
        store.setId(id);
        store.setCode(code);
        return store;
    }
}
