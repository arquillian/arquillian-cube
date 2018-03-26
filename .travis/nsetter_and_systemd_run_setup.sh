#!/usr/bin/env bash
if ! which nsenter > /dev/null; then
    echo "Did not find nsenter. Installing it."
    NSENTER_BUILD_DIR=$(mktemp -d /tmp/nsenter-build-XXXXXX)
    pushd ${NSENTER_BUILD_DIR}
    curl -L https://www.kernel.org/pub/linux/utils/util-linux/v2.31/util-linux-2.31.tar.gz | tar -zxf-
    cd util-linux-2.31
    ./configure --without-ncurses
    make nsenter
    sudo cp nsenter /usr/local/bin
    rm -rf "${NSENTER_BUILD_DIR}"
    popd
fi
# OpenShift >= 3.7 requires systemd-run (but Ubuntu Trusty still uses Init)
# Should be in Kubernetes to correctly detect if systemd-run is present -
# Hack it till then
if ! which systemd-run > /dev/null; then
    echo "Did not find systemd-run. Hacking it to work around Kubernetes calling it."
    echo '#!/bin/bash
    echo "all arguments: "$@
    while [[ $# -gt 0 ]]
    do
      key="$1"
      if [[ "${key}" != "--" ]]; then
        shift
        continue
      fi
      shift
      break
    done
    echo "remaining args: "$@
    exec $@' | sudo tee /usr/bin/systemd-run >/dev/null
    sudo chmod +x /usr/bin/systemd-run
fi
