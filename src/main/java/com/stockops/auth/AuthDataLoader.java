package com.stockops.auth;

import com.stockops.entity.Role;
import com.stockops.entity.Store;
import com.stockops.entity.User;
import com.stockops.repository.RoleRepository;
import com.stockops.repository.StoreRepository;
import com.stockops.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Seeds the default administrator account required for first-time access, plus dev/demo fixtures
 * (sample store master data and per-role test accounts) when {@code stockops.test-accounts.password}
 * is configured. Production leaves that property blank, so only the administrator is created and no
 * sample stores are inserted.
 *
 * @author StockOps Team
 * @since 1.0
 */
@Component
public class AuthDataLoader implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@stockops.com";
    private static final String ADMIN_ROLE = "ADMIN";

    /** Roles whose users belong to a store and therefore require a {@code store_id}. */
    private static final Set<String> STORE_ROLES = Set.of("STORE_MANAGER", "STORE_STAFF");

    /** Arbitrary store the seeded store-role accounts are attached to (one branch, its manager + staff). */
    private static final String LINKED_STORE_CODE = "ST007";

    private static final List<SeedAccount> TEST_ACCOUNTS = List.of(
            new SeedAccount("general-admin@stockops.com", "StockOps 일반관리자", "GENERAL_ADMIN"),
            new SeedAccount("center-manager@stockops.com", "StockOps 센터관리자", "CENTER_MANAGER"),
            new SeedAccount("warehouse-manager@stockops.com", "StockOps 창고관리자", "WAREHOUSE_MANAGER"),
            new SeedAccount("store-manager@stockops.com", "StockOps 지점매니저", "STORE_MANAGER"),
            new SeedAccount("store-staff@stockops.com", "StockOps 지점직원", "STORE_STAFF")
    );

    private static final List<SeedStore> SAMPLE_STORES = buildSampleStores();

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StoreRepository storeRepository;
    private final PasswordEncoder passwordEncoder;
    private final String testAccountPassword;

    /**
     * Creates the authentication data loader.
     *
     * @param userRepository user repository
     * @param roleRepository role repository
     * @param storeRepository store repository
     * @param passwordEncoder password encoder
     * @param testAccountPassword shared password for dev/demo fixtures; blank disables them
     */
    public AuthDataLoader(final UserRepository userRepository,
                          final RoleRepository roleRepository,
                          final StoreRepository storeRepository,
                          final PasswordEncoder passwordEncoder,
                          @Value("${stockops.test-accounts.password:}") final String testAccountPassword) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.storeRepository = storeRepository;
        this.passwordEncoder = passwordEncoder;
        this.testAccountPassword = testAccountPassword;
    }

    /**
     * Inserts the default administrator, and—when dev/demo fixtures are enabled—the sample stores and
     * per-role test accounts, linking the store-role accounts to an arbitrary branch.
     *
     * @param args application arguments
     */
    @Override
    @Transactional
    public void run(final ApplicationArguments args) {
        seedAccount(new SeedAccount(ADMIN_EMAIL, "StockOps Admin", ADMIN_ROLE), "admin123", null);

        if (!testAccountPassword.isBlank()) {
            seedStores();
            final Long linkedStoreId = resolveLinkedStoreId();
            for (SeedAccount account : TEST_ACCOUNTS) {
                final Long storeId = STORE_ROLES.contains(account.role()) ? linkedStoreId : null;
                seedAccount(account, testAccountPassword, storeId);
            }
        }
    }

    private void seedStores() {
        for (SeedStore store : SAMPLE_STORES) {
            if (storeRepository.findByCode(store.code()).isPresent()) {
                continue;
            }
            final Store entity = new Store();
            entity.setCode(store.code());
            entity.setName(store.name());
            entity.setLocation(store.location());
            entity.setContact(store.contact());
            entity.setActive(true);
            entity.setDeleted(false);
            storeRepository.save(entity);
        }
    }

    private Long resolveLinkedStoreId() {
        return storeRepository.findByCode(LINKED_STORE_CODE)
                .map(Store::getId)
                .orElse(null);
    }

    private void seedAccount(final SeedAccount account, final String password, final Long storeId) {
        final Optional<User> existing = userRepository.findByEmail(account.email());
        if (existing.isPresent()) {
            // Backfill store linkage for store-role accounts created before the stores existed.
            final User user = existing.get();
            if (storeId != null && user.getStoreId() == null) {
                user.setStoreId(storeId);
                userRepository.save(user);
            }
            return;
        }

        final Role role = roleRepository.findByName(account.role())
                .orElseThrow(() -> new IllegalStateException("Missing role: " + account.role()));

        final User user = new User();
        user.setEmail(account.email());
        user.setPassword(passwordEncoder.encode(password));
        user.setName(account.name());
        user.setEnabled(true);
        user.setRole(role);
        user.setStoreId(storeId);

        userRepository.save(user);
    }

    private static List<SeedStore> buildSampleStores() {
        // {지점명, 소재지} — 서울 25개 자치구 + 주요 도시 20곳. code/contact는 인덱스로 결정적 생성.
        final String[][] data = {
                {"강남", "서울 강남구"}, {"서초", "서울 서초구"}, {"송파", "서울 송파구"},
                {"강동", "서울 강동구"}, {"마포", "서울 마포구"}, {"용산", "서울 용산구"},
                {"종로", "서울 종로구"}, {"명동", "서울 중구"}, {"성수", "서울 성동구"},
                {"건대입구", "서울 광진구"}, {"청량리", "서울 동대문구"}, {"중랑", "서울 중랑구"},
                {"성북", "서울 성북구"}, {"미아", "서울 강북구"}, {"창동", "서울 도봉구"},
                {"노원", "서울 노원구"}, {"연신내", "서울 은평구"}, {"신촌", "서울 서대문구"},
                {"목동", "서울 양천구"}, {"화곡", "서울 강서구"}, {"구로", "서울 구로구"},
                {"가산", "서울 금천구"}, {"여의도", "서울 영등포구"}, {"사당", "서울 동작구"},
                {"신림", "서울 관악구"}, {"수원", "경기 수원시"}, {"분당", "경기 성남시 분당구"},
                {"일산", "경기 고양시"}, {"평촌", "경기 안양시"}, {"부천", "경기 부천시"},
                {"의정부", "경기 의정부시"}, {"용인", "경기 용인시"}, {"동탄", "경기 화성시"},
                {"인천논현", "인천 남동구"}, {"송도", "인천 연수구"}, {"부산서면", "부산 부산진구"},
                {"해운대", "부산 해운대구"}, {"대구동성로", "대구 중구"}, {"대전둔산", "대전 서구"},
                {"광주충장로", "광주 동구"}, {"울산삼산", "울산 남구"}, {"창원상남", "경남 창원시"},
                {"청주성안", "충북 청주시"}, {"전주객사", "전북 전주시"}, {"천안불당", "충남 천안시"}
        };

        final List<SeedStore> stores = new ArrayList<>(data.length);
        for (int i = 0; i < data.length; i++) {
            final String code = String.format("ST%03d", i + 1);
            final String name = data[i][0] + "점";
            final String contact = String.format("0507-%04d-%04d", 1000 + i, 2000 + i);
            stores.add(new SeedStore(code, name, data[i][1], contact));
        }
        return List.copyOf(stores);
    }

    private record SeedAccount(String email, String name, String role) {
    }

    private record SeedStore(String code, String name, String location, String contact) {
    }
}
