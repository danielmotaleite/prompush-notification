
## Description

This is a Rundeck Notification plugin that will send a message to an Prometheus Gateway.

[![Build Status](https://travis-ci.org/marcelosousaalmeida/prompush-notification.svg)](https://travis-ci.org/marcelosousaalmeida/prompush-notification)

## Build / Deploy

To build the project from source, run: `gradle build`. The resulting jar will be found in `build/libs`.

Copy the  jar to Rundeck plugins directory. For example, on an RPM installation:

    cp build/libs/prompush-notification-1.0.0.jar /var/lib/rundeck/libext

or for a launcher:

    cp build/libs/prompush-notification-1.0.0.jar $RDECK_BASE/libext

## Usage

To use the plugin, configure your job to send a notification for on start, success or failure.
There are just two configuration properties for the plugin:

* pushGwUrl: URL of Prometheus Gateway that you want to send notifications (Ex:. http://172.16.0.2:9091).
* pushGwJobName: The name for job label (Default: "rundeck")

The example job below sends a notification on start:

```YAML
- name: Prometheus Pushgateway job
  description: This is a example job.
  project: examples
  loglevel: INFO
  multipleExecutions: true
  sequence:
    keepgoing: false
    strategy: node-first
    commands:
    - script: |-
        #!/bin/bash
        hostname
  notification:
  onfailure:
    plugin:
      configuration:
        pushGwJobName: rundeck
        pushGwUrl: http://172.16.0.2:9091
      type: PromPushNotification
  onstart:
    plugin:
      configuration:
        pushGwJobName: rundeck
        pushGwUrl: http://172.16.0.2:9091
      type: PromPushNotification
  onsuccess:
    plugin:
      configuration:
        pushGwJobName: rundeck
        pushGwUrl: http://172.16.0.2:9091
      type: PromPushNotification
```

## Troubleshooting

Output from the Prometheus Pushgateway plugin can be found in Rundeck's service.log.

## License

MIT. See the `LICENSE` file for more information.
