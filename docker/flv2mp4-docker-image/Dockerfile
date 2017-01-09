FROM centos:7

MAINTAINER Hemani Katyal <hkatyal@redhat.com>

RUN yum install epel-release -y && yum update -y && \ 
    rpm --import http://li.nux.ro/download/nux/RPM-GPG-KEY-nux.ro && \
    rpm -Uvh http://li.nux.ro/download/nux/dextop/el7/x86_64/nux-dextop-release-0-5.el7.nux.noarch.rpm && \
    yum install ffmpeg ffmpeg-devel -y

ADD flv2mp4.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/flv2mp4.sh
CMD ["/usr/local/bin/flv2mp4.sh"]
