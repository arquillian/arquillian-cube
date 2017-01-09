#!/bin/sh

cd /recording/
for f in *.flv; do ffmpeg -i "$f" -crf 18 -preset ultrafast "${f%flv}mp4"; done
<<<<<<< 995f396444bcb54a422c567f79e5e5d89d73fdff
echo "CONVERSION COMPLETED"
=======
echo "CONVERSION COMPLETED"


>>>>>>> Adds Dockerfile for flv-mp4 conversion.
