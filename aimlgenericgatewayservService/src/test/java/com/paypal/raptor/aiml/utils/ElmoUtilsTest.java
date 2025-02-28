package com.paypal.raptor.aiml.utils;

import static com.paypal.raptor.aiml.utils.ElmoUtils.parseAudienceFromSecurityContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.Test;
import org.testng.Assert;
import com.paypal.platform.auth.securitycontext.SecurityContext;
import com.paypal.platform.auth.securitycontext.User;
import com.paypal.raptor.aiml.model.Audience;
import com.paypal.raptor.aiml.model.ElmoResource;



/**
 * @author: sheena
 **/
public class ElmoUtilsTest {

	@Test
	public void parseAudienceValTest() {

		ElmoResource.ElmoExperiment experiment = new ElmoResource.ElmoExperiment();
		experiment.setName("cosmos_ai_model_a_exp_v2");
		experiment.setAudienceType(Audience.AudienceType.account_id.name());
		List<String> treatments = new ArrayList<>();
		treatments.add("Ctrl_cosmos_ai_model_a_exp_v2");
		treatments.add("Trmt_cosmos_ai_model_a_exp_v2");
		experiment.setTreatments(treatments);

		User user = new User();
		user.setEncryptedAccountNumber("bbb");
		user.setAccountNumber("1154635410118335931");

		SecurityContext securityContext = new SecurityContext();
		securityContext.setActor(user);
		Optional<SecurityContext> securityContextOptional = Optional.of(securityContext);

		Audience result = parseAudienceFromSecurityContext(experiment.getAudienceType(), securityContextOptional);
		Assert.assertNotNull(result);
	}
}
