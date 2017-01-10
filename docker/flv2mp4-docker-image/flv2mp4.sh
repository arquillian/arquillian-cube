#!/bin/sh

cd /recording/
for f in *.flv; do ffmpeg -i "$f" -crf 18 -preset ultrafast "${f%flv}mp4"; done
echo "CONVERSION COMPLETED"