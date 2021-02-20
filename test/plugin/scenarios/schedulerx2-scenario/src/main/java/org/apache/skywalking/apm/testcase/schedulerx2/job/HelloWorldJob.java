package org.apache.skywalking.apm.testcase.schedulerx2.job;

import com.alibaba.schedulerx.worker.domain.JobContext;
import com.alibaba.schedulerx.worker.log.LogFactory;
import com.alibaba.schedulerx.worker.log.Logger;
import com.alibaba.schedulerx.worker.processor.JavaProcessor;
import com.alibaba.schedulerx.worker.processor.ProcessResult;
import org.springframework.stereotype.Component;

/**
 * @author yunlong
 */
@Component
public class HelloWorldJob extends JavaProcessor {
    private static final Logger LOGGER = LogFactory.getLogger("data");

    @Override
    public ProcessResult process(JobContext context) throws Exception {
        LOGGER.info("jobName={}, parameter={}, scheduleTime={}, dataTime={}", context.getJobName(),
                context.getJobParameters(), context.getScheduleTime().toString("yyyy-MM-dd HH:mm:ss"),
                context.getDataTime().toString("yyyy-MM-dd HH:mm:ss"));
        Thread.sleep(5000);
        return new ProcessResult(true);
    }

}