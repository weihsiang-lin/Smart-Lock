"""
WSGI config for smart_lock project.

It exposes the WSGI callable as a module-level variable named ``application``.

For more information on this file, see
https://docs.djangoproject.com/en/1.6/howto/deployment/wsgi/
"""

import os
import sys
path = os.path.dirname(os.path.dirname(__file__))
if path not in sys.path:
    sys.path.append(path)
os.environ.setdefault("DJANGO_SETTINGS_MODULE", "smart_lock.settings")

from django.core.wsgi import get_wsgi_application
application = get_wsgi_application()
