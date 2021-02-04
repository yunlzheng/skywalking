/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.receiver.envoy;

import com.google.protobuf.TextFormat;
import io.envoyproxy.envoy.data.accesslog.v3.HTTPAccessLogEntry;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.skywalking.apm.network.common.v3.DetectPoint;
import org.apache.skywalking.apm.network.logging.v3.JSONLog;
import org.apache.skywalking.apm.network.logging.v3.LogData;
import org.apache.skywalking.apm.network.logging.v3.LogDataBody;
import org.apache.skywalking.apm.network.servicemesh.v3.ServiceMeshMetric;
import org.apache.skywalking.oap.log.analyzer.module.LogAnalyzerModule;
import org.apache.skywalking.oap.log.analyzer.provider.log.ILogAnalyzerService;
import org.apache.skywalking.oap.server.library.module.ModuleManager;

import static org.apache.skywalking.apm.util.StringUtil.isNotBlank;
import static org.apache.skywalking.oap.server.library.util.ProtoBufJsonUtils.toJSON;

/**
 * {@code ErrorLogsAnalyzer} analyzes the error logs and persists them to the log system.
 */
@Slf4j
public class ErrorLogsAnalyzer {
    private final ILogAnalyzerService logAnalyzerService;

    public ErrorLogsAnalyzer(final ModuleManager manager) {
        logAnalyzerService = manager.find(LogAnalyzerModule.NAME)
                                    .provider()
                                    .getService(ILogAnalyzerService.class);
    }

    public void analyze(final List<ServiceMeshMetric.Builder> result,
                        final HTTPAccessLogEntry logEntry) {
        result.stream()
              .filter(this::hasError)
              .findFirst()
              .ifPresent(metrics -> {
                  try {
                      final LogData logData = convertToLogData(logEntry, metrics);
                      logAnalyzerService.doAnalysis(logData);
                  } catch (IOException e) {
                      log.error(
                          "Failed to parse error log entry to log data: {}",
                          TextFormat.shortDebugString(logEntry),
                          e
                      );
                  }
              });
    }

    public LogData convertToLogData(final HTTPAccessLogEntry logEntry,
                                    final ServiceMeshMetric.Builder metrics) throws IOException {
        final boolean isServerSide = metrics.getDetectPoint() == DetectPoint.server;
        final String svc = isServerSide ? metrics.getDestServiceName() : metrics.getSourceServiceName();
        final String svcInst = isServerSide ? metrics.getDestServiceInstance() : metrics.getSourceServiceInstance();

        return LogData
            .newBuilder()
            .setService(svc)
            .setServiceInstance(svcInst)
            .setEndpoint(metrics.getEndpoint())
            .setTimestamp(metrics.getEndTime())
            .setBody(
                LogDataBody
                    .newBuilder()
                    .setJson(
                        JSONLog
                            .newBuilder()
                            .setJson(toJSON(logEntry))
                    )
            )
            .build();
    }

    private boolean hasError(final ServiceMeshMetric.Builder it) {
        return isNotBlank(it.getInternalErrorCode());
    }
}
