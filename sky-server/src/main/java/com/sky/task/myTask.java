package com.sky.task;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@Slf4j
public class myTask {
    /**
     * 自定义定时任务类
     */

//    @Scheduled(cron = "0/5 * * * * ?")
    public void excuteTask(){
        log.info("定时任务开始执行：{}",new Date());
    }
}
