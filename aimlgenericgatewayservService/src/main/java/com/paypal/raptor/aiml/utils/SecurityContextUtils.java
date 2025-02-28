package com.paypal.raptor.aiml.utils;

import java.util.Optional;
import org.apache.logging.log4j.util.Strings;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.platform.auth.securitycontext.User;


/**
 * @author: qingwu
 **/
public class SecurityContextUtils {

	public static boolean isSecurityContextEmpty(Optional<SecurityContext> optionalSecurityContext) {
		return optionalSecurityContext==null || !optionalSecurityContext.isPresent() || optionalSecurityContext.get().getActor() == null || optionalSecurityContext.get().getSubjects() == null;
	}

	public static String getEncryptedAccountNumber(final Optional<SecurityContext> securityContext) {
		if(!isSecurityContextEmpty(securityContext)){
			return securityContext.map(SecurityContext::getActor)
							.map(User::getEncryptedAccountNumber)
							.orElse(Strings.EMPTY);
		}
		return Strings.EMPTY;
	}

	public static String getAccountNumber(final Optional<SecurityContext> securityContext) {
		if(!isSecurityContextEmpty(securityContext)){
			return securityContext.map(SecurityContext::getActor)
					.map(User::getAccountNumber)
					.orElse(Strings.EMPTY);
		}
		return Strings.EMPTY;
	}

}
