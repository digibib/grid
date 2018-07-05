FROM java:8
MAINTAINER Oslo Public Library opl@deichman.no

ENV SCALA_HOME /usr/local/share/scala

RUN curl -sL https://deb.nodesource.com/setup_8.x | bash -

RUN apt-get -y update && apt-get install -y awscli nodejs build-essential libgd-tools graphicsmagick imagemagick pngquant exiftool nginx jq &&\
  apt-get clean

RUN wget http://downloads.lightbend.com/scala/2.11.8/scala-2.11.8.tgz && \
  tar xvzf scala-2.11.8.tgz && \
  mv scala-2.11.8 $SCALA_HOME

RUN wget https://dl.bintray.com/sbt/native-packages/sbt/0.13.11/sbt-0.13.11.tgz && \
  tar xvzf sbt-0.13.11.tgz && \
  mv sbt $SCALA_HOME/sbt && \
  $SCALA_HOME/sbt/bin/sbt

ENV PATH $PATH:$SCALA_HOME/bin:$SCALA_HOME/sbt/bin

# keepalive script for sbt apps to stay alive
RUN echo "while [ true ]; do sleep 1; done" > /keep-alive && chmod +x /keep-alive

WORKDIR /opt
COPY . /opt
RUN cd kahuna && bash ./setup.sh && bash ./dist.sh
RUN sbt compile

ADD ./entrypoint.sh /entrypoint.sh
