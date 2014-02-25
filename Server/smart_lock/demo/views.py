import os
import datetime

from demo.models import User, EntranceLog, MyDevice
from gcm.models import get_device_model # Google Cloud Messaging

from django.core import serializers
from django.core.paginator import Paginator, EmptyPage, PageNotAnInteger
from django.http import HttpResponse, HttpResponseRedirect
from django.shortcuts import redirect, render_to_response
from django.views.generic import TemplateView

# Create your views here.
def index(request):
    #print os.path.dirname(os.path.dirname(__file__))
    return render_to_response('index.html')

def logs(request):
    query = EntranceLog.objects.all().order_by('-id')
    paginator = Paginator(query, 20)

    page = request.GET.get('page')
    try:
        logs = paginator.page(page)
    except PageNotAnInteger:
        # If page is not an integer, deliver first page.
        logs = paginator.page(1)
    except EmptyPage:
        # If page is out of range (e.g. 9999), deliver last page of results.
        logs = paginator.page(paginator.num_pages)
    return render_to_response('logs.html', {'logs': logs})

def logs_for_android(request):
    query = EntranceLog.objects.all().order_by('-id')
    json_logs = serializers.serialize('json', query, use_natural_keys=True)
    return HttpResponse(json_logs, content_type='application/json')

def users(request):
    users = User.objects.all().order_by('-id')
    return render_to_response('users.html', {'users': users})

def users_for_android(request):
    query = User.objects.all()
    json_users = serializers.serialize('json', query)
    return HttpResponse(json_users, content_type='application/json')

class CreateUserView(TemplateView):
    template_name = 'create.html'
    def get_context_data(self, **kwargs):
        request = self.request
        return {}
    def post(self, request, *args, **kwargs):
        name = request.POST.get('name')
        imei = request.POST.get('imei')
        enabled = request.POST.get('enabled')
        administrator = request.POST.get('administrator')

        auth = 0
        admin = 0
        if enabled:
            auth = 1

        if administrator:
            admin = 1
            mydevice = MyDevice(name=name, dev_id=imei)
            mydevice.save()

        insert = User(name=name, imei=imei, auth=auth, admin=admin)
        insert.save()
        return redirect('/demo/users')

def enable_user(request, u_id):
    user = User.objects.get(id=u_id)
    user.auth = 1
    user.save()
    return HttpResponseRedirect('/demo/users')

def enable_user_for_android(request, u_id):
    user = User.objects.get(id=u_id)
    user.auth = 1
    user.save()
    return HttpResponseRedirect('/demo/users/android')

def disable_user(request, u_id):
    user = User.objects.get(id=u_id)
    user.auth = 0
    user.save()
    return HttpResponseRedirect('/demo/users')

def disable_user_for_android(request, u_id):
    user = User.objects.get(id=u_id)
    user.auth = 0
    user.save()
    return HttpResponseRedirect('/demo/users/android')

def delete_user(request, u_id):
    user = User.objects.get(id=u_id)
    mydevice = MyDevice.objects.get(dev_id=user.imei)
    mydevice.delete()
    user.delete()
    return HttpResponseRedirect('/demo/users')

def delete_user_for_android(request, u_id):
    user = User.objects.get(id=u_id)
    mydevice = MyDevice.objects.get(dev_id=user.imei)
    mydevice.delete()
    user.delete()
    return HttpResponseRedirect('/demo/users/android')

def entrance(request, imei):
    user = User.objects.get(imei=imei)
    admins = User.objects.filter(admin=1).filter(auth=1)
    timestamp = datetime.datetime.today()
    if user.auth:
        entrance_log = EntranceLog(user_id=user.id, time=timestamp)
        entrance_log.save()
        for admin in admins:
            print "IMEI: %s " % admin.imei
            notification = get_device_model().objects.get(dev_id=admin.imei)
            notification.send_message("%s went home." % user.name)
        return HttpResponse("Hello, %s." % user.name)
    else:
        entrance_log = EntranceLog(user_id='1', time=timestamp)
        entrance_log.save()
        for admin in admins:
            notification = get_device_model().objects.get(dev_id=admin.imei)
            notification.send_message("Someone tried to invade your home.")
        return HttpResponse("You are NOT authorized to enter.")
