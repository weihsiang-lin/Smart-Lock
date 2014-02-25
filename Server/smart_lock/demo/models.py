from django.db import models
from gcm.models import AbstractDevice

# Create your models here.
class UserManager(models.Manager):
    def get_by_natural_key(self, name):
        return self.get(name=name)

class User(models.Model):
    objects = UserManager()

    name = models.CharField(max_length=64, unique=True)
    imei = models.CharField(max_length=15)
    auth = models.IntegerField()
    admin = models.IntegerField()

    def natural_key(self):
        return (self.name,)

    #class Meta:
    #    unique_together = (('name'),)

class EntranceLog(models.Model):
    user = models.ForeignKey(User)
    time = models.DateTimeField()

class MyDevice(AbstractDevice):
    pass
