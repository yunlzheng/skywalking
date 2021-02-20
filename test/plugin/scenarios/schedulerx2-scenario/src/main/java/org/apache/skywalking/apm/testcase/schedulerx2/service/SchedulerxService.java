package org.apache.skywalking.apm.testcase.schedulerx2.service;

import com.alibaba.schedulerx.worker.domain.JobContext;
import org.apache.skywalking.apm.testcase.schedulerx2.job.HelloWorldJob;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author yunlong
 */
@Service
public class SchedulerxService {

    @Autowired
    private HelloWorldJob helloWorldJob;

    public void schedule() throws Exception {
        JobContext build = JobContext.newBuilder()
                .setJobName("hello")
                .setJobParameters("params")
                .setScheduleTime(new DateTime())
                .setDataTime(new DateTime())
                .build();
        helloWorldJob.process(build);
    }

}
