package com.verygood.security.larky.modules.vgs.vault;

import com.verygood.security.larky.console.testing.TestingConsole;
import com.verygood.security.larky.modules.VaultModule;
import net.starlark.java.eval.EvalException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class VaultModuleTest {

    // This is the path to VaultModule ServiceLoader config in the test classpath
    // Do not reference src/test/resources/META-INF/services because it is not put in the classpath at runtime,
    // and thus not reference by ServiceLoader
    private static final Path VAULT_CONFIG_PATH = Paths.get(
            "target","test-classes", "META-INF", "services",
            "com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault"
    );
    private static String VAULT_SAVED_CONFIG;
    private VaultModule vault;

    private static final TestingConsole console = new TestingConsole();

    @BeforeAll
    public static void setUp() throws Exception {
        VAULT_SAVED_CONFIG = getVaultImpl();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        setVaultImpl(VAULT_SAVED_CONFIG);
    }

    @BeforeEach
    public void setUpEach() throws Exception {
        setVaultImpl("");
        System.setProperty(VaultModule.PROPERTY_NAME,"false");
    }

    @Test
    public void testNoopModule_put_exception() throws Exception {
        vault = new VaultModule();

        Assertions.assertThrows(EvalException.class, () -> {
            vault.put("fail", null, null);
        });
    }

    @Test
    public void testNoopModule_get_exception() throws Exception {
        vault = new VaultModule();

        Assertions.assertThrows(EvalException.class, () -> {
            vault.get("fail", null);
        });
    }

    @Test
    public void testDefaultModule_ok() throws Exception {
        System.setProperty(VaultModule.PROPERTY_NAME,"true");
        vault = new VaultModule();

        String secret = "passing";
        String token = (String) vault.put(secret, null, null);
        String result = (String) vault.get(token, null);

        Assertions.assertEquals(result, secret);

    }

    @Test
    public void testSPIModule_single_ok() throws Exception {
        setVaultImpl("com.verygood.security.larky.modules.vgs.vault.DefaultVault");
        vault = new VaultModule();

        String secret = "4111111111111111";
        String token = (String) vault.put(secret, null, null);
        String result = (String) vault.get(token, null);

        Assertions.assertEquals(token, "tok_1537796765");
        Assertions.assertEquals(result, secret);
    }

    @Test
    public void testSPIModule_multiple_exception() throws Exception {
        setVaultImpl("com.verygood.security.larky.modules.vgs.vault.DefaultVault\n"
                + "com.verygood.security.larky.modules.vgs.vault.NoopVault\n");

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            vault = new VaultModule();
        });
    }

    private static void setVaultImpl(String implementationURI) throws Exception {
        Files.write(
                VAULT_CONFIG_PATH,
                implementationURI.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String getVaultImpl() throws Exception {
        return new String(Files.readAllBytes(VAULT_CONFIG_PATH));
    }
}