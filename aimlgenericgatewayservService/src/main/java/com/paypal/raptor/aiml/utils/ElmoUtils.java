package com.paypal.raptor.aiml.utils;

import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.raptor.aiml.common.exception.AbtestException;
import com.paypal.raptor.aiml.model.Audience;
import com.paypal.raptor.aiml.model.RoutingValue;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import java.util.Optional;
import java.util.UUID;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;


/**
 * @author: sheena
 **/
public class ElmoUtils {

	public static void checkExperimentsEmpty(RoutingValue routingValue) throws AbtestException {
		if(CollectionUtils.isEmpty(routingValue.getElmoResource().getExperiments())) {
			throw new AbtestException("Experiment is empty.", NOT_FOUND);
		}
	}

	/**
	 Parse audience value from {@link SecurityContext}.
	 @throws AbtestException if the audience type is not correct or the audience value is empty.
	**/
	public static Audience parseAudienceFromSecurityContext(String audienceType,
								  final Optional<SecurityContext> securityContext) throws AbtestException {
		Audience audience = null;
		String randomStr = UUID.randomUUID().toString();
		// retrieve audience value from SecurityContext
		if (Audience.AudienceType.uid.name().equals(audienceType)) {
			String encryptedAccountNumber = SecurityContextUtils.getEncryptedAccountNumber(securityContext);
			if(StringUtils.isEmpty(encryptedAccountNumber)) {
				encryptedAccountNumber = randomStr;
			}
			audience = new Audience(Audience.AudienceType.uid, encryptedAccountNumber);
		} else if (Audience.AudienceType.account_id.name().equals(audienceType)) {
			String accountNumber = SecurityContextUtils.getAccountNumber(securityContext);
			if(StringUtils.isEmpty(accountNumber)) {
				accountNumber = randomStr;
			}
			audience = new Audience(Audience.AudienceType.account_id, accountNumber);
		} else {
			throw new AbtestException("Unsupported audience type:" + audienceType, BAD_REQUEST);
		}

		if (StringUtils.isEmpty(audience.getAudienceValue())) {
			throw new AbtestException("Audience value is not set in request.", BAD_REQUEST);
		}

		return audience;
	}

}
