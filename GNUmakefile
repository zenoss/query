##############################################################################
#
# Copyright (C) Zenoss, Inc. 2013, all rights reserved.
#
# This content is made available according to terms specified in
# License.zenoss under the directory where your Zenoss product is installed.
#
##############################################################################


PROJECT=$(PWD)

PROJECT_NAME=central-query

JAVADIR=$(PROJECT)

TARGETDIR=$(JAVADIR)/target

INSTALLDIR ?= $(PROJECT)/install

SUPERVISOR_CONF = $(PROJECT_NAME)_supervisor.conf

SUPERVISORD_DIR = $(INSTALLDIR)/etc/supervisor

default: build

clean:
	cd $(JAVADIR) && mvn clean

build-java:
	cd $(JAVADIR) && mvn package

build: build-java

install:
	cd $(JAVADIR) && mvn clean package -P assemble
	mkdir -p $(INSTALLDIR)/log && cd $(INSTALLDIR) && tar -xvf $(TARGETDIR)/*tar.gz
	mkdir -p $(SUPERVISORD_DIR)
	ln -s ../$(PROJECT_NAME)/$(SUPERVISOR_CONF) $(SUPERVISORD_DIR)/$(SUPERVISOR_CONF)
