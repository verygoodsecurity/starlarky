package com.verygood.security.larky.modules.vgs.vault;

import com.verygood.security.larky.modules.VaultModule;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.Starlark;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

// Tests that VaultModule SPI functionality works as expected
public class VaultModuleSPITest {

    // This is the path to VaultModule ServiceLoader config in the test classpath
    // Do not reference src/test/resources/META-INF/services because it is not put in the classpath at runtime,
    // and thus not reference by ServiceLoader
    private static final Path VAULT_CONFIG_PATH = Paths.get(
            "target", "test-classes", "META-INF", "services",
            "com.verygood.security.larky.modules.vgs.vault.spi.LarkyVault"
    );
    private static String VAULT_SAVED_CONFIG;
    private VaultModule vault;

    @BeforeAll
    public static void setUp() throws Exception {
        VAULT_SAVED_CONFIG = getVaultImpl();
    }

    @AfterAll
    public static void tearDown() throws Exception {
        setVaultImpl(VAULT_SAVED_CONFIG);
    }

    @Test
    public void testNoopModule_exception() throws Exception {
        // Setup Noop Vault
        setVaultImpl("");
        System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "false");
        vault = new VaultModule();

        // Assert Exceptions
        Assertions.assertThrows(EvalException.class,
                () -> {
                    vault.redact("fail", Starlark.NONE, Starlark.NONE, null);
                },
                "vault.redact operation must be overridden"
        );
        Assertions.assertThrows(EvalException.class,
                () -> {
                    vault.reveal("fail", Starlark.NONE);
                },
                "vault.reveal operation must be overridden"
        );
    }

    @Test
    public void testDefaultModule_ok() throws Exception {

        // Setup Default Vault through system config
        setVaultImpl("");
        System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "true");
        vault = new VaultModule();

        // Invoke Vault
        String secret = "4111111111111111";
        String alias = (String) vault.redact(secret, Starlark.NONE, Starlark.NONE, null);
        String result = (String) vault.reveal(alias, Starlark.NONE);

        // Assert OK
        Assertions.assertTrue(alias.contains("tok_"));
        Assertions.assertEquals(secret, result);
    }

    @Test
    public void testSPIModule_single_ok() throws Exception {
        // Setup Default Vault through SPI config
        setVaultImpl("com.verygood.security.larky.modules.vgs.vault.defaults.DefaultVault");
        System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "false");
        vault = new VaultModule();

        // Invoke Vault
        String secret = "4111111111111111";
        String alias = (String) vault.redact(secret, Starlark.NONE, Starlark.NONE, null);
        String result = (String) vault.reveal(alias, Starlark.NONE);

        // Assert OK
        Assertions.assertTrue(alias.contains("tok_"));
        Assertions.assertEquals(secret, result);
    }

    @Test
    public void testSPIModule_multiple_exception() throws Exception {

        // Setup multiple vault SPI configs
        setVaultImpl("com.verygood.security.larky.modules.vgs.vault.defaults.DefaultVault\n"
                + "com.verygood.security.larky.modules.vgs.vault.NoopVault\n");
        System.setProperty(VaultModule.ENABLE_INMEMORY_PROPERTY, "false");

        // Assert Exception
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> {
                    vault = new VaultModule();
                },
                "VaultModule expecting only 1 vault provider of type LarkyVault, found 2"
        );
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