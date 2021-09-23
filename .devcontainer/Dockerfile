# See here for image contents: https://github.com/microsoft/vscode-dev-containers/tree/v0.194.0/containers/debian/.devcontainer/base.Dockerfile

# [Choice] Debian version: bullseye, buster, stretch
ARG VARIANT="buster"
FROM mcr.microsoft.com/vscode/devcontainers/base:0-${VARIANT}

COPY --from=openjdk:8-jdk-slim-buster /usr/local/openjdk-8 /usr/local/openjdk-8
COPY --from=openjdk:11-jdk-slim-buster /usr/local/openjdk-11 /usr/local/openjdk-11
COPY --from=openjdk:17-jdk-slim-buster /usr/local/openjdk-17 /usr/local/openjdk-17
COPY --from=openjdk:18-jdk-slim-buster /usr/local/openjdk-18 /usr/local/openjdk-18

ENV JAVA_8_HOME="/usr/local/openjdk-8"
ENV JAVA_11_HOME="/usr/local/openjdk-11"
ENV JAVA_17_HOME="/usr/local/openjdk-17"
ENV JAVA_18_HOME="/usr/local/openjdk-18"
ENV JAVA_HOME="${JAVA_11_HOME}"
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN wget -O java/dd-java-agent.jar https://dtdg.co/latest-java-tracer

# ** [Optional] Uncomment this section to install additional packages. **
# RUN apt-get update && export DEBIAN_FRONTEND=noninteractive \
#  && apt-get -y install jq

