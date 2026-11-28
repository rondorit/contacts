#!/bin/sh
# Regenerates user-guide.pdf from user-guide.html (headless Chrome).
cd "$(dirname "$0")" && google-chrome --headless --no-sandbox --disable-gpu --no-pdf-header-footer \
  --print-to-pdf=user-guide.pdf "file://$PWD/user-guide.html"
