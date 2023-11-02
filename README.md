# <img height="25" src="./images/AILLogoSmall.png" width="40"/> mail-service

<a href="https://www.legosoft.com.mx"><img height="150px" src="./images/Icon.png" alt="AI Legorreta" align="left"/></a>
Microservice that acts mail service. It receives Kafka events and transform it to emails.

The `mail-service` depends on existing mail server, could be local or could be in the cloud.
This microservice can be called by two channels: the first channel is an `Event Driven` application (i.e. Kafka), 
the mail server receives events to send and email. The second channel is via REST calls.


## Introduction

The `mail-service` microservice send mails from existing `templates` and a json with data. The preconditions are:

- A `template` must be defined in the `sys-ui` front microservice and stored in `Alfresco`. This templates if used
many times (e.g., receive many message that use in the same `template`) it is kept in memory cache.

- The `json` data that has to be remplaze for the variable declaration inside the `template`.

- A mail server in order to send the emails. It can be a local server (e.g., `James` mail server) or a cloud server.

## How to use it

### Send Events

Send an Event via  **Kafka** with the next json structure:

```json
{
    "template":"template",
    "to": "email to the receiver",
    "subject":"mail subject",
    "body":{
        "key": "value",
        ...
        }
    }
}
```

#### Json

The **Event Request** is a **JSON** with the follow properties:

* **template:** The name of the template stored in `Alfresco.
* **to:** eMail to the receiver.
* **body:** Json structure that conform of the **datasource** defined for this **template**.

*__Example:__*

```json
{
    "template":"carta",
    "to": "rlh@legosoft.com.mx",
    "subject":"Bienvenido a LegoSoft",
    "body":{
      "nombre": "Ricardo",
      "apellido": "Legorreta",
      "días": 4,
      "monto": "345.00",
      "dependientes": {
          "nombre": "Paola Portilla", 
          "direccion": {
            "calle": "Av. Popocatepelt 34",
            "colonia": "Lomas de Bezares"
          }
        }
    }
}
```

#### Process

The `mail-service`microservice receive a **JSON** from the **Kafka** channel or via **REST** and reads the **template**
from the **Alfresco** database. It must exist and no validation is done, since we supposed de `sys-ui` front
microservice did all validations previously to store the template.

It merges the **template** <HTML> file with the **JSON** data. All variables must match and no validation is done, again
we supposed that teh **Kafka** producer or **Rest** caller already dids all data validation.

Is an error exist and the mail is not sent and generate an **ERROR EVENT** via **Kafka**. It is responsability to the 
caller to receive the errors, or at least they al listen by the `audit-service` to store the error.

note: The **template** is kept in memory, is there is a case the next event or REST call came with the same **template**. 
This is for the case when multiple mails are received from the **ingestor-service** microservice with the same 
**template**.


### Param database

This microservice utilizes param database to read the templates.

### Events generated

All events generated to `kafka` and are just mail error events

### Events listener

Listener any event with the topic `mail`

  
## Running on Docker Desktop

### Create the image manually

```
./gradlew bootBuildImage
```

### Publish the image to GitHub manually

```
./gradlew bootBuildImage \
   --imageName ghcr.io/rlegorreta/mail-service \
   --publishImage \
   -PregistryUrl=ghcr.io \
   -PregistryUsername=rlegorreta \
   -PregistryToken=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
```

### Publish the image to GitHub from the IntelliJ

To publish the image to GitHub from the IDE IntelliJ a file inside the directory `.github/workflows/commit-stage.yml`
was created.

To validate the manifest file for kubernetes run the following command:

```
kubeval --strict -d k8s
```

This file compiles de project, test it (for this project is disabled for some bug), test vulnerabilities running
skype, commits the code, sends a report of vulnerabilities, creates the image and lastly push the container image.

<img height="340" src="./images/commit-stage.png" width="550"/>

For detail information see `.github/workflows/commit-stage.yml` file.


### Run the image inside the Docker desktop

```
docker run \
    --net ailegorretaNet \
    -p 8352:8352 \
    -e SPRING_PROFILES_ACTIVE=local \
    pref-service
```

Or a better method use the `docker-compose` tool. Go to the directory `ailegorreta-deployment/docker-platform` and run
the command:

```
docker-compose up
```

## Run inside Kubernetes

### Manually

If we do not use the `Tilt`tool nd want to do it manually, first we need to create the image:

Fist step:

```
./gradlew bootBuildImage
```

Second step:

Then we have to load the image inside the minikube executing the command:

```
image load ailegorreta/mail-service --profile ailegorreta 
```

To verify that the image has been loaded we can execute the command that lists all minikube images:

```
kubectl get pods --all-namespaces -o jsonpath="{..image}" | tr -s '[[:space:]]' '\n' | sort | uniq -c\n
```

Third step:

Then execute the deployment defined in the file `k8s/deployment.yml` with the command:

```
kubectl apply -f k8s/deployment.yml
```

And after the deployment can be deleted executing:

```
kubectl apply -f k8s/deployment.yml
```

Fourth step:

For service discovery we need to create a service applying with the file: `k8s/service.yml` executing the command:

```
kubectl apply -f k8s/service.yml
```

And after the process we can delete the service executing:

```
kubectl deltete -f k8s/service.yml
```

Fifth step:

If we want to use the project outside kubernetes we have to forward the port as follows:

```
kubectl port-forward service/config-service 8352:80
```

Appendix:

If we want to see the logs for this `pod` we can execute the following command:

```
kubectl logs deployment/mail-service
```

### Using Tilt tool

To avoid all these boilerplate steps is much better and faster to use the `Tilt` tool as follows: first create see the
file located in the root directory of the project called `TiltFile`. This file has the content:

```
# Tilt file for mail-service
# Build
custom_build(
    # Name of the container image
    ref = 'mail-service',
    # Command to build the container image
    command = './gradlew bootBuildImage --imageName $EXPECTED_REF',
    # Files to watch that trigger a new build
    deps = ['build.gradle', 'src']
)

# Deploy
k8s_yaml(['k8s/deployment.yml', 'k8s/service.yml'])

# Manage
k8s_resource('config-service', port_forwards=['8352'])
```

To execute all five steps manually we just need to execute the command:

```
tilt up
```

In order to see the log of the deployment process please visit the following URL:

```
http://localhost:10350
```

Or execute outside Tilt the command:

```
kubectl logs deployment/mail-service
```

In order to undeploy everything just execute the command:

```
tilt down
```

To run inside a docker desktop the microservice need to use http://mail-service:8352 to 8352 path


### Reference Documentation

* [Spring Boot Gateway](https://cloud.spring.io/spring-cloud-gateway/reference/html/)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.0.1/maven-plugin/reference/html/)
* [Config Client Quick Start](https://docs.spring.io/spring-cloud-config/docs/current/reference/html/#_client_side_usage)
* [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/3.0.1/reference/htmlsingle/#production-ready)

### Links to Springboot 3 Observability

https://tanzu.vmware.com/developer/guides/observability-reactive-spring-boot-3/

Baeldung:

https://www.baeldung.com/spring-boot-3-observability



### Contact AI Legorreta

Feel free to reach out to AI Legorreta on [web page](https://legosoft.com.mx).


Version: 2.0.0
©LegoSoft Soluciones, S.C., 2023
