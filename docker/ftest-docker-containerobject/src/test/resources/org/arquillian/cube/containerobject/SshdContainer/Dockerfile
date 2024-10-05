FROM ubuntu

RUN apt-get update
RUN apt-get install -y openssh-server
RUN mkdir -p /var/run/sshd

# create a test user
RUN useradd test -d /home/test && \
    mkdir -p /home/test/.ssh && \
    echo 'ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQDMQ6XuOyuzQ+MhypKDSXmc8vXhXqQ7Lz+x4bMPL3nkv1tllbC577P39H/gUwsKSjywdnFV6imZwp3tqTlgfhszYVWEsH+ZnXvpxioMEQP4Z5S6aho0yYjL1I10Gv2M0oMN5DEFBKHllJJeL59XzJSlo0pi5CYH5g2LFd8iVzzQlasNsu0Y+I+/eaghuEL3RCznU5HeMlOcvYcHFuH9ZGLATgcihkEQV/EjozLmyzDIbNvugt+3zrAlYqktokd1OMBSC39a3o/ZCk11qKQYRStAIq4/dvcj38dKn7E2zoTXlkTTlxVAXzkWTDx58pVmorhYMA20+qU7zWnx4aXLF4mN12mPNLL7mCvSLZbz5Wcx0G1Wf+YDwsepXNkUkYC8aA/0T2fQYTtHANH0pqRl5fiSb1Jsxbs0vVCCQbqKor13OcHVnISV8ygcIBSyApN0xjAD35VTiiRMIwjNdTLzGZYYq8IvQ/7sxbWB3+Hzs4fkX9gJK52KByuLjV+2cbAe7T8= login@example.com' > /home/test/.ssh/authorized_keys && \
    chown -R test:test /home/test && \
    chmod 0600 /home/test/.ssh/authorized_keys && \
    echo "test:test" | chpasswd


ENTRYPOINT ["/usr/sbin/sshd", "-D", "-e"]
