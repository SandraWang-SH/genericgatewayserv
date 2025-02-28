package com.paypal.raptor.aiml.limit;

import com.ebay.kernel.cal.api.CalTransaction;
import com.google.common.util.concurrent.RateLimiter;
import com.paypal.raptor.aiml.common.enums.LimiterParameters;
import com.paypal.raptor.aiml.common.exception.GatewayException;
import com.paypal.raptor.aiml.common.exception.GatewayExceptionHandler;
import com.paypal.raptor.aiml.model.PredictRequest;
import com.paypal.raptor.aiml.utils.GatewayUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.ebay.kernel.cal.api.CalStatus.EXCEPTION;
import static com.ebay.kernel.configuration.ConfigurationContext.ERR_MSG;
import static com.paypal.aiml.common.cal.CalLogHelper.createTransaction;
import static javax.ws.rs.core.Response.Status.TOO_MANY_REQUESTS;

@Slf4j
@Component
@Aspect
public class RateLimitAspect {

    public static final String MODEL_DENIED_BY_LIMITER = "MODEL_DENIED_BY_LIMITER";
    public static final String LIMITER_ERROR_RESP = "The system is busy, please try again later!!!";

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);
    private static Map<String, RateLimiter> rateLimitMap = new ConcurrentHashMap<>();


    @Around("@annotation(com.paypal.raptor.aiml.limit.RateLimit)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Pair<LimiterParameters, String> limiterParametersPair = getLimiterParameters(joinPoint);
        LimiterParameters limiterParameters = limiterParametersPair.getLeft();
        String multiTarget = limiterParametersPair.getRight();
        String key = String.join(":", method.getName(), multiTarget);

        long timeOut = 0L;
        TimeUnit timeUnit = TimeUnit.SECONDS;
        RateLimiter rateLimiter = null;
        if (Objects.nonNull(limiterParameters)) {
            timeOut = limiterParameters.getTimeOut();
            timeUnit = limiterParameters.getTimeUnit();
            rateLimiter = getLimiterByKey(key, limiterParameters.getPermitsPerSecond());
        } else {
            RateLimit limit = method.getAnnotation(RateLimit.class);
            if (Objects.nonNull(limit)) {
                timeOut = limit.timeOut();
                timeUnit = limit.timeunit();
                rateLimiter = getLimiterByKey(key, limit.permitsPerSecond());
            }
        }

        if (Objects.nonNull(rateLimiter)) {
            boolean acquire = rateLimiter.tryAcquire(timeOut, timeUnit);
            if (!acquire) {
                CalTransaction calTransaction = createTransaction(MODEL_DENIED_BY_LIMITER, multiTarget);
                calTransaction.setStatus(EXCEPTION);
                calTransaction.addData(ERR_MSG, LIMITER_ERROR_RESP);
                calTransaction.completed();

                logger.error("Request is denied by limiter, limiter key is {}, ", key);
                return GatewayExceptionHandler.toErrorResponse(new GatewayException(
                        LIMITER_ERROR_RESP, TOO_MANY_REQUESTS));
            }
        }
        return joinPoint.proceed();

    }

    private RateLimiter getLimiterByKey(String key, double permitsPerSecond) {
        RateLimiter rateLimiter = null;
        if (!rateLimitMap.containsKey(key)) {
            rateLimiter = RateLimiter.create(permitsPerSecond);
            rateLimitMap.put(key, rateLimiter);
        }
        return rateLimitMap.get(key);
    }

    private Pair<LimiterParameters, String> getLimiterParameters(ProceedingJoinPoint joinPoint) {
        LimiterParameters limiterParameters;
        String modelProject = "";
        String target = "";
        Object[] methodParameters = joinPoint.getArgs();
        for (Object methodParameter : methodParameters) {
            if (methodParameter instanceof MultipartFormDataInput) {
                target = GatewayUtils.generateCalNameForMultiEndpoint(methodParameter);
            } else if (methodParameter instanceof PredictRequest) {
                modelProject = ((PredictRequest) methodParameter).getProject();
                target = GatewayUtils.generateCalNameForEndpoint(methodParameter);
            }
        }
        limiterParameters = LimiterParameters.getLimiterParameters(modelProject);
        return Pair.of(limiterParameters, target);
    }
}
