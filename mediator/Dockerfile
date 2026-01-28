FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre@sha256:62cbd08eb3e57da61f1d6f39d8358035b9eaf90cb65d93d522ed0248a4d8e6a7

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb:NO.UTF-8' TZ="Europe/Oslo"

COPY build/install/*/lib /app/lib

ENTRYPOINT ["java", "-cp", "/app/lib/*", "no.nav.dagpenger.behandling.mediator.AppKt"]
