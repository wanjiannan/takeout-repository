package com.sky.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class RecordTimeAspect {

    @Around("execution(* com.sky.service.impl.*.*(..))")
    public Object recordTime(ProceedingJoinPoint pjp) throws Throwable {
        //记录当前时间
        long begin=System.currentTimeMillis();
        //执行原有业务
        Object proceed = pjp.proceed();
        //记录结束时间
        long end=System.currentTimeMillis();
        log.info("当前程序：{}的运行时间：{}ms",pjp.getSignature(),end-begin);

        return proceed;
    }
}
