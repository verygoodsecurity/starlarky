package com.verygood.security.larky.modules;

import com.google.common.collect.ImmutableList;
import com.verygood.security.larky.modules.vgs.crypto.ChaseCrypto;
import java.util.List;
import java.util.ServiceLoader;
import lombok.SneakyThrows;
import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.StarlarkBytes;

@StarlarkBuiltin(
    name = "chase",
    category = "BUILTIN",
    doc = "The Chase module contains a few methods for fetching a CA signed public JWK" +
          " to serve Chase Bank so they can encrypt the payloads that they send and to" +
          " fetch the subsequent private keys for decryption operations."
)

public class ChaseModule implements ChaseCrypto {

  public static final ChaseModule INSTANCE = new ChaseModule();

  private final ChaseCrypto chaseCrypto;

  ChaseModule() {

    ServiceLoader<ChaseCrypto> loader = ServiceLoader.load(ChaseCrypto.class);
    List<ChaseCrypto> providers = ImmutableList.copyOf(loader.iterator());

    if (providers.isEmpty()) {
      chaseCrypto = new ChaseCrypto() {
        @Override
        public String decrypt(StarlarkBytes jwe_bytes) {
          return new String(jwe_bytes.toByteArray());
        }

        @Override
        public String pan_decrypt(StarlarkBytes jwe_bytes){
          return new String(jwe_bytes.toByteArray());
        }
        
        @Override
        public String getKeys() {
          return "";
        }
      };
    } else {
      if (providers.size() != 1) {
        throw new IllegalArgumentException(String.format(
            "CryptoChaseModule expecting only 1 vault provider of type ChaseCrypto, found %d",
            providers.size()
        ));
      }
      chaseCrypto = providers.get(0);
    }
  }


  @StarlarkMethod(name = "get_keys")
  public String getKeys() {
    return chaseCrypto.getKeys();
  }

  @SneakyThrows
  @StarlarkMethod(name = "decrypt",
      doc = "The decrypt function takes a Compact JWE from Chase, and returns the " +
            "decrypted payload without exposing the private key used for decryption.",
      parameters = {
          @Param(
              name = "jwe",
              allowedTypes = {
                  @ParamType(type = StarlarkBytes.class)
              })
      })
  public String decrypt(StarlarkBytes jweBytes) {
    return chaseCrypto.decrypt(jweBytes);
  }

  @SneakyThrows
  @StarlarkMethod(name = "pan_decrypt",
          doc = "The decrypt function takes a JWE Encrypted PAN from Chase, and returns the " +
                  "decrypted value without exposing the private key used for decryption.",
          parameters = {
                  @Param(
                          name = "jwe",
                          allowedTypes = {
                                  @ParamType(type = StarlarkBytes.class)
                          })
          })
  public String pan_decrypt(StarlarkBytes jweBytes) {
    return chaseCrypto.pan_decrypt(jweBytes);
  }

}
