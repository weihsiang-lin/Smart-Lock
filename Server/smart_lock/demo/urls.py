from demo.views import CreateUserView

from django.conf.urls import patterns, include, url

urlpatterns = patterns('',
    # Examples:
    # url(r'^$', 'smart_lock.views.home', name='home'),
    # url(r'^blog/', include('blog.urls')),

    url(r'^$', 'demo.views.index'),
    url(r'^entrance/(?P<imei>[^/]+)/$', 'demo.views.entrance'),
    url(r'^logs$', 'demo.views.logs'),
    url(r'^logs/android$', 'demo.views.logs_for_android'),
    url(r'^users$', 'demo.views.users'),
    url(r'^users/android$', 'demo.views.users_for_android'),
    url(r'^users/create/$', CreateUserView.as_view()),
    url(r'^users/enable/(?P<u_id>[^/]+)/', 'demo.views.enable_user'),
    url(r'^users/android/enable/(?P<u_id>[^/]+)/', 'demo.views.enable_user_for_android'),
    url(r'^users/disable/(?P<u_id>[^/]+)/', 'demo.views.disable_user'),
    url(r'^users/android/disable/(?P<u_id>[^/]+)/', 'demo.views.disable_user_for_android'),
    url(r'^users/delete/(?P<u_id>[^/]+)/', 'demo.views.delete_user'),
    url(r'^users/android/delete/(?P<u_id>[^/]+)/', 'demo.views.delete_user_for_android'),
)
