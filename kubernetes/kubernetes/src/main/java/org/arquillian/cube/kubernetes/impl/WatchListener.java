package org.arquillian.cube.kubernetes.impl;

import io.fabric8.kubernetes.api.model.v3_1.Container;
import io.fabric8.kubernetes.api.model.v3_1.Event;
import io.fabric8.kubernetes.api.model.v3_1.Pod;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClient;
import io.fabric8.kubernetes.clnt.v3_1.KubernetesClientException;
import io.fabric8.kubernetes.clnt.v3_1.Watch;
import io.fabric8.kubernetes.clnt.v3_1.Watcher;
import io.fabric8.kubernetes.clnt.v3_1.dsl.LogWatch;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.arquillian.cube.impl.util.Strings;
import org.arquillian.cube.kubernetes.api.Configuration;
import org.arquillian.cube.kubernetes.api.Logger;
import org.arquillian.cube.kubernetes.api.Session;
import org.xnio.IoUtils;

public class WatchListener {

    private final Session session;
    private final KubernetesClient client;
    private final Configuration configuration;

    private FileWriter eventLogWriter;
    private String currentClassName;
    private String currentMethodName;

    private final Map<String, Collection<Closeable>> watchersMap = new ConcurrentHashMap<>();
    private Watch watchLog;

    private Watch watchEvents;

    private String logPath;

    WatchListener(Session session, KubernetesClient client, Configuration configuration) {
        this.session = session;
        this.client = client;
        this.configuration = configuration;
    }

    void setupEventListener() {
        final Watcher<Event> watcher = new Watcher<Event>() {
            @Override
            public void eventReceived(Action action, Event event) {
                final Logger logger = session.getLogger();

                final String watcherLogs = String.format("[%s] [%s] [%s:%s]: (%s) %s\n",
                    event.getLastTimestamp(), event.getType(),
                    event.getInvolvedObject().getKind(), event.getInvolvedObject().getName(),
                    event.getReason(), event.getMessage());

                logger.info(watcherLogs);

                switch (action) {
                    case ADDED:
                    case MODIFIED:
                    case DELETED:
                    case ERROR:
                        try {
                            if (configuration.isLogCopyEnabled()) {
                                setupEventLogWriter();
                                eventLogWriter.append(watcherLogs);
                                eventLogWriter.flush();
                            }
                        } catch (IOException e) {
                            throw new RuntimeException("Error storing kubernetes events", e);
                        }
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        };

        watchEvents = client.events().inNamespace(session.getNamespace()).watch(watcher);
    }

    void cleanupEventsListener() {
        if (watchEvents != null) {
            watchEvents.close();
        }

        if (eventLogWriter != null) {
            try {
                eventLogWriter.close();
            } catch (IOException e) {
                session.getLogger().error("Error closing kubernetes events file: " + e);
            }
        }
    }

    void setupConsoleListener() {
        if (!configuration.isLogCopyEnabled()) {
            return;
        }

        logPath = configuration.getLogPath();
        if (Strings.isNullOrEmpty(logPath))
            logPath = String.format("%s/target/surefire-reports", System.getProperty("user.dir"));
        session.getLogger().info(String.format("Storing pods console logs into dir %s", logPath));
        new File(logPath).mkdirs();

        final Watcher<Pod> watcher = new Watcher<Pod>() {
            @Override
            public void eventReceived(Action action, Pod pod) {
                switch (action) {
                    case ADDED:
                    case MODIFIED:
                        if (pod.getStatus().getPhase().equalsIgnoreCase("Running")) {
                            addConsole(pod.getMetadata().getName());
                        }
                        break;
                    case DELETED:
                    case ERROR:
                        delConsole(pod.getMetadata().getName());
                        break;
                }
            }

            @Override
            public void onClose(KubernetesClientException cause) {
            }
        };

        watchLog = client.pods().inNamespace(session.getNamespace()).watch(watcher);
    }

    void cleanupConsoleListener() {
        if (watchLog != null) {
            watchLog.close();
        }
        watchersMap.forEach((k, v) -> {
            IoUtils.safeClose(v.toArray(new Closeable[0]));
        });
        watchersMap.clear();
    }

    private void addConsole(final String podName) {
        if (watchersMap.containsKey(podName))
            return;

        String className = session.getCurrentClassName();
        String methodName = session.getCurrentMethodName();
        String fileName = logPath;

        if (Strings.isNullOrEmpty(className))
            className = "NOCLASS";
        fileName += String.format("/%s", className);

        if (Strings.isNotNullOrEmpty(methodName))
            fileName += String.format("-%s", methodName);

        try {
            Collection<Closeable> fds = new ArrayList<Closeable>();
            List<Container> containers = client.pods().inNamespace(session.getNamespace()).withName(podName).get()
                .getSpec().getContainers();
            if (containers.size() == 1) {
                fileName += String.format("-%s.log", podName);
                final FileOutputStream stream = new FileOutputStream(fileName);
                LogWatch lw = client.pods().inNamespace(session.getNamespace()).withName(podName).watchLog(stream);
                fds.add(lw);
                fds.add(stream);
            } else {
                for (Container container : containers) {
                    String containerName = container.getName();
                    String fileNameContainer = String.format("%s-%s-%s.log", fileName, podName, containerName);
                    final FileOutputStream stream = new FileOutputStream(fileNameContainer);
                    LogWatch lw = client.pods().inNamespace(session.getNamespace()).withName(podName).inContainer(containerName).watchLog(stream);
                    fds.add(lw);
                    fds.add(stream);
                }
            }

            watchersMap.put(podName, fds);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(String.format("Error storing the console log for pod %s", podName), e);
        }

    }

    private void delConsole(String podName) {
        Collection<Closeable> lw = watchersMap.get(podName);
        if (lw == null)
            return;

        watchersMap.remove(podName);
        IoUtils.safeClose(lw.toArray(new Closeable[0]));
    }

    private void setupEventLogWriter() {
        String className = session.getCurrentClassName();
        String methodName = session.getCurrentMethodName();

        if (className != null && className.equals(currentClassName)
            && methodName != null && methodName.equals(currentMethodName))
            return;

        currentClassName = className;
        currentMethodName = methodName;
        String fileName = logPath;

        if (Strings.isNullOrEmpty(className))
            className = "NOCLASS";
        fileName += String.format("/%s", className);

        if (Strings.isNotNullOrEmpty(methodName))
            fileName += String.format("-%s", methodName);
        fileName += "-KUBE_EVENTS.log";

        try {
            if (eventLogWriter != null) {
                eventLogWriter.close();
            }
            eventLogWriter = new FileWriter(fileName, true);
        } catch (IOException e) {
            throw new RuntimeException("Error storing kubernetes events", e);
        }
    }
}
