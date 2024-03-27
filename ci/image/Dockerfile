FROM --platform=linux/amd64 ubuntu:24.04

# set environment variables for android SDK
ENV ANDROID_SDK_TOOLS_VERSION=11076708
ENV ANDROID_SDK_TOOLS_CHECKSUM=2d2d50857e4eb553af5a6dc3ad507a17adf43d115264b1afc116f95c92e5e258
ENV ANDROID_HOME="/opt/android-sdk-linux"
ENV ANDROID_SDK_ROOT=$ANDROID_HOME
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools:$ANDROID_HOME/cmdline-tools/bin:$ANDROID_HOME/platform-tools

# set environment variable for gradle
ENV GRADLE_VERSION=8.3

ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=en_US.UTF-8
ENV GH_CLI_VERSION=2.23.0
ENV GHTOKEN_VERSION=2.0.1
ENV HOME=/home/mobiledevops

# basic setup and dependencies installation
RUN apt-get -qq update && \
    apt-get -qqy --no-install-recommends install apt-utils build-essential openjdk-17-jdk openjdk-17-jre-headless software-properties-common libssl-dev libffi-dev python3-dev cargo pkg-config libstdc++6 libpulse0 libglu1-mesa openssh-server zip unzip curl lldb git wget > /dev/null && \
    rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*

# install Android SDK
RUN curl -s https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_TOOLS_VERSION}_latest.zip > /tools.zip && \
    echo "${ANDROID_SDK_TOOLS_CHECKSUM} /tools.zip" | sha256sum -c && \
    unzip -qq /tools.zip -d $ANDROID_HOME && \
    rm -v /tools.zip && \
    mkdir -p $ANDROID_HOME/licenses/ && \
    echo -e "8933bad161af4178b1185d1a37fbf41ea5269c55\nd56f5187479451eabf01fb78af6dfcb131a6481e\n24333f8a63b6825ea9c5514f83c2829b004d1fee" > $ANDROID_HOME/licenses/android-sdk-license && \
    echo -e "84831b9409646a918e30573bab4c9c91346d8abd\n504667f4c0de7af1a06de9f4b1727b84351f2910" > $ANDROID_HOME/licenses/android-sdk-preview-license --licenses && \
    yes | $ANDROID_HOME/cmdline-tools/bin/sdkmanager --licenses --sdk_root=${ANDROID_SDK_ROOT}

# setup work directory
WORKDIR $HOME/app

# install SDKMAN and Gradle
RUN curl -s "https://get.sdkman.io" | bash && \
    bash -c "source $HOME/.sdkman/bin/sdkman-init.sh && sdk install gradle ${GRADLE_VERSION}"

# install Google Cloud SDK
RUN curl -sSL https://sdk.cloud.google.com | bash -s -- --disable-prompts --install-dir=${HOME} && \
    echo 'export PATH=$PATH:${HOME}/google-cloud-sdk/bin' >> $HOME/.bashrc

# install gh-token
RUN wget -O ghtoken https://github.com/Link-/gh-token/releases/download/v${GHTOKEN_VERSION}/linux-amd64 && \
    echo "f76e8cb35f0b04b59073a486cc952e50fa9f1c930a25619ea9abcf44a13165c4  ghtoken" | shasum -c - && \
    chmod u+x ./ghtoken && \
    mv ./ghtoken /usr/local/bin/ghtoken

# install GitHub CLI
RUN mkdir ghcli && cd ghcli && \
    wget https://github.com/cli/cli/releases/download/v${GH_CLI_VERSION}/gh_${GH_CLI_VERSION}_linux_386.tar.gz -O ghcli.tar.gz && \
    tar --strip-components=1 -xf ghcli.tar.gz && \
    mv bin/gh /usr/local/bin && cd ../ && rm -rf ./ghcli

# reset work directory
WORKDIR $HOME/app
