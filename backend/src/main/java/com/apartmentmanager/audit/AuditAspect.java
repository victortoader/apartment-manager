package com.apartmentmanager.audit;

import com.apartmentmanager.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditService auditService;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Object result = pjp.proceed();

        if (result instanceof ResponseEntity<?> response && !response.getStatusCode().is2xxSuccessful()) {
            return result;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : "SYSTEM";
        String role = resolveRole(auth);
        String message = resolveMessage(pjp, audited, unwrapResponse(result));

        auditService.log(username, role, audited.action(), message, null);
        return result;
    }

    private Object unwrapResponse(Object result) {
        if (result instanceof ResponseEntity<?> response) {
            return response.getBody();
        }
        return result;
    }

    private String resolveRole(Authentication auth) {
        if (auth != null) {
            for (GrantedAuthority authority : auth.getAuthorities()) {
                if (authority.getAuthority().startsWith("ROLE_")) {
                    return authority.getAuthority().substring("ROLE_".length());
                }
            }
        }
        return "UNKNOWN";
    }

    private String resolveMessage(ProceedingJoinPoint pjp, Audited audited, Object result) {
        String template = audited.message();
        if (template.indexOf('{') < 0) {
            return template;
        }

        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        EvaluationContext context = new StandardEvaluationContext();
        Parameter[] parameters = method.getParameters();
        Object[] args = pjp.getArgs();
        for (int i = 0; i < parameters.length; i++) {
            context.setVariable(parameters[i].getName(), i < args.length ? args[i] : null);
        }
        context.setVariable("result", result);

        StringBuilder sb = new StringBuilder();
        int cursor = 0;
        while (cursor < template.length()) {
            int open = template.indexOf('{', cursor);
            if (open < 0) {
                sb.append(template, cursor, template.length());
                break;
            }
            int close = template.indexOf('}', open);
            if (close < 0) {
                sb.append(template, cursor, template.length());
                break;
            }
            sb.append(template, cursor, open);
            String expression = template.substring(open + 1, close);
            if (!expression.startsWith("#")) {
                expression = "#" + expression;
            }
            Object value = parser.parseExpression(expression).getValue(context);
            sb.append(value != null ? value : "null");
            cursor = close + 1;
        }
        return sb.toString();
    }
}
