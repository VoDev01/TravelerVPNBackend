FROM debian:bookworm

RUN apt-get update && apt-get install -y git wget unzip

RUN wget https://hashicorp-releases.yandexcloud.net/terraform/1.15.8/terraform_1.15.8_linux_amd64.zip \
    && unzip terraform_1.15.8_linux_amd64.zip \
    && mv terraform /usr/local/bin/ \
    && rm terraform_1.15.8_linux_amd64.zip