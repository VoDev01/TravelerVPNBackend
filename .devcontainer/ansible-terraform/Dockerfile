FROM debian:bookworm

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-venv \
    python3-pip \
    sshpass \
    git \
    bash \
    wget \
    unzip \
    locales

RUN sed -i '/en_US.UTF-8 UTF-8/s/^# //' /etc/locale.gen && \
    locale-gen

ENV LANG=en_US.UTF-8
ENV LANGUAGE=en_US:en
ENV LC_ALL=en_US.UTF-8

RUN wget https://hashicorp-releases.yandexcloud.net/terraform/1.15.8/terraform_1.15.8_linux_amd64.zip \
    && unzip terraform_1.15.8_linux_amd64.zip \
    && mv terraform /usr/local/bin/ \
    && rm terraform_1.15.8_linux_amd64.zip

RUN python3 -m venv /opt/venv && \
    /opt/venv/bin/pip install --no-cache-dir --upgrade pip && \
    /opt/venv/bin/pip install --no-cache-dir ansible ansible-lint

ENV PATH="/opt/venv/bin:$PATH"

WORKDIR /workspace

CMD ["/bin/bash"]
