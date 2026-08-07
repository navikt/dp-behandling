FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25@sha256:800efded9f5f9443a1c173a0e40b6dba7539512da10340f753c439e97d4f891e

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.mediator.AppKt"]
