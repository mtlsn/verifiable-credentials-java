package com.danubetech.verifiablecredentials.w3ctestsuite;

import com.danubetech.verifiablecredentials.VerifiableCredential;
import com.danubetech.verifiablecredentials.jwt.FromJwtConverter;
import com.danubetech.verifiablecredentials.jwt.JwtVerifiableCredential;
import com.danubetech.verifiablecredentials.jwt.JwtVerifiablePresentation;
import com.danubetech.verifiablecredentials.jwt.ToJwtConverter;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.JSONObjectUtils;
import org.apache.commons.codec.binary.Base64;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.security.GeneralSecurityException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class Generator {

	public static void main(String[] args) throws Exception {

		// command line args

		List<String> argsList = Arrays.asList(args);
		String argJwt = argJwt(argsList);
		String argAud = argAud(argsList);
		boolean argNoJws = argNoJws(argsList);
		boolean argPresentation = argPresentation(argsList);
		boolean argDecode = argDecode(argsList);
		String argInput = argInput(argsList);

		if (argInput == null) throw new NullPointerException();

		// input file

		String input = readInput(new File(argInput));

		if (input == null || input.trim().length() < 1) throw new NullPointerException();

		// do the work

		try {

			String output = null;

			if (argJwt == null) {

				VerifiableCredential verifiableCredential = VerifiableCredential.fromJson(input);

				output = verifiableCredential.toJson();
			} else {

				RSAKey rsaKey = readRSAKey(argJwt);

				if (argDecode) {

					JwtVerifiableCredential jwtVerifiableCredential = JwtVerifiableCredential.fromCompactSerialization(input);
					if (! jwtVerifiableCredential.verify_RSA_RS256(rsaKey.toPublicJWK())) throw new GeneralSecurityException("Invalid signature.");

					VerifiableCredential verifiableCredential = FromJwtConverter.fromJwtVerifiableCredential(jwtVerifiableCredential);

					output = verifiableCredential.toJson();
				} else {

					VerifiableCredential verifiableCredential = VerifiableCredential.fromJson(input);
					JwtVerifiableCredential jwtVerifiableCredential = ToJwtConverter.toJwtVerifiableCredential(verifiableCredential, argAud);

					if (argPresentation) {

						jwtVerifiableCredential.sign_RSA_RS256(rsaKey);

						JwtVerifiablePresentation jwtVerifiablePresentation = JwtVerifiablePresentation.fromJwtVerifiableCredential(jwtVerifiableCredential, argAud);
						output = jwtVerifiablePresentation.sign_RSA_RS256(rsaKey);
					} else {

						if (argNoJws) {

							output = JSONObjectUtils.toJSONString(jwtVerifiableCredential.getPayload().toJSONObject());
						} else {

							output = jwtVerifiableCredential.sign_RSA_RS256(rsaKey);
						}
					}
				}
			}

			System.out.println(output);
		} catch (Exception ex) {

			System.err.println(ex.getMessage());
			ex.printStackTrace(System.err);
		}
	}

	/*
	 * Helper methods
	 */

	static String argJwt(List<String> argsList) {

		int pos = argsList.indexOf("--jwt");
		if (pos == -1) return null;
		return argsList.get(pos+1);
	}

	static String argAud(List<String> argsList) {

		int pos = argsList.indexOf("--jwt-aud");
		if (pos == -1) return null;
		return argsList.get(pos+1);
	}

	static boolean argNoJws(List<String> argsList) {

		return argsList.contains("--jwt-no-jws");
	}

	static boolean argPresentation(List<String> argsList) {

		return argsList.contains("--jwt-presentation");
	}

	static boolean argDecode(List<String> argsList) {

		return argsList.contains("--jwt-decode");
	}

	static String argInput(List<String> argsList) {

		return argsList.get(argsList.size()-1);
	}

	static RSAKey readRSAKey(String jwt) throws ParseException, JOSEException {

		Map<String, Object> jsonObject = JSONObjectUtils.parse(new String(Base64.decodeBase64(jwt)));
		Map<String, Object> rs256PrivateKeyJwk = (Map<String, Object>) jsonObject.get("rs256PrivateKeyJwk");

		RSAKey jwk = (RSAKey) JWK.parse(rs256PrivateKeyJwk);

		return jwk;
	}

	static String readInput(File input) throws Exception {

		BufferedReader reader = new BufferedReader(new FileReader(input));
		StringBuffer buffer = new StringBuffer();

		String line;
		while ((line = reader.readLine()) != null) buffer.append(line);

		reader.close();

		return buffer.toString();
	}
}
