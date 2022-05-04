package com.verygood.security.larky.modules.crypto;


import com.nimbusds.jose.crypto.RSADecrypter;
import com.nimbusds.jwt.EncryptedJWT;
import java.io.*;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.MGF1ParameterSpec;
import java.util.Base64;

import javax.crypto.spec.PSource;
import javax.crypto.spec.OAEPParameterSpec;

import net.starlark.java.annot.Param;
import net.starlark.java.annot.ParamType;
import net.starlark.java.annot.StarlarkBuiltin;
import net.starlark.java.annot.StarlarkMethod;
import net.starlark.java.eval.EvalException;
import net.starlark.java.eval.NoneType;
import net.starlark.java.eval.Starlark;
import net.starlark.java.eval.StarlarkBytes;
import net.starlark.java.eval.StarlarkInt;
import net.starlark.java.eval.StarlarkThread;
import net.starlark.java.eval.StarlarkValue;



@StarlarkBuiltin(
	name="Chase",
	category="BUILTIN",
	doc="The Chase module contains a few methods for fetching a CA Signed public JWK" +
		"To serve Chase Bank so they can encrypt the payloads that they send, and to" +
		"Fetch the subsequent private keys for decryption operations."
)

public class CryptoChaseModule implements StarlarkValue {
	public static final CryptoChaseModule INSTANCE = new CryptoChaseModule();

	@StarlarkMethod(name ="get_keys", structField = false)
	public static String get_keys() {
		String public_jwk_chain = "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"www.verygoodsecurity.com\",\"x5t\":\"SFdWU6UCcP4FCUkKwnkz3r5J8KA\",\"alg\":\"RSA-OAEP\",\"use\":\"enc\",\"expires_on\":2222222222,\"n\":\"30Q8U2luwan1SAJYplPgHUVARq7gAFm1Z0-09XueRX1fCeK2TRoTpeWUzGXR52DLAQWHmTTSwcNHzRw20LtCKGh44ksLEe0N0ymJfpqJkhwba9CtUEcGQ8KBcjWd_rCL3nIONA7U594rTcOOlqeRR8edAPvHwMIp2iE9Sh9lhWU4BARWKFVgaDCcEOMMhBGS0AsOSyyiwssBCjKCXE4IqbfmXdk9FarXM6IqrLvgCcQW_co115NFw2DzxPGBpcaYhHoXfArrb3Y0PQ25pyxfC9jJlyhBynK5PhhG2yaWypFx8ckDIum5Puw472nTAHjAaAAiW6i4sRbvSigcIXmITQ\",\"e\":\"AQAB\",\"x5c\":[\"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\"]}]}\"";
		return public_jwk_chain;	
	}

	@StarlarkMethod( name = "decrypt",
		parameters = {
			@Param(
				name = "jwe",
				allowedTypes = {
					@ParamType( type = StarlarkBytes.class)
				})
		})
	public static String decrypt(StarlarkBytes jwe_bytes) throws Exception{
		/*
			The jwe_bytes argument is a larky byte string (b"<jwe>") of a 
			JWE Compact Serialized object, with a RSA-OAEP-256. This fetches 
			the private key internally.
		*/

		String pk = privateKey(); /* When we have multiple keys, add a key_id arg */
		StringBuilder pkcs8Lines = new StringBuilder();
		BufferedReader rdr = new BufferedReader(new StringReader(pk));
		String line;
		while ((line = rdr.readLine()) != null ) {
			pkcs8Lines.append(line);
		}
		String pkcs8Pem = pkcs8Lines.toString();
		pkcs8Pem = pkcs8Pem.replace("-----BEGIN PRIVATE KEY-----", "");
		pkcs8Pem = pkcs8Pem.replace("-----END PRIVATE KEY-----", "");
		pkcs8Pem = pkcs8Pem.replaceAll("\\s+","");

		byte [] pkcs8EncodedBytes = Base64.getDecoder().decode(pkcs8Pem);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(pkcs8EncodedBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");
		PrivateKey privKey = kf.generatePrivate(keySpec);
		String[] jwe_str = jwe_bytes.toString().split("\"");
		String jwe_string = "";
		if(jwe_str.length == 2 && jwe_str[0].equals("b")){
			jwe_string = jwe_str[1];
		}

		EncryptedJWT jwt = EncryptedJWT.parse(jwe_string);
		RSADecrypter decrypter = new RSADecrypter(privKey);
		jwt.decrypt(decrypter);
		return jwt.getJWTClaimsSet().toJSONObject().toString();
	}

	public static PrivateKey getPrivateKey(byte[] keyText) throws Exception {
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyText);
		KeyFactory kf =
				KeyFactory.getInstance("RSA");
		return kf.generatePrivate(spec);
	}

	private static String privateKey(){
		/*
			Right now the key Private key is hard coded.
			In the future, we need to be able to link the private key to 
			a Key ID. Then, this function will take a key_id argument to 
			determine the relevant private key to fetch. 
		*/
		String pk = "-----BEGIN PRIVATE KEY-----\n" +
			/* Private key snipped */
		"-----END PRIVATE KEY-----";

		return pk;
	}

}